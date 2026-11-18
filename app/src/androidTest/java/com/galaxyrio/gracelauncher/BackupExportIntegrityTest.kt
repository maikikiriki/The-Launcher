package com.galaxyrio.gracelauncher

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.galaxyrio.gracelauncher.data.AcronixBackup
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.security.MessageDigest
import java.util.zip.ZipFile

@RunWith(AndroidJUnit4::class)
class BackupExportIntegrityTest {
    @Test fun maliciouslyNestedManifestIsRejectedWithoutApplyingData() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = java.io.File.createTempFile("malformed-backup-test", ".zip", context.cacheDir)
        try {
            java.util.zip.ZipOutputStream(file.outputStream()).use { zip ->
                zip.putNextEntry(java.util.zip.ZipEntry("manifest.json"))
                zip.write(("[".repeat(17) + "]".repeat(17)).toByteArray())
                zip.closeEntry()
            }
            org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
                runBlocking { AcronixBackup.inspect(context, android.net.Uri.fromFile(file)) }
            }
        } finally { file.delete() }
    }

    @Test fun safExportTruncatesOldDocumentAndVerifiesWrittenBytes() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = java.io.File.createTempFile("acronix-export-destination", ".zip", context.cacheDir)
        try {
            // A pre-existing SAF document must not retain trailing bytes from a longer file.
            file.writeBytes(ByteArray(256 * 1024) { 0x55.toByte() })
            AcronixBackup.export(context, android.net.Uri.fromFile(file))
            assertTrue(AcronixBackup.inspect(context, android.net.Uri.fromFile(file)).payloadCount >= 1)
            // A partial document must never be reported as a verified export.
            val bytes = file.readBytes()
            file.writeBytes(bytes.copyOf(bytes.size - 22))
            assertThrows(Exception::class.java) {
                runBlocking { AcronixBackup.inspect(context, android.net.Uri.fromFile(file)) }
            }
        } finally { file.delete() }
    }

    @Test fun exportManifestCoversEveryPayload() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = AcronixBackup.shareFile(context)
        try {
            assertTrue(AcronixBackup.inspect(context, android.net.Uri.fromFile(file)).payloadCount >= 1)
            ZipFile(file).use { zip ->
                val manifest = JSONObject(zip.getInputStream(zip.getEntry("manifest.json")).bufferedReader().use { it.readText() })
                assertEquals(2, manifest.getInt("version"))
                val entries = manifest.getJSONObject("entries")
                val names = zip.entries().asSequence().map { it.name }.toSet() - "manifest.json"
                assertEquals(names, entries.keys().asSequence().toSet())
                for (name in names) {
                    val bytes = zip.getInputStream(zip.getEntry(name)).use { it.readBytes() }
                    assertEquals(bytes.size.toLong(), entries.getJSONObject(name).getLong("size"))
                    val hash = MessageDigest.getInstance("SHA-256").digest(bytes)
                        .joinToString("") { "%02x".format(it.toInt() and 0xff) }
                    assertEquals(hash, entries.getJSONObject(name).getString("sha256"))
                }
            }
        } finally { file.delete() }
    }
}
