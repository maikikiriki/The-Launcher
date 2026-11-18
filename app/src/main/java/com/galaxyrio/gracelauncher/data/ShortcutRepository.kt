package com.galaxyrio.gracelauncher.data

import android.content.Context
import android.content.ComponentName
import android.net.Uri
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LauncherShortcut(
    val id: String,
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?,
    val activity: ComponentName? = null,
    val user: UserHandle? = null,
    val userSerial: Long? = null,
    val isPrivateSpace: Boolean = false,
) {
    private val iconCacheKey = Any()
    val key: String get() = (userSerial?.let { "profile:$it:" } ?: "") + "shortcut:$packageName/${Uri.encode(id)}"
    fun asApp(owner: LauncherApp): LauncherApp {
        val identity = copy(user = user ?: owner.user, userSerial = userSerial ?: owner.userSerial,
            isPrivateSpace = isPrivateSpace || owner.isPrivateSpace)
        return LauncherApp(componentName = activity ?: owner.componentName, label = label, icon = icon, shortcut = identity,
            user = identity.user, userSerial = identity.userSerial, isPrivateSpace = identity.isPrivateSpace,
            showPrivateIndicator = owner.showPrivateIndicator,
            isWorkProfile = owner.isWorkProfile, showWorkIndicator = owner.showWorkIndicator,
            isCloneProfile = owner.isCloneProfile,
            originalIconColor = owner.originalIconColor, iconSourceRevision = iconCacheKey)
    }
}

enum class ShortcutStatus { Loading, Ready, DefaultLauncherRequired, Error }

data class ShortcutResult(
    val status: ShortcutStatus,
    val shortcuts: List<LauncherShortcut> = emptyList(),
    /** Presentation only. Keep provider artwork intact for re-theming and the single editor. */
    val styledApps: Map<String, LauncherApp> = emptyMap(),
) {
    fun asApps(owner: LauncherApp): List<LauncherApp> = shortcuts.map { shortcut ->
        val item = shortcut.asApp(owner)
        styledApps[item.key] ?: item
    }
}

class ShortcutRepository(context: Context) : AutoCloseable {
    private data class QueryKey(val packageName: String, val activity: String, val user: UserHandle,
        val userSerial: Long?, val isPrivateSpace: Boolean)

    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val density = context.resources.displayMetrics.densityDpi
    private val user = Process.myUserHandle()
    private val cache = AsyncQueryCache<QueryKey, ShortcutResult>(
        ttlMillis = 30_000,
        shouldCache = { it.status == ShortcutStatus.Ready },
        load = ::queryShortcuts,
    )
    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = invalidate(packageName)
        override fun onPackageAdded(packageName: String, user: UserHandle) = invalidate(packageName)
        override fun onPackageChanged(packageName: String, user: UserHandle) = invalidate(packageName)
        override fun onPackagesAvailable(packages: Array<out String>, user: UserHandle, replacing: Boolean) {
            packages.forEach(::invalidate)
        }
        override fun onPackagesUnavailable(packages: Array<out String>, user: UserHandle, replacing: Boolean) {
            packages.forEach(::invalidate)
        }
        override fun onShortcutsChanged(packageName: String, shortcuts: MutableList<ShortcutInfo>, user: UserHandle) {
            invalidate(packageName)
        }
    }
    private val callbackRegistered = runCatching {
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
    }.isSuccess

    fun hasAccess(): Boolean = runCatching { launcherApps.hasShortcutHostPermission() }.getOrDefault(false)

    /** No I/O query or placeholder frame when a previously prefetched result is available. */
    fun peek(app: LauncherApp): ShortcutResult? {
        if (!hasAccess()) {
            cache.invalidate()
            return ShortcutResult(ShortcutStatus.DefaultLauncherRequired)
        }
        return cache.peek(app.queryKey())
    }

    suspend fun shortcutsFor(app: LauncherApp): ShortcutResult {
        peek(app)?.let { return it }
        val result = cache.get(app.queryKey())
        // The HOME role can be revoked while a Binder query is running.
        if (!hasAccess()) {
            cache.invalidate()
            return ShortcutResult(ShortcutStatus.DefaultLauncherRequired)
        }
        return result
    }

    /** Bounded by the shared query semaphore; simultaneous touch prefetches are coalesced. */
    suspend fun prefetch(apps: List<LauncherApp>) = coroutineScope {
        if (!hasAccess()) {
            cache.invalidate()
            return@coroutineScope
        }
        apps.distinctBy { it.key }.forEach { app -> launch { shortcutsFor(app) } }
    }

    private fun LauncherApp.queryKey() = QueryKey(packageName, componentName.flattenToString(),
        user ?: this@ShortcutRepository.user, userSerial, isPrivateSpace)

    private fun invalidate(packageName: String) {
        cache.invalidate { it.packageName == packageName }
    }

    private suspend fun queryShortcuts(key: QueryKey): ShortcutResult {
        if (!hasAccess()) return ShortcutResult(ShortcutStatus.DefaultLauncherRequired)
        return try {
            val query = LauncherApps.ShortcutQuery()
                .setPackage(key.packageName)
                .setQueryFlags(
                    LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED,
                )
            val shortcuts = launcherApps.getShortcuts(query, key.user).orEmpty()
                .filter { info ->
                    // Third-party browsers such as Vivaldi may change the owning Activity
                    // when updating a pinned web shortcut. A pinned ID belongs to its
                    // package/user and should not disappear merely because that Activity
                    // differs from the app's current launcher component.
                    info.isEnabled && (info.isPinned ||
                        info.activity == null || info.activity?.flattenToString() == key.activity)
                }
                .distinctBy { it.id }
                .sortedWith(compareBy({ !it.isDeclaredInManifest }, { it.rank }))
                .map { info ->
                    LauncherShortcut(
                        id = info.id,
                        packageName = info.`package`,
                        label = info.shortLabel?.toString().orEmpty(),
                        icon = runCatching {
                            launcherApps.getShortcutIconDrawable(info, density)
                                ?.toBitmap(width = 120, height = 120)?.asImageBitmap()
                        }.getOrNull(),
                        activity = info.activity,
                        user = key.user.takeIf { key.userSerial != null }, userSerial = key.userSerial, isPrivateSpace = key.isPrivateSpace,
                    )
                }
            ShortcutResult(ShortcutStatus.Ready, shortcuts)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: SecurityException) {
            ShortcutResult(if (hasAccess()) ShortcutStatus.Error else ShortcutStatus.DefaultLauncherRequired)
        } catch (_: Exception) {
            ShortcutResult(ShortcutStatus.Error)
        }
    }

    fun launch(shortcut: LauncherShortcut, sourceBounds: Rect? = null, options: Bundle? = null): Boolean {
        val launched = runCatching {
            launcherApps.startShortcut(shortcut.packageName, shortcut.id, sourceBounds, options, shortcut.user ?: user)
        }.isSuccess
        // Failed starts can be caused by browser updates invalidating a saved ID.
        // Keep the user's placement, but never keep a stale provider query cached.
        if (!launched) invalidate(shortcut.packageName)
        return launched
    }

    /**
     * Best-effort repair after an actual failed launch. Do Binder work off main.
     * Never retarget to a different shortcut ID, app, or user/profile: doing so
     * could open a different page or a personal app instead of the work copy.
     * No background polling and no silent deletion of the pinned item.
     */
    suspend fun repairPinnedShortcut(shortcut: LauncherShortcut): Boolean = withContext(Dispatchers.IO) {
        if (!hasAccess() || shortcut.isPrivateSpace) return@withContext false
        val targetUser = shortcut.user ?: user
        runCatching {
            val candidates = launcherApps.getShortcuts(
                LauncherApps.ShortcutQuery().setPackage(shortcut.packageName)
                    .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED),
                targetUser,
            ).orEmpty()
            val current = candidates.firstOrNull { it.id == shortcut.id && it.isEnabled }
                ?: return@runCatching false
            if (!current.isPinned) pin(listOf(shortcut))
            invalidate(shortcut.packageName)
            // The provider must still expose this exact identity; the UI prompts
            // the owner to re-pin manually when an ID has actually disappeared.
            true
        }.getOrDefault(false)
    }

    /** Preserve dynamic shortcuts once the user adds them to a persistent surface. */
    suspend fun pin(shortcuts: List<LauncherShortcut>) = withContext(Dispatchers.IO) {
        if (!hasAccess()) return@withContext
        shortcuts.groupBy { it.packageName to (it.user ?: user) }.forEach { (target, entries) ->
            val (pkg, targetUser) = target
            runCatching {
                val existing = launcherApps.getShortcuts(LauncherApps.ShortcutQuery().setPackage(pkg)
                    .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED), targetUser).orEmpty().map { it.id }
                val next = (existing + entries.map { it.id }).distinct()
                if (next.toSet() != existing.toSet()) {
                    launcherApps.pinShortcuts(pkg, next, targetUser)
                    invalidate(pkg)
                }
            }
        }
    }

    override fun close() {
        if (callbackRegistered) launcherApps.unregisterCallback(callback)
        cache.close()
    }
}
