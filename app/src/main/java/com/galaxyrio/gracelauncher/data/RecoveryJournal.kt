package com.galaxyrio.gracelauncher.data

import java.io.File
import java.io.FileOutputStream
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

/** Single-operation journal. Root MUST be a private persistent directory, never cache.
 * Callers must serialize writers, retain a verified snapshot, and provide an idempotent
 * rollback. This component alone cannot make Room/preferences/files transactional.
 * Not wired to restore until snapshot and startup recovery integration are verified.
 */
internal class RecoveryJournal(private val root: File) {
    private val journal get() = File(root, "recovery.journal")

    @Synchronized fun begin(snapshot: String) {
        require(snapshot.matches(Regex("[a-zA-Z0-9_-]{1,80}")))
        check(!journal.exists()) { "Recovery already pending" }
        persist("APPLYING\n$snapshot\n")
    }

    @Synchronized fun commit() {
        val snapshot = read().second
        persist("COMMITTED\n$snapshot\n")
        clear()
    }

    /** Failure or interruption leaves APPLYING intact so rollback is retried. */
    @Synchronized fun recover(rollback: (String) -> Unit): Boolean {
        if (!journal.exists()) return false
        val (phase, snapshot) = read()
        if (phase == "APPLYING") rollback(snapshot)
        clear()
        return phase == "APPLYING"
    }

    private fun read(): Pair<String, String> {
        require(journal.length() in 1..128) { "Invalid recovery journal" }
        val lines = journal.readLines(Charsets.UTF_8)
        require(lines.size == 2 && lines[0] in setOf("APPLYING", "COMMITTED") &&
            lines[1].matches(Regex("[a-zA-Z0-9_-]{1,80}"))) { "Invalid recovery journal" }
        return lines[0] to lines[1]
    }

    private fun persist(value: String) {
        check(root.isDirectory || root.mkdirs())
        val temporary = File(root, "recovery.pending")
        FileOutputStream(temporary).use { output ->
            output.write(value.toByteArray(Charsets.UTF_8))
            output.fd.sync()
        }
        Files.move(temporary.toPath(), journal.toPath(), StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING)
        syncDirectory()
    }

    private fun clear() {
        Files.delete(journal.toPath())
        syncDirectory()
    }

    private fun syncDirectory() {
        FileChannel.open(root.toPath(), StandardOpenOption.READ).use { it.force(true) }
    }
}
