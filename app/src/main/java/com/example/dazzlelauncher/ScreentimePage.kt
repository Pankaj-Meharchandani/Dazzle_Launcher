package com.example.dazzlelauncher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ScreentimePage(viewModel: LauncherViewModel, shouldUseDarkText: Boolean, useWallpaper: Boolean, iconShape: IconShape) {
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
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(getIconShape(iconShape))
                                    .background(usage.appInfo?.dominantColor?.let { Color(it) } ?: contentColor.copy(alpha = 0.1f))
                            ) {
                                usage.appInfo?.icon?.let {
                                    Image(
                                        bitmap = it, 
                                        contentDescription = null, 
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
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
