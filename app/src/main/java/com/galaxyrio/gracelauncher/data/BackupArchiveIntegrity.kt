package com.galaxyrio.gracelauncher.data

import java.io.InputStream
import java.io.File
import java.util.zip.ZipFile
import java.security.MessageDigest
import java.util.zip.ZipInputStream

/** Verifies a previously parsed manifest against an archive without extracting entries.
 * Hashes detect corruption, not an attacker able to replace both manifest and payload.
 * This component does not authorize restoration or establish snapshot consistency.
 */
internal object BackupArchiveIntegrity {
    data class Entry(val size: Long, val sha256: String)

    /** ZipInputStream alone does not require an intact central directory. */
    fun verifyFile(file: File, expected: Map<String, Entry>, maxFile: Long, maxTotal: Long, maxEntries: Int) {
        require(expected.size <= maxEntries)
        ZipFile(file).use { zip ->
            val names = mutableSetOf<String>()
            val entries = zip.entries()
            var count = 0
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                require(++count <= maxEntries && !entry.isDirectory && names.add(entry.name))
                require(entry.name in expected && entry.size == expected.getValue(entry.name).size)
            }
            require(names == expected.keys) { "Incomplete central directory" }
        }
        file.inputStream().use { verify(it, expected, maxFile, maxTotal, maxEntries) }
    }

    fun verify(input: InputStream, expected: Map<String, Entry>, maxFile: Long, maxTotal: Long, maxEntries: Int) {
        require(maxFile >= 0 && maxTotal >= 0 && maxEntries > 0)
        require(expected.size <= maxEntries)
        require(expected.all { (name, entry) -> safePath(name) && entry.size in 0..maxFile &&
            entry.sha256.matches(Regex("[0-9a-f]{64}")) })
        val seen = mutableSetOf<String>()
        var total = 0L
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                require(!entry.isDirectory && safePath(entry.name) && seen.add(entry.name)) { "Invalid archive entry" }
                require(seen.size <= maxEntries) { "Too many entries" }
                val declared = requireNotNull(expected[entry.name]) { "Unexpected entry" }
                val digest = MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(8192)
                var size = 0L
                while (true) {
                    val count = zip.read(buffer)
                    if (count < 0) break
                    require(count.toLong() <= maxFile - size && count.toLong() <= maxTotal - total) { "Archive exceeds limits" }
                    size += count
                    total += count
                    digest.update(buffer, 0, count)
                }
                val hash = digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
                require(size == declared.size && hash == declared.sha256) { "Entry integrity failure" }
                zip.closeEntry()
            }
        }
        require(seen == expected.keys) { "Missing archive entry" }
    }

    private fun safePath(path: String): Boolean = path.length in 1..512 &&
        !path.startsWith('/') && !path.contains('\\') && !path.contains('\u0000') &&
        path.split('/').all { it.isNotEmpty() && it != "." && it != ".." }
}
