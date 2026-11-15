package com.galaxyrio.gracelauncher.ui.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import com.galaxyrio.gracelauncher.R
import com.galaxyrio.gracelauncher.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.takeUnless { it === this }?.activity()
    else -> null
}

@Composable
private fun foldPreferences(override: SharedPreferences? = null): Pair<SharedPreferences, Int> {
    val context = LocalContext.current
    val prefs = remember(context, override) { override ?: context.getSharedPreferences("acronix_fold_layouts", Context.MODE_PRIVATE) }
    var revision by remember { mutableIntStateOf(0) }
    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> revision++ }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return prefs to revision
}

/** Presentation overlay only. Android widget identity and membership always come from Room. */
@Composable
internal fun foldPresentation(shared: HomeLayout, preferences: SharedPreferences? = null): HomeLayout {
    val context = LocalContext.current
    val (prefs, revision) = foldPreferences(preferences)
    val layouts by produceState(FoldLayouts(shared = shared), shared, revision) {
        value = withContext(Dispatchers.IO) { runCatching { FoldLayoutStore(prefs).load(shared) }
            .getOrDefault(FoldLayouts(shared = shared)) }
    }
    var hingeVisible by remember { mutableStateOf(false) }
    LaunchedEffect(context) {
        val activity = context.activity() ?: return@LaunchedEffect
        try {
            WindowInfoTracker.getOrCreate(activity).windowLayoutInfo(activity).collect { info ->
                hingeVisible = info.displayFeatures.any { it is FoldingFeature }
            }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            hingeVisible = false
        }
    }
    val surface = foldSurfaceSelection(prefs.getString("surface", null),
        prefs.getBoolean("fold_signal_inner", false), hingeVisible)
    return surface?.let { layouts.presentation(shared, it) } ?: shared
}

@Composable
internal fun AcronixFoldSettings(shared: HomeLayout, onBack: () -> Unit, preferences: SharedPreferences? = null) {
    val (prefs, revision) = foldPreferences(preferences)
    val scope = rememberCoroutineScope()
    var layouts by remember { mutableStateOf(FoldLayouts(shared = shared)) }
    var failed by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(true) }
    var surface by remember { mutableStateOf(FoldSurface.Outer) }
    var copyTarget by remember { mutableStateOf<FoldSurface?>(null) }
    LaunchedEffect(revision, shared) {
        busy = true
        try { layouts = withContext(Dispatchers.IO) { FoldLayoutStore(prefs).load(shared) };
            prefs.getString("surface", null)?.let { name -> runCatching { FoldSurface.valueOf(name) }.getOrNull()?.let { surface = it } }; failed = false }
        catch (error: Exception) { if (error is CancellationException) throw error; failed = true }
        finally { busy = false }
    }
    fun save(value: FoldLayouts) {
        scope.launch {
            busy = true
            try { withContext(Dispatchers.IO) { FoldLayoutStore(prefs).save(value) }; layouts = value }
            catch (error: Exception) { if (error is CancellationException) throw error; failed = true }
            finally { busy = false }
        }
    }
    SettingsScaffold(stringResource(R.string.acronix_fold_title), "acronix_fold", onBack) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.acronix_fold_summary))
            Text(stringResource(R.string.fold_detection))
            if (failed) Text(stringResource(R.string.fold_error), color = MaterialTheme.colorScheme.error)
            Button(modifier = Modifier.testTag("fold_mode"), enabled = !busy && !failed, onClick = { save(layouts.copy(independent = !layouts.independent)) }) {
                Text(stringResource(if (layouts.independent) R.string.fold_shared else R.string.fold_independent))
            }
            Button(enabled = !busy && !failed, onClick = {
                scope.launch { withContext(Dispatchers.IO) {
                    if (!prefs.edit().remove("surface").putBoolean("fold_signal_inner", true).commit()) failed = true
                } }
            }) { Text(stringResource(R.string.fold_auto)) }
            Button(modifier = Modifier.testTag("fold_undo"), enabled = !busy && !failed && prefs.contains("previous_layouts"), onClick = {
                scope.launch {
                    busy = true
                    try { withContext(Dispatchers.IO) { FoldLayoutStore(prefs).undo() } }
                    catch (error: Exception) { if (error is CancellationException) throw error; failed = true }
                    finally { busy = false }
                }
            }) { Text(stringResource(R.string.fold_undo)) }
            FoldSurface.entries.forEach { choice ->
                Button(modifier = Modifier.testTag("fold_surface:${choice.name}"), enabled = !busy && !failed, onClick = {
                    surface = choice
                    scope.launch { withContext(Dispatchers.IO) {
                        if (!prefs.edit().putString("surface", choice.name).commit()) failed = true
                    } }
                }) { Text(stringResource(R.string.fold_select, stringResource(if (choice == FoldSurface.Outer) R.string.fold_outer else R.string.fold_inner))) }
            }
            val selected = layouts.layout(surface)
            Text(stringResource(R.string.fold_preview, stringResource(if (surface == FoldSurface.Outer) R.string.fold_outer else R.string.fold_inner), selected.topOffsetDp.toInt(), selected.clockCalendarEnabled.toString()))
            Button(modifier = Modifier.testTag("fold_clock"), enabled = layouts.independent && !busy && !failed, onClick = {
                save(layouts.edit(surface, selected.copy(clockCalendarEnabled = !selected.clockCalendarEnabled)))
            }) { Text(stringResource(R.string.fold_toggle_clock)) }
            Row {
                listOf(-16f, 16f).forEach { delta ->
                    Button(modifier = Modifier.testTag("fold_offset:${delta.toInt()}"), enabled = layouts.independent && !busy && !failed, onClick = {
                        save(layouts.edit(surface, selected.copy(topOffsetDp = (selected.topOffsetDp + delta).coerceIn(-1200f, 1200f))))
                    }) { Text("${delta.toInt()} dp") }
                }
            }
            Button(modifier = Modifier.testTag("fold_copy"), enabled = layouts.independent && !busy && !failed,
                onClick = { copyTarget = if (surface == FoldSurface.Outer) FoldSurface.Inner else FoldSurface.Outer }) {
                Text(stringResource(R.string.fold_copy_other))
            }
        }
    }
    copyTarget?.let { target ->
        AlertDialog(onDismissRequest = { copyTarget = null }, title = { Text(stringResource(R.string.fold_confirm)) },
            text = { Text(stringResource(R.string.fold_copy_warning)) },
            confirmButton = { TextButton(modifier = Modifier.testTag("fold_copy_confirm"), onClick = { save(layouts.copyTo(surface, target)); copyTarget = null }) { Text(stringResource(R.string.fold_copy)) } },
            dismissButton = { TextButton(onClick = { copyTarget = null }) { Text(stringResource(R.string.settings_cancel)) } })
    }
}
