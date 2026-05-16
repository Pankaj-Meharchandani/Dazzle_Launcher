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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
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
import com.example.dazzlelauncher.ui.theme.DazzleLauncherTheme
import kotlinx.coroutines.launch

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
    val shouldUseDarkText by viewModel.shouldUseDarkText.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = false
        )
    )

    // Smoothly animate home screen alpha based on drawer expansion
    val homeAlpha by animateFloatAsState(
        targetValue = if (scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded) 0f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "homeAlpha"
    )

    var showSettings by remember { mutableStateOf(false) }
    var isDefault by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isDefault = viewModel.isDefaultLauncher(context)
    }

    BackHandler(enabled = scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded) {
        scope.launch { scaffoldState.bottomSheetState.partialExpand() }
    }

    if (showSettings) {
        SettingsScreen(
            currentMode = mode,
            useWallpaper = useWallpaper,
            onModeChange = { viewModel.setMode(it) },
            onWallpaperToggle = { viewModel.setUseWallpaper(it) },
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
                            onToggleHome = { viewModel.toggleHomeApp(it) }
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
            sheetContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = Color.Transparent
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(if (useWallpaper) Color.Transparent else MaterialTheme.colorScheme.background)
                    .graphicsLayer { alpha = homeAlpha }
            ) {
                HomeScreen(
                    apps = homeApps,
                    dockApps = dockApps,
                    onAppClick = { pkg -> viewModel.launchApp(context, pkg) },
                    onLongClick = { showSettings = true },
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
                    shouldUseDarkText = shouldUseDarkText
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
    onLongClick: () -> Unit,
    isDefault: Boolean,
    onSetDefault: () -> Unit,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
    onToggleDock: (AppInfo) -> Unit,
    shouldUseDarkText: Boolean
) {
    val itemsPerPage = 20
    val pages = if (apps.isEmpty()) 1 else (apps.size + itemsPerPage - 1) / itemsPerPage
    val pagerState = rememberPagerState(pageCount = { pages })

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
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick
            )
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

        Text(
            text = "Dazzle",
            style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 24.dp),
            color = if (shouldUseDarkText) Color.Black else MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
        ) { page ->
            val startIdx = page * itemsPerPage
            val endIdx = minOf(startIdx + itemsPerPage, apps.size)
            val pageApps = if (apps.isEmpty()) emptyList() else apps.subList(startIdx, endIdx)
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                userScrollEnabled = false
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

        // Page Indicator and Dock area
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (pages > 1) {
                Row(
                    Modifier.height(24.dp).padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(pages) { iteration ->
                        val color = if (pagerState.currentPage == iteration) 
                            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        Box(
                            modifier = Modifier
                                .padding(3.dp)
                                .clip(CircleShape)
                                .background(color)
                                .size(6.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            Dock(apps = dockApps, onAppClick = onAppClick, onLongClick = onToggleDock)
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
    onToggleHome: (AppInfo) -> Unit
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

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            placeholder = { Text("Search apps...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent
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
                    AppItem(app, onAppClick)
                    Checkbox(
                        checked = isOnHome,
                        onCheckedChange = { onToggleHome(app) },
                        modifier = Modifier.scale(0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    currentMode: LauncherMode,
    useWallpaper: Boolean,
    onModeChange: (LauncherMode) -> Unit,
    onWallpaperToggle: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Text("Settings", style = MaterialTheme.typography.displaySmall)
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
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
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
