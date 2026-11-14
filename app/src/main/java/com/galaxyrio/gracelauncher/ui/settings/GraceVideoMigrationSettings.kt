package com.galaxyrio.gracelauncher.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.galaxyrio.gracelauncher.R
import com.galaxyrio.gracelauncher.data.FontLibrary
import com.galaxyrio.gracelauncher.data.GraceButtonAction
import com.galaxyrio.gracelauncher.data.GraceButtonGesture
import com.galaxyrio.gracelauncher.data.GraceButtonTarget
import com.galaxyrio.gracelauncher.data.LauncherApp
import com.galaxyrio.gracelauncher.data.LauncherSettings
import com.galaxyrio.gracelauncher.data.PrivateSpaceDisplay
import com.galaxyrio.gracelauncher.data.ThemeMode
import com.galaxyrio.gracelauncher.ui.LauncherActions
import com.galaxyrio.gracelauncher.ui.LauncherUiState
import com.materialkolor.PaletteStyle

/**
 * Opt-in reproduction of *observable* settings from the user's Grace 1.2.4 recording.
 * No access to Grace's private app data: folders, home layout, widget permissions,
 * account details and unavailable third-party apps are deliberately left untouched.
 */
internal fun applyGraceVideoAppearance(current: LauncherSettings, state: LauncherUiState): LauncherSettings {
    val packs = state.iconPacks.firstOrNull { pack ->
        pack.label.contains("horux", true) || pack.label.contains("exported icon pack", true)
    }
    fun app(name: String): LauncherApp? = state.allApps.firstOrNull {
        !it.isWorkProfile && !it.isPrivateSpace && it.shortcut == null &&
            it.label.equals(name, true)
    }
    var gestures = current.graceButton.copy(enabled = true)
    fun assign(gesture: GraceButtonGesture, label: String) {
        app(label)?.let { installed ->
            gestures = gestures.withTarget(gesture, GraceButtonTarget(GraceButtonAction.App, installed.key))
        }
    }
    assign(GraceButtonGesture.Tap, "ChatGPT")
    gestures = gestures.withTarget(GraceButtonGesture.LongPress,
        GraceButtonTarget(GraceButtonAction.QuickSettings))
    gestures = gestures.withTarget(GraceButtonGesture.DoubleTap,
        GraceButtonTarget(GraceButtonAction.LockScreen))
    assign(GraceButtonGesture.SwipeUp, "Gemini")
    assign(GraceButtonGesture.SwipeDown, "ChatGPT")
    assign(GraceButtonGesture.SwipeLeft, "Vivaldi")
    assign(GraceButtonGesture.SwipeRight, "FairEmail")

    val updated = current.copy(
        useDynamicColors = true,
        paletteStyle = PaletteStyle.Vibrant,
        darkMode = ThemeMode.System,
        amoledMode = true,
        fontLibrary = FontLibrary(selected = listOf(FontLibrary.Josefin)),
        allowHapticFeedback = true,
        allowScreenRotation = true,
        clockEnabled = true,
        calendarAgenda = true,
        mediaPlayer = false,
        mediaKeepWhilePlaying = true,
        search = current.search.copy(enabled = true, suggestions = true,
            contacts = true, fuzzy = true, internet = true),
        workProfile = current.workProfile.copy(enabled = true,
            display = PrivateSpaceDisplay.Folder, showIndicator = true),
        graceButton = gestures,
    )
    return if (packs == null) updated else updated.withIconPacks(listOf(packs.packageName))
}

@Composable
internal fun GraceVideoMigrationSettings(
    uiState: LauncherUiState,
    actions: LauncherActions,
    onBack: () -> Unit,
) {
    var confirm by remember { mutableStateOf(false) }
    var applied by remember { mutableStateOf(false) }
    SettingsScaffold(stringResource(R.string.acronix_migration_title),
        "acronix_migration", onBack) { padding: PaddingValues ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(stringResource(R.string.acronix_migration_summary), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.acronix_migration_details))
            Text(stringResource(R.string.acronix_migration_limit), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { confirm = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.acronix_migration_apply))
            }
            if (applied) Text(stringResource(R.string.acronix_migration_applied),
                color = MaterialTheme.colorScheme.primary)
        }
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text(stringResource(R.string.acronix_migration_title)) },
        text = { Text(stringResource(R.string.acronix_migration_confirm)) },
        confirmButton = {
            TextButton(onClick = {
                actions.updateSettings { current -> applyGraceVideoAppearance(current, uiState) }
                confirm = false
                applied = true
            }) { Text(stringResource(R.string.acronix_migration_apply)) }
        },
        dismissButton = {
            TextButton(onClick = { confirm = false }) {
                Text(stringResource(R.string.settings_cancel))
            }
        },
    )
}
