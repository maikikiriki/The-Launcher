package com.galaxyrio.gracelauncher.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.UUID

/**
 * Verified, private export snapshots. NOT a crash-safe restore implementation:
 * data across Room, preferences and files is not captured in one global transaction.
 * Never show a Restore button for these points until durable rollback is integrated.
 */
internal object AcronixRecoveryPoints {
    data class Point(val name: String, val createdAt: Long, val bytes: Long)

    const val MAX_POINTS = 3
    private val mutex = Mutex()
    private val fileName = Regex("^point-[0-9]{13}-[0-9a-f]{32}\\.zip$")

    private fun root(context: Context): File =
        File(context.noBackupFilesDir, "acronix_recovery_points")

    private fun available(dir: File): List<Point> =
        dir.listFiles().orEmpty().asSequence()
            .filter { it.isFile && !Files.isSymbolicLink(it.toPath()) && fileName.matches(it.name) }
            .map { Point(it.name, it.lastModified(), it.length()) }
            .sortedWith(compareByDescending<Point> { it.createdAt }.thenByDescending { it.name })
            .toList()

    suspend fun list(context: Context): List<Point> = withContext(Dispatchers.IO) {
        mutex.lock()
        try { available(root(context)) } finally { mutex.unlock() }
    }

    suspend fun create(context: Context): Point = withContext(Dispatchers.IO) {
        mutex.lock()
        try {
            val dir = root(context)
            check(dir.isDirectory || dir.mkdirs()) { "Cannot create private recovery directory" }
            val pending = File.createTempFile(".pending-", ".zip", dir)
            try {
                // export() also inspects the actual bytes written to the destination.
                // A failed export leaves all previously verified points untouched.
                AcronixBackup.export(context, Uri.fromFile(pending))
                FileOutputStream(pending, true).use { it.fd.sync() }
                val name = "point-${System.currentTimeMillis()}-${UUID.randomUUID().toString().replace("-", "")}.zip"
                val point = File(dir, name)
                Files.move(pending.toPath(), point.toPath(), StandardCopyOption.ATOMIC_MOVE)
                // Verify the published file before touching any older point.
                try {
                    AcronixBackup.inspect(context, Uri.fromFile(point))
                } catch (error: Exception) {
                    point.delete()
                    throw error
                }
                // Best-effort directory durability. A failure on a vendor filesystem
                // must not remove the newly verified snapshot or old snapshots.
                runCatching {
                    FileChannel.open(dir.toPath(), StandardOpenOption.READ).use { it.force(true) }
                }
                // Prune only after a new point is fully published and verified.
                available(dir).drop(MAX_POINTS).forEach { older ->
                    File(dir, older.name).delete()
                }
                Point(name, point.lastModified(), point.length())
            } finally {
                pending.delete()
            }
        } finally {
            mutex.unlock()
        }
    }

    suspend fun verify(context: Context, point: Point): Boolean = withContext(Dispatchers.IO) {
        mutex.lock()
        try {
            val dir = root(context)
            val file = File(dir, point.name)
            if (!fileName.matches(point.name) || !file.isFile ||
                Files.isSymbolicLink(file.toPath()) || file.length() <= 0L) return@withContext false
            try {
                AcronixBackup.inspect(context, Uri.fromFile(file))
                true
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                false
            }
        } finally {
            mutex.unlock()
        }
    }
}
