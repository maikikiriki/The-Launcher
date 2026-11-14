package com.galaxyrio.gracelauncher.data

import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupArchiveIntegrityTest {
    private fun archive(vararg files: Pair<String, String>): ByteArray = ByteArrayOutputStream().also { out ->
        ZipOutputStream(out).use { zip -> files.forEach { (name, content) ->
            zip.putNextEntry(ZipEntry(name)); zip.write(content.toByteArray()); zip.closeEntry()
        } }
    }.toByteArray()

    private fun expected(content: String): BackupArchiveIntegrity.Entry {
        val bytes = content.toByteArray()
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }
        return BackupArchiveIntegrity.Entry(bytes.size.toLong(), hash)
    }

    private fun verify(zip: ByteArray, entries: Map<String, BackupArchiveIntegrity.Entry>, file: Long = 100, total: Long = 100) =
        BackupArchiveIntegrity.verify(zip.inputStream(), entries, file, total, 8)

    @Test fun verifiesAllExpectedFiles() {
        verify(archive("data.json" to "data", "files/icon" to "icon"),
            mapOf("data.json" to expected("data"), "files/icon" to expected("icon")))
    }

    @Test fun rejectsChangedMissingAndUnexpectedEntries() {
        assertThrows(IllegalArgumentException::class.java) { verify(archive("data" to "changed"), mapOf("data" to expected("original"))) }
        assertThrows(IllegalArgumentException::class.java) { verify(archive("data" to "ok"), mapOf("data" to expected("ok"), "missing" to expected(""))) }
        assertThrows(IllegalArgumentException::class.java) { verify(archive("extra" to "x"), emptyMap()) }
    }

    @Test fun rejectsTraversalAndOversizedPayload() {
        assertThrows(IllegalArgumentException::class.java) { verify(archive("../data" to "x"), mapOf("../data" to expected("x"))) }
        assertThrows(IllegalArgumentException::class.java) { verify(archive("data" to "12345"), mapOf("data" to expected("12345")), total = 4) }
    }
}
