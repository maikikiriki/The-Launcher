package com.galaxyrio.gracelauncher

import android.content.Context
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.galaxyrio.gracelauncher.data.HomeLayout
import com.galaxyrio.gracelauncher.data.FoldLayoutStore
import com.galaxyrio.gracelauncher.data.FoldSurface
import com.galaxyrio.gracelauncher.ui.settings.AcronixFoldSettings
import com.galaxyrio.gracelauncher.ui.settings.foldPresentation
import com.galaxyrio.gracelauncher.ui.theme.GraceLauncherTheme
import java.util.UUID
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class FoldPlacementUiTest {
    @get:Rule val compose = createComposeRule()

    private fun click(tag: String) {
        compose.waitUntil(10_000) {
            runCatching { !compose.onNodeWithTag(tag).fetchSemanticsNode().config.contains(SemanticsProperties.Disabled) }
                .getOrDefault(false)
        }
        compose.onNodeWithTag(tag).performScrollTo().performClick()
    }

    @Test fun placementFlowsFromSettingsThroughPersistenceToHomeWithoutReplacingWidgets() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "fold-ui-test-${UUID.randomUUID()}"
        val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        val shared = HomeLayout(widgetId = 41, homeOrder = listOf("app:a", "widget:41"))
        var rendered = shared
        try {
            compose.setContent {
                val home = foldPresentation(shared, prefs)
                SideEffect { rendered = home }
                GraceLauncherTheme(dynamicColor = false) {
                    AcronixFoldSettings(shared, onBack = {}, preferences = prefs)
                }
            }
            click("fold_mode")
            click("fold_surface:Outer")
            click("fold_offset:16")
            compose.waitUntil(10_000) { rendered.topOffsetDp == 16f }
            compose.runOnIdle {
                assertEquals(41, rendered.widgetId)
                assertEquals(shared.homeOrder, rendered.homeOrder)
            }
            click("fold_copy")
            // Dialog confirmation is not inside scroll content.
            compose.onNodeWithTag("fold_copy_confirm").performClick()
            compose.waitUntil(10_000) { FoldLayoutStore(prefs).load(shared).inner != null }
            click("fold_surface:Inner")
            compose.waitUntil(10_000) { rendered.topOffsetDp == 16f }
            click("fold_undo")
            compose.waitUntil(10_000) { rendered.topOffsetDp == 0f }
            compose.runOnIdle {
                val saved = FoldLayoutStore(prefs).load(shared)
                assertEquals(16f, saved.layout(FoldSurface.Outer).topOffsetDp)
                assertNull(saved.inner)
            }
            click("fold_mode")
            compose.waitUntil(10_000) { !FoldLayoutStore(prefs).load(shared).independent }
            compose.runOnIdle { assertEquals(shared, rendered) }
        } finally { context.deleteSharedPreferences(name) }
    }
}
