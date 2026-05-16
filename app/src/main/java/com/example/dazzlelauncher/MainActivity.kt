package com.example.dazzlelauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.dazzlelauncher.ui.theme.DazzleLauncherTheme

class MainActivity : ComponentActivity() {
    private val viewModel: LauncherViewModel by viewModels()

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_USER_PRESENT) {
                viewModel.shuffleHomeApps()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(unlockReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(unlockReceiver, filter)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Do nothing, we are the home screen
            }
        })

        setContent {
            DazzleLauncherTheme {
                LauncherScreen(viewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(unlockReceiver)
    }
}

@Composable
fun LauncherScreen(viewModel: LauncherViewModel) {
    val homeApps by viewModel.homeApps.collectAsState()
    val allApps by viewModel.allApps.collectAsState()
    val mode by viewModel.mode.collectAsState()
    val context = LocalContext.current

    var showDrawer by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
        ) {
            if (showSettings) {
                SettingsScreen(
                    currentMode = mode,
                    onModeChange = { viewModel.setMode(it) },
                    onClose = { showSettings = false }
                )
            } else if (showDrawer && mode == LauncherMode.HOME_AND_DRAWER) {
                AppDrawer(
                    apps = allApps,
                    homeApps = homeApps,
                    onAppClick = { pkg ->
                        viewModel.launchApp(context, pkg)
                        showDrawer = false
                    },
                    onToggleHome = { viewModel.toggleHomeApp(it) },
                    onClose = { showDrawer = false }
                )
            } else {
                HomeScreen(
                    apps = homeApps,
                    onAppClick = { pkg -> viewModel.launchApp(context, pkg) },
                    onLongClick = { showSettings = true },
                    onSwipeUp = {
                        if (mode == LauncherMode.HOME_AND_DRAWER) {
                            showDrawer = true
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    apps: List<AppInfo>,
    onAppClick: (String) -> Unit,
    onLongClick: () -> Unit,
    onSwipeUp: () -> Unit
) {
    var offsetY by remember { mutableStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        offsetY += dragAmount
                    },
                    onDragEnd = {
                        if (offsetY < -100) {
                            onSwipeUp()
                        }
                        offsetY = 0f
                    }
                )
            }
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick
            )
    ) {
        Spacer(modifier = Modifier.height(64.dp))
        Text(
            text = "Dazzle",
            style = MaterialTheme.typography.displayMedium,
            modifier = Modifier.padding(horizontal = 24.dp),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.weight(1f).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(apps) { app ->
                AppItem(app, onAppClick)
            }
        }
        
        Text(
            text = "Swipe up for apps",
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
fun AppDrawer(
    apps: List<AppInfo>,
    homeApps: List<AppInfo>,
    onAppClick: (String) -> Unit,
    onToggleHome: (AppInfo) -> Unit,
    onClose: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Apps",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onClose) {
                Text("✕")
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.weight(1f).fillMaxSize(),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(apps) { app ->
                val isOnHome = homeApps.any { it.packageName == app.packageName }
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
    onModeChange: (LauncherMode) -> Unit,
    onClose: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Launcher Mode", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = currentMode == LauncherMode.HOME_ONLY,
                onClick = { onModeChange(LauncherMode.HOME_ONLY) }
            )
            Text("Home Only (iOS Style)")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = currentMode == LauncherMode.HOME_AND_DRAWER,
                onClick = { onModeChange(LauncherMode.HOME_AND_DRAWER) }
            )
            Text("Home & App Drawer")
        }
        
        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text("Done")
        }
    }
}

@Composable
fun AppItem(app: AppInfo, onClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .clickable { onClick(app.packageName) }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .padding(4.dp)
        ) {
            app.icon?.let {
                Image(
                    bitmap = it.toBitmap().asImageBitmap(),
                    contentDescription = app.label,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Text(
            text = app.label,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
            lineHeight = 14.sp
        )
    }
}
