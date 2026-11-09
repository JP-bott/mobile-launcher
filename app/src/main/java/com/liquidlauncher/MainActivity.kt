package com.liquidlauncher

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LaunchableApp(
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: Drawable
)

class MainActivity : ComponentActivity() {
    private fun loadApps(): List<LaunchableApp> {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        return pm.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .map { LaunchableApp(it.loadLabel(pm).toString(), it.activityInfo.packageName, it.activityInfo.name, it.loadIcon(pm)) }
            .distinctBy { it.packageName to it.activityName }
            .sortedBy { it.label.lowercase(Locale.getDefault()) }
    }

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var apps by remember { mutableStateOf(loadApps()) }
            val prefs = remember { getSharedPreferences("launcher", MODE_PRIVATE) }
            var pinned by remember { mutableStateOf(prefs.getString("dock", "") ?: "") }
            val pinnedApps = remember(apps, pinned) {
                val keys = pinned.split("|").filter { it.isNotBlank() }
                val selected = keys.mapNotNull { key -> apps.find { it.packageName + "/" + it.activityName == key } }
                if (keys.isEmpty()) apps.take(4) else selected
            }
            var chooserApp by remember { mutableStateOf<LaunchableApp?>(null) }
            LaunchedEffect(Unit) {
                while (true) {
                    delay(30000)
                    apps = loadApps()
                }
            }
            var locked by remember { mutableStateOf(false) }
            var search by remember { mutableStateOf("") }
            val context = LocalContext.current
            val wallpaper = Brush.linearGradient(
                listOf(Color(0xFF122C58), Color(0xFF546FA6), Color(0xFFB6A7CB), Color(0xFF334D87))
            )

            MaterialTheme {
                Box(Modifier.fillMaxSize().background(wallpaper)) {
                    if (locked) {
                        LockPreview(onUnlock = { locked = false })
                    } else {
                        Column(
                            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                                .padding(horizontal = 14.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 20.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Liquid", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                                Text(
                                    "Lock preview  ◉", color = Color.White, fontSize = 12.sp,
                                    modifier = Modifier.clip(RoundedCornerShape(30.dp))
                                        .background(Color.White.copy(alpha = .18f))
                                        .clickable { locked = true }.padding(12.dp)
                                )
                            }
                            Box(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                                    .background(Color.White.copy(alpha = .22f)).padding(14.dp)
                            ) {
                                if (search.isEmpty()) Text("⌕  Search apps", color = Color.White.copy(alpha = .85f))
                                BasicTextField(
                                    value = search, onValueChange = { search = it }, singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 16.sp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Text("Hold an app to pin or remove it from the dock", color = Color.White.copy(alpha = .85f), fontSize = 11.sp)
                            Spacer(Modifier.height(16.dp))
                            val filtered = remember(apps, search) {
                                apps.filter { it.label.contains(search, ignoreCase = true) }
                            }
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(4),
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(22.dp)
                            ) {
                                items(filtered, key = { it.packageName + "/" + it.activityName }) { app ->
                                    AppTile(app, onLongPress = { chooserApp = app }) {
                                        try {
                                            val launch = Intent(Intent.ACTION_MAIN)
                                                .addCategory(Intent.CATEGORY_LAUNCHER)
                                                .setComponent(ComponentName(app.packageName, app.activityName))
                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            context.startActivity(launch)
                                        } catch (_: Exception) {
                                            android.widget.Toast.makeText(context, "Unable to open app", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                            Row(
                                Modifier.fillMaxWidth().padding(bottom = 12.dp).height(86.dp)
                                    .clip(RoundedCornerShape(34.dp))
                                    .background(Color.White.copy(alpha = .24f)),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                pinnedApps.take(4).forEach { app ->
                                    Image(
                                        bitmap = remember(app.icon) { app.icon.toBitmap().asImageBitmap() },
                                        contentDescription = app.label,
                                        modifier = Modifier.size(54.dp).clickable {
                                            try {
                                                context.startActivity(
                                                    Intent(Intent.ACTION_MAIN)
                                                        .addCategory(Intent.CATEGORY_LAUNCHER)
                                                        .setComponent(ComponentName(app.packageName, app.activityName))
                                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                )
                                            } catch (_: Exception) {}
                                        }
                                    )
                                }
                            }
                        }
                    }
                    chooserApp?.let { target ->
                        val key = target.packageName + "/" + target.activityName
                        val isPinned = pinnedApps.any { it.packageName + "/" + it.activityName == key }
                        AlertDialog(
                            onDismissRequest = { chooserApp = null },
                            title = { Text(target.label) },
                            text = { Text(if (isPinned) "Remove this shortcut from your dock?" else "Pin this app to your dock? Up to four apps can be pinned.") },
                            confirmButton = {
                                TextButton(onClick = {
                                    val current = pinnedApps.map { it.packageName + "/" + it.activityName }.toMutableList()
                                    if (isPinned) current.remove(key) else {
                                        if (current.size >= 4) current.removeAt(current.lastIndex)
                                        current.add(key)
                                    }
                                    pinned = current.joinToString("|")
                                    prefs.edit().putString("dock", pinned).apply()
                                    chooserApp = null
                                }) { Text(if (isPinned) "Remove" else "Pin") }
                            },
                            dismissButton = { TextButton(onClick = { chooserApp = null }) { Text("Cancel") } }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppTile(app: LaunchableApp, onLongPress: () -> Unit, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongPress), horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            bitmap = remember(app.icon) { app.icon.toBitmap().asImageBitmap() },
            contentDescription = app.label, modifier = Modifier.size(58.dp)
        )
        Spacer(Modifier.height(5.dp))
        Text(
            app.label, fontSize = 11.sp, color = Color.White,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LockPreview(onUnlock: () -> Unit) {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(1000L)
        }
    }
    val time = remember(now) { SimpleDateFormat("h:mm", Locale.getDefault()).format(now) }
    val date = remember(now) { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(now) }
    var dragged by remember { mutableFloatStateOf(0f) }
    Box(
        Modifier.fillMaxSize().pointerInput(Unit) {
            detectVerticalDragGestures(
                onVerticalDrag = { _, amount -> dragged += amount },
                onDragEnd = {
                    if (dragged < -90f) onUnlock()
                    dragged = 0f
                }
            )
        }
    ) {
        Column(
            Modifier.fillMaxWidth().statusBarsPadding().padding(top = 78.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("●", color = Color.White.copy(alpha = .9f), fontSize = 22.sp)
            Spacer(Modifier.height(16.dp))
            Text(date, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium)
            Text(time, color = Color.White, fontSize = 90.sp, fontWeight = FontWeight.Light, lineHeight = 100.sp)
        }
        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Swipe up to open", color = Color.White, fontSize = 15.sp, modifier = Modifier.clickable { onUnlock() })
            Spacer(Modifier.height(12.dp))
            Box(Modifier.width(126.dp).height(5.dp).clip(RoundedCornerShape(50)).background(Color.White))
        }
    }
}

private fun Drawable.toBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val oldBounds = bounds
    setBounds(0, 0, 128, 128)
    draw(canvas)
    bounds = oldBounds
    return bitmap
}
