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
    @Test fun exportManifestCoversEveryPayload() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = AcronixBackup.shareFile(context)
        try {
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
