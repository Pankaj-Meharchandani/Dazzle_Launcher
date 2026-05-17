package com.example.dazzlelauncher

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

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
    useWallpaper: Boolean,
    is24Hour: Boolean,
    iconShape: IconShape
) {
    val context = LocalContext.current
    var currentTime by remember { mutableStateOf(Calendar.getInstance()) }
    
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Calendar.getInstance()
            viewModel.fetchNextAlarm()
            viewModel.fetchCalendarEvent()
            viewModel.fetchUsageStats()
            val seconds = Calendar.getInstance().get(Calendar.SECOND)
            kotlinx.coroutines.delay((60 - seconds) * 1000L)
        }
    }

    val usageStats by viewModel.usageStats.collectAsState()
    val totalTimeToday = remember(usageStats) { 
        usageStats.sumOf { it.usageTime } 
    }
    val widgetType by viewModel.widgetType.collectAsState()
    val batteryInfo by viewModel.batteryInfo.collectAsState()
    val nextAlarm by viewModel.nextAlarm.collectAsState()
    val calendarEvent by viewModel.calendarEvent.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchUsageStats()
    }

    val timeFormatter = remember(is24Hour) { 
        SimpleDateFormat(if (is24Hour) "HH:mm" else "h:mm", Locale.getDefault()) 
    }
    val amPmFormatter = remember { SimpleDateFormat("a", Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val totalHeight = maxHeight
        // Reserve height for: Top padding (80), Banner (~60), Clock area (~120), Dock area (~150), bottom padding (16)
        val reservedHeight = 80.dp + (if (!isDefault) 60.dp else 0.dp) + 120.dp + 150.dp + 16.dp
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

            val titleText = if (pagerState.currentPage == 0) "Screen Time" else ""
            
            if (pagerState.currentPage == 0) {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 24.dp),
                    color = if (shouldUseDarkText) Color.Black else MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = timeFormatter.format(currentTime.time),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.Normal,
                                fontSize = 56.sp
                            ),
                            color = if (shouldUseDarkText) Color.Black else Color.White
                        )
                        if (!is24Hour) {
                            Text(
                                text = amPmFormatter.format(currentTime.time),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(bottom = 10.dp, start = 4.dp),
                                color = if (shouldUseDarkText) Color.Black else Color.White
                            )
                        }
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = dateFormatter.format(currentTime.time),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (shouldUseDarkText) Color.Black else Color.White
                        )
                        Text(
                            text = when(widgetType) {
                                WidgetType.SCREEN_TIME -> "${formatDuration(totalTimeToday)} Today"
                                WidgetType.BATTERY_TEMP -> batteryInfo
                                WidgetType.NEXT_ALARM -> nextAlarm
                                WidgetType.CALENDAR_EVENT -> calendarEvent
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = if (shouldUseDarkText) Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(44.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                pageSpacing = 16.dp,
                beyondViewportPageCount = 1
            ) { page ->
                if (page == 0) {
                    ScreentimePage(viewModel = viewModel, shouldUseDarkText = shouldUseDarkText, useWallpaper = useWallpaper, iconShape = iconShape)
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
                                labelColor = if (shouldUseDarkText) Color.Black else Color.White,
                                iconShape = iconShape
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .clickable {
                                val packageName = "com.example.waller"
                                val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                                if (intent != null) {
                                    context.startActivity(intent)
                                } else {
                                    val browserIntent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://github.com/Pankaj-Meharchandani/Waller/releases/latest")
                                    )
                                    context.startActivity(browserIntent)
                                }
                            }
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(getIconShape(iconShape))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Wallpaper,
                                contentDescription = "Wallpaper",
                                modifier = Modifier.size(32.dp),
                                tint = if (shouldUseDarkText || !isSystemInDarkTheme()) Color.Black else Color.White
                            )
                        }
                        Text(
                            text = "Wallpaper",
                            fontSize = 11.sp,
                            color = if (shouldUseDarkText) Color.Black else Color.White,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .clickable { onOpenSettings() }
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(getIconShape(iconShape))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Launcher Settings",
                                modifier = Modifier.size(32.dp),
                                tint = if (shouldUseDarkText || !isSystemInDarkTheme()) Color.Black else Color.White
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
                Dock(apps = dockApps, onAppClick = onAppClick, onLongClick = onToggleDock, iconShape = iconShape)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun Dock(apps: List<AppInfo>, onAppClick: (String) -> Unit, onLongClick: (AppInfo) -> Unit, iconShape: IconShape) {
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
                        showLabel = false,
                        iconShape = iconShape
                    )
                }
            }
        }
    }
}
