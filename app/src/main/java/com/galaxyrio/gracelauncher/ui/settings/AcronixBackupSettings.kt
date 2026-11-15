package com.galaxyrio.gracelauncher.ui.settings

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.galaxyrio.gracelauncher.R
import com.galaxyrio.gracelauncher.data.AcronixBackup
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun AcronixBackupSettings(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var pendingRestore by remember { mutableStateOf<Uri?>(null) }
    val saved = stringResource(R.string.acronix_backup_saved)
    val restored = stringResource(R.string.acronix_backup_restored)
    val errorText = stringResource(R.string.acronix_backup_error)
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null && !busy) scope.launch {
            busy = true
            try {
                AcronixBackup.export(context, uri)
                status = saved
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                status = "$errorText: ${error.message.orEmpty().take(160)}"
            } finally { busy = false }
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && !busy) pendingRestore = uri
    }
    val checkedText = stringResource(R.string.acronix_backup_checked)
    val inspector = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && !busy) scope.launch {
            busy = true
            try {
                AcronixBackup.inspect(context, uri)
                status = checkedText
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                status = errorText
            } finally { busy = false }
        }
    }
    SettingsScaffold(stringResource(R.string.acronix_backup_title), "acronix_backup", onBack) { padding: PaddingValues ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.acronix_backup_summary), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.acronix_backup_warning),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.acronix_backup_restore_disabled),
                color = MaterialTheme.colorScheme.error)
            Button(onClick = {
                exporter.launch("Acronix-backup.zip")
            }, modifier = Modifier.fillMaxWidth(), enabled = !busy) {
                Text(stringResource(R.string.acronix_backup_export))
            }
            Button(onClick = {
                importer.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
            }, modifier = Modifier.fillMaxWidth(), enabled = false) {
                Text(stringResource(R.string.acronix_backup_import))
            }
            Button(onClick = {
                if (!busy) scope.launch {
                    busy = true
                    try {
                        val file = AcronixBackup.shareFile(context)
                        val uri = FileProvider.getUriForFile(context,
                            "${context.packageName}.acronixfiles", file)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/zip"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            clipData = ClipData.newUri(context.contentResolver, "Acronix backup", uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent,
                            context.getString(R.string.acronix_backup_share)))
                    } catch (error: Exception) {
                        if (error is CancellationException) throw error
                        status = "$errorText: ${error.message.orEmpty().take(160)}"
                    } finally { busy = false }
                }
            }, modifier = Modifier.fillMaxWidth(), enabled = !busy) {
                Text(stringResource(R.string.acronix_backup_share))
            }
            Button(onClick = { inspector.launch(arrayOf("application/zip", "application/octet-stream")) },
                modifier = Modifier.fillMaxWidth(), enabled = !busy) {
                Text(stringResource(R.string.acronix_backup_check))
            }
            if (busy) Text(stringResource(R.string.acronix_backup_busy))
            status?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        }
    }
    if (pendingRestore != null) AlertDialog(
        onDismissRequest = { pendingRestore = null },
        title = { Text(stringResource(R.string.acronix_backup_confirm_title)) },
        text = { Text(stringResource(R.string.acronix_backup_confirm)) },
        confirmButton = {
            TextButton(onClick = {
                val uri = pendingRestore
                pendingRestore = null
                if (uri != null) scope.launch {
                    busy = true
                    try {
                        AcronixBackup.restore(context, uri)
                        status = restored
                    } catch (error: Exception) {
                        if (error is CancellationException) throw error
                        status = "$errorText: ${error.message.orEmpty().take(160)}"
                    } finally { busy = false }
                }
            }) { Text(stringResource(R.string.acronix_backup_import)) }
        },
        dismissButton = {
            TextButton(onClick = { pendingRestore = null }) {
                Text(stringResource(R.string.settings_cancel))
            }
        }
    )
}
