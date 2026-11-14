package com.galaxyrio.gracelauncher.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.galaxyrio.gracelauncher.R
import java.text.Normalizer
import java.util.Locale

/** Local, bounded settings search. No package scanning or background polling. */
internal data class DeviceSettingMatch(val id: String, val label: String, val action: String)

internal object DeviceSettingsSearch {
    private data class Destination(val id: String, val title: Int, val action: String, val keywords: String)

    private val destinations = listOf(
        Destination("developer", R.string.acronix_device_developer, Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS,
            "desarrollador developers developer options opciones programador depuracion usb debugging"),
        Destination("wifi", R.string.acronix_device_wifi, Settings.ACTION_WIFI_SETTINGS,
            "wifi wi-fi internet red conexiones network wlan"),
        Destination("bluetooth", R.string.acronix_device_bluetooth, Settings.ACTION_BLUETOOTH_SETTINGS,
            "bluetooth dispositivos vinculados paired devices"),
        Destination("apps", R.string.acronix_device_apps, Settings.ACTION_APPLICATION_SETTINGS,
            "aplicaciones apps aplicaciones instaladas installed applications permisos"),
        Destination("battery", R.string.acronix_device_battery, Settings.ACTION_BATTERY_SAVER_SETTINGS,
            "bateria battery ahorro energia power saving"),
        Destination("display", R.string.acronix_device_display, Settings.ACTION_DISPLAY_SETTINGS,
            "pantalla brillo display dark mode screen brightness"),
        Destination("security", R.string.acronix_device_security, Settings.ACTION_SECURITY_SETTINGS,
            "seguridad security bloqueo pantalla lock"),
        Destination("accessibility", R.string.acronix_device_accessibility, Settings.ACTION_ACCESSIBILITY_SETTINGS,
            "accesibilidad accessibility lector pantalla"),
        Destination("notifications", R.string.acronix_device_notifications, Settings.ACTION_APP_NOTIFICATION_SETTINGS,
            "notificaciones avisos notifications"),
    )

    private fun normalize(value: String): String =
        Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "").trim()

    fun find(context: Context, query: String): List<DeviceSettingMatch> {
        val term = normalize(query)
        if (term.length < 2) return emptyList()
        return destinations.asSequence().filter { destination ->
            val haystack = normalize(context.getString(destination.title) + " " + destination.keywords)
            haystack.contains(term) || haystack.split(' ').any { it.startsWith(term) }
        }.take(8).map { destination ->
            DeviceSettingMatch(destination.id, context.getString(destination.title), destination.action)
        }.toList()
    }

    fun open(context: Context, setting: DeviceSettingMatch): Boolean = try {
        val intent = Intent(setting.action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent.resolveActivity(context.packageManager) == null) false
        else { context.startActivity(intent); true }
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
