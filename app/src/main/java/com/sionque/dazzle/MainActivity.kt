package com.sionque.dazzle

import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.graphics.drawable.toBitmap
import com.sionque.dazzle.ui.theme.DazzleLauncherTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    companion object {
        private const val WIDGET_CONFIGURE_REQUEST = 0xDA23
    }

    private val viewModel: LauncherViewModel by viewModels()
    lateinit var appWidgetHost: AppWidgetHost
    val appWidgetManager by lazy { AppWidgetManager.getInstance(this) }
    private var configuringWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var replacingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> viewModel.shuffleHomeApps()
                Intent.ACTION_USER_PRESENT -> viewModel.incrementUnlockCount()
            }
        }
    }

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_PACKAGE_ADDED,
                Intent.ACTION_PACKAGE_REMOVED,
                Intent.ACTION_PACKAGE_CHANGED,
                Intent.ACTION_PACKAGE_REPLACED -> viewModel.loadApps()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appWidgetHost = AppWidgetHost(this, 0xDA22)
        enableEdgeToEdge()

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(unlockReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(unlockReceiver, filter)
        }

        val packageFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(packageReceiver, packageFilter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(packageReceiver, packageFilter)
        }

        setContent {
            DazzleLauncherTheme {
                LauncherRoot(
                    viewModel = viewModel,
                    widgetHost = appWidgetHost,
                    widgetManager = appWidgetManager,
                    onConfigureWidget = ::configureWidget
                )
            }
        }
    }

    private fun configureWidget(appWidgetId: Int, oldWidgetId: Int) {
        configuringWidgetId = appWidgetId
        replacingWidgetId = oldWidgetId
        try {
            appWidgetHost.startAppWidgetConfigureActivityForResult(
                this,
                appWidgetId,
                0,
                WIDGET_CONFIGURE_REQUEST,
                null
            )
        } catch (_: SecurityException) {
            finishWidgetConfiguration(success = false)
        } catch (_: RuntimeException) {
            finishWidgetConfiguration(success = false)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == WIDGET_CONFIGURE_REQUEST) {
            finishWidgetConfiguration(resultCode == Activity.RESULT_OK)
        }
    }

    private fun finishWidgetConfiguration(success: Boolean) {
        if (configuringWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        if (success) {
            if (replacingWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                appWidgetHost.deleteAppWidgetId(replacingWidgetId)
                viewModel.removeHomeWidget(replacingWidgetId)
            }
            viewModel.addHomeWidget(configuringWidgetId)
        } else {
            appWidgetHost.deleteAppWidgetId(configuringWidgetId)
        }

        configuringWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        replacingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    }

    override fun onStart() {
        super.onStart()
        appWidgetHost.startListening()
    }

    override fun onStop() {
        appWidgetHost.stopListening()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        // The app list is populated at startup, but installs can complete while
        // the launcher is in the background. Refresh it whenever we return so
        // newly installed apps appear in the drawer immediately.
        viewModel.loadApps()
        viewModel.checkDefaultLauncher(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(unlockReceiver)
        unregisterReceiver(packageReceiver)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherRoot(
    viewModel: LauncherViewModel,
    widgetHost: AppWidgetHost,
    widgetManager: AppWidgetManager,
    onConfigureWidget: (Int, Int) -> Unit
) {
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
    val homeWidgets by viewModel.homeWidgets.collectAsState()
    val postureAlertEnabled by viewModel.postureAlertEnabled.collectAsState()
    val postureSensitivity by viewModel.postureSensitivity.collectAsState()
    val postureDuration by viewModel.postureDuration.collectAsState()
    val postureQuietStart by viewModel.postureQuietStart.collectAsState()
    val postureQuietEnd by viewModel.postureQuietEnd.collectAsState()
    val blockedApps by viewModel.blockedApps.collectAsState()
    val limitedApps by viewModel.limitedApps.collectAsState()
    val appBlockStart by viewModel.appBlockStart.collectAsState()
    val appBlockEnd by viewModel.appBlockEnd.collectAsState()
    val forceAllowEnabled by viewModel.forceAllowEnabled.collectAsState()
    val forceAllowCondition by viewModel.forceAllowCondition.collectAsState()
    val mindlessScrollEnabled by viewModel.mindlessScrollEnabled.collectAsState()
    val mindlessScrollMinutes by viewModel.mindlessScrollMinutes.collectAsState()
    val mindlessScrollApps by viewModel.mindlessScrollApps.collectAsState()
    val context = LocalContext.current
    val effectiveDarkText = shouldUseDarkText || (!useWallpaper && !isSystemInDarkTheme())

    SideEffect {
        (context as? Activity)?.let { activity ->
            WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
                isAppearanceLightStatusBars = effectiveDarkText
                isAppearanceLightNavigationBars = effectiveDarkText
            }
        }
    }
    val scope = rememberCoroutineScope()
    var showOnboarding by remember {
        mutableStateOf(
            !context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
                .getBoolean("onboarding_complete", false)
        )
    }

    fun completeOnboarding() {
        context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("onboarding_complete", true)
            .apply()
        showOnboarding = false
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permissions if needed
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_PHONE_STATE)
        }
        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }
    
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
    var showExercisePicker by remember { mutableStateOf(false) }
    var exerciseCondition by remember { mutableStateOf<ForceAllowCondition?>(null) }
    var showSelectiveShuffle by remember { mutableStateOf(false) }
    var pendingWidgetId by remember { mutableIntStateOf(AppWidgetManager.INVALID_APPWIDGET_ID) }
    var replacingWidgetId by remember { mutableIntStateOf(AppWidgetManager.INVALID_APPWIDGET_ID) }
    var forceAllowPackage by remember { mutableStateOf<String?>(null) }
    var showWidgetPicker by remember { mutableStateOf(false) }

    fun commitWidget() {
        if (replacingWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            widgetHost.deleteAppWidgetId(replacingWidgetId)
            viewModel.removeHomeWidget(replacingWidgetId)
        }
        viewModel.addHomeWidget(pendingWidgetId)
        pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        replacingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    }

    val widgetPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val selectedId = result.data?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            pendingWidgetId
        ) ?: pendingWidgetId
        if (result.resultCode != Activity.RESULT_OK || selectedId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            if (pendingWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                widgetHost.deleteAppWidgetId(pendingWidgetId)
            }
            pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
            replacingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
            return@rememberLauncherForActivityResult
        }

        pendingWidgetId = selectedId
        val info = widgetManager.getAppWidgetInfo(selectedId)
        if (info?.configure != null) {
            onConfigureWidget(selectedId, replacingWidgetId)
            pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
            replacingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        } else {
            commitWidget()
        }
    }

    fun selectWidgetProvider(provider: AppWidgetProviderInfo) {
        showWidgetPicker = false
        val bound = runCatching {
            widgetManager.bindAppWidgetIdIfAllowed(pendingWidgetId, provider.provider)
        }.getOrDefault(false)
        if (bound) {
            val info = widgetManager.getAppWidgetInfo(pendingWidgetId)
            if (info?.configure != null) {
                onConfigureWidget(pendingWidgetId, replacingWidgetId)
                pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
                replacingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
            } else {
                commitWidget()
            }
        } else {
            widgetPickerLauncher.launch(
                Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidgetId)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider.provider)
            )
        }
    }

    BackHandler(enabled = true) {
        if (scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded) {
            scope.launch { scaffoldState.bottomSheetState.partialExpand() }
        } else if (showSelectiveShuffle) {
            showSelectiveShuffle = false
        } else if (showSettings) {
            showSettings = false
        } else if (showWidgetPicker) {
            showWidgetPicker = false
            if (pendingWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                widgetHost.deleteAppWidgetId(pendingWidgetId)
                pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
                replacingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
            }
        }
        // If already on home screen and drawer closed, do nothing (ignore back gesture)
    }

    val widgetType by viewModel.widgetType.collectAsState()

    if (showWidgetPicker) {
        WidgetPickerDialog(
            widgetManager = widgetManager,
            onSelect = ::selectWidgetProvider,
            onDismiss = {
                showWidgetPicker = false
                if (pendingWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    widgetHost.deleteAppWidgetId(pendingWidgetId)
                    pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
                    replacingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
                }
            }
        )
    }

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
            postureAlertEnabled = postureAlertEnabled,
            postureSensitivity = postureSensitivity,
            postureDuration = postureDuration,
            postureQuietStart = postureQuietStart,
            postureQuietEnd = postureQuietEnd,
            allApps = allApps,
            limitedApps = limitedApps,
            appBlockStart = appBlockStart,
            appBlockEnd = appBlockEnd,
            forceAllowEnabled = forceAllowEnabled,
            forceAllowCondition = forceAllowCondition,
            mindlessScrollEnabled = mindlessScrollEnabled,
            mindlessScrollMinutes = mindlessScrollMinutes,
            mindlessScrollApps = mindlessScrollApps,
            onModeChange = { viewModel.setMode(it) },
            onShuffleTypeChange = { viewModel.setShuffleType(it) },
            onOpenSelectiveShuffle = { showSelectiveShuffle = true },
            onWallpaperToggle = { viewModel.setUseWallpaper(it) },
            onBlurDrawerToggle = { viewModel.setBlurDrawer(it) },
            onTimeFormatToggle = { viewModel.setIs24Hour(it) },
            onWidgetTypeChange = { viewModel.setWidgetType(it) },
            onIconShapeChange = { viewModel.setIconShape(it) },
            onPostureAlertToggle = { viewModel.setPostureAlertEnabled(it, context) },
            onPostureSensitivityChange = { viewModel.setPostureSensitivity(it, context) },
            onPostureDurationChange = { viewModel.setPostureDuration(it, context) },
            onPostureQuietHoursChange = { start, end -> viewModel.setPostureQuietHours(start, end, context) },
            onToggleLimitedApp = { viewModel.toggleLimitedApp(it) },
            onAppBlockHoursChange = { start, end -> viewModel.setAppBlockHours(start, end) },
            onForceAllowToggle = { viewModel.setForceAllowEnabled(it) },
            onForceAllowConditionChange = { viewModel.setForceAllowCondition(it) },
            onMindlessScrollToggle = { enabled ->
                if (enabled && !viewModel.hasUsageAccess(context)) {
                    viewModel.setMindlessScrollEnabled(true)
                    viewModel.openUsageSettings(context)
                } else {
                    viewModel.setMindlessScrollEnabled(enabled)
                }
            },
            onMindlessScrollMinutesChange = { viewModel.setMindlessScrollMinutes(it) },
            onToggleMindlessScrollApp = { viewModel.toggleMindlessScrollApp(it) },
            onOpenGuide = {
                showSettings = false
                showOnboarding = true
            },
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
                            if (viewModel.isAppBlocked(pkg) && forceAllowEnabled) {
                                forceAllowPackage = pkg
                            } else {
                                viewModel.launchApp(context, pkg)
                                scope.launch { scaffoldState.bottomSheetState.partialExpand() }
                            }
                        },
                        onToggleHome = { viewModel.toggleHomeApp(it) },
                        shouldUseDarkText = shouldUseDarkText,
                        useWallpaper = useWallpaper,
                        blurDrawer = blurDrawer,
                        iconShape = iconShape,
                        launcherMode = mode,
                        blockedApps = blockedApps
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
                    onAppClick = { pkg ->
                        if (viewModel.isAppBlocked(pkg) && forceAllowEnabled) {
                            forceAllowPackage = pkg
                        } else {
                            viewModel.launchApp(context, pkg)
                        }
                    },
                    onOpenSettings = { showSettings = true },
                    onOpenExercises = { showExercisePicker = true },
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
                    blurDrawer = blurDrawer,
                    viewModel = viewModel,
                    useWallpaper = useWallpaper,
                    is24Hour = is24Hour,
                    iconShape = iconShape,
                    homeWidgets = homeWidgets,
                    widgetHost = widgetHost,
                    widgetManager = widgetManager,
                    onAddWidget = {
                        val appWidgetId = widgetHost.allocateAppWidgetId()
                        replacingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
                        pendingWidgetId = appWidgetId
                        showWidgetPicker = true
                    },
                    onReplaceWidget = { oldWidgetId ->
                        val appWidgetId = widgetHost.allocateAppWidgetId()
                        replacingWidgetId = oldWidgetId
                        pendingWidgetId = appWidgetId
                        showWidgetPicker = true
                    },
                    onRemoveWidget = { appWidgetId ->
                        widgetHost.deleteAppWidgetId(appWidgetId)
                        viewModel.removeHomeWidget(appWidgetId)
                    },
                    onResizeWidget = { appWidgetId, deltaX, deltaY, anchorEnd ->
                        viewModel.resizeHomeWidget(appWidgetId, deltaX, deltaY, anchorEnd)
                    },
                    onMoveWidget = { appWidgetId, direction ->
                        viewModel.moveHomeWidget(appWidgetId, direction)
                    },
                    blockedApps = blockedApps
                )
            }
        }
    }

    forceAllowPackage?.let { packageName ->
        ForceAllowFlow(
            condition = forceAllowCondition,
            onOpenAnyway = {
                forceAllowPackage = null
                viewModel.launchApp(context, packageName, ignoreBlock = true)
            },
            onDismiss = { forceAllowPackage = null }
        )
    }

    if (showExercisePicker) {
        ExercisePickerDialog(
            onSelect = { condition ->
                showExercisePicker = false
                exerciseCondition = condition
            },
            onDismiss = { showExercisePicker = false }
        )
    }

    exerciseCondition?.let { condition ->
        StandaloneExerciseFlow(
            condition = condition,
            onDismiss = { exerciseCondition = null }
        )
    }

    if (showOnboarding) {
        OnboardingGuide(
            currentIconShape = iconShape,
            onIconShapeChange = { viewModel.setIconShape(it) },
            onSetDefaultLauncher = {
                context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
            },
            onComplete = ::completeOnboarding
        )
    }
}

@Composable
private fun WidgetPickerDialog(
    widgetManager: AppWidgetManager,
    onSelect: (AppWidgetProviderInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val providers = remember(widgetManager) {
        widgetManager.installedProviders
            .distinctBy { it.provider.flattenToString() }
            .sortedWith(compareBy({ it.loadLabel(context.packageManager).toString().lowercase() }, { it.label }))
    }
    val filteredProviders = providers.filter { provider ->
        val appName = runCatching {
            context.packageManager.getApplicationLabel(
                context.packageManager.getApplicationInfo(provider.provider.packageName, 0)
            ).toString()
        }.getOrDefault("")
        val widgetName = runCatching { provider.loadLabel(context.packageManager).toString() }.getOrDefault("")
        query.isBlank() || "$appName $widgetName".contains(query, ignoreCase = true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.94f),
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Column {
                Text(
                    text = "Add widget",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Choose something useful for your home screen",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    singleLine = true,
                    placeholder = { Text("Search widgets") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = RoundedCornerShape(18.dp)
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = filteredProviders,
                    key = { it.provider.flattenToString() }
                ) { provider ->
                    val icon = remember(provider) {
                        runCatching {
                            provider.loadIcon(
                                context,
                                context.resources.displayMetrics.densityDpi
                            )?.toBitmap(72, 72)?.asImageBitmap()
                        }.getOrNull()
                    }
                    val appName = runCatching {
                        context.packageManager.getApplicationLabel(
                            context.packageManager.getApplicationInfo(provider.provider.packageName, 0)
                        ).toString()
                    }.getOrDefault("Widget")
                    val widgetName = runCatching {
                        provider.loadLabel(context.packageManager).toString()
                    }.getOrDefault("Widget")

                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(provider) },
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(52.dp),
                                shape = RoundedCornerShape(15.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                if (icon != null) {
                                    Image(icon, contentDescription = null, modifier = Modifier.padding(6.dp))
                                } else {
                                    Icon(
                                        Icons.Default.Widgets,
                                        contentDescription = null,
                                        modifier = Modifier.padding(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
                                Text(widgetName, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                                Text(
                                    appName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            Icon(Icons.Default.Widgets, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
