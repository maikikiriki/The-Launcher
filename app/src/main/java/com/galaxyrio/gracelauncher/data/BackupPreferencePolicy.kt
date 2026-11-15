package com.galaxyrio.gracelauncher.data

/** Explicitly reviewed launcher-owned preferences only; new keys require review. */
internal object BackupPreferencePolicy {
    val files = setOf("grace_launcher_preferences", "launcher_appearance", "acronix_fold_layouts")

    fun allows(file: String, key: String): Boolean = when (file) {
        "grace_launcher_preferences" -> key == "favorite_components" || key == "favorite_component_order"
        "launcher_appearance" -> key == "text_mode" || key == "themed_icons" ||
            key.startsWith("label:") || key.startsWith("category:")
        "acronix_fold_layouts" -> key in setOf("layouts", "surface", "fold_signal_inner")
        else -> false
    }
}
