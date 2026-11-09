package com.liquidlauncher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import org.json.JSONArray

data class LauncherApp(
    val key: String,
    val packageName: String,
    val activityName: String,
    val label: String,
    val icon: Drawable
)

class LauncherStore(private val context: Context) {
    private val preferences = context.getSharedPreferences("liquid_launcher_v2", Context.MODE_PRIVATE)

    fun installedApps(): List<LauncherApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val matches = if (Build.VERSION.SDK_INT >= 33) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
        return matches.asSequence()
            .filter { it.activityInfo.packageName != context.packageName }
            .map {
                val component = ComponentName(it.activityInfo.packageName, it.activityInfo.name)
                LauncherApp(
                    key = component.flattenToString(),
                    packageName = it.activityInfo.packageName,
                    activityName = it.activityInfo.name,
                    label = it.loadLabel(pm).toString(),
                    icon = it.loadIcon(pm)
                )
            }
            .distinctBy { it.key }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    fun loadKeys(name: String): List<String>? {
        val data = preferences.getString(name, null) ?: return null
        return runCatching {
            val values = JSONArray(data)
            (0 until values.length()).map { values.getString(it) }
        }.getOrNull()
    }

    fun saveKeys(name: String, keys: List<String>) {
        val arr = JSONArray()
        keys.forEach { arr.put(it) }
        preferences.edit().putString(name, arr.toString()).apply()
    }

    fun launch(app: LauncherApp) {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(ComponentName(app.packageName, app.activityName))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        context.startActivity(intent)
    }
}

fun Drawable.bitmapForIcon(size: Int = 144): Bitmap {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val previous = bounds
    setBounds(0, 0, size, size)
    draw(canvas)
    bounds = previous
    return bitmap
}
