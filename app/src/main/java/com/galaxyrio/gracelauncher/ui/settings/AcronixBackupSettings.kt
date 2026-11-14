package com.galaxyrio.gracelauncher.ui.settings

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import com.galaxyrio.gracelauncher.R
import com.galaxyrio.gracelauncher.data.AcronixBackup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
internal fun AcronixBackupSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var importing by remember { mutableStateOf<Uri?>(null) }
    var busy by remember { mutableStateOf(false) }
    val success = stringResource(R.string.acronix_backup_success)
    val imported = stringResource(R.string.acronix_backup_imported)
    val failure = stringResource(R.string.acronix_backup_failure)
    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_LONG).show()

    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null && !busy) scope.launch {
            busy = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val stream = context.contentResolver.openOutputStream(uri) ?: error("No se pudo abrir el archivo")
                    stream.use { AcronixBackup.export(context, it) }
                }
            }
            busy = false
            toast(if (result.isSuccess) success else "$failure: ${result.exceptionOrNull()?.message.orEmpty()}")
        }
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        importing = uri
    }
    fun share() = scope.launch {
        busy = true
        val outcome = runCatching {
            val file = withContext(Dispatchers.IO) {
                val directory = File(context.cacheDir, "acronix_share")
                check(directory.mkdirs() || directory.isDirectory)
                val target = File(directory, "Acronix-settings.zip")
                target.outputStream().use { AcronixBackup.export(context, it) }
                target
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newUri(context.contentResolver, "Acronix settings", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.acronix_backup_share)))
        }
        busy = false
        if (outcome.isFailure) toast("$failure: ${outcome.exceptionOrNull()?.message.orEmpty()}")
    }

    SettingsScaffold(stringResource(R.string.acronix_backup_title), "acronix_backup", onBack) { padding ->
        SettingsList(padding) {
            item {
                SettingsActionItem(stringResource(R.string.acronix_backup_export),
                    stringResource(R.string.acronix_backup_export_summary), 0, 3, "acronix_export",
                    enabled = !busy) {
                    save.launch("Acronix-settings.zip")
                }
            }
            item {
                SettingsActionItem(stringResource(R.string.acronix_backup_import),
                    stringResource(R.string.acronix_backup_import_summary), 1, 3, "acronix_import",
                    enabled = !busy) {
                    open.launch(arrayOf("application/zip", "application/octet-stream"))
                }
            }
            item {
                SettingsActionItem(stringResource(R.string.acronix_backup_share),
                    stringResource(R.string.acronix_backup_share_summary), 2, 3, "acronix_share",
                    enabled = !busy, onClick = { share() })
            }
            item {
                Text(stringResource(R.string.acronix_backup_limitations))
            }
        }
    }
    if (importing != null) AlertDialog(
        onDismissRequest = { importing = null },
        title = { Text(stringResource(R.string.acronix_backup_confirm_title)) },
        text = { Text(stringResource(R.string.acronix_backup_confirm_message)) },
        confirmButton = {
            TextButton(onClick = {
                val uri = importing
                importing = null
                if (uri != null && !busy) scope.launch {
                    busy = true
                    val result = runCatching {
                        withContext(Dispatchers.IO) {
                            val stream = context.contentResolver.openInputStream(uri) ?: error("No se pudo abrir la copia")
                            stream.use { AcronixBackup.restore(context, it) }
                        }
                    }
                    busy = false
                    toast(if (result.isSuccess) imported else "$failure: ${result.exceptionOrNull()?.message.orEmpty()}")
                }
            }) { Text(stringResource(R.string.acronix_backup_import)) }
        },
        dismissButton = {
            TextButton(onClick = { importing = null }) {
                Text(stringResource(R.string.settings_cancel))
            }
        },
    )
}
