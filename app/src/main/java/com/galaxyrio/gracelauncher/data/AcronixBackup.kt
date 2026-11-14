package com.galaxyrio.gracelauncher.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Base64
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Portable backup of Acronix-owned Room tables, SharedPreferences and filesDir.
 *
 * Does not read another application's sandbox, copy system widget bindings or
 * capture notification/contact content. Uses SAF for user-controlled storage.
 */
internal object AcronixBackup {
    private const val FORMAT = 1
    private const val SCHEMA_VERSION = 34
    private const val MAX_JSON_BYTES = 24L * 1024 * 1024
    private const val MAX_TOTAL_BYTES = 160L * 1024 * 1024
    private val tables = listOf(
        "launcher_settings", "hidden_apps", "folders", "item_icons", "saved_shortcuts", "app_popups",
    )

    suspend fun export(context: Context, output: OutputStream) = withContext(Dispatchers.IO) {
        val payload = readSnapshot(context)
        var total = payload.size.toLong()
        check(total <= MAX_JSON_BYTES) { "La configuración supera el límite permitido" }
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("acronix.json"))
            zip.write(payload)
            zip.closeEntry()
            val root = context.filesDir.canonicalFile
            root.walkTopDown().filter(File::isFile).forEach { file ->
                val canonical = file.canonicalFile
                if (!canonical.path.startsWith(root.path + File.separator)) return@forEach
                val relative = canonical.relativeTo(root).invariantSeparatorsPath
                check(relative.split('/').none { it == ".." || it == "." || it.isBlank() })
                total += file.length()
                check(total <= MAX_TOTAL_BYTES) { "La copia supera el límite de 160 MB" }
                zip.putNextEntry(ZipEntry("files/" + relative))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    private suspend fun readSnapshot(context: Context): ByteArray {
        val db = LauncherDatabase.getInstance(context)
        val roomData = JSONObject()
        db.withTransaction {
            val sql = db.openHelper.writableDatabase
            for (table in tables) {
                val rows = JSONArray()
                sql.query("SELECT * FROM \"$table\"").use { cursor ->
                    while (cursor.moveToNext()) {
                        val row = JSONObject()
                        for (i in 0 until cursor.columnCount) {
                            val key = cursor.getColumnName(i)
                            val value: Any = when (cursor.getType(i)) {
                                Cursor.FIELD_TYPE_NULL -> JSONObject.NULL
                                Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(i)
                                Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(i)
                                Cursor.FIELD_TYPE_STRING -> cursor.getString(i)
                                Cursor.FIELD_TYPE_BLOB -> JSONObject().put("base64",
                                    Base64.encodeToString(cursor.getBlob(i), Base64.NO_WRAP))
                                else -> JSONObject.NULL
                            }
                            row.put(key, value)
                        }
                        rows.put(row)
                    }
                }
                roomData.put(table, rows)
            }
        }
        val prefs = JSONObject()
        val prefDir = File(context.applicationInfo.dataDir, "shared_prefs")
        prefDir.listFiles()?.filter { it.isFile && it.extension == "xml" }?.forEach { file ->
            val name = file.nameWithoutExtension
            val entries = JSONObject()
            context.getSharedPreferences(name, Context.MODE_PRIVATE).all.forEach { (key, value) ->
                val encoded = when (value) {
                    is String -> JSONObject().put("type", "s").put("value", value)
                    is Boolean -> JSONObject().put("type", "b").put("value", value)
                    is Int -> JSONObject().put("type", "i").put("value", value)
                    is Long -> JSONObject().put("type", "l").put("value", value)
                    is Float -> JSONObject().put("type", "f").put("value", value.toDouble())
                    is Set<*> -> JSONObject().put("type", "ss")
                        .put("value", JSONArray(value.filterIsInstance<String>()))
                    else -> null
                }
                if (encoded != null) entries.put(key, encoded)
            }
            prefs.put(name, entries)
        }
        return JSONObject()
            .put("format", "acronix-settings")
            .put("version", FORMAT)
            .put("roomSchema", SCHEMA_VERSION)
            .put("package", context.packageName)
            .put("tables", roomData)
            .put("preferences", prefs)
            .toString().toByteArray(Charsets.UTF_8)
    }

    suspend fun import(context: Context, input: InputStream) = withContext(Dispatchers.IO) {
        val staging = File(context.cacheDir, "acronix-import-" + System.nanoTime())
        check(staging.mkdirs()) { "No se pudo preparar la importación" }
        try {
            var payload: ByteArray? = null
            var uncompressed = 0L
            ZipInputStream(input).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val name = entry.name
                    check(!name.startsWith('/') && '\\' !in name &&
                        name.split('/').none { it == ".." || it == "." }) { "Ruta no válida en la copia" }
                    if (!entry.isDirectory) {
                        when {
                            name == "acronix.json" -> {
                                check(payload == null) { "Configuración duplicada" }
                                val buffer = ByteArrayOutputStream()
                                val count = longArrayOf(uncompressed)
                                copyBounded(zip, buffer, count, MAX_JSON_BYTES)
                                uncompressed = count[0]
                                payload = buffer.toByteArray()
                            }
                            name.startsWith("files/") -> {
                                val relative = name.removePrefix("files/")
                                check(relative.isNotBlank()) { "Ruta vacía" }
                                val file = File(staging, relative)
                                val canonical = file.canonicalFile
                                check(canonical.path.startsWith(staging.canonicalPath + File.separator)) {
                                    "Ruta de archivo no válida"
                                }
                                check(canonical.parentFile?.mkdirs() == true || canonical.parentFile?.isDirectory == true)
                                canonical.outputStream().use { out ->
                                    val count = longArrayOf(uncompressed)
                                    copyBounded(zip, out, count, MAX_TOTAL_BYTES)
                                    uncompressed = count[0]
                                }
                            }
                            else -> error("Archivo de copia no reconocido")
                        }
                    }
                    zip.closeEntry()
                }
            }
            val jsonBytes = payload ?: error("Falta la configuración de Acronix")
            check(jsonBytes.size.toLong() <= MAX_JSON_BYTES)
            val document = JSONObject(String(jsonBytes, Charsets.UTF_8))
            check(document.optString("format") == "acronix-settings" &&
                document.optInt("version") == FORMAT &&
                document.optInt("roomSchema") == SCHEMA_VERSION &&
                document.optString("package") == context.packageName) {
                "La copia pertenece a otra versión o aplicación"
            }
            val storedTables = document.getJSONObject("tables")
            for (table in tables) check(storedTables.has(table)) { "Falta la tabla $table" }
            val prefs = document.getJSONObject("preferences")

            // Validate table columns and value types before deleting anything.
            val database = LauncherDatabase.getInstance(context)
            val permitted = tables.associateWith { table ->
                val columns = mutableSetOf<String>()
                database.openHelper.writableDatabase.query("PRAGMA table_info(\"$table\")").use { cursor ->
                    val nameIndex = cursor.getColumnIndexOrThrow("name")
                    while (cursor.moveToNext()) columns.add(cursor.getString(nameIndex))
                }
                columns
            }
            for (table in tables) {
                val rows = storedTables.getJSONArray(table)
                check(rows.length() <= 50000) { "Demasiados elementos en $table" }
                for (index in 0 until rows.length()) {
                    val record = rows.getJSONObject(index)
                    val keys = record.keys()
                    while (keys.hasNext()) {
                        val column = keys.next()
                        check(column in permitted.getValue(table)) { "Columna no admitida: $column" }
                        val value = record.get(column)
                        check(value === JSONObject.NULL || value is String || value is Number ||
                            value is Boolean || (value is JSONObject && value.has("base64"))) {
                            "Valor no admitido"
                        }
                    }
                }
            }

            database.withTransaction {
                val sql = database.openHelper.writableDatabase
                for (table in tables) {
                    sql.delete(table, null, null)
                    val records = storedTables.getJSONArray(table)
                    for (index in 0 until records.length()) {
                        val source = records.getJSONObject(index)
                        val values = ContentValues()
                        val keys = source.keys()
                        while (keys.hasNext()) {
                            val column = keys.next()
                            when (val value = source.get(column)) {
                                JSONObject.NULL -> values.putNull(column)
                                is String -> values.put(column, value)
                                is Boolean -> values.put(column, if (value) 1 else 0)
                                is Int -> values.put(column, value)
                                is Long -> values.put(column, value)
                                is Float -> values.put(column, value)
                                is Double -> values.put(column, value)
                                is Number -> values.put(column, value.toDouble())
                                is JSONObject -> values.put(column,
                                    Base64.decode(value.getString("base64"), Base64.DEFAULT))
                            }
                        }
                        check(sql.insert(table, SQLiteDatabase.CONFLICT_NONE, values) != -1L) {
                            "No se pudo restaurar $table"
                        }
                    }
                }
            }

            // SharedPreferences are not Room-backed. Preserve type information.
            val names = prefs.keys()
            while (names.hasNext()) {
                val name = names.next()
                check(name.isNotBlank() && '/' !in name && '\\' !in name && ".." !in name)
                val values = prefs.getJSONObject(name)
                val editor = context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear()
                val keys = values.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val cell = values.getJSONObject(key)
                    when (cell.getString("type")) {
                        "s" -> editor.putString(key, cell.getString("value"))
                        "b" -> editor.putBoolean(key, cell.getBoolean("value"))
                        "i" -> editor.putInt(key, cell.getInt("value"))
                        "l" -> editor.putLong(key, cell.getLong("value"))
                        "f" -> editor.putFloat(key, cell.getDouble("value").toFloat())
                        "ss" -> {
                            val array = cell.getJSONArray("value")
                            editor.putStringSet(key, (0 until array.length()).map {
                                array.getString(it)
                            }.toSet())
                        }
                        else -> error("Preferencia no admitida")
                    }
                }
                check(editor.commit()) { "No se pudieron escribir las preferencias" }
            }
            val target = context.filesDir.canonicalFile
            staging.walkTopDown().filter(File::isFile).forEach { file ->
                val relative = file.relativeTo(staging).invariantSeparatorsPath
                val destination = File(target, relative).canonicalFile
                check(destination.path.startsWith(target.path + File.separator))
                destination.parentFile?.mkdirs()
                file.copyTo(destination, overwrite = true)
            }
        } finally {
            staging.deleteRecursively()
        }
    }

    private fun copyBounded(input: InputStream, output: OutputStream, total: LongArray, max: Long) {
        val bytes = ByteArray(16384)
        while (true) {
            val count = input.read(bytes)
            if (count == -1) break
            total[0] += count
            check(total[0] <= max) { "La copia excede el tamaño de seguridad" }
            output.write(bytes, 0, count)
        }
    }
}
