package com.galaxyrio.gracelauncher.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

enum class FoldSurface { Outer, Inner }

internal fun foldSurfaceSelection(manual: String?, mapFoldToInner: Boolean, foldReported: Boolean): FoldSurface? =
    manual?.let { runCatching { FoldSurface.valueOf(it) }.getOrNull() }
        ?: if (mapFoldToInner && foldReported) FoldSurface.Inner else null

/** Layout metadata only: copying never allocates/deletes an Android widget host ID. */
data class FoldLayouts(
    val independent: Boolean = false,
    val shared: HomeLayout = HomeLayout(),
    val outer: HomeLayout? = null,
    val inner: HomeLayout? = null,
) {
    fun layout(surface: FoldSurface): HomeLayout = if (!independent) shared else
        (if (surface == FoldSurface.Outer) outer else inner) ?: shared

    fun edit(surface: FoldSurface, layout: HomeLayout): FoldLayouts = if (!independent) copy(shared = layout)
        else if (surface == FoldSurface.Outer) copy(outer = layout) else copy(inner = layout)

    fun copyTo(source: FoldSurface, target: FoldSurface): FoldLayouts {
        require(independent) { "Enable independent layouts before copying" }
        return edit(target, layout(source))
    }

    fun presentation(base: HomeLayout, surface: FoldSurface): HomeLayout {
        if (!independent) return base
        val selected = layout(surface)
        return base.copy(topOffsetDp = selected.topOffsetDp, baselineOffsetDp = selected.baselineOffsetDp,
            baselineEnabled = selected.baselineEnabled, clockCalendarEnabled = selected.clockCalendarEnabled)
    }

    // Switching mode retains the independent drafts so returning does not erase them.
    fun sharedMode(): FoldLayouts = copy(independent = false)
}

/** Separate, versioned preferences. Not wired into rendering until display detection is verified. */
class FoldLayoutStore internal constructor(private val preferences: SharedPreferences) {
    constructor(context: Context) : this(context.getSharedPreferences("acronix_fold_layouts", Context.MODE_PRIVATE))

    fun load(existing: HomeLayout): FoldLayouts {
        val encoded = preferences.getString("layouts", null) ?: return FoldLayouts(shared = existing)
        val json = JSONObject(encoded)
        require(json.getInt("version") == 1) { "Unsupported fold layout version" }
        return FoldLayouts(json.getBoolean("independent"), HomeLayout.decode(json.getString("shared")),
            json.optString("outer").takeIf { it.isNotEmpty() }?.let { HomeLayout.decode(it) },
            json.optString("inner").takeIf { it.isNotEmpty() }?.let { HomeLayout.decode(it) })
    }

    fun undo(): Boolean {
        val previous = preferences.getString("previous_layouts", null) ?: return false
        val current = preferences.getString("layouts", null)
        val editor = preferences.edit().putString("layouts", previous)
        if (current == null) editor.remove("previous_layouts") else editor.putString("previous_layouts", current)
        check(editor.commit()) { "Unable to restore previous placement" }
        return true
    }

    fun save(value: FoldLayouts) {
        val json = JSONObject().put("version", 1).put("independent", value.independent)
            .put("shared", value.shared.encode())
        value.outer?.let { json.put("outer", it.encode()) }
        value.inner?.let { json.put("inner", it.encode()) }
        val editor = preferences.edit()
        preferences.getString("layouts", null)?.let { editor.putString("previous_layouts", it) }
        check(editor.putString("layouts", json.toString()).commit()) { "Unable to persist layouts" }
    }
}
