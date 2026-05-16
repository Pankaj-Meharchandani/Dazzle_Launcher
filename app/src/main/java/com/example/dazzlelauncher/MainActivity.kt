package com.example.dazzlelauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.dazzlelauncher.ui.theme.DazzleLauncherTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val viewModel: LauncherViewModel by viewModels()

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF || intent?.action == Intent.ACTION_USER_PRESENT) {
                viewModel.shuffleHomeApps()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(unlockReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(unlockReceiver, filter)
        }

        setContent {
            DazzleLauncherTheme {
                LauncherRoot(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkDefaultLauncher(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(unlockReceiver)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherRoot(viewModel: LauncherViewModel) {
    val homeApps by viewModel.homeApps.collectAsState()
    val dockApps by viewModel.dockApps.collectAsState()
    val allApps by viewModel.allApps.collectAsState()
    val mode by viewModel.mode.collectAsState()
    val useWallpaper by viewModel.useWallpaper.collectAsState()
    val blurDrawer by viewModel.blurDrawer.collectAsState()
    val isDefault by viewModel.isDefault.collectAsState()
    val shouldUseDarkText by viewModel.shouldUseDarkText.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = false
        )
    )

    // Smoothly animate home screen alpha and blur based on drawer expansion
    val drawerExpansion by animateFloatAsState(
        targetValue = if (scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "drawerExpansion"
    )

    val homeAlpha = 1f - (drawerExpansion * 0.8f) // Fade out slightly
    val homeBlur = if (blurDrawer) (drawerExpansion * 20f).dp else 0.dp

    var showSettings by remember { mutableStateOf(false) }

    BackHandler(enabled = scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded) {
        scope.launch { scaffoldState.bottomSheetState.partialExpand() }
    }

    if (showSettings) {
        SettingsScreen(
            currentMode = mode,
            useWallpaper = useWallpaper,
            blurDrawer = blurDrawer,
            onModeChange = { viewModel.setMode(it) },
            onWallpaperToggle = { viewModel.setUseWallpaper(it) },
            onBlurDrawerToggle = { viewModel.setBlurDrawer(it) },
            onClose = { showSettings = false }
        )
    } else {
        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetContent = {
                if (mode == LauncherMode.HOME_AND_DRAWER) {
                    Box(modifier = Modifier.fillMaxHeight(0.92f)) {
                        AppDrawerContent(
                            apps = allApps,
                            homeApps = homeApps,
                            dockApps = dockApps,
                            isExpanded = scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded,
                            onAppClick = { pkg ->
                                viewModel.launchApp(context, pkg)
                                scope.launch { scaffoldState.bottomSheetState.partialExpand() }
                            },
                            onToggleHome = { viewModel.toggleHomeApp(it) },
                            shouldUseDarkText = shouldUseDarkText,
                            blurDrawer = blurDrawer
                        )
                    }
                }
            },
            sheetPeekHeight = if (mode == LauncherMode.HOME_AND_DRAWER) 40.dp else 0.dp,
            sheetDragHandle = {
                if (mode == LauncherMode.HOME_AND_DRAWER) {
                    BottomSheetDefaults.DragHandle()
                }
            },
            sheetContainerColor = if (blurDrawer) {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            },
            sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = Color.Transparent
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(if (useWallpaper) Color.Transparent else MaterialTheme.colorScheme.background)
                    .graphicsLayer { 
                        alpha = homeAlpha
                        // Only works on API 31+
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            renderEffect = android.graphics.RenderEffect.createBlurEffect(
                                homeBlur.toPx(), homeBlur.toPx(), android.graphics.Shader.TileMode.CLAMP
                            ).asComposeRenderEffect()
                        }
                    }
            ) {
                HomeScreen(
                    apps = homeApps,
                    dockApps = dockApps,
                    onAppClick = { pkg -> viewModel.launchApp(context, pkg) },
                    onOpenSettings = { showSettings = true },
                    isDefault = isDefault,
                    onSetDefault = {
                        val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                        context.startActivity(intent)
                    },
                    onSwipeUp = {
                        if (mode == LauncherMode.HOME_AND_DRAWER) {
                            scope.launch { scaffoldState.bottomSheetState.expand() }
                        }
                    },
                    onSwipeDown = {
                        viewModel.openNotifications(context)
                    },
                    onToggleDock = { viewModel.toggleDockApp(it) },
                    shouldUseDarkText = shouldUseDarkText,
                    viewModel = viewModel,
                    useWallpaper = useWallpaper
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    apps: List<AppInfo>,
    dockApps: List<AppInfo>,
    onAppClick: (String) -> Unit,
    onOpenSettings: () -> Unit,
    isDefault: Boolean,
    onSetDefault: () -> Unit,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
    onToggleDock: (AppInfo) -> Unit,
    shouldUseDarkText: Boolean,
    viewModel: LauncherViewModel,
    useWallpaper: Boolean
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val totalHeight = maxHeight
        // Reserve height for: Top padding (80), Banner (~60), Title area (~100), Dock area (~150), bottom padding (16)
        val reservedHeight = 80.dp + (if (!isDefault) 60.dp else 0.dp) + 100.dp + 150.dp + 16.dp
        val pagerHeight = totalHeight - reservedHeight
        val rows = (pagerHeight.value / 105).toInt().coerceIn(1, 6)
        val itemsPerPage = rows * 4
        val appPages = if (apps.isEmpty()) 1 else (apps.size + itemsPerPage - 1) / itemsPerPage
        val pagerState = rememberPagerState(initialPage = 1, pageCount = { appPages + 1 })

        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    var totalDrag = 0f
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            totalDrag += dragAmount
                        },
                        onDragEnd = {
                            if (totalDrag < -100) onSwipeUp()
                            if (totalDrag > 100) onSwipeDown()
                            totalDrag = 0f
                        }
                    )
                }
                .padding(top = 80.dp)
        ) {
            if (!isDefault) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { onSetDefault() },
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Set Dazzle as your default launcher",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }

            val titleText = if (pagerState.currentPage == 0) "Screen Time" else "Dazzle"
            Text(
                text = titleText,
                style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 24.dp),
                color = if (shouldUseDarkText) Color.Black else MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                pageSpacing = 16.dp,
                beyondViewportPageCount = 1
            ) { page ->
                if (page == 0) {
                    ScreentimePage(viewModel = viewModel, shouldUseDarkText = shouldUseDarkText, useWallpaper = useWallpaper)
                } else {
                    val appPage = page - 1
                    val startIdx = appPage * itemsPerPage
                    val endIdx = minOf(startIdx + itemsPerPage, apps.size)
                    val pageApps = if (apps.isEmpty()) emptyList() else apps.subList(startIdx, endIdx)
                    
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        userScrollEnabled = false,
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(
                            items = pageApps,
                            key = { it.key }
                        ) { app ->
                            AppItem(
                                app = app, 
                                onClick = onAppClick,
                                onLongClick = { onToggleDock(app) },
                                labelColor = if (shouldUseDarkText) Color.Black else Color.White
                            )
                        }
                    }
                }
            }

            // Indicator
            Row(
                modifier = Modifier.fillMaxWidth().height(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(pagerState.pageCount) { iteration ->
                    val color = if (pagerState.currentPage == iteration) 
                        MaterialTheme.colorScheme.primary else (if (shouldUseDarkText) Color.Black.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.4f))
                    
                    if (iteration == 0) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(color)
                                .size(width = 12.dp, height = 4.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .padding(3.dp)
                                .clip(CircleShape)
                                .background(color)
                                .size(6.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            if (pagerState.currentPage == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .clickable { onOpenSettings() }
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(HexShape)
                                .background(if (shouldUseDarkText) Color.Black.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Launcher Settings",
                                modifier = Modifier.size(32.dp),
                                tint = if (shouldUseDarkText) Color.Black else Color.White
                            )
                        }
                        Text(
                            text = "Settings",
                            fontSize = 11.sp,
                            color = if (shouldUseDarkText) Color.Black else Color.White,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                Dock(apps = dockApps, onAppClick = onAppClick, onLongClick = onToggleDock)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun Dock(apps: List<AppInfo>, onAppClick: (String) -> Unit, onLongClick: (AppInfo) -> Unit) {
    val scale = if (apps.size >= 5) 0.85f else 1f
    
    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(100.dp),
        color = Color.Black.copy(alpha = 0.3f),
        shape = RoundedCornerShape(32.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            apps.forEach { app ->
                Box(modifier = Modifier.scale(scale)) {
                    AppItem(
                        app = app, 
                        onClick = onAppClick, 
                        onLongClick = { onLongClick(app) },
                        showLabel = false
                    )
                }
            }
        }
    }
}

@Composable
fun AppDrawerContent(
    apps: List<AppInfo>,
    homeApps: List<AppInfo>,
    dockApps: List<AppInfo>,
    isExpanded: Boolean,
    onAppClick: (String) -> Unit,
    onToggleHome: (AppInfo) -> Unit,
    shouldUseDarkText: Boolean,
    blurDrawer: Boolean
) {
    var searchQuery by remember { mutableStateOf("") }
    val gridState = rememberLazyGridState()

    LaunchedEffect(isExpanded) {
        if (!isExpanded) {
            gridState.scrollToItem(0)
            searchQuery = ""
        }
    }

    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isEmpty()) apps else {
            apps.filter { it.label.contains(searchQuery, ignoreCase = true) }
        }
    }

    val labelColor = if (blurDrawer) {
        if (shouldUseDarkText) Color.Black else Color.White
    } else {
        if (isSystemInDarkTheme()) Color.White else MaterialTheme.colorScheme.onSurface
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                placeholder = { Text("Search apps...", color = labelColor.copy(alpha = 0.6f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = labelColor.copy(alpha = 0.7f)) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                textStyle = TextStyle(color = labelColor),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = labelColor.copy(alpha = 0.1f),
                    unfocusedContainerColor = labelColor.copy(alpha = 0.1f),
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = labelColor,
                    focusedTextColor = labelColor,
                    unfocusedTextColor = labelColor
                )
            )
            
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(
                    items = filteredApps,
                    key = { it.key }
                ) { app ->
                    val isOnHome = homeApps.any { it.packageName == app.packageName } || 
                                   dockApps.any { it.packageName == app.packageName }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AppItem(app, onAppClick, labelColor = labelColor)
                        Checkbox(
                            checked = isOnHome,
                            onCheckedChange = { onToggleHome(app) },
                            modifier = Modifier.scale(0.7f),
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                                uncheckedColor = labelColor.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    currentMode: LauncherMode,
    useWallpaper: Boolean,
    blurDrawer: Boolean,
    onModeChange: (LauncherMode) -> Unit,
    onWallpaperToggle: (Boolean) -> Unit,
    onBlurDrawerToggle: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)
    
    Surface(
        modifier = Modifier.fillMaxSize(), 
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose, modifier = Modifier.offset(x = (-12).dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("Settings", style = MaterialTheme.typography.displaySmall)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text("Appearance", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Show System Wallpaper", style = MaterialTheme.typography.titleMedium)
                    Text("Use your home screen background", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = useWallpaper, onCheckedChange = onWallpaperToggle)
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Frosted App Drawer", style = MaterialTheme.typography.titleMedium)
                    Text("Blurred wallpaper in drawer", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = blurDrawer, onCheckedChange = onBlurDrawerToggle)
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text("Launcher Mode", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            
            ModeOption(
                title = "Home & App Drawer",
                description = "Classic Android feel with a swipe-up drawer",
                selected = currentMode == LauncherMode.HOME_AND_DRAWER,
                onClick = { onModeChange(LauncherMode.HOME_AND_DRAWER) }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            ModeOption(
                title = "Home Only",
                description = "All apps on your home screen pages",
                selected = currentMode == LauncherMode.HOME_ONLY,
                onClick = { onModeChange(LauncherMode.HOME_ONLY) }
            )
            
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = onClose, 
                modifier = Modifier.fillMaxWidth().navigationBarsPadding(), 
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Back to Home")
            }
        }
    }
}

@Composable
fun ModeOption(title: String, description: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodySmall)
            }
            RadioButton(selected = selected, onClick = null)
        }
    }
}

private val HexShape = HexagonShape()

@Composable
fun ScreentimePage(viewModel: LauncherViewModel, shouldUseDarkText: Boolean, useWallpaper: Boolean) {
    val usageStats by viewModel.usageStats.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var hasAccess by remember { mutableStateOf(viewModel.hasUsageAccess(context)) }
    
    val filteredStats = remember(usageStats) {
        usageStats.filter { it.usageTime > 0 && it.packageName != context.packageName }
    }
    val totalTime = filteredStats.sumOf { it.usageTime }
    val dateFormatter = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasAccess = viewModel.hasUsageAccess(context)
                if (hasAccess) viewModel.fetchUsageStats()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(selectedDate) {
        if (hasAccess) viewModel.fetchUsageStats()
    }

    val contentColor = if (shouldUseDarkText) Color.Black else Color.White

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = if (useWallpaper) Color.Transparent else MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (!hasAccess) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Lock, 
                            contentDescription = null, 
                            modifier = Modifier.size(64.dp),
                            tint = if (shouldUseDarkText) Color.Black else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Usage access required", 
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge,
                            color = contentColor
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.openUsageSettings(context) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Grant Permission")
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.changeDate(-1) }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day", tint = contentColor)
                    }
                    Text(
                        text = dateFormatter.format(selectedDate.time),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = contentColor
                    )
                    IconButton(
                        onClick = { viewModel.changeDate(1) },
                        enabled = !isToday(selectedDate)
                    ) {
                        Icon(
                            Icons.Default.ChevronRight, 
                            contentDescription = "Next Day", 
                            tint = if (isToday(selectedDate)) contentColor.copy(alpha = 0.3f) else contentColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (shouldUseDarkText) Color.Black.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, contentColor.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Time Today", style = MaterialTheme.typography.labelLarge, color = contentColor.copy(alpha = 0.7f))
                        Text(
                            text = formatDuration(totalTime),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredStats) { usage ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(44.dp)) {
                                usage.appInfo?.icon?.let {
                                    Image(bitmap = it, contentDescription = null, modifier = Modifier.fillMaxSize())
                                } ?: Box(modifier = Modifier.fillMaxSize().background(contentColor.copy(alpha = 0.1f), CircleShape))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    usage.appInfo?.label ?: usage.packageName.split(".").last(), 
                                    style = MaterialTheme.typography.titleMedium,
                                    color = contentColor
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { if (totalTime > 0) usage.usageTime.toFloat() / totalTime else 0f },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = if (shouldUseDarkText) Color.Black else MaterialTheme.colorScheme.primary,
                                    trackColor = contentColor.copy(alpha = 0.1f)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                formatDuration(usage.usageTime), 
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = contentColor
                            )
                        }
                    }
                }
            }
        }
    }
}

fun isToday(calendar: Calendar): Boolean {
    val today = Calendar.getInstance()
    return calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
           calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
}

fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

@Composable
fun AppItem(
    app: AppInfo, 
    onClick: (String) -> Unit, 
    onLongClick: () -> Unit = {},
    showLabel: Boolean = true,
    labelColor: Color = if (isSystemInDarkTheme()) Color.White else MaterialTheme.colorScheme.onSurface
) {
    Column(
        modifier = Modifier
            .combinedClickable(
                onClick = { onClick(app.packageName) },
                onLongClick = onLongClick
            )
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(HexShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            app.icon?.let {
                Image(
                    bitmap = it,
                    contentDescription = app.label,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        if (showLabel) {
            Text(
                text = app.label,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
                lineHeight = 14.sp,
                color = labelColor,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.5f),
                        offset = Offset(1f, 1f),
                        blurRadius = 3f
                    )
                )
            )
        }
    }
}
