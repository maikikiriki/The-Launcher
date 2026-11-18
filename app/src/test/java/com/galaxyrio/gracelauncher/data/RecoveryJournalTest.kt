package com.galaxyrio.gracelauncher.data

import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test

class RecoveryJournalTest {
    private fun fixture(test: (java.io.File) -> Unit) {
        val root = Files.createTempDirectory("recovery-test").toFile()
        try { test(root) } finally { root.deleteRecursively() }
    }

    @Test fun restartRollsBackAndRepeatedRecoveryIsSafe() = fixture { root ->
        RecoveryJournal(root).begin("snapshot_1")
        var calls = 0
        assertTrue(RecoveryJournal(root).recover { assertEquals("snapshot_1", it); calls++ })
        assertFalse(RecoveryJournal(root).recover { calls++ })
        assertEquals(1, calls)
    }

    @Test fun failedRollbackRemainsPendingAcrossRestart() = fixture { root ->
        RecoveryJournal(root).begin("snapshot_1")
        assertThrows(IllegalStateException::class.java) {
            RecoveryJournal(root).recover { error("Injected failure during rollback") }
        }
        assertTrue(RecoveryJournal(root).recover { assertEquals("snapshot_1", it) })
    }

    @Test fun completedOperationIsNotRolledBack() = fixture { root ->
        val journal = RecoveryJournal(root)
        journal.begin("snapshot_1")
        journal.commit()
        assertFalse(RecoveryJournal(root).recover { fail("Committed operation") })
    }

    @Test fun secondOperationCannotOverwritePendingRecovery() = fixture { root ->
        RecoveryJournal(root).begin("original")
        assertThrows(IllegalStateException::class.java) { RecoveryJournal(root).begin("other") }
        RecoveryJournal(root).recover { assertEquals("original", it) }
    }

    @Test fun corruptJournalIsRetainedWithoutInvokingRollback() = fixture { root ->
        val file = java.io.File(root, "recovery.journal").apply { writeText("broken") }
        assertThrows(IllegalArgumentException::class.java) {
            RecoveryJournal(root).recover { fail("Unvalidated snapshot") }
        }
        assertTrue(file.exists())
    }
}
