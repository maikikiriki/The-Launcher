package com.galaxyrio.gracelauncher

import androidx.test.platform.app.InstrumentationRegistry
import com.galaxyrio.gracelauncher.data.*
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class FoldLayoutPersistenceTest {
    @Test fun corruptionCannotReplaceLastValidUndoAndRecoveryRemainsAvailable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "fold-corruption-test-${UUID.randomUUID()}"
        val prefs = context.getSharedPreferences(name, 0)
        try {
            val store = FoldLayoutStore(prefs)
            val original = FoldLayouts(independent = true, shared = HomeLayout(widgetId = 41),
                outer = HomeLayout(widgetId = 42, homeOrder = listOf("app:a", "widget:42")))
            store.save(original)
            store.save(original.edit(FoldSurface.Outer, HomeLayout(topOffsetDp = 64f)))
            assertTrue(prefs.edit().putString("layouts", "truncated").commit())
            assertThrows(Exception::class.java) { store.load(HomeLayout()) }
            assertTrue(store.undo())
            assertEquals(original, store.load(HomeLayout()))
            val current = prefs.getString("layouts", null)
            assertTrue(prefs.edit().putString("previous_layouts", "broken").commit())
            assertThrows(Exception::class.java) { store.undo() }
            assertEquals(current, prefs.getString("layouts", null))
        } finally { context.deleteSharedPreferences(name) }
    }

    @Test fun savingAfterCorruptionPreservesThePreviousValidLayout() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "fold-save-test-${UUID.randomUUID()}"
        val prefs = context.getSharedPreferences(name, 0)
        try {
            val store = FoldLayoutStore(prefs)
            val original = FoldLayouts(shared = HomeLayout(widgetId = 41))
            store.save(original)
            store.save(original.copy(independent = true))
            assertTrue(prefs.edit().putString("layouts", "broken").commit())
            store.save(original.copy(outer = HomeLayout(topOffsetDp = 80f)))
            assertTrue(store.undo())
            assertEquals(original, store.load(HomeLayout()))
        } finally { context.deleteSharedPreferences(name) }
    }

    @Test fun independentLayoutsSurviveReloadWithoutWritingLegacyState() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "fold-test-${UUID.randomUUID()}"
        val prefs = context.getSharedPreferences(name, 0)
        try {
            val store = FoldLayoutStore(prefs)
            val legacy = HomeLayout(widgetId = 41)
            assertEquals(legacy, store.load(legacy).shared)
            assertFalse(prefs.contains("layouts"))
            val value = FoldLayouts(independent = true, shared = legacy,
                outer = HomeLayout(widgetId = 42), inner = HomeLayout(widgetId = 43))
            store.save(value)
            assertEquals(value, FoldLayoutStore(prefs).load(legacy))
            store.save(value.edit(FoldSurface.Outer, HomeLayout(topOffsetDp = 64f)))
            assertTrue(FoldLayoutStore(prefs).undo())
            assertEquals(value, FoldLayoutStore(prefs).load(legacy))
        } finally { context.deleteSharedPreferences(name) }
    }
}
