package com.liquidlauncher

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

private const val HOST_ID = 19026

@Composable
fun WidgetPanel(onClose: () -> Unit, onWidgetAdded: (Int) -> Unit = {}) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val manager = remember { AppWidgetManager.getInstance(context) }
    val host = remember { AppWidgetHost(context, HOST_ID) }
    val pref = remember { context.getSharedPreferences("launcher_widgets", Context.MODE_PRIVATE) }
    var widgetId by remember { mutableIntStateOf(pref.getInt("widget_id", -1)) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { outcome ->
        val id = outcome.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
        if (outcome.resultCode == android.app.Activity.RESULT_OK && id > 0) {
            val old = widgetId
            widgetId = id
            pref.edit().putInt("widget_id", id).apply()
            onWidgetAdded(id)
            if (old > 0 && old != id) runCatching { host.deleteAppWidgetId(old) }
        } else if (id > 0) {
            runCatching { host.deleteAppWidgetId(id) }
        }
    }
    DisposableEffect(host) {
        runCatching { host.startListening() }
        onDispose { runCatching { host.stopListening() } }
    }
    Column(
        Modifier.fillMaxSize().background(Color(0xF71B2438))
            .statusBarsPadding().navigationBarsPadding().padding(18.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Widgets", color = Color.White, style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
            TextButton(onClick = onClose) { Text("Done") }
        }
        Spacer(Modifier.height(12.dp))
        Text("Add an Android widget. Your provider's widget will be displayed below.", color = Color.White)
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = {
            val id = host.allocateAppWidgetId()
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            }
            try {
                picker.launch(intent)
            } catch (_: Exception) {
                runCatching { host.deleteAppWidgetId(id) }
                android.widget.Toast.makeText(context, "Widget picker unavailable", android.widget.Toast.LENGTH_SHORT).show()
            }
        }) { Text("＋ Add Widget") }
        Spacer(Modifier.height(20.dp))
        val info = remember(widgetId) {
            if (widgetId > 0) manager.getAppWidgetInfo(widgetId) else null
        }
        if (info != null) {
            androidx.compose.runtime.key(widgetId) {
                AndroidView(
                    factory = { host.createView(it, widgetId, info).apply { setAppWidget(widgetId, info) } },
                    modifier = Modifier.fillMaxWidth().height(240.dp)
                )
            }
            TextButton(onClick = {
                runCatching { host.deleteAppWidgetId(widgetId) }
                widgetId = -1
                pref.edit().remove("widget_id").apply()
            }) { Text("Remove Widget") }
        } else {
            Text("No widget selected", color = Color.White.copy(alpha = .75f))
        }
    }
}
