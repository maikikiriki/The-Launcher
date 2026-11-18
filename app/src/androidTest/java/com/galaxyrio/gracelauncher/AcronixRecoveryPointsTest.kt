package com.galaxyrio.gracelauncher

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.galaxyrio.gracelauncher.data.AcronixRecoveryPoints
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AcronixRecoveryPointsTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun retainsThreeValidatedPointsAndNeverTouchesTheRealPrivateDirectory() = runBlocking {
        // Only a disposable test directory is used, never the actual private points.
        val directory = File(context.cacheDir, "recovery-test-${UUID.randomUUID()}")
        try {
            val first = AcronixRecoveryPoints.create(context, directory)
            assertTrue(AcronixRecoveryPoints.verify(context, first, directory))
            val points = (1..3).map { AcronixRecoveryPoints.create(context, directory) }
            val retained = AcronixRecoveryPoints.list(context, directory)
            assertEquals(3, retained.size)
            assertTrue(retained.none { it.name == first.name })
            assertEquals(points.map { it.name }.toSet(), retained.map { it.name }.toSet())
            retained.forEach { point ->
                assertTrue(point.bytes > 0L)
                assertTrue(AcronixRecoveryPoints.verify(context, point, directory))
            }
            val broken = retained.first()
            File(directory, broken.name).writeBytes(byteArrayOf(1, 2, 3, 4))
            assertFalse(AcronixRecoveryPoints.verify(context, broken, directory))
            assertEquals(3, AcronixRecoveryPoints.list(context, directory).size)
        } finally { directory.deleteRecursively() }
    }
}
