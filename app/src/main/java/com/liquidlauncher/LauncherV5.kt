package com.liquidlauncher

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
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
import kotlin.math.abs
import kotlin.math.roundToInt

private val white = Color.White

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LauncherV5(apps: List<LauncherApp>, store: LauncherStore) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = remember { LayoutManager(context) }
    val positions = remember { PositionRepository(context) }
    val haptic = LocalHapticFeedback.current
    val byKey = remember(apps) { apps.associateBy { it.key } }
    var folders by remember { mutableStateOf(settings.folders()) }
    var tiles by remember { mutableStateOf(positions.load()) }
    var dock by remember { mutableStateOf(store.loadKeys("dock").orEmpty()) }
    var settingsRevision by remember { mutableIntStateOf(0) }
    var settingsShown by remember { mutableStateOf(false) }
    var libraryShown by remember { mutableStateOf(false) }
    var widgetShown by remember { mutableStateOf(false) }
    var lockShown by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<LauncherApp?>(null) }
    var folderOpen by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }
    val visual = remember(settingsRevision) { Triple(settings.showLabels, settings.iconSize, settings.tintIcons) }

    fun save(next: List<HomeTile>) {
        tiles = next
        positions.save(next)
    }

    fun setDock(keys: List<String>) {
        dock = keys.distinct().take(4)
        store.saveKeys("dock", dock)
    }

    fun launch(app: LauncherApp) {
        try { store.launch(app) }
        catch (_: Exception) { Toast.makeText(context, "Cannot open ${app.label}", Toast.LENGTH_SHORT).show() }
    }

    LaunchedEffect(apps) {
        if (apps.isNotEmpty()) {
            val valid = apps.map { it.key }.toSet() + folders.map { it.token }
            if (tiles == null) {
                // Migrate the previous launcher layout without discarding user choices.
                val old = store.loadKeys("home")
                val initial = (old ?: apps.take(20).map { it.key }).filter { it in valid }
                save(PositionEngine.fromOrdered(initial))
            } else {
                val cleaned = PositionEngine.prune(tiles.orEmpty(), valid)
                if (cleaned != tiles) save(cleaned)
            }
            if (dock.isEmpty() && store.loadKeys("dock") == null) {
                val hints = listOf("dialer", "messag", "browser", "camera")
                val preferred = hints.mapNotNull { hint ->
                    apps.firstOrNull { it.packageName.contains(hint, true) || it.label.contains(hint, true) }
                }.distinctBy { it.key }
                setDock((preferred + apps).distinctBy { it.key }.take(4).map { it.key })
            } else {
                val clean = dock.filter { it in valid }
                if (clean != dock) setDock(clean)
            }
        }
    }

    val pages = maxOf(1, (tiles.orEmpty().maxOfOrNull { it.page } ?: 0) + 1)
    val pager = rememberPagerState(pageCount = { pages + if (editing) 1 else 0 })
    BackHandler(settingsShown || libraryShown || widgetShown || lockShown || editing || folderOpen != null || pager.currentPage > 0) {
        when {
            folderOpen != null -> folderOpen = null
            settingsShown -> { settingsShown = false; settingsRevision++ }
            widgetShown -> widgetShown = false
            libraryShown -> { libraryShown = false; search = "" }
            lockShown -> lockShown = false
            editing -> editing = false
            else -> scope.launch { pager.animateScrollToPage(0) }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            if (settings.darkScrim) listOf(Color(0x550C1428), Color(0x110C1428), Color(0x90080F21))
            else listOf(Color(0x110C1428), Color.Transparent, Color(0x55080F21))
        )))

        if (lockShown) {
            PreviewLock { lockShown = false }
        } else {
            Column(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 21.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (editing) {
                        GlassChip("Done") { editing = false }
                        Text("Edit Home Screen", color = white, fontSize = 13.sp)
                        GlassChip("Widgets") { widgetShown = true }
                    } else {
                        Spacer(Modifier.width(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GlassChip("◈") { lockShown = true }
                            GlassChip("•••") { settingsShown = true }
                        }
                    }
                }

                HorizontalPager(
                    state = pager, modifier = Modifier.weight(1f),
                    userScrollEnabled = !editing
                ) { page ->
                    val visible = tiles.orEmpty().filter { it.page == page }
                    BoxWithConstraints(
                        Modifier.fillMaxSize().padding(horizontal = 13.dp)
                    ) {
                        val cellW = maxWidth / PositionEngine.COLUMNS
                        val cellH = minOf(102.dp, maxHeight / PositionEngine.ROWS)
                        val density = LocalDensity.current
                        val cwPx = with(density) { cellW.toPx() }
                        val chPx = with(density) { cellH.toPx() }
                        if (page == pages && editing) {
                            Box(Modifier.fillMaxSize().clickable {
                                // New empty page becomes permanent when an icon is moved onto it.
                                editing = false
                            }, contentAlignment = Alignment.Center) {
                                Text("Drag an icon here to create a page", color = white.copy(alpha = .8f))
                            }
                        }
                        visible.forEach { tile ->
                            val app = byKey[tile.id]
                            val folder = folders.firstOrNull { it.token == tile.id }
                            var dx by remember(tile.id) { mutableFloatStateOf(0f) }
                            var dy by remember(tile.id) { mutableFloatStateOf(0f) }
                            var dragging by remember(tile.id) { mutableStateOf(false) }
                            val scale by animateFloatAsState(if (dragging) 1.12f else 1f, spring(), label = "iconLift")
                            val latestTiles by rememberUpdatedState(tiles)
                            val gesture = Modifier.pointerInput(tile.id, page) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        editing = true
                                        dragging = true
                                        if (settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dx += amount.x; dy += amount.y
                                    },
                                    onDragEnd = {
                                        val targetCol = (tile.column + (dx / cwPx).roundToInt()).coerceIn(0, 3)
                                        val targetRow = (tile.row + (dy / chPx).roundToInt()).coerceIn(0, 4)
                                        val edgePage = when {
                                            dx > cwPx * 1.7f -> page + 1
                                            dx < -cwPx * 1.7f && page > 0 -> page - 1
                                            else -> page
                                        }
                                        val target = latestTiles.orEmpty().firstOrNull {
                                            it.id != tile.id && it.page == edgePage &&
                                                (targetRow to targetCol) in PositionEngine.occupies(it)
                                        }
                                        val existing = latestTiles.orEmpty()
                                        when {
                                            target != null && tile.type == TileType.APP &&
                                                target.type == TileType.APP &&
                                                abs(dx / cwPx - (target.column - tile.column)) < .35f &&
                                                abs(dy / chPx - (target.row - tile.row)) < .35f -> {
                                                val newFolder = settings.createFolder(tile.id, target.id)
                                                folders = folders + newFolder
                                                settings.saveFolders(folders)
                                                save(PositionEngine.createFolder(existing, tile.id, target.id, newFolder.token))
                                            }
                                            target != null -> save(PositionEngine.swap(existing, tile.id, target.id))
                                            else -> {
                                                val moved = PositionEngine.move(existing, tile.id, edgePage, targetRow, targetCol)
                                                if (moved != existing) {
                                                    save(moved)
                                                    if (edgePage != page) scope.launch { pager.animateScrollToPage(edgePage) }
                                                }
                                            }
                                        }
                                        dx = 0f; dy = 0f; dragging = false
                                    },
                                    onDragCancel = { dx = 0f; dy = 0f; dragging = false }
                                )
                            }
                            Box(
                                Modifier.offset(x = cellW * tile.column, y = cellH * tile.row)
                                    .size(cellW, cellH)
                                    .offset { IntOffset(dx.roundToInt(), dy.roundToInt()) }
                                    .graphicsLayer { scaleX = scale; scaleY = scale }
                                    .zIndex(if (dragging) 5f else 0f)
                                    .then(gesture),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                if (app != null) {
                                    V5Shortcut(app, editing, visual.first, visual.second, visual.third,
                                        onClick = { if (editing) selected = app else launch(app) },
                                        onOptions = { selected = app })
                                } else if (folder != null) {
                                    V5Folder(folder, byKey) { folderOpen = folder.id }
                                }
                            }
                        }
                    }
                }
                Row(
                    Modifier.height(24.dp), horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pages) { index ->
                        Box(
                            Modifier.padding(horizontal = 4.dp)
                                .size(if (pager.currentPage == index) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(white.copy(alpha = if (pager.currentPage == index) 1f else .40f))
                        )
                    }
                }
                Box(
                    Modifier.padding(horizontal = 15.dp).fillMaxWidth().height(82.dp)
                        .clip(RoundedCornerShape(29.dp))
                        .background(white.copy(alpha = .23f))
                        .border(.7.dp, white.copy(alpha = .20f), RoundedCornerShape(29.dp))
                ) {
                    Row(
                        Modifier.fillMaxSize().padding(horizontal = 9.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        dock.mapNotNull { byKey[it] }.forEach { app ->
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                V5Shortcut(app, editing, false, visual.second, visual.third,
                                    onClick = { if (editing) selected = app else launch(app) },
                                    onOptions = { selected = app })
                            }
                        }
                    }
                }
                Box(
                    Modifier.padding(top = 9.dp, bottom = 8.dp)
                        .clip(CircleShape).background(white.copy(alpha = .20f))
                        .clickable { libraryShown = true }
                        .padding(horizontal = 23.dp, vertical = 7.dp)
                ) { Text("⌕  Search", color = white, fontSize = 13.sp) }
            }
        }

        AnimatedVisibility(libraryShown && !lockShown, enter = fadeIn(), exit = fadeOut()) {
            Column(
                Modifier.fillMaxSize().background(Color(0xF21A2437))
                    .statusBarsPadding().navigationBarsPadding()
                    .padding(horizontal = 17.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("App Library", color = white, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    GlassChip("Done") { libraryShown = false; search = "" }
                }
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(white.copy(alpha = .13f)).padding(14.dp)
                ) {
                    if (search.isEmpty()) Text("⌕  Search applications", color = white.copy(alpha = .65f))
                    BasicTextField(
                        search, onValueChange = { search = it }, singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = white, fontSize = 16.sp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 15.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (listOf("All") + apps.map { it.category }.distinct().sorted()).forEach { label ->
                        GlassChip(label) { category = label }
                    }
                }
                val filtered = remember(apps, search, category) {
                    apps.filter { it.label.contains(search, ignoreCase = true) && (category == "All" || it.category == category) }
                }
                LazyVerticalGrid(
                    GridCells.Fixed(4), horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(19.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filtered, key = { it.key }) { app ->
                        V5Shortcut(app, false, true, visual.second, visual.third,
                            onClick = { libraryShown = false; launch(app) },
                            onOptions = { selected = app })
                    }
                }
            }
        }

        selected?.let { app ->
            val inDock = app.key in dock
            val onHome = tiles.orEmpty().any { it.id == app.key }
            AlertDialog(
                onDismissRequest = { selected = null },
                title = { Text(app.label) },
                text = {
                    Column {
                        TextButton(onClick = {
                            if (inDock) setDock(dock - app.key)
                            else setDock((dock + app.key).takeLast(4))
                            selected = null
                        }) { Text(if (inDock) "Remove from Dock" else "Add to Dock") }
                        TextButton(onClick = {
                            if (onHome) save(tiles.orEmpty().filterNot { it.id == app.key })
                            else save(PositionEngine.insert(tiles.orEmpty(), app.key))
                            selected = null
                        }) { Text(if (onHome) "Remove from Home" else "Add to Home") }
                        Text("Long-press and drag shortcuts to reposition or combine them into folders.", fontSize = 12.sp)
                    }
                },
                confirmButton = { TextButton(onClick = { selected = null }) { Text("Done") } }
            )
        }
        val activeFolder = folders.firstOrNull { it.id == folderOpen }
        if (activeFolder != null) {
            FolderSheet(
                folder = activeFolder, appMap = byKey,
                onLaunch = { launch(it); folderOpen = null },
                onClose = { folderOpen = null },
                onRename = { value ->
                    folders = folders.map { if (it.id == activeFolder.id) it.copy(title = value) else it }
                    settings.saveFolders(folders)
                },
                onRemoveApp = { key ->
                    val remaining = activeFolder.apps - key
                    folders = folders.filterNot { it.id == activeFolder.id }
                    if (remaining.size > 1) folders = folders + activeFolder.copy(apps = remaining)
                    settings.saveFolders(folders)
                    var next = tiles.orEmpty()
                    if (remaining.size <= 1) {
                        val original = next.firstOrNull { it.id == activeFolder.token }
                        next = next.filterNot { it.id == activeFolder.token }
                        remaining.firstOrNull()?.let { survivor ->
                            if (original != null) next = next + original.copy(id = survivor, type = TileType.APP)
                        }
                        folderOpen = null
                    }
                    save(PositionEngine.insert(next, key))
                }
            )
        }
        if (settingsShown) LauncherSettings(settings) {
            settingsShown = false
            settingsRevision++
        }
        if (widgetShown) WidgetPanel { widgetShown = false }
    }
}

@Composable
private fun GlassChip(label: String, onClick: () -> Unit) {
    Box(
        Modifier.clip(CircleShape)
            .background(white.copy(alpha = .20f))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp)
    ) { Text(label, color = white, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun V5Shortcut(
    app: LauncherApp,
    editing: Boolean,
    label: Boolean,
    size: Int,
    themed: Boolean,
    onClick: () -> Unit,
    onOptions: () -> Unit
) {
    Column(
        Modifier.width(82.dp).clickable(onClick = onClick).padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            ThemedAppIcon(app, size, themed)
            val count = LauncherNotificationService.badgeCounts[app.packageName] ?: 0
            if (count > 0 && !editing) {
                Box(
                    Modifier.offset(x = 6.dp, y = (-5).dp)
                        .clip(CircleShape).background(Color(0xFFEA4754))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) { Text(if (count > 99) "99+" else count.toString(), color = white, fontSize = 11.sp) }
            }
            if (editing) {
                Box(
                    Modifier.offset(x = 5.dp, y = (-5).dp).size(21.dp)
                        .clip(CircleShape).background(Color(0xFFF05762))
                        .clickable { onOptions() }, contentAlignment = Alignment.Center
                ) { Text("−", color = white, fontSize = 17.sp) }
            }
        }
        if (label) {
            Spacer(Modifier.height(5.dp))
            Text(
                app.label, color = white, fontSize = 11.sp, maxLines = 1,
                textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun V5Folder(folder: HomeFolder, apps: Map<String, LauncherApp>, onClick: () -> Unit) {
    Column(
        Modifier.width(82.dp).clickable(onClick = onClick).padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            Modifier.size(61.dp).clip(RoundedCornerShape(17.dp))
                .background(white.copy(alpha = .32f)).padding(7.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            folder.apps.take(4).chunked(2).forEach { group ->
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    group.mapNotNull { apps[it] }.forEach { app ->
                        Image(
                            bitmap = remember(app.key) { app.icon.bitmapForIcon(80).asImageBitmap() },
                            contentDescription = null,
                            modifier = Modifier.size(21.dp).clip(RoundedCornerShape(5.dp))
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(folder.title, color = white, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PreviewLock(onClose: () -> Unit) {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) { while (true) { now = Date(); delay(10000L) } }
    val time = remember(now) { SimpleDateFormat("h:mm", Locale.getDefault()).format(now) }
    val date = remember(now) { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(now) }
    var swipe by remember { mutableFloatStateOf(0f) }
    Box(
        Modifier.fillMaxSize().pointerInput(Unit) {
            detectVerticalDragGestures(
                onVerticalDrag = { change, delta -> change.consume(); swipe += delta },
                onDragEnd = { if (swipe < -80f) onClose(); swipe = 0f }
            )
        }
    ) {
        Column(
            Modifier.fillMaxWidth().statusBarsPadding().padding(top = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("◈", color = white, fontSize = 18.sp)
            Spacer(Modifier.height(12.dp))
            Text(date, color = white, fontSize = 19.sp)
            Text(time, color = white, fontSize = 92.sp, fontWeight = FontWeight.Light)
        }
        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Swipe up to return", color = white, fontSize = 14.sp,
                modifier = Modifier.clickable { onClose() })
            Spacer(Modifier.height(13.dp))
            Box(Modifier.width(115.dp).height(5.dp).clip(CircleShape).background(white))
        }
    }
}
