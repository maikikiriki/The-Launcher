package com.galaxyrio.gracelauncher.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupPreferencePolicyTest {
    @Test fun foldLayoutExportsExcludeUndoHistory() {
        assertTrue(BackupPreferencePolicy.allows("acronix_fold_layouts", "layouts"))
        assertTrue(BackupPreferencePolicy.allows("acronix_fold_layouts", "surface"))
        assertFalse(BackupPreferencePolicy.allows("acronix_fold_layouts", "previous_layouts"))
    }

    @Test fun includesKnownLauncherPreferences() {
        assertTrue(BackupPreferencePolicy.allows("grace_launcher_preferences", "favorite_component_order"))
        assertTrue(BackupPreferencePolicy.allows("launcher_appearance", "text_mode"))
        assertTrue(BackupPreferencePolicy.allows("launcher_appearance", "label:com.example/.Main"))
    }

    @Test fun excludesUnknownPreferencesAndFiles() {
        assertFalse(BackupPreferencePolicy.allows("grace_launcher_preferences", "auth_token"))
        assertFalse(BackupPreferencePolicy.allows("launcher_appearance", "password"))
        assertFalse(BackupPreferencePolicy.allows("third_party", "text_mode"))
    }
}
