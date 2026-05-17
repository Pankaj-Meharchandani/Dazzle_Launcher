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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.dazzlelauncher.ui.theme.DazzleLauncherTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: LauncherViewModel by viewModels()

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                viewModel.shuffleHomeApps()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
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
    val is24Hour by viewModel.is24Hour.collectAsState()
    val isDefault by viewModel.isDefault.collectAsState()
    val shouldUseDarkText by viewModel.shouldUseDarkText.collectAsState()
    val iconShape by viewModel.iconShape.collectAsState()
    val shuffleType by viewModel.shuffleType.collectAsState()
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
    var showSelectiveShuffle by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        if (scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded) {
            scope.launch { scaffoldState.bottomSheetState.partialExpand() }
        } else if (showSelectiveShuffle) {
            showSelectiveShuffle = false
        } else if (showSettings) {
            showSettings = false
        }
        // If already on home screen and drawer closed, do nothing (ignore back gesture)
    }

    val widgetType by viewModel.widgetType.collectAsState()

    if (showSelectiveShuffle) {
        SelectiveShuffleScreen(
            viewModel = viewModel,
            onClose = { showSelectiveShuffle = false }
        )
    } else if (showSettings) {
        SettingsScreen(
            currentMode = mode,
            shuffleType = shuffleType,
            useWallpaper = useWallpaper,
            blurDrawer = blurDrawer,
            is24Hour = is24Hour,
            widgetType = widgetType,
            iconShape = iconShape,
            onModeChange = { viewModel.setMode(it) },
            onShuffleTypeChange = { viewModel.setShuffleType(it) },
            onOpenSelectiveShuffle = { showSelectiveShuffle = true },
            onWallpaperToggle = { viewModel.setUseWallpaper(it) },
            onBlurDrawerToggle = { viewModel.setBlurDrawer(it) },
            onTimeFormatToggle = { viewModel.setIs24Hour(it) },
            onWidgetTypeChange = { viewModel.setWidgetType(it) },
            onIconShapeChange = { viewModel.setIconShape(it) },
            onClose = { showSettings = false }
        )
    } else {
        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetContent = {
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
                        blurDrawer = blurDrawer,
                        iconShape = iconShape,
                        launcherMode = mode
                    )
                }
            },
            sheetPeekHeight = if (mode == LauncherMode.HOME_AND_DRAWER) 40.dp else 0.dp,
            sheetDragHandle = {
                if (mode == LauncherMode.HOME_AND_DRAWER) {
                    BottomSheetDefaults.DragHandle()
                } else {
                    // Small drag handle or none for minimal search
                    Spacer(modifier = Modifier.height(16.dp))
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
                        scope.launch { scaffoldState.bottomSheetState.expand() }
                    },
                    onSwipeDown = {
                        viewModel.openNotifications(context)
                    },
                    onToggleDock = { viewModel.toggleDockApp(it) },
                    shouldUseDarkText = shouldUseDarkText,
                    viewModel = viewModel,
                    useWallpaper = useWallpaper,
                    is24Hour = is24Hour,
                    iconShape = iconShape
                )
            }
        }
    }
}
