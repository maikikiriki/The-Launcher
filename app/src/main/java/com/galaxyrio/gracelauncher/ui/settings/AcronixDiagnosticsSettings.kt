package com.galaxyrio.gracelauncher.ui.settings

import android.Manifest
import android.app.role.RoleManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.StatFs
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.galaxyrio.gracelauncher.R

/** On-demand, read-only diagnosis. No user data is collected or transmitted. */
private data class DiagnosticRow(val label: Int, val info: String, val status: Int, val action: Intent? = null)

@Composable
internal fun AcronixDiagnosticsSettings(onBack: () -> Unit) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    var openFailed by remember { mutableStateOf(false) }
    val checks = remember(context, refresh) { collectDiagnostics(context) }
    SettingsScaffold(stringResource(R.string.acronix_diag_title), "acronix_diagnostics", onBack) { padding: PaddingValues ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.acronix_diag_intro), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.acronix_diag_privacy),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { refresh++ }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.acronix_diag_refresh))
            }
            if (openFailed) Text(stringResource(R.string.acronix_diag_open_failed),
                color = MaterialTheme.colorScheme.error)
            checks.forEach { check ->
                Text(stringResource(check.label), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(check.status), color = if (check.status == R.string.acronix_diag_action)
                    MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                Text(check.info, style = MaterialTheme.typography.bodyMedium)
                check.action?.let { intent ->
                    Button(onClick = {
                        try {
                            context.startActivity(Intent(intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            openFailed = false
                        } catch (_: Exception) { openFailed = true }
                    }) { Text(stringResource(R.string.acronix_diag_open_settings)) }
                }
            }
        }
    }
}

private fun collectDiagnostics(context: Context): List<DiagnosticRow> {
    fun text(id: Int) = context.getString(id)
    fun installed(className: String) = try {
        @Suppress("DEPRECATION")
        context.packageManager.getServiceInfo(ComponentName(context.packageName, className), 0)
        true
    } catch (_: PackageManager.NameNotFoundException) { false }
    fun appDetails() = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.parse("package:" + context.packageName))
    val home = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            context.getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_HOME)
        else {
            @Suppress("DEPRECATION")
            val activity = context.packageManager.resolveActivity(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY)
            activity?.activityInfo?.packageName == context.packageName
        }
    } catch (_: Exception) { null }
    val accessibilityClass = "com.galaxyrio.gracelauncher.platform.GraceAccessibilityService"
    val notificationClass = "com.galaxyrio.gracelauncher.data.media.MediaNotificationListener"
    val accessibilityAvailable = installed(accessibilityClass)
    val notificationsAvailable = installed(notificationClass)
    val accessibilityGranted = try {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            .orEmpty().split(':').any {
                ComponentName.unflattenFromString(it)?.let { c ->
                    c.packageName == context.packageName && c.className == accessibilityClass
                } == true
            }
    } catch (_: Exception) { false }
    val notificationsGranted = try {
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    } catch (_: Exception) { false }
    fun permission(p: String) = ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED
    val profiles = try { context.getSystemService(LauncherApps::class.java)?.profiles?.size } catch (_: Exception) { null }
    val widgets = try { AppWidgetManager.getInstance(context).installedProviders.size } catch (_: Exception) { null }
    val freeMiB = try { StatFs(context.filesDir.absolutePath).availableBytes / 1048576L } catch (_: Exception) { null }
    val ok = R.string.acronix_diag_ok
    val action = R.string.acronix_diag_action
    val info = R.string.acronix_diag_information
    val unavailable = R.string.acronix_diag_unavailable
    return listOf(
        DiagnosticRow(R.string.acronix_diag_home, text(R.string.acronix_diag_home_info),
            when(home) { true -> ok; false -> action; null -> info }, Intent(Settings.ACTION_HOME_SETTINGS)),
        DiagnosticRow(R.string.acronix_diag_accessibility,
            text(if (accessibilityAvailable) R.string.acronix_diag_special_permission else R.string.acronix_diag_build_limited),
            if (!accessibilityAvailable) unavailable else if (accessibilityGranted) ok else action,
            if (accessibilityAvailable) Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS) else null),
        DiagnosticRow(R.string.acronix_diag_notifications,
            text(if (notificationsAvailable) R.string.acronix_diag_special_permission else R.string.acronix_diag_build_limited),
            if (!notificationsAvailable) unavailable else if (notificationsGranted) ok else action,
            if (notificationsAvailable) Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS) else null),
        DiagnosticRow(R.string.acronix_diag_contacts, text(R.string.acronix_diag_app_permission),
            if (permission(Manifest.permission.READ_CONTACTS)) ok else action, appDetails()),
        DiagnosticRow(R.string.acronix_diag_calendar, text(R.string.acronix_diag_app_permission),
            if (permission(Manifest.permission.READ_CALENDAR)) ok else action, appDetails()),
        DiagnosticRow(R.string.acronix_diag_widgets,
            widgets?.let { context.getString(R.string.acronix_diag_widget_count, it) }
                ?: text(R.string.acronix_diag_unknown), info),
        DiagnosticRow(R.string.acronix_diag_profiles,
            profiles?.let { context.getString(R.string.acronix_diag_profile_count, it) }
                ?: text(R.string.acronix_diag_unknown), info),
        DiagnosticRow(R.string.acronix_diag_storage,
            freeMiB?.let { context.getString(R.string.acronix_diag_storage_count, it) }
                ?: text(R.string.acronix_diag_unknown), info),
        DiagnosticRow(R.string.acronix_diag_backup, text(R.string.acronix_diag_backup_blocked), unavailable),
        DiagnosticRow(R.string.acronix_diag_android,
            context.getString(R.string.acronix_diag_android_version, Build.VERSION.SDK_INT), info)
    )
}
