package com.galaxyrio.gracelauncher.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * User-controlled on-device backup. The archive may contain personal preferences,
 * contact shortcuts and folder names. It is NEVER sent to a server by this class.
 *
 * This format is only for Acronix itself, not direct access to Grace's sandbox.
 */
object AcronixBackup {
    data class Inspection(val payloadCount: Int)

    /** Checks v2 archive structure and payload integrity, never applies configuration. */
    suspend fun inspect(context: Context, uri: Uri): Inspection = withContext(Dispatchers.IO) {
        val file = File.createTempFile("acronix_inspect_", ".zip", context.cacheDir)
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { copyLimited(input, it, MAX_BYTES + 4L * 1024 * 1024) }
            } ?: error("Unable to read backup")
            val expected = linkedMapOf<String, BackupArchiveIntegrity.Entry>()
            java.util.zip.ZipFile(file).use { zip ->
                val entry = requireNotNull(zip.getEntry("manifest.json")) { "Missing manifest" }
                val buffer = java.io.ByteArrayOutputStream()
                zip.getInputStream(entry).use { copyLimited(it, buffer, 1024L * 1024) }
                val bytes = buffer.toByteArray()
                val manifestText = String(bytes, Charsets.UTF_8)
                BackupJsonLimits.checkNesting(manifestText)
                val manifest = JSONObject(manifestText)
                require(manifest.getString("format") == "acronix-backup" && manifest.getInt("version") == 2 &&
                    manifest.getInt("dbVersion") == 34) { "Unsupported backup version" }
                val entries = manifest.getJSONObject("entries")
                require(entries.length() in 1..4095 && entries.has(DATA_ENTRY))
                for (name in entries.keys()) {
                    require(name == DATA_ENTRY || assetDirs.any { directory ->
                        name.startsWith("files/$directory/") && safeFileName(name.substringAfterLast('/')) &&
                            name == "files/$directory/${name.substringAfterLast('/')}"
                    }) { "Unexpected backup resource" }
                    val item = entries.getJSONObject(name)
                    expected[name] = BackupArchiveIntegrity.Entry(item.getLong("size"), item.getString("sha256"))
                }
                expected["manifest.json"] = BackupArchiveIntegrity.Entry(bytes.size.toLong(), sha256(bytes))
            }
            BackupArchiveIntegrity.verifyFile(file, expected, MAX_FILE, MAX_BYTES, 4096)
            Inspection(expected.size - 1)
        } finally { file.delete() }
    }
    private const val VERSION = 1
    private const val MAX_BYTES = 128L * 1024L * 1024L
    private const val MAX_FILE = 24L * 1024L * 1024L
    private val tables = listOf("launcher_settings", "hidden_apps", "folders", "item_icons",
        "saved_shortcuts", "app_popups")
    private val assetDirs = listOf("clock_fonts", "item_icons")
    private const val DATA_ENTRY = "data.json"

    /**
     * Build and verify the archive privately before touching the SAF destination.
     * If snapshot generation fails (for example, disk full or a changed asset),
     * the user's existing document is not truncated. The provider itself is not
     * transactional: a failed write can still leave a partial external document.
     * Only report success after reading the written document back and validating it.
     */
    suspend fun export(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        val staged = File.createTempFile("acronix_export_staged_", ".zip", context.cacheDir)
        try {
            staged.outputStream().use { exportTo(context, it) }
            context.contentResolver.openOutputStream(uri, "wt")?.use { destination ->
                staged.inputStream().use { source -> source.copyTo(destination) }
            } ?: error("Unable to open destination")
            inspect(context, uri)
        } finally {
            staged.delete()
        }
    }

    suspend fun shareFile(context: Context): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "acronix_shared").apply { mkdirs() }
        dir.listFiles().orEmpty().forEach { it.delete() }
        val output = File(dir, "Acronix-backup.zip")
        try {
            output.outputStream().use { exportTo(context, it) }
            inspect(context, Uri.fromFile(output))
            output
        } catch (error: Exception) {
            output.delete()
            throw error
        }
    }

    private fun exportTo(context: Context, output: OutputStream) {
        // Database rows share a transaction. Preferences and assets are separate stores;
        // this is not yet a globally atomic snapshot and restoration remains disabled.
        val db = LauncherDatabase.getInstance(context).openHelper.writableDatabase
        db.beginTransaction()
        val rows = try {
            exportTables(db).also { db.setTransactionSuccessful() }
        } finally { db.endTransaction() }
        val data = JSONObject().put("format", "acronix-backup").put("version", 2)
            .put("dbVersion", 34).put("tables", rows)
            .put("preferences", exportPreferences(context))
        val archive = File.createTempFile("acronix_export_", ".zip", context.cacheDir)
        try {
            val expected = linkedMapOf<String, BackupArchiveIntegrity.Entry>()
            var total = 0L
            ZipOutputStream(archive.outputStream().buffered()).use { zip ->
                fun add(name: String, source: InputStream) {
                    require(expected.size < 4095) { "Too many backup files" }
                    val digest = MessageDigest.getInstance("SHA-256")
                    var size = 0L
                    zip.putNextEntry(ZipEntry(name))
                    source.use { input ->
                        val buffer = ByteArray(8192)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            size += count; total += count
                            require(size <= MAX_FILE && total <= MAX_BYTES) { "Backup exceeds size limit" }
                            digest.update(buffer, 0, count)
                            zip.write(buffer, 0, count)
                        }
                    }
                    zip.closeEntry()
                    expected[name] = BackupArchiveIntegrity.Entry(size, digest.digest()
                        .joinToString("") { "%02x".format(it.toInt() and 0xff) })
                }
                add(DATA_ENTRY, data.toString().byteInputStream(Charsets.UTF_8))
                for (directory in assetDirs) {
                    File(context.filesDir, directory).listFiles().orEmpty()
                        .filter { it.isFile && safeFileName(it.name) }.forEach { file ->
                            // Fail instead of silently omitting an oversized resource.
                            add("files/$directory/${file.name}", file.inputStream())
                        }
                }
                val entries = JSONObject()
                expected.forEach { (name, entry) -> entries.put(name,
                    JSONObject().put("size", entry.size).put("sha256", entry.sha256)) }
                val manifest = JSONObject().put("format", "acronix-backup").put("version", 2)
                    .put("createdAt", System.currentTimeMillis()).put("android", Build.VERSION.SDK_INT)
                    .put("dbVersion", 34).put("entries", entries)
                add("manifest.json", manifest.toString().byteInputStream(Charsets.UTF_8))
            }
            BackupArchiveIntegrity.verifyFile(archive, expected, MAX_FILE, MAX_BYTES, 4096)
            archive.inputStream().use { it.copyTo(output) }
        } finally {
            archive.delete()
        }
    }

    /** Restoration is disabled until crash recovery and archive integrity are verified. */
    suspend fun restore(context: Context, uri: Uri): Nothing {
        throw IllegalStateException("Backup restoration is temporarily unavailable")
    }

    // Retained for audit only. This implementation is not safe against process death.
    private suspend fun restoreUnverified(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        val staging = File(context.cacheDir, "acronix_restore_${UUID.randomUUID()}")
        check(staging.mkdirs())
        val oldFiles = File(staging, "_rollback")
        oldFiles.mkdirs()
        val staged = File(staging, "files").apply { mkdirs() }
        try {
            var dataBytes: ByteArray? = null
            var manifest: JSONObject? = null
            var total = 0L
            var count = 0
            context.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input.buffered()).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        count++
                        require(count <= 4096 && !entry.isDirectory) { "Invalid archive entries" }
                        val name = entry.name
                        require(name == "manifest.json" || name == DATA_ENTRY ||
                            assetDirs.any { name.startsWith("files/$it/") && safeFileName(name.substringAfterLast('/')) &&
                                name == "files/$it/${name.substringAfterLast('/')}" }) { "Unsafe archive entry" }
                        val maxEntry = if (name == DATA_ENTRY) MAX_FILE else MAX_FILE
                        val sink = if (name.startsWith("files/")) {
                            val target = File(staged, name.removePrefix("files/"))
                            check(target.parentFile?.mkdirs() == true || target.parentFile?.isDirectory == true)
                            target.outputStream()
                        } else java.io.ByteArrayOutputStream()
                        var size = 0L
                        sink.use { out ->
                            val buffer = ByteArray(8192)
                            while (true) {
                                val n = zip.read(buffer)
                                if (n < 0) break
                                size += n; total += n
                                require(size <= maxEntry && total <= MAX_BYTES) { "Backup exceeds size limit" }
                                out.write(buffer, 0, n)
                            }
                        }
                        if (name == DATA_ENTRY || name == "manifest.json") {
                            val bytes = (sink as java.io.ByteArrayOutputStream).toByteArray()
                            if (name == DATA_ENTRY) dataBytes = bytes else manifest = JSONObject(String(bytes, Charsets.UTF_8))
                        }
                        zip.closeEntry()
                    }
                }
            } ?: error("Unable to read backup")
            val bytes = requireNotNull(dataBytes) { "Missing backup data" }
            val meta = requireNotNull(manifest) { "Missing backup manifest" }
            require(meta.optString("format") == "acronix-backup" && meta.optInt("version") == VERSION)
            require(meta.optString("checksum") == sha256(bytes)) { "Backup integrity failure" }
            val data = JSONObject(String(bytes, Charsets.UTF_8))
            require(data.optString("format") == "acronix-backup" && data.optInt("dbVersion") == 34)
            val rows = data.getJSONObject("tables")
            val preferences = data.getJSONObject("preferences")
            val db = LauncherDatabase.getInstance(context).openHelper.writableDatabase
            checkTables(db, rows)
            val originalPrefs = exportPreferences(context)

            // Move file directories to private rollback storage. Never follow entry paths.
            val moved = mutableListOf<String>()
            try {
                for (name in assetDirs) {
                    val old = File(context.filesDir, name)
                    val backup = File(oldFiles, name)
                    if (old.exists()) {
                        check(old.renameTo(backup)) { "Failed to secure existing $name" }
                    }
                    moved += name
                    val replacement = File(staged, name)
                    if (replacement.isDirectory) {
                        check(replacement.renameTo(old)) { "Failed to restore $name" }
                    }
                }

                db.beginTransaction()
                try {
                    for (table in tables) db.execSQL("DELETE FROM $table")
                    for (table in tables) {
                        val array = rows.getJSONArray(table)
                        for (i in 0 until array.length()) {
                            val row = array.getJSONObject(i)
                            val values = ContentValues()
                            val columns = tableColumns(db, table)
                            val keys = row.keys()
                            while (keys.hasNext()) {
                                val key = keys.next()
                                require(key in columns) { "Unexpected database column" }
                                when (val value = row.get(key)) {
                                    JSONObject.NULL -> values.putNull(key)
                                    is Int -> values.put(key, value)
                                    is Long -> values.put(key, value)
                                    is Double -> values.put(key, value)
                                    is String -> values.put(key, value)
                                    else -> error("Invalid database value")
                                }
                            }
                            require(db.insert(table, SQLiteDatabase.CONFLICT_ABORT, values) != -1L)
                        }
                    }
                    restorePreferences(context, preferences)
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            } catch (error: Exception) {
                // Restore private assets and preferences even when database transaction fails.
                for (name in moved.asReversed()) {
                    File(context.filesDir, name).deleteRecursively()
                    val backup = File(oldFiles, name)
                    if (backup.exists()) check(backup.renameTo(File(context.filesDir, name)))
                }
                restorePreferences(context, originalPrefs)
                throw error
            }
            // The editor and open screens should be reopened so in-memory caches reload.
            // Widget bindings are system-owned and may require a new bind approval.
        } finally {
            staging.deleteRecursively()
        }
    }

    private fun exportTables(db: SupportSQLiteDatabase): JSONObject = JSONObject().apply {
        for (table in tables) {
            val array = JSONArray()
            db.query("SELECT * FROM $table").use { cursor ->
                while (cursor.moveToNext()) {
                    val row = JSONObject()
                    for (index in 0 until cursor.columnCount) {
                        val key = cursor.getColumnName(index)
                        when (cursor.getType(index)) {
                            Cursor.FIELD_TYPE_NULL -> row.put(key, JSONObject.NULL)
                            Cursor.FIELD_TYPE_INTEGER -> row.put(key, cursor.getLong(index))
                            Cursor.FIELD_TYPE_FLOAT -> row.put(key, cursor.getDouble(index))
                            Cursor.FIELD_TYPE_STRING -> row.put(key, cursor.getString(index))
                            else -> error("Unsupported database field")
                        }
                    }
                    array.put(row)
                    require(array.length() <= 100_000)
                }
            }
            put(table, array)
        }
    }

    private fun checkTables(db: SupportSQLiteDatabase, data: JSONObject) {
        for (table in tables) {
            val expected = tableColumns(db, table)
            val rows = data.getJSONArray(table)
            require(rows.length() <= 100_000)
            for (i in 0 until rows.length()) {
                val row = rows.getJSONObject(i)
                val keys = row.keys()
                while (keys.hasNext()) require(keys.next() in expected)
            }
        }
    }

    private fun tableColumns(db: SupportSQLiteDatabase, table: String): Set<String> {
        require(table in tables)
        val result = mutableSetOf<String>()
        db.query("PRAGMA table_info($table)").use { cursor ->
            val index = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) result += cursor.getString(index)
        }
        require(result.isNotEmpty())
        return result
    }

    private fun exportPreferences(context: Context): JSONObject = JSONObject().apply {
        BackupPreferencePolicy.files.forEach { name ->
            val values = JSONObject()
            for ((key, value) in context.getSharedPreferences(name, Context.MODE_PRIVATE).all) {
                if (!BackupPreferencePolicy.allows(name, key)) continue
                val item = JSONObject()
                when (value) {
                    is String -> item.put("type", "string").put("value", value)
                    is Boolean -> item.put("type", "boolean").put("value", value)
                    is Int -> item.put("type", "int").put("value", value)
                    is Long -> item.put("type", "long").put("value", value)
                    is Float -> item.put("type", "float").put("value", value.toDouble())
                    is Set<*> -> item.put("type", "set").put("value", JSONArray(value.filterIsInstance<String>()))
                    else -> continue
                }
                values.put(key, item)
            }
            put(name, values)
        }
    }

    private fun restorePreferences(context: Context, all: JSONObject) {
        val names = all.keys()
        while (names.hasNext()) {
            val name = names.next()
            require(name.matches(Regex("[A-Za-z0-9_.-]{1,100}")))
            val data = all.getJSONObject(name)
            val editor = context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear()
            val keys = data.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val item = data.getJSONObject(key)
                when (item.getString("type")) {
                    "string" -> editor.putString(key, item.getString("value"))
                    "boolean" -> editor.putBoolean(key, item.getBoolean("value"))
                    "int" -> editor.putInt(key, item.getInt("value"))
                    "long" -> editor.putLong(key, item.getLong("value"))
                    "float" -> editor.putFloat(key, item.getDouble("value").toFloat())
                    "set" -> {
                        val value = item.getJSONArray("value")
                        editor.putStringSet(key, (0 until value.length()).map { value.getString(it) }.toSet())
                    }
                    else -> error("Unexpected preference type")
                }
            }
            check(editor.commit())
        }
    }

    private fun safeFileName(name: String) = name.length in 1..160 &&
        name.matches(Regex("[\\p{L}\\p{N}_. -]+")) && name != "." && name != ".."

    private fun copyLimited(input: InputStream, output: OutputStream, limit: Long) {
        val buffer = ByteArray(8192)
        var total = 0L
        while (true) {
            val count = input.read(buffer)
            if (count < 0) return
            total += count
            require(total <= limit) { "File too large" }
            output.write(buffer, 0, count)
        }
    }

    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
