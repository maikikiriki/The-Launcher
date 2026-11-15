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
private fun foldPreferences(): Pair<SharedPreferences, Int> {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("acronix_fold_layouts", Context.MODE_PRIVATE) }
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
internal fun foldPresentation(shared: HomeLayout): HomeLayout {
    val context = LocalContext.current
    val (prefs, revision) = foldPreferences()
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
    val manual = runCatching { FoldSurface.valueOf(prefs.getString("surface", "Outer")!!) }.getOrDefault(FoldSurface.Outer)
    return layouts.presentation(shared, if (hingeVisible) FoldSurface.Inner else manual)
}

@Composable
internal fun AcronixFoldSettings(shared: HomeLayout, onBack: () -> Unit) {
    val (prefs, revision) = foldPreferences()
    val scope = rememberCoroutineScope()
    var layouts by remember { mutableStateOf(FoldLayouts(shared = shared)) }
    var failed by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(true) }
    var surface by remember { mutableStateOf(FoldSurface.Outer) }
    var copyTarget by remember { mutableStateOf<FoldSurface?>(null) }
    LaunchedEffect(revision, shared) {
        busy = true
        try { layouts = withContext(Dispatchers.IO) { FoldLayoutStore(prefs).load(shared) }; failed = false }
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
            Text("Android selects the inner layout when it reports a fold in this window. Otherwise choose the screen manually below.")
            if (failed) Text("Unable to read or save layouts. Existing data has been retained.", color = MaterialTheme.colorScheme.error)
            Button(enabled = !busy && !failed, onClick = { save(layouts.copy(independent = !layouts.independent)) }) {
                Text(if (layouts.independent) "Use shared layout" else "Enable independent placement")
            }
            FoldSurface.entries.forEach { choice ->
                Button(enabled = !busy && !failed, onClick = {
                    surface = choice
                    scope.launch { withContext(Dispatchers.IO) {
                        if (!prefs.edit().putString("surface", choice.name).commit()) failed = true
                    } }
                }) { Text("Edit / select ${choice.name}") }
            }
            val selected = layouts.layout(surface)
            Text("${surface.name}: offset ${selected.topOffsetDp.toInt()} dp; clock ${selected.clockCalendarEnabled}")
            Button(enabled = layouts.independent && !busy && !failed, onClick = {
                save(layouts.edit(surface, selected.copy(clockCalendarEnabled = !selected.clockCalendarEnabled)))
            }) { Text("Toggle clock and calendar") }
            Row {
                listOf(-16f, 16f).forEach { delta ->
                    Button(enabled = layouts.independent && !busy && !failed, onClick = {
                        save(layouts.edit(surface, selected.copy(topOffsetDp = (selected.topOffsetDp + delta).coerceIn(-1200f, 1200f))))
                    }) { Text("${delta.toInt()} dp") }
                }
            }
            Button(enabled = layouts.independent && !busy && !failed,
                onClick = { copyTarget = if (surface == FoldSurface.Outer) FoldSurface.Inner else FoldSurface.Outer }) {
                Text("Copy placement to other screen")
            }
        }
    }
    copyTarget?.let { target ->
        AlertDialog(onDismissRequest = { copyTarget = null }, title = { Text("Copy placement?") },
            text = { Text("Replace $target placement with $surface. Widgets and favorites remain shared.") },
            confirmButton = { TextButton(onClick = { save(layouts.copyTo(surface, target)); copyTarget = null }) { Text("Copy") } },
            dismissButton = { TextButton(onClick = { copyTarget = null }) { Text("Cancel") } })
    }
}
