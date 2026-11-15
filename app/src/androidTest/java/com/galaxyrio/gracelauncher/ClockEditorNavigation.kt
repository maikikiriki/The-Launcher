package com.galaxyrio.gracelauncher

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.test.platform.app.InstrumentationRegistry

/** Navigate actual editor groups before asking a lazy list to compose the target. */
internal fun clockEditorControl(tag: String, find: (String) -> SemanticsNodeInteraction,
    settle: () -> Unit): SemanticsNodeInteraction {
    if (!tag.startsWith("clock_color:")) {
        find("clock_tab:Style").performClick()
        settle()
        val group = when {
            tag.startsWith("clock_region:") -> tag.substringAfter("clock_region:").substringBefore(':')
            tag.startsWith("clock_secondary_") -> "Secondary"
            tag.startsWith("clock_tertiary_") -> "Tertiary"
            tag.startsWith("clock_agenda_") -> "Agenda"
            tag.startsWith("clock_task_") -> "Task"
            tag.startsWith("clock_rotate_") || tag.startsWith("clock_region_order") ||
                tag.startsWith("clock_region_spacing") || tag.startsWith("clock_bottom_padding") -> "Layout"
            else -> "Primary"
        }
        val heading = if (group == "Layout") "clock_layout_heading" else "clock_region_heading:$group"
        find("clock_style_controls").performScrollToNode(hasTestTag(heading))
        val collapsed = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.widget_group_collapsed)
        if (find(heading).fetchSemanticsNode().config[SemanticsProperties.StateDescription] == collapsed) {
            find(heading).performClick()
            settle()
        }
    }
    find("clock_style_controls").performScrollToNode(hasTestTag(tag))
    return find(tag)
}
