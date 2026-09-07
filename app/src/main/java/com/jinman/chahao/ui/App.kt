package com.jinman.chahao.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.itemsIndexed as lazyListItems
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jinman.chahao.ReadNav
import com.jinman.chahao.ScoutViewModel
import com.jinman.chahao.SearchNav
import com.jinman.chahao.UiState
import com.jinman.chahao.data.Comic
import com.jinman.chahao.data.chapterLabel
import com.jinman.chahao.data.methodLabel
import kotlinx.coroutines.launch

private val ColorLine = Color(0xFFD8D2C8)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun App(vm: ScoutViewModel, clipboardTick: Int) {
    val state by vm.state.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    LaunchedEffect(clipboardTick) {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = cm.primaryClip?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)?.coerceToText(ctx)?.toString().orEmpty()
        vm.ingestClipboard(text)
    }
    LaunchedEffect(state.toast) {
        val t = state.toast ?: return@LaunchedEffect
        snack.showSnackbar(t)
        vm.dismissToast()
    }

    fun handle(navResult: SearchNav) {
        when (navResult) {
            is SearchNav.Detail -> {
                nav.navigate("session") { launchSingleTop = true }
                nav.navigate("detail/${navResult.id}")
            }
            SearchNav.Picker -> nav.navigate("session") { launchSingleTop = true }
            is SearchNav.Fail -> scope.launch { snack.showSnackbar(navResult.message) }
        }
    }

    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    val showBar = route in setOf("home", "session", "favorites")

    Scaffold(
        containerColor = Paper,
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            if (showBar) {
                NavigationBar(containerColor = Paper, tonalElevation = 0.dp) {
                    NavigationBarItem(
                        selected = route == "home" || route == "session",
                        onClick = {
                            nav.navigate(if (state.session.isNotEmpty()) "session" else "home") {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Search, contentDescription = null) },
                        label = { Text("查号") },
                        colors = navColors(),
                    )
                    NavigationBarItem(
                        selected = route == "favorites",
                        onClick = {
                            nav.navigate("favorites") {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Bookmark, contentDescription = null) },
                        label = { Text("收藏") },
                        colors = navColors(),
                    )
                }
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            NavHost(nav, startDestination = "home") {
                composable("home") {
                    HomeScreen(
                        state = state,
                        vm = vm,
                        onSearch = { scope.launch { handle(vm.searchDraft()) } },
                        onOpenSession = { nav.navigate("session") },
                        onSettings = { nav.navigate("settings") },
                    )
                }
                composable("session") {
                    SessionScreen(
                        state = state,
                        nav = nav,
                        onSettings = { nav.navigate("settings") },
                        onHome = {
                            vm.setDraft("")
                            nav.navigate("home") { launchSingleTop = true }
                        },
                    ) { id ->
                        scope.launch { handle(vm.searchOne(id)) }
                    }
                }
                composable("detail/{id}") { entry ->
                    val id = entry.arguments?.getString("id").orEmpty()
                    val comic = state.cache[id] ?: state.favorites[id]?.comic
                    if (comic == null) {
                        LaunchedEffect(id) { handle(vm.searchOne(id)) }
                    } else {
                        DetailScreen(
                            comic = comic,
                            state = state,
                            vm = vm,
                            onBack = { nav.popBackStack() },
                            onStartRead = {
                                scope.launch {
                                    when (val result = vm.startRead(comic)) {
                                        is ReadNav.Chapters -> nav.navigate("chapters/${result.albumId}")
                                        is ReadNav.Reader -> nav.navigate("read/${result.albumId}/${result.chapterId}")
                                        is ReadNav.Fail -> snack.showSnackbar(result.message)
                                    }
                                }
                            },
                        )
                    }
                }
                composable("favorites") { FavoritesScreen(state, vm, nav) }
                composable("settings") { SettingsScreen(state, nav) }
                composable("blacklist") {
                    BlacklistScreen(state, nav) { id ->
                        scope.launch { handle(vm.searchOne(id)) }
                    }
                }
                composable("chapters/{id}") { entry ->
                    val id = entry.arguments?.getString("id").orEmpty()
                    val comic = state.cache[id] ?: state.favorites[id]?.comic
                    if (comic == null) {
                        LaunchedEffect(id) { handle(vm.searchOne(id)) }
                    } else {
                        ChapterListScreen(comic, nav)
                    }
                }
                composable("read/{albumId}/{chapterId}") { entry ->
                    val albumId = entry.arguments?.getString("albumId").orEmpty()
                    val chapterId = entry.arguments?.getString("chapterId").orEmpty()
                    ReaderScreen(state, vm, albumId, chapterId) { nav.popBackStack() }
                }
            }
            if (state.pendingClipboard != null) {
                PendingBar(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    onSearch = { scope.launch { handle(vm.searchPending()) } },
                    onDismiss = vm::dismissPending,
                )
            }
        }
    }

    if (state.picker.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = vm::closePicker,
            title = { Text("选择一个禁漫车") },
            text = {
                Column {
                    Text("这段里有多个完整禁漫车，选一个进行搜索。", color = Muted, fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                    state.picker.forEach { item ->
                        TextButton(
                            onClick = {
                                vm.closePicker()
                                scope.launch { handle(vm.searchOne(item.id)) }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("JM${item.id}", fontFamily = FontFamily.Monospace, color = Ink)
                            Spacer(Modifier.weight(1f))
                            Text(methodLabel(item.method), color = Muted, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {},
        )
    }
}

@Composable
private fun navColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = Accent,
    selectedTextColor = Accent,
    indicatorColor = Surface2,
    unselectedIconColor = Subtle,
    unselectedTextColor = Subtle,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeScreen(
    state: UiState,
    vm: ScoutViewModel,
    onSearch: () -> Unit,
    onOpenSession: () -> Unit,
    onSettings: () -> Unit,
) {
    val ctx = LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("禁漫助手", fontSize = 22.sp, fontWeight = FontWeight.Medium, color = Ink, modifier = Modifier.weight(1f))
            IconButton(onClick = onSettings) {
                Icon(Icons.Default.Settings, contentDescription = "设置", tint = Muted)
            }
        }
        Spacer(Modifier.height(16.dp))
        Column(
            Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Surface)
                .padding(16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("原文", fontWeight = FontWeight.Medium)
                Row {
                    if (state.draft.isNotBlank()) {
                        TextButton(onClick = { vm.setDraft("") }) { Text("清除") }
                    }
                    TextButton(onClick = {
                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val text = cm.primaryClip?.takeIf { it.itemCount > 0 }
                        ?.getItemAt(0)?.coerceToText(ctx)?.toString().orEmpty()
                    if (text.isBlank()) vm.setDraft(state.draft)
                    else vm.setDraft(text)
                }) { Text("读取剪贴板") }
                }
            }
            OutlinedTextField(
                value = state.draft,
                onValueChange = vm::setDraft,
                modifier = Modifier.fillMaxWidth().height(120.dp),
                placeholder = { Text("粘贴抖音评论或车号。支持纯数字、暗号拼接。") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Accent,
                    unfocusedContainerColor = Surface2,
                    focusedContainerColor = Surface2,
                ),
            )
            if (state.extracted.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text("识别到的禁漫车", color = Subtle, fontSize = 12.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.extracted.forEach {
                        Text(
                            "JM${it.id}  ${methodLabel(it.method)}",
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Accent.copy(alpha = 0.1f))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            color = Accent,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            } else if (state.draft.isNotBlank()) {
                Text("未能识别禁漫车", color = Subtle, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onSearch,
                enabled = !state.searching && state.extracted.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = AccentFg),
                shape = RoundedCornerShape(12.dp),
            ) {
                if (state.searching) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = AccentFg, strokeWidth = 2.dp)
                } else {
                    Text("搜索")
                }
            }
        }
        if (state.session.isNotEmpty()) {
            TextButton(onClick = onOpenSession) {
                Text("查看本次识别 · ${state.session.size} 个车号", color = Accent)
            }
        }
        Spacer(Modifier.height(8.dp))
        val samples = listOf(
            "纯数字" to "1467240",
            "抖音评论" to "@三角洲上瘾《不出货版》: 1464253",
            "用户名带数字" to "@胖虎12138: 1468151[星星眼]",
            "阿拉伯暗号" to "146局游戏81个大红51小金",
            "中文暗号" to "回来刷了会动漫资讯，十四小时看了六万七千九百四十六条动漫信息",
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            samples.forEach { (label, text) ->
                TextButton(onClick = { vm.setDraft(text) }) {
                    Text(label, color = Muted, fontSize = 12.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SessionScreen(
    state: UiState,
    nav: NavHostController,
    onSettings: () -> Unit,
    onHome: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val ctx = LocalContext.current
    var skipClick by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("本次识别", fontWeight = FontWeight.Medium)
                Text("点进详情；长按复制车号（不含 JM）", color = Subtle, fontSize = 12.sp)
            }
            TextButton(onClick = { nav.navigate("home") }) { Text("改原文") }
            IconButton(onClick = onSettings) {
                Icon(Icons.Default.Settings, contentDescription = "设置", tint = Muted)
            }
        }
        Spacer(Modifier.height(12.dp))
        if (state.session.isEmpty()) {
            Text("还没有识别记录", color = Muted)
        } else {
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Surface),
            ) {
                state.session.forEachIndexed { i, item ->
                    if (i > 0) HorizontalDivider(color = ColorLine)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (skipClick) skipClick = false
                                    else onOpen(item.id)
                                },
                                onLongClick = {
                                    skipClick = true
                                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("id", item.id))
                                    Toast.makeText(ctx, "已复制 ${item.id}", Toast.LENGTH_SHORT).show()
                                },
                            )
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("JM${item.id}", fontFamily = FontFamily.Monospace, fontSize = 16.sp)
                        Text(
                            if (item.searched) "已查" else "未查",
                            color = if (item.searched) Accent else Subtle,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onHome,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Surface2, contentColor = Ink),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("返回主页")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MetaBlock(label: String, values: List<String>) {
    if (values.isEmpty()) return
    Text(label, color = Subtle, fontSize = 12.sp, modifier = Modifier.padding(top = 16.dp))
    FlowRow(
        modifier = Modifier.padding(top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        values.forEach { tag ->
            Text(
                tag,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Surface2)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                fontSize = 12.sp,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DetailScreen(
    comic: Comic,
    state: UiState,
    vm: ScoutViewModel,
    onBack: () -> Unit,
    onStartRead: () -> Unit,
) {
    var viewer by remember { mutableStateOf<Int?>(null) }
    val favorited = state.favorites.containsKey(comic.id)
    val blocked = state.blacklist.containsKey(comic.id)
    val gallery: List<Pair<String?, String?>> =
        listOf(null to null) + comic.extraPages.map { it.photoId to it.file }
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
            Text("JM${comic.id}", fontFamily = FontFamily.Monospace, color = Muted)
        }
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            gallery.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { item ->
                        val index = gallery.indexOf(item)
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(3f / 4f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewer = index },
                        ) {
                            CoverImage(comic.id, item.first, item.second, Modifier.fillMaxSize())
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text("禁漫车 JM${comic.id}", fontFamily = FontFamily.Monospace, color = Muted, fontSize = 12.sp)
            Text(
                if (comic.found) comic.name else "没有这部",
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp),
            )
            MetaBlock("作者", comic.authors)
            MetaBlock("作品", comic.works)
            MetaBlock("登场人物", comic.actors)
            MetaBlock("分类标签", comic.tags)
            if (comic.description.isNotBlank()) {
                Text("描述", color = Subtle, fontSize = 12.sp, modifier = Modifier.padding(top = 16.dp))
                Text(comic.description, color = Muted, modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(Modifier.height(16.dp))
            if (comic.found) {
                Button(
                    onClick = onStartRead,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = AccentFg),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Default.MenuBook, null)
                    Spacer(Modifier.size(8.dp))
                    Text("开始阅读")
                }
                Spacer(Modifier.height(12.dp))
            }
            Button(
                onClick = { vm.toggleFavorite(comic) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (favorited) Surface2 else Accent,
                    contentColor = if (favorited) Ink else AccentFg,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(if (favorited) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, null)
                Spacer(Modifier.size(8.dp))
                Text(if (favorited) "取消收藏" else "收藏")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { vm.toggleBlacklist(comic) }) {
                    Text(
                        if (blocked) "移出黑名单" else "加入黑名单",
                        color = Subtle,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
    val open = viewer
    if (open != null && gallery.isNotEmpty()) {
        Dialog(
            onDismissRequest = { viewer = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            val pagerState = rememberPagerState(initialPage = open, pageCount = { gallery.size })
            BackHandler { viewer = null }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black),
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    val item = gallery[page]
                    CoverImage(
                        comic.id,
                        item.first,
                        item.second,
                        Modifier.fillMaxSize(),
                        forceFit = true,
                    )
                }
                IconButton(
                    onClick = { viewer = null },
                    modifier = Modifier
                        .statusBarsPadding()
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f)),
                ) {
                    Icon(Icons.Default.Close, contentDescription = "关闭", tint = Color.White)
                }
                Text(
                    "${pagerState.currentPage + 1} / ${gallery.size}",
                    color = Color.White.copy(alpha = 0.85f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(16.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun FavoritesScreen(state: UiState, vm: ScoutViewModel, nav: NavHostController) {
    val ctx = LocalContext.current
    val list = state.favorites.values
        .filter { if (state.favTab == "pending") !it.exported else it.exported }
        .sortedByDescending { it.savedAt }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        ctx.contentResolver.openInputStream(uri)?.use {
            vm.importJson(it.readBytes().toString(Charsets.UTF_8))
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("收藏", fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            IconButton(onClick = { nav.navigate("settings") }) {
                Icon(Icons.Default.Settings, contentDescription = "设置", tint = Muted)
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Surface)
                .padding(4.dp),
        ) {
            listOf("pending" to "未导出", "exported" to "已导出").forEach { (id, label) ->
                val on = state.favTab == id
                TextButton(
                    onClick = { vm.setFavTab(id) },
                    modifier = Modifier
                        .weight(1f)
                        .background(if (on) Surface2 else Color.Transparent, RoundedCornerShape(8.dp)),
                ) { Text(label, color = if (on) Ink else Muted) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = {
                val json = vm.backupJson()
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, json)
                    putExtra(Intent.EXTRA_SUBJECT, "禁漫助手收藏")
                }
                ctx.startActivity(Intent.createChooser(send, "搬家导出"))
            }) { Text("搬家导出") }
            TextButton(onClick = { import.launch("*/*") }) { Text("导入") }
        }
        if (list.isEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Surface)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(if (state.favTab == "pending") "没有未导出的收藏" else "还没有已导出的收藏")
                Text("详情页点收藏，封面会出现在这里。长按可选中后导出。", color = Muted, fontSize = 14.sp)
            }
        } else {
            Text(
                "${list.size} 本" + if (state.selecting) " · 已选 ${state.selected.size}" else " · 长按多选",
                color = Muted,
                fontSize = 14.sp,
            )
            if (state.selecting) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { vm.selectAll(list.map { it.comic.id }) }) { Text("全选") }
                    TextButton(onClick = vm::exitSelect) { Text("取消") }
                    TextButton(onClick = vm::deleteSelected) { Text("删除") }
                    if (state.favTab == "exported") {
                        TextButton(onClick = { vm.markUnexported(state.selected) }) { Text("撤回") }
                    } else {
                        TextButton(onClick = {
                            val ids = state.selected.toList()
                            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("ids", ids.joinToString(",")))
                            vm.markExported(ids)
                        }) { Text("导出") }
                    }
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(list, key = { it.comic.id }) { fav ->
                    val on = fav.comic.id in state.selected
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .border(if (on) 1.dp else 0.dp, Accent, RoundedCornerShape(12.dp))
                            .combinedClickable(
                                onClick = {
                                    if (state.selecting) vm.toggleSelect(fav.comic.id)
                                    else nav.navigate("detail/${fav.comic.id}")
                                },
                                onLongClick = { vm.enterSelect(fav.comic.id) },
                            ),
                    ) {
                        Box(Modifier.aspectRatio(3f / 4f)) {
                            CoverImage(fav.comic.id, modifier = Modifier.fillMaxSize())
                            Text(
                                "JM${fav.comic.id}",
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(6.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Ink.copy(alpha = 0.75f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                color = AccentFg,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                            )
                            if (state.selecting) {
                                Box(
                                    Modifier
                                        .align(Alignment.TopStart)
                                        .padding(6.dp)
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(if (on) Accent else Paper.copy(alpha = 0.5f))
                                        .border(1.dp, if (on) Accent else Paper, CircleShape),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingBar(modifier: Modifier, onSearch: () -> Unit, onDismiss: () -> Unit) {
    Row(
        modifier
            .navigationBarsPadding()
            .padding(16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Ink)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("检测到新复制的内容", color = AccentFg, modifier = Modifier.weight(1f), fontSize = 14.sp)
        TextButton(onClick = onSearch) { Text("搜索新内容", color = AccentFg) }
        TextButton(onClick = onDismiss) { Text("×", color = AccentFg) }
    }
}

@Composable
private fun SettingsScreen(
    state: UiState,
    nav: NavHostController,
) {
    val count = state.blacklist.size
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
            Text("设置", fontWeight = FontWeight.Medium, fontSize = 18.sp)
        }
        Spacer(Modifier.height(16.dp))
        Column(
            Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Surface),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { nav.navigate("blacklist") }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("黑名单")
                    Text(
                        if (count == 0) "还没有拉黑的车号" else "${count} 部",
                        color = Subtle,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Subtle,
                )
            }
        }
    }
}

@Composable
private fun BlacklistScreen(
    state: UiState,
    nav: NavHostController,
    onOpen: (String) -> Unit,
) {
    val list = state.blacklist.values.sortedByDescending { it.addedAt }
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
            Text("黑名单", fontWeight = FontWeight.Medium, fontSize = 18.sp)
        }
        Spacer(Modifier.height(12.dp))
        if (list.isEmpty()) {
            Text("还没有拉黑的车号", color = Muted, modifier = Modifier.padding(top = 24.dp))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Surface),
            ) {
                lazyListItems(list, key = { it.id }) { index, item ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpen(item.id) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                item.name.ifBlank { "JM${item.id}" },
                                fontSize = 15.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "JM${item.id}",
                                color = Subtle,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Subtle,
                        )
                    }
                    if (index < list.lastIndex) {
                        HorizontalDivider(color = ColorLine, thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterListScreen(comic: Comic, nav: NavHostController) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
            Text("选择话数", fontWeight = FontWeight.Medium, fontSize = 18.sp)
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(Surface),
        ) {
            lazyListItems(comic.chapters, key = { it.id }) { index, ch ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { nav.navigate("read/${comic.id}/${ch.id}") }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(chapterLabel(index, ch), modifier = Modifier.weight(1f), fontSize = 15.sp)
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Subtle,
                    )
                }
                if (index < comic.chapters.lastIndex) {
                    HorizontalDivider(color = ColorLine, thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun ReaderScreen(
    state: UiState,
    vm: ScoutViewModel,
    albumId: String,
    chapterId: String,
    onBack: () -> Unit,
) {
    LaunchedEffect(chapterId) { vm.loadChapterPages(chapterId) }
    val view = LocalView.current
    DisposableEffect(view) {
        val oldV = view.isVerticalScrollBarEnabled
        val oldH = view.isHorizontalScrollBarEnabled
        view.isVerticalScrollBarEnabled = false
        view.isHorizontalScrollBarEnabled = false
        onDispose {
            view.isVerticalScrollBarEnabled = oldV
            view.isHorizontalScrollBarEnabled = oldH
        }
    }
    val comic = state.cache[albumId] ?: state.favorites[albumId]?.comic
    val title = comic?.chapters?.indexOfFirst { it.id == chapterId }?.takeIf { it >= 0 }?.let { i ->
        chapterLabel(i, comic.chapters[i])
    } ?: "阅读"
    val listState = rememberLazyListState()
    val total = state.readPages.size
    val current by remember(total) {
        derivedStateOf {
            if (total == 0) 0
            else {
                val last = listState.layoutInfo.visibleItemsInfo.maxByOrNull { it.index }?.index ?: 0
                (last + 1).coerceIn(1, total)
            }
        }
    }
    val fraction = if (total == 0) 0f else current / total.toFloat()
    Column(
        Modifier
            .fillMaxSize()
            .background(Paper)
            .statusBarsPadding(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
            Text(
                title,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (total > 0) {
                Text(
                    "$current/$total",
                    color = Subtle,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(end = 12.dp),
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(ColorLine)) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .background(Accent),
            )
        }
        if (state.reading && state.readPages.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = true,
            ) {
                lazyItems(state.readPages, key = { it.file }) { page ->
                    CoverImage(
                        id = albumId,
                        photoId = page.photoId,
                        file = page.file,
                        modifier = Modifier.fillMaxWidth(),
                        fillWidth = true,
                    )
                }
            }
        }
    }
}
