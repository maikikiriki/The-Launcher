package com.galaxyrio.gracelauncher

import androidx.test.platform.app.InstrumentationRegistry
import com.galaxyrio.gracelauncher.data.*
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class FoldLayoutPersistenceTest {
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
        } finally { context.deleteSharedPreferences(name) }
    }
}
