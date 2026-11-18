package com.galaxyrio.gracelauncher

import androidx.compose.ui.unit.dp
import com.galaxyrio.gracelauncher.ui.components.compactHomeMaximumTop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompactHomeLayoutTest {
    @Test fun landscapeCoverPreservesSpaceForFavorites() {
        val max = compactHomeMaximumTop(300.dp, 28.dp, 780.dp, 340.dp)
        assertEquals(124.dp, max)
        assertTrue(300.dp - max >= 176.dp)
    }

    @Test fun portraitAndTabletKeepExistingAnchors() {
        assertEquals(700.dp, compactHomeMaximumTop(756.dp, 28.dp, 350.dp, 820.dp))
        assertEquals(600.dp, compactHomeMaximumTop(656.dp, 28.dp, 1200.dp, 800.dp))
    }

    @Test fun tinySplitWindowNeverMovesHomeAboveSafeTop() {
        assertEquals(34.dp, compactHomeMaximumTop(120.dp, 34.dp, 550.dp, 190.dp))
    }
}
