package com.liquidlauncher

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val white = Color.White
private const val PAGE_CAPACITY = 20

class MainActivity : ComponentActivity() {
    private var refreshNumber by mutableIntStateOf(0)

    override fun onResume() {
        super.onResume()
        refreshNumber++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = LauncherStore(applicationContext)
        setContent {
            MaterialTheme {
                val apps by produceState<List<LauncherApp>>(emptyList(), refreshNumber) {
                    value = withContext(Dispatchers.IO) { store.installedApps() }
                }
                LauncherScreen(apps, store)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LauncherScreen(apps: List<LauncherApp>, store: LauncherStore) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appMap = remember(apps) { apps.associateBy { it.key } }
    var layout by remember { mutableStateOf(store.loadKeys("home")) }
    var dock by remember { mutableStateOf(store.loadKeys("dock")) }
    var lockPreview by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<LauncherApp?>(null) }
    var search by remember { mutableStateOf("") }

    LaunchedEffect(apps) {
        if (apps.isNotEmpty()) {
            val available = apps.map { it.key }.toSet()
            if (layout == null) {
                layout = apps.map { it.key }
                store.saveKeys("home", layout.orEmpty())
            } else {
                val merged = layout.orEmpty().filter { it in available }
                // Newly installed apps appear in the library; they can be added to Home.
                if (merged != layout) {
                    layout = merged
                    store.saveKeys("home", merged)
                }
            }
            if (dock == null) {
                dock = apps.take(4).map { it.key }
                store.saveKeys("dock", dock.orEmpty())
            } else {
                val present = dock.orEmpty().filter { it in available }.distinct().take(4)
                if (present != dock) {
                    dock = present
                    store.saveKeys("dock", present)
                }
            }
        }
    }

    val homeKeys = layout.orEmpty().filter { it in appMap && it !in dock.orEmpty() }
    val pageCount = maxOf(1, (homeKeys.size + PAGE_CAPACITY - 1) / PAGE_CAPACITY)
    val pager = rememberPagerState(pageCount = { pageCount + 1 })
    val libraryPage = pageCount

    fun launch(app: LauncherApp) {
        try {
            store.launch(app)
        } catch (_: Exception) {
            Toast.makeText(context, "Unable to open ${app.label}", Toast.LENGTH_SHORT).show()
        }
    }

    fun saveHome(values: List<String>) {
        layout = values.distinct()
        store.saveKeys("home", layout.orEmpty())
    }

    fun saveDock(values: List<String>) {
        dock = values.distinct().take(4)
        store.saveKeys("dock", dock.orEmpty())
    }

    BackHandler(enabled = lockPreview || pager.currentPage != 0 || search.isNotEmpty()) {
        when {
            lockPreview -> lockPreview = false
            search.isNotEmpty() -> search = ""
            else -> scope.launch { pager.animateScrollToPage(0) }
        }
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.linearGradient(
                listOf(Color(0xFF102A51), Color(0xFF2F548D), Color(0xFF7778B0), Color(0xFFA88EA9))
            )
        )
    ) {
        // Subtle atmospheric depth, with no expensive per-frame blur.
        Box(Modifier.size(330.dp).offset(x = (-105).dp, y = 95.dp)
            .background(Brush.radialGradient(listOf(Color(0x5588C6FF), Color.Transparent)), CircleShape))
        Box(Modifier.size(320.dp).align(Alignment.BottomEnd).offset(x = 105.dp, y = (-100).dp)
            .background(Brush.radialGradient(listOf(Color(0x66F3B2E9), Color.Transparent)), CircleShape))

        if (lockPreview) {
            LockScreenPreview(onDismiss = { lockPreview = false })
        } else {
            Column(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                    .padding(horizontal = 14.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Liquid", fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = white)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlassAction("⌕ Search") {
                            scope.launch { pager.animateScrollToPage(libraryPage) }
                        }
                        GlassAction("◈ Lock") { lockPreview = true }
                    }
                }

                HorizontalPager(
                    state = pager,
                    modifier = Modifier.weight(1f),
                    beyondViewportPageCount = 1
                ) { page ->
                    if (page == libraryPage) {
                        Column(Modifier.fillMaxSize()) {
                            Text(
                                "App Library", color = white,
                                fontSize = 30.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 6.dp, top = 8.dp, bottom = 12.dp)
                            )
                            OutlinedTextField(
                                value = search,
                                onValueChange = { search = it },
                                singleLine = true,
                                placeholder = { Text("Search your apps", color = white.copy(alpha = .75f)) },
                                shape = RoundedCornerShape(22.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = white, unfocusedTextColor = white,
                                    focusedContainerColor = white.copy(alpha = .16f),
                                    unfocusedContainerColor = white.copy(alpha = .13f),
                                    focusedIndicatorColor = white.copy(alpha = .45f),
                                    unfocusedIndicatorColor = white.copy(alpha = .20f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(16.dp))
                            val filtered = remember(apps, search) {
                                apps.filter { it.label.contains(search, ignoreCase = true) }
                            }
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(4),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(20.dp),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 18.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(filtered, key = { it.key }) { app ->
                                    AppShortcut(app, onClick = { launch(app) }, onHold = { selected = app })
                                }
                            }
                        }
                    } else {
                        Column(Modifier.fillMaxSize()) {
                            if (page == 0) {
                                ClockWidget(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 20.dp))
                            } else {
                                Spacer(Modifier.height(8.dp))
                            }
                            val pageApps = homeKeys.drop(page * PAGE_CAPACITY).take(PAGE_CAPACITY)
                                .mapNotNull { appMap[it] }
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(4),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(23.dp),
                                userScrollEnabled = true,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(top = 4.dp, bottom = 14.dp)
                            ) {
                                items(pageApps, key = { it.key }) { app ->
                                    AppShortcut(app, onClick = { launch(app) }, onHold = { selected = app })
                                }
                            }
                        }
                    }
                }

                Row(
                    Modifier.fillMaxWidth().height(27.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(pageCount + 1) { index ->
                        Box(
                            Modifier.padding(horizontal = 4.dp)
                                .size(if (index == pager.currentPage) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(white.copy(alpha = if (index == pager.currentPage) 1f else .4f))
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .height(88.dp)
                        .clip(RoundedCornerShape(34.dp))
                        .background(white.copy(alpha = .19f))
                        .border(1.dp, white.copy(alpha = .34f), RoundedCornerShape(34.dp))
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dock.orEmpty().mapNotNull { appMap[it] }.forEach { app ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            AppImage(app, size = 58, modifier = Modifier
                                .clip(RoundedCornerShape(17.dp))
                                .clickable { launch(app) })
                        }
                    }
                }
            }
        }

        selected?.let { app ->
            val inDock = app.key in dock.orEmpty()
            val inHome = app.key in layout.orEmpty()
            val homeList = layout.orEmpty()
            val index = homeList.indexOf(app.key)
            AlertDialog(
                onDismissRequest = { selected = null },
                title = { Text(app.label) },
                text = {
                    Column {
                        Text("Customize shortcut")
                        Spacer(Modifier.height(8.dp))
                        if (inDock) TextButton(onClick = {
                            saveDock(dock.orEmpty() - app.key)
                            saveHome(layout.orEmpty() + app.key)
                            selected = null
                        }) { Text("Remove from Dock") }
                        else TextButton(onClick = {
                            saveDock((dock.orEmpty() + app.key).takeLast(4))
                            selected = null
                        }) { Text("Add to Dock") }

                        if (inHome) {
                            TextButton(onClick = {
                                saveHome(homeList - app.key)
                                selected = null
                            }) { Text("Remove from Home (keep in Library)") }
                            if (index > 0) {
                                TextButton(onClick = {
                                    val updated = homeList.toMutableList()
                                    updated.removeAt(index)
                                    updated.add(index - 1, app.key)
                                    saveHome(updated)
                                    selected = null
                                }) { Text("Move earlier") }
                            }
                            if (index < homeList.lastIndex) {
                                TextButton(onClick = {
                                    val updated = homeList.toMutableList()
                                    updated.removeAt(index)
                                    updated.add(index + 1, app.key)
                                    saveHome(updated)
                                    selected = null
                                }) { Text("Move later") }
                            }
                            TextButton(onClick = {
                                val updated = homeList.toMutableList()
                                updated.remove(app.key)
                                updated.add(minOf(((index / PAGE_CAPACITY) + 1) * PAGE_CAPACITY, updated.size), app.key)
                                saveHome(updated)
                                selected = null
                            }) { Text("Move toward next page") }
                        } else {
                            TextButton(onClick = {
                                saveHome(homeList + app.key)
                                selected = null
                            }) { Text("Add to Home") }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { selected = null }) { Text("Done") } }
            )
        }
    }
}

@Composable
private fun GlassAction(label: String, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(22.dp))
            .background(white.copy(alpha = .17f))
            .border(1.dp, white.copy(alpha = .26f), RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) { Text(label, color = white, fontSize = 12.sp) }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppShortcut(app: LauncherApp, onClick: () -> Unit, onHold: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onHold),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppImage(app, 60, Modifier.clip(RoundedCornerShape(18.dp)))
        Spacer(Modifier.height(6.dp))
        Text(
            app.label, color = white, fontSize = 11.sp, fontWeight = FontWeight.Medium,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AppImage(app: LauncherApp, size: Int, modifier: Modifier = Modifier) {
    val bitmap = remember(app.key) { app.icon.bitmapForIcon().asImageBitmap() }
    Image(bitmap = bitmap, contentDescription = app.label, modifier = modifier.size(size.dp))
}

@Composable
private fun ClockWidget(modifier: Modifier = Modifier) {
    val now = rememberMinuteClock()
    val hour = remember(now) { SimpleDateFormat("h:mm", Locale.getDefault()).format(now) }
    val date = remember(now) { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(now) }
    Row(
        modifier.height(132.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(white.copy(alpha = .14f))
            .border(1.dp, white.copy(alpha = .26f), RoundedCornerShape(30.dp))
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("Today", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = white.copy(alpha = .86f))
            Text(hour, fontSize = 45.sp, fontWeight = FontWeight.Light, color = white)
            Text(date, fontSize = 12.sp, color = white.copy(alpha = .86f))
        }
        Text("✦", fontSize = 46.sp, color = white.copy(alpha = .65f))
    }
}

@Composable
private fun rememberMinuteClock(): Date {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(10000L)
        }
    }
    return now
}

@Composable
private fun LockScreenPreview(onDismiss: () -> Unit) {
    val now = rememberMinuteClock()
    val hour = remember(now) { SimpleDateFormat("h:mm", Locale.getDefault()).format(now) }
    val date = remember(now) { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(now) }
    var dragDistance by remember { mutableFloatStateOf(0f) }
    Box(
        Modifier.fillMaxSize().pointerInput(Unit) {
            detectVerticalDragGestures(
                onVerticalDrag = { _, drag -> dragDistance += drag },
                onDragEnd = {
                    if (dragDistance < -80f) onDismiss()
                    dragDistance = 0f
                }
            )
        }
    ) {
        Column(
            Modifier.fillMaxWidth().statusBarsPadding().padding(top = 65.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("◈", color = white.copy(alpha = .85f), fontSize = 22.sp)
            Spacer(Modifier.height(12.dp))
            Text(date, color = white, fontSize = 19.sp, fontWeight = FontWeight.Medium)
            Text(hour, color = white, fontSize = 91.sp, fontWeight = FontWeight.Light, lineHeight = 104.sp)
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier.clip(RoundedCornerShape(22.dp))
                    .background(white.copy(alpha = .13f))
                    .border(1.dp, white.copy(alpha = .25f), RoundedCornerShape(22.dp))
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) { Text("Preview • Android security stays active", color = white, fontSize = 12.sp) }
        }
        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Swipe up to open", color = white, fontSize = 15.sp, modifier = Modifier.clickable { onDismiss() })
            Spacer(Modifier.height(17.dp))
            Box(Modifier.width(120.dp).height(5.dp).clip(CircleShape).background(white))
        }
    }
}
