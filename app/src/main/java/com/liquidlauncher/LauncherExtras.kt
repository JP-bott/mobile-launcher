package com.liquidlauncher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LauncherSettings(
    manager: LayoutManager,
    onClose: () -> Unit
) {
    var labels by remember { mutableStateOf(manager.showLabels) }
    var scrim by remember { mutableStateOf(manager.darkScrim) }
    var haptics by remember { mutableStateOf(manager.haptics) }
    var size by remember { mutableFloatStateOf(manager.iconSize.toFloat()) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Home Screen Settings") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Show app labels", Modifier.weight(1f))
                    Switch(checked = labels, onCheckedChange = {
                        labels = it; manager.showLabels = it
                    })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Darken wallpaper", Modifier.weight(1f))
                    Switch(checked = scrim, onCheckedChange = {
                        scrim = it; manager.darkScrim = it
                    })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Drag haptics", Modifier.weight(1f))
                    Switch(checked = haptics, onCheckedChange = {
                        haptics = it; manager.haptics = it
                    })
                }
                Spacer(Modifier.height(12.dp))
                Text("Icon size: ${size.toInt()} dp")
                Slider(
                    value = size, valueRange = 48f..70f, steps = 21,
                    onValueChange = { size = it },
                    onValueChangeFinished = { manager.iconSize = size.toInt() }
                )
                Text("Changes are saved on your device.", fontSize = 12.sp)
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Done") } }
    )
}

@Composable
fun FolderSheet(
    folder: HomeFolder,
    appMap: Map<String, LauncherApp>,
    onLaunch: (LauncherApp) -> Unit,
    onClose: () -> Unit,
    onRename: (String) -> Unit,
    onRemoveApp: (String) -> Unit
) {
    var title by remember(folder.id) { mutableStateOf(folder.title) }
    AlertDialog(
        onDismissRequest = onClose,
        title = {
            OutlinedTextField(value = title, onValueChange = {
                title = it.take(30)
                onRename(title)
            }, singleLine = true, label = { Text("Folder name") })
        },
        text = {
            Column {
                Text("Tap an app to open it. Remove an app to place it back on Home.", fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                val folderApps = folder.apps.mapNotNull { appMap[it] }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.heightIn(max = 350.dp)
                ) {
                    items(folderApps, key = { it.key }) { app ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(6.dp)
                        ) {
                            androidx.compose.foundation.Image(
                                bitmap = remember(app.key) { app.icon.bitmapForIcon().asImageBitmap() },
                                contentDescription = app.label,
                                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(15.dp))
                                    .clickable { onLaunch(app) }
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(app.label, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            TextButton(onClick = { onRemoveApp(app.key) }) { Text("Remove", fontSize = 10.sp) }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Done") } }
    )
}
