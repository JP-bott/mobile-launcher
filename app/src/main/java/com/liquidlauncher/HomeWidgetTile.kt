package com.liquidlauncher

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Displays a real Android app widget inside a home grid tile.
 * Widget ownership is persisted by PositionRepository, and Android remains
 * responsible for widget content and provider lifecycle.
 */
@Composable
fun HomeWidgetTile(widgetId: Int, editing: Boolean, onRemove: () -> Unit) {
    val context = LocalContext.current
    val host = remember { AppWidgetHost(context, 19026) }
    val provider = remember(widgetId) {
        AppWidgetManager.getInstance(context).getAppWidgetInfo(widgetId)
    }
    DisposableEffect(host) {
        runCatching { host.startListening() }
        onDispose { runCatching { host.stopListening() } }
    }
    Box(
        Modifier.fillMaxSize().padding(4.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = .23f)),
        contentAlignment = Alignment.Center
    ) {
        if (provider == null) {
            Text("Widget unavailable", color = Color.White)
        } else {
            key(widgetId) {
                AndroidView(
                    factory = { ctx ->
                        host.createView(ctx, widgetId, provider).apply {
                            setAppWidget(widgetId, provider)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        if (editing) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(7.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFE64B59))
                    .clickable(onClick = onRemove)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) { Text("Remove", color = Color.White) }
        }
    }
}
