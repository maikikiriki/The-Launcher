package com.galaxyrio.gracelauncher

import com.galaxyrio.gracelauncher.data.*
import org.junit.Assert.*
import org.junit.Test

class FoldLayoutsTest {
    @Test fun sharedDefaultUsesExistingLayoutOnBothSurfaces() {
        val existing = HomeLayout(widgetId = 41)
        val state = FoldLayouts(shared = existing)
        assertEquals(existing, state.layout(FoldSurface.Outer))
        assertEquals(existing, state.layout(FoldSurface.Inner))
    }
    @Test fun independentEditsAndModeSwitchPreserveOtherSurface() {
        val state = FoldLayouts(independent = true, shared = HomeLayout(widgetId = 41))
            .edit(FoldSurface.Outer, HomeLayout(widgetId = 42))
        assertEquals(41, state.layout(FoldSurface.Inner).widgetId)
        assertEquals(42, state.sharedMode().copy(independent = true).layout(FoldSurface.Outer).widgetId)
    }
    @Test fun copyingRetainsWidgetIdentityAndOriginalState() {
        val original = FoldLayouts(independent = true, outer = HomeLayout(widgetId = 42))
        val copied = original.copyTo(FoldSurface.Outer, FoldSurface.Inner)
        assertEquals(42, copied.layout(FoldSurface.Inner).widgetId)
        assertNull(original.inner)
    }
}
