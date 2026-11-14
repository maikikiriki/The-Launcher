package com.galaxyrio.gracelauncher.data

import org.json.JSONArray
import org.json.JSONObject

enum class SearchEngine(val id: String, val urlTemplate: String? = null) {
    Google("google", "https://www.google.com/search?q={query}"),
    Bing("bing", "https://www.bing.com/search?q={query}"),
    Sogou("sogou", "https://www.sogou.com/web?query={query}"),
    DuckDuckGo("duckduckgo", "https://duckduckgo.com/?q={query}"),
    Qwant("qwant", "https://www.qwant.com/?q={query}"),
    Startpage("startpage", "https://www.startpage.com/sp/search?query={query}"),
    Custom("custom"),
}

enum class SearchResultCategory(val id: String) {
    Apps("apps"), Contacts("contacts"), Web("web"), Device("device");

    companion object {
        val defaultOrder: List<SearchResultCategory> = entries.toList()
        fun normalized(values: List<SearchResultCategory>): List<SearchResultCategory> =
            (values + defaultOrder).distinct()
    }
}

data class SearchSettings(
    val enabled: Boolean = true,
    val suggestions: Boolean = true,
    val contacts: Boolean = false,
    val fuzzy: Boolean = true,
    val internet: Boolean = false,
    val hiddenApps: Boolean = true,
    val recentAppKeys: List<String> = emptyList(),
    val engine: SearchEngine = SearchEngine.Google,
    val customEngineName: String = "",
    val customEngineUrl: String = "",
    val deviceSettings: Boolean = false,
    val resultOrder: List<SearchResultCategory> = SearchResultCategory.defaultOrder,
) {
    fun recordApp(key: String) = copy(recentAppKeys = (listOf(key) + recentAppKeys).distinct().take(5))

    fun encode(): String = JSONObject().put("enabled", enabled).put("suggestions", suggestions)
        .put("contacts", contacts).put("fuzzy", fuzzy).put("internet", internet).put("hiddenApps", hiddenApps)
        .put("recentApps", JSONArray(recentAppKeys.distinct().take(5)))
        .put("engine", engine.id).put("customEngineName", customEngineName).put("customEngineUrl", customEngineUrl)
        .put("deviceSettings", deviceSettings)
        .put("resultOrder", JSONArray(SearchResultCategory.normalized(resultOrder).map { it.id })).toString()

    companion object {
        fun decode(value: String?): SearchSettings = runCatching {
            if (value == null) return SearchSettings()
            val json = JSONObject(value)
            val recent = json.optJSONArray("recentApps") ?: JSONArray()
            SearchSettings(
                enabled = json.optBoolean("enabled", true), suggestions = json.optBoolean("suggestions", true),
                contacts = json.optBoolean("contacts", false), fuzzy = json.optBoolean("fuzzy", true),
                internet = json.optBoolean("internet", false), hiddenApps = json.optBoolean("hiddenApps", true),
                recentAppKeys = (0 until recent.length()).mapNotNull { (recent.opt(it) as? String)?.takeIf(String::isNotBlank) }.distinct().take(5),
                engine = SearchEngine.entries.firstOrNull { it.id == json.optString("engine") } ?: SearchEngine.Google,
                customEngineName = json.optString("customEngineName", ""),
                customEngineUrl = json.optString("customEngineUrl", ""),
                deviceSettings = json.optBoolean("deviceSettings", false),
                resultOrder = (json.optJSONArray("resultOrder") ?: JSONArray()).let { order ->
                    SearchResultCategory.normalized((0 until order.length()).mapNotNull { index ->
                        SearchResultCategory.entries.firstOrNull { it.id == order.optString(index) }
                    })
                },
            )
        }.getOrDefault(SearchSettings())
    }
}
