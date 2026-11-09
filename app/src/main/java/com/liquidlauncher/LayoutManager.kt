package com.liquidlauncher

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class HomeFolder(val id: String, val title: String, val apps: List<String>) {
    val token: String get() = "folder:$id"
}

class LayoutManager(context: Context) {
    private val prefs = context.getSharedPreferences("liquid_launcher_layout", Context.MODE_PRIVATE)

    fun folders(): List<HomeFolder> = runCatching {
        val data = JSONArray(prefs.getString("folders", "[]"))
        (0 until data.length()).map { i ->
            val item = data.getJSONObject(i)
            val keys = item.getJSONArray("apps")
            HomeFolder(
                item.getString("id"),
                item.optString("title", "Folder"),
                (0 until keys.length()).map { keys.getString(it) }
            )
        }
    }.getOrDefault(emptyList())

    fun saveFolders(folders: List<HomeFolder>) {
        val json = JSONArray()
        folders.forEach { folder ->
            val item = JSONObject()
            item.put("id", folder.id)
            item.put("title", folder.title)
            val apps = JSONArray()
            folder.apps.distinct().forEach { apps.put(it) }
            item.put("apps", apps)
            json.put(item)
        }
        prefs.edit().putString("folders", json.toString()).apply()
    }

    fun createFolder(first: String, second: String): HomeFolder =
        HomeFolder(UUID.randomUUID().toString(), "Folder", listOf(first, second).distinct())

    var showLabels: Boolean
        get() = prefs.getBoolean("show_labels", true)
        set(value) { prefs.edit().putBoolean("show_labels", value).apply() }

    var darkScrim: Boolean
        get() = prefs.getBoolean("dark_scrim", true)
        set(value) { prefs.edit().putBoolean("dark_scrim", value).apply() }

    var haptics: Boolean
        get() = prefs.getBoolean("haptics", true)
        set(value) { prefs.edit().putBoolean("haptics", value).apply() }

    var iconSize: Int
        get() = prefs.getInt("icon_size", 61).coerceIn(48, 70)
        set(value) { prefs.edit().putInt("icon_size", value.coerceIn(48, 70)).apply() }
}
