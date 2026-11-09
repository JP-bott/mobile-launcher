package com.liquidlauncher

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val ink = Color.White
private const val APPS_PER_PAGE = 20

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PremiumLauncher(apps: List<LauncherApp>, store: LauncherStore) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val settings = remember { LayoutManager(context) }
    var settingsOpen by remember { mutableStateOf(false) }
    var widgetsOpen by remember { mutableStateOf(false) }
    var settingsVersion by remember { mutableIntStateOf(0) }
    var folders by remember { mutableStateOf(settings.folders()) }
    var openFolderId by remember { mutableStateOf<String?>(null) }
    var folderSource by remember { mutableStateOf<LauncherApp?>(null) }
    val byKey = remember(apps) { apps.associateBy { it.key } }
    var home by remember { mutableStateOf(store.loadKeys("home")) }
    var dock by remember { mutableStateOf(store.loadKeys("dock")) }
    var editing by remember { mutableStateOf(false) }
    var libraryOpen by remember { mutableStateOf(false) }
    var previewOpen by remember { mutableStateOf(false) }
    var optionsFor by remember { mutableStateOf<LauncherApp?>(null) }
    var search by remember { mutableStateOf("") }

    LaunchedEffect(apps) {
        if (apps.isNotEmpty()) {
            val known = apps.map { it.key }.toSet() + folders.map { it.token }
            if (home == null) {
                // Show only the first page on first launch; full catalog remains in App Library.
                home = apps.take(APPS_PER_PAGE).map { it.key }
                store.saveKeys("home", home.orEmpty())
            } else {
                val cleaned = home.orEmpty().filter { it in known }.distinct()
                if (cleaned != home) {
                    home = cleaned
                    store.saveKeys("home", cleaned)
                }
            }
            if (dock == null) {
                val preferred = listOf("dialer", "contacts", "message", "chrome", "camera")
                val chosen = preferred.mapNotNull { hint ->
                    apps.firstOrNull { it.packageName.contains(hint, ignoreCase = true) }
                }.distinctBy { it.key }.take(4)
                dock = (chosen + apps.filterNot { it.key in chosen.map { c -> c.key } })
                    .take(4).map { it.key }
                store.saveKeys("dock", dock.orEmpty())
            } else {
                val cleaned = dock.orEmpty().filter { it in known }.distinct().take(4)
                if (cleaned != dock) {
                    dock = cleaned
                    store.saveKeys("dock", cleaned)
                }
            }
        }
    }

    fun persistHome(list: List<String>) {
        home = list.distinct()
        store.saveKeys("home", home.orEmpty())
    }
    fun persistDock(list: List<String>) {
        dock = list.distinct().take(4)
        store.saveKeys("dock", dock.orEmpty())
    }
    fun launch(app: LauncherApp) {
        if (editing) {
            optionsFor = app
        } else {
            try {
                store.launch(app)
            } catch (_: Exception) {
                Toast.makeText(context, "Unable to launch ${app.label}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val homeKeys = home.orEmpty().filter { (it in byKey || folders.any { folder -> folder.token == it }) && it !in dock.orEmpty() }
    val pageCount = maxOf(1, (homeKeys.size + APPS_PER_PAGE - 1) / APPS_PER_PAGE)
    val pager = rememberPagerState(pageCount = { pageCount })
    BackHandler(editing || libraryOpen || previewOpen || settingsOpen || widgetsOpen || openFolderId != null || pager.currentPage > 0) {
        when {
            previewOpen -> previewOpen = false
            openFolderId != null -> openFolderId = null
            settingsOpen -> settingsOpen = false
            widgetsOpen -> widgetsOpen = false
            libraryOpen -> { libraryOpen = false; search = "" }
            editing -> editing = false
            else -> scope.launch { pager.animateScrollToPage(0) }
        }
    }

    Box(Modifier.fillMaxSize()) {
        // Android's HOME wallpaper is shown behind this transparent window.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(0x66060B16), Color(0x22060B16), Color(0x88060B16)))
            )
        )
        if (previewOpen) {
            LockLook(onDismiss = { previewOpen = false })
        } else {
            Column(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 7.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (editing) {
                        CircleAction("Done") { editing = false }
                        Text("Edit Home Screen", color = ink, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            CircleAction("Widgets") { widgetsOpen = true }
                            CircleAction("＋") { libraryOpen = true }
                        }
                    } else {
                        Spacer(Modifier.width(38.dp))
                        Spacer(Modifier.weight(1f))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircleAction("◈") { previewOpen = true }
                            CircleAction("•••") { settingsOpen = true }
                        }
                    }
                }

                HorizontalPager(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    state = pager,
                    userScrollEnabled = !editing,
                    beyondViewportPageCount = 1
                ) { page ->
                    val pageKeys = homeKeys.drop(page * APPS_PER_PAGE).take(APPS_PER_PAGE)
                    Column(
                        Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (page == 0) {
                            CompactDateHeader()
                        } else Spacer(Modifier.height(26.dp))

                        BoxWithConstraints(Modifier.fillMaxSize()) {
                            val cellWidth = maxWidth / 4
                            val cellHeight = minOf(102.dp, maxHeight / 5)
                            val density = LocalDensity.current
                            val icons = pageKeys
                            Column {
                                icons.chunked(4).forEachIndexed { rowIndex, row ->
                                    Row(
                                        Modifier.fillMaxWidth().height(cellHeight),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        row.forEachIndexed { colIndex, key ->
                                            val app = byKey[key]
                                            val folder = folders.firstOrNull { it.token == key }
                                            Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                                                var dragX by remember(key) { mutableFloatStateOf(0f) }
                                                var dragY by remember(key) { mutableFloatStateOf(0f) }
                                                var dragging by remember(key) { mutableStateOf(false) }
                                                val cellPx = with(density) { cellWidth.toPx() }
                                                val rowPx = with(density) { cellHeight.toPx() }
                                                val activeScale by animateFloatAsState(if (dragging) 1.12f else 1f, animationSpec = spring(), label = "Drag scale")
                                                val gesture = Modifier.pointerInput(key, page) {
                                                    detectDragGesturesAfterLongPress(
                                                        onDragStart = { editing = true; dragging = true; if (settings.haptics) haptics.performHapticFeedback(HapticFeedbackType.LongPress) },
                                                        onDrag = { change, delta ->
                                                            change.consume()
                                                            dragX += delta.x
                                                            dragY += delta.y
                                                        },
                                                        onDragEnd = {
                                                            val newCol = (colIndex + (dragX / cellPx).roundToInt()).coerceIn(0, 3)
                                                            val newRow = (rowIndex + (dragY / rowPx).roundToInt()).coerceIn(0, 4)
                                                            val from = page * APPS_PER_PAGE + rowIndex * 4 + colIndex
                                                            val destination = (page * APPS_PER_PAGE + newRow * 4 + newCol)
                                                                .coerceIn(0, homeKeys.lastIndex.coerceAtLeast(0))
                                                            if (from in homeKeys.indices && destination in homeKeys.indices && from != destination) {
                                                                val reordered = homeKeys.toMutableList()
                                                                val moved = reordered.removeAt(from)
                                                                reordered.add(destination, moved)
                                                                persistHome(reordered + home.orEmpty().filterNot { it in reordered })
                                                            }
                                                            dragX = 0f; dragY = 0f; dragging = false
                                                        },
                                                        onDragCancel = {
                                                            dragX = 0f; dragY = 0f; dragging = false
                                                        }
                                                    )
                                                }
                                                if (app != null) {
                                                    HomeIcon(
                                                        app = app,
                                                        editing = editing,
                                                        showLabel = settings.showLabels,
                                                        iconSize = settings.iconSize,
                                                        onClick = { launch(app) },
                                                        onOptions = { optionsFor = app },
                                                        modifier = gesture
                                                            .offset { IntOffset(dragX.roundToInt(), dragY.roundToInt()) }
                                                            .graphicsLayer { scaleX = activeScale; scaleY = activeScale }
                                                            .zIndex(if (dragging) 10f else 0f)
                                                    )
                                                } else if (folder != null) {
                                                    FolderIcon(
                                                        folder = folder,
                                                        apps = byKey,
                                                        editing = editing,
                                                        modifier = gesture
                                                            .offset { IntOffset(dragX.roundToInt(), dragY.roundToInt()) }
                                                            .graphicsLayer { scaleX = activeScale; scaleY = activeScale }
                                                            .zIndex(if (dragging) 10f else 0f)
                                                    ) { openFolderId = folder.id }
                                                }
                                            }
                                        }
                                        repeat(4 - row.size) {
                                            Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    Modifier.height(26.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(pageCount) { i ->
                        Box(Modifier.padding(horizontal = 4.dp)
                            .size(if (pager.currentPage == i) 7.dp else 6.dp)
                            .clip(CircleShape)
                            .background(ink.copy(alpha = if (pager.currentPage == i) 0.98f else 0.35f)))
                    }
                }

Box(
                    Modifier.padding(top = 9.dp, bottom = 9.dp)
                        .clip(RoundedCornerShape(25.dp))
                        .background(Color(0x44FFFFFF))
                        .clickable { libraryOpen = true }
                        .padding(horizontal = 18.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) { Text("⌕  Search", color = ink, fontSize = 13.sp) }


                Box(
                    Modifier.padding(horizontal = 14.dp)
                        .fillMaxWidth().height(88.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(Color(0x66FFFFFF))
                        .border(0.8.dp, ink.copy(alpha = 0.20f), RoundedCornerShape(30.dp))
                ) {
                    Row(
                        Modifier.fillMaxSize().padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        dock.orEmpty().mapNotNull { byKey[it] }.forEach { app ->
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                HomeIcon(
                                    app = app, editing = editing, showLabel = false, iconSize = settings.iconSize,
                                    onClick = { launch(app) },
                                    onOptions = { optionsFor = app }
                                )
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = libraryOpen && !previewOpen,
            enter = fadeIn(), exit = fadeOut()
        ) {
            Column(
                Modifier.fillMaxSize()
                    .background(Color(0xF21B2639))
                    .statusBarsPadding().navigationBarsPadding()
                    .padding(horizontal = 18.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("App Library", fontSize = 31.sp, fontWeight = FontWeight.Bold, color = ink)
                    CircleAction("Done") { libraryOpen = false; search = "" }
                }
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(Color(0x30FFFFFF)).padding(15.dp)
                ) {
                    if (search.isEmpty()) Text("⌕  Search apps", color = Color(0xBBFFFFFF))
                    BasicTextField(
                        value = search, onValueChange = { search = it },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = ink, fontSize = 16.sp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(24.dp))
                val shown = remember(apps, search) {
                    apps.filter { it.label.contains(search, ignoreCase = true) }
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    items(shown, key = { it.key }) { app ->
                        HomeIcon(app, editing = false, onClick = {
                            libraryOpen = false
                            launch(app)
                        }, onOptions = { optionsFor = app })
                    }
                }
            }
        }

        optionsFor?.let { app ->
            val inDock = app.key in dock.orEmpty()
            val inHome = app.key in home.orEmpty()
            AlertDialog(
                onDismissRequest = { optionsFor = null },
                title = { Text(app.label) },
                text = {
                    Column {
                        TextButton(onClick = {
                            if (inDock) persistDock(dock.orEmpty() - app.key)
                            else persistDock((dock.orEmpty() + app.key).takeLast(4))
                            optionsFor = null
                        }) { Text(if (inDock) "Remove from Dock" else "Add to Dock") }
                        TextButton(onClick = {
                            if (inHome) persistHome(home.orEmpty() - app.key)
                            else persistHome(home.orEmpty() + app.key)
                            optionsFor = null
                        }) { Text(if (inHome) "Remove from Home" else "Add to Home") }
                        if (inHome && !inDock) {
                            TextButton(onClick = {
                                folderSource = app
                                optionsFor = null
                            }) { Text("Create folder with…") }
                            val index = home.orEmpty().indexOf(app.key)
                            if (index >= 0 && home.orEmpty().size > 1) {
                                TextButton(onClick = {
                                    val copy = home.orEmpty().toMutableList()
                                    copy.removeAt(index)
                                    copy.add((index + 20).coerceAtMost(copy.size), app.key)
                                    persistHome(copy)
                                    optionsFor = null
                                }) { Text("Move toward next page") }
                            }
                        }
                        Text("The app remains available in App Library.", fontSize = 12.sp)
                    }
                },
                confirmButton = { TextButton(onClick = { optionsFor = null }) { Text("Done") } }
            )
        }
        if (widgetsOpen) {
            WidgetPanel(onClose = { widgetsOpen = false })
        }
        if (settingsOpen) {
            LauncherSettings(settings) {
                settingsOpen = false
                settingsVersion++
            }
        }
        val displayedFolder = folders.firstOrNull { it.id == openFolderId }
        if (displayedFolder != null) {
            FolderSheet(
                folder = displayedFolder,
                appMap = byKey,
                onLaunch = { launch(it); openFolderId = null },
                onClose = { openFolderId = null },
                onRename = { title ->
                    folders = folders.map { if (it.id == displayedFolder.id) it.copy(title = title) else it }
                    settings.saveFolders(folders)
                },
                onRemoveApp = { key ->
                    val remaining = displayedFolder.apps - key
                    folders = folders.filterNot { it.id == displayedFolder.id }
                    val updated = home.orEmpty().toMutableList()
                    val idx = updated.indexOf(displayedFolder.token)
                    if (idx >= 0) {
                        updated.removeAt(idx)
                        if (remaining.size <= 1) {
                            remaining.firstOrNull()?.let { updated.add(idx, it) }
                        } else {
                            folders = folders + displayedFolder.copy(apps = remaining)
                            updated.add(idx, displayedFolder.token)
                        }
                        updated.add(key)
                        persistHome(updated)
                    }
                    settings.saveFolders(folders)
                    if (remaining.size <= 1) openFolderId = null
                }
            )
        }
        folderSource?.let { source ->
            val candidates = home.orEmpty().mapNotNull { byKey[it] }.filter { it.key != source.key }
            AlertDialog(
                onDismissRequest = { folderSource = null },
                title = { Text("Create a folder") },
                text = {
                    Column(Modifier.heightIn(max = 350.dp).verticalScroll(rememberScrollState())) {
                        candidates.forEach { target ->
                            TextButton(onClick = {
                                val folder = settings.createFolder(source.key, target.key)
                                folders = folders + folder
                                settings.saveFolders(folders)
                                val updated = home.orEmpty().toMutableList()
                                val first = updated.indexOf(source.key)
                                updated.remove(source.key)
                                updated.remove(target.key)
                                updated.add(first.coerceIn(0, updated.size), folder.token)
                                persistHome(updated)
                                folderSource = null
                                openFolderId = folder.id
                            }) { Text(target.label) }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { folderSource = null }) { Text("Cancel") } }
            )
        }

    }
}

@Composable
private fun CircleAction(text: String, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(23.dp))
            .background(Color(0x44FFFFFF))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp)
    ) { Text(text, color = ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun CompactDateHeader() {
    val now = liveClock()
    val date = remember(now) { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(now) }
    Row(
        Modifier.fillMaxWidth().height(66.dp).padding(horizontal = 16.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(date, fontSize = 15.sp, color = ink.copy(alpha = .95f), fontWeight = FontWeight.Medium)
            Text("Have a great day", fontSize = 12.sp, color = ink.copy(alpha = .8f))
        }
        Text("✦", fontSize = 27.sp, color = ink.copy(alpha = .85f))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeIcon(
    app: LauncherApp,
    editing: Boolean,
    onClick: () -> Unit,
    onOptions: () -> Unit,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
    iconSize: Int = 61
) {
    Column(
        modifier.clickable { onClick() }
            .padding(top = 5.dp)
            .width(80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Image(
                bitmap = remember(app.key) { app.icon.bitmapForIcon(160).asImageBitmap() },
                contentDescription = app.label,
                modifier = Modifier.size(iconSize.dp)
                    .shadow(3.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
            )
            if (editing) {
                Box(Modifier.offset(x = 5.dp, y = (-5).dp)
                    .size(20.dp).clip(CircleShape).background(Color(0xFFEB4B5B))
                    .clickable { onOptions() }, contentAlignment = Alignment.Center
                ) {
                    Text("−", color = ink, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            }
        }
        if (showLabel) {
            Spacer(Modifier.height(5.dp))
            Text(
                app.label, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FolderIcon(
    folder: HomeFolder,
    apps: Map<String, LauncherApp>,
    editing: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier.width(80.dp).clickable(onClick = onClick).padding(top = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            Modifier.size(61.dp).clip(RoundedCornerShape(17.dp))
                .background(Color.White.copy(alpha = .36f)).padding(7.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            folder.apps.take(4).chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    row.forEach { key ->
                        apps[key]?.let { app ->
                            Image(
                                bitmap = remember(key) { app.icon.bitmapForIcon(80).asImageBitmap() },
                                contentDescription = app.label,
                                modifier = Modifier.size(21.dp).clip(RoundedCornerShape(5.dp))
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(folder.title, color = Color.White, fontSize = 11.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun liveClock(): Date {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(10000)
        }
    }
    return now
}

@Composable
private fun LockLook(onDismiss: () -> Unit) {
    val now = liveClock()
    val time = remember(now) { SimpleDateFormat("h:mm", Locale.getDefault()).format(now) }
    val date = remember(now) { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(now) }
    var swipe by remember { mutableFloatStateOf(0f) }
    Box(
        Modifier.fillMaxSize().pointerInput(Unit) {
            detectVerticalDragGestures(
                onVerticalDrag = { change, delta -> change.consume(); swipe += delta },
                onDragEnd = {
                    if (swipe < -80f) onDismiss()
                    swipe = 0f
                }
            )
        }
    ) {
        Column(
            Modifier.fillMaxWidth().statusBarsPadding().padding(top = 85.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("◈", fontSize = 17.sp, color = ink)
            Spacer(Modifier.height(16.dp))
            Text(date, color = ink, fontSize = 18.sp)
            Text(time, color = ink, fontSize = 94.sp, fontWeight = FontWeight.Light, lineHeight = 104.sp)
        }
        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 35.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Swipe up to open preview", color = ink, fontSize = 14.sp,
                modifier = Modifier.clickable { onDismiss() })
            Spacer(Modifier.height(15.dp))
            Box(Modifier.width(115.dp).height(5.dp).clip(CircleShape).background(ink))
        }
    }
}
