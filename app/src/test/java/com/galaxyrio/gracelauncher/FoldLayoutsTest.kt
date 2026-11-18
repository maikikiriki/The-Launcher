package com.galaxyrio.gracelauncher

import com.galaxyrio.gracelauncher.data.*
import org.junit.Assert.*
import org.junit.Test

class FoldLayoutsTest {
    @Test fun manualSelectionWinsAndAbsentFoldDoesNotGuessOuter() {
        assertEquals(FoldSurface.Outer, foldSurfaceSelection("Outer", true, true))
        assertNull(foldSurfaceSelection(null, false, true))
        assertNull(foldSurfaceSelection(null, true, false))
        assertEquals(FoldSurface.Inner, foldSurfaceSelection(null, true, true))
    }

    @Test fun renderedPlacementNeverReplacesWidgetIdentityOrFavorites() {
        val base = HomeLayout(widgetId = 41, homeOrder = listOf("app:a", "widget:41"))
        val state = FoldLayouts(independent = true, outer = HomeLayout(widgetId = 99, topOffsetDp = 32f))
        val rendered = state.presentation(base, FoldSurface.Outer)
        assertEquals(41, rendered.widgetId)
        assertEquals(base.homeOrder, rendered.homeOrder)
        assertEquals(32f, rendered.topOffsetDp)
        assertEquals(base, state.sharedMode().presentation(base, FoldSurface.Outer))
    }
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
