package com.galaxyrio.gracelauncher.data

import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.os.Bundle
import android.os.Process
import android.util.Log

/** Stateless launch path, shared with routing tests; does not initialize inventory observers. */
internal object AppLaunchRouter {
    fun launch(
        context: android.content.Context,
        app: LauncherApp,
        sourceBounds: Rect? = null,
        options: Bundle? = null,
    ): Boolean = runCatching {
        val user = app.user ?: Process.myUserHandle()
        if (user == Process.myUserHandle()) {
            // Match Launcher3/Lawnchair: current-user entry activities may perform
            // OEM authentication or forwarding before opening the actual app.
            val intent = Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(app.componentName)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                .also { it.sourceBounds = sourceBounds }
            context.startActivity(intent, options)
        } else {
            // Never fall back to the personal installation of a cross-profile app.
            context.getSystemService(LauncherApps::class.java).startMainActivity(
                app.componentName, user, sourceBounds, options,
            )
        }
    }.onFailure { error ->
        Log.e("AppRepository", "Unable to launch ${app.componentName} for ${app.user ?: Process.myUserHandle()}", error)
    }.isSuccess

}
