package com.example.dazzlelauncher

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
fun SettingsScreen(
    currentMode: LauncherMode,
    shuffleType: ShuffleType,
    useWallpaper: Boolean,
    blurDrawer: Boolean,
    is24Hour: Boolean,
    widgetType: WidgetType,
    iconShape: IconShape,
    postureAlertEnabled: Boolean,
    postureSensitivity: Int,
    postureDuration: Int,
    postureQuietStart: String,
    postureQuietEnd: String,
    onModeChange: (LauncherMode) -> Unit,
    onShuffleTypeChange: (ShuffleType) -> Unit,
    onOpenSelectiveShuffle: () -> Unit,
    onWallpaperToggle: (Boolean) -> Unit,
    onBlurDrawerToggle: (Boolean) -> Unit,
    onTimeFormatToggle: (Boolean) -> Unit,
    onWidgetTypeChange: (WidgetType) -> Unit,
    onIconShapeChange: (IconShape) -> Unit,
    onPostureAlertToggle: (Boolean) -> Unit,
    onPostureSensitivityChange: (Int) -> Unit,
    onPostureDurationChange: (Int) -> Unit,
    onPostureQuietHoursChange: (String, String) -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)
    
    val context = LocalContext.current
    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (android.provider.Settings.canDrawOverlays(context)) {
            onPostureAlertToggle(true)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(), 
        color = MaterialTheme.colorScheme.background
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    "Settings", 
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            SettingsSection(title = "Launcher Mode") {
                ModeOption(
                    title = "Classic",
                    description = "Home screen & App Drawer",
                    selected = currentMode == LauncherMode.HOME_AND_DRAWER,
                    onClick = { onModeChange(LauncherMode.HOME_AND_DRAWER) }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                ModeOption(
                    title = "Minimal",
                    description = "All apps on home screen",
                    selected = currentMode == LauncherMode.HOME_ONLY,
                    onClick = { onModeChange(LauncherMode.HOME_ONLY) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SettingsSection(title = "Shuffling") {
                ModeOption(
                    title = "Full Shuffle",
                    description = "Shuffle all apps on home screen",
                    selected = shuffleType == ShuffleType.FULL,
                    onClick = { onShuffleTypeChange(ShuffleType.FULL) }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                ModeOption(
                    title = "Selective Shuffle",
                    description = "Shuffle only selected apps",
                    selected = shuffleType == ShuffleType.SELECTIVE,
                    onClick = { onShuffleTypeChange(ShuffleType.SELECTIVE) }
                )

                if (shuffleType == ShuffleType.SELECTIVE) {
                    SettingsClickableItem(
                        icon = Icons.Default.Edit,
                        title = "Select Apps",
                        value = "Choose apps to shuffle",
                        onClick = onOpenSelectiveShuffle
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            SettingsSection(title = "Appearance") {
                SettingsToggleItem(
                    icon = Icons.Default.Wallpaper,
                    title = "System Wallpaper",
                    description = "Use your device's current wallpaper",
                    checked = useWallpaper,
                    onCheckedChange = onWallpaperToggle
                )
                
                SettingsToggleItem(
                    icon = Icons.Default.BlurOn,
                    title = "Frosted Glass Effect",
                    description = "Blurred background in app drawer",
                    checked = blurDrawer,
                    onCheckedChange = onBlurDrawerToggle
                )
                
                SettingsToggleItem(
                    icon = Icons.Default.Schedule,
                    title = "24-Hour Clock",
                    description = "Switch between 12h and 24h format",
                    checked = is24Hour,
                    onCheckedChange = onTimeFormatToggle
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            SettingsSection(title = "Customization") {
                var showWidgetSelector by remember { mutableStateOf(false) }
                val widgetLabel = when(widgetType) {
                    WidgetType.SCREEN_TIME -> "Screen Time"
                    WidgetType.NEXT_ALARM -> "Next Alarm"
                    WidgetType.BATTERY_TEMP -> "Battery & Temperature"
                    WidgetType.CALENDAR_EVENT -> "Calendar Event"
                }

                SettingsClickableItem(
                    icon = Icons.Default.Widgets,
                    title = "Home Widget",
                    value = widgetLabel,
                    onClick = { showWidgetSelector = true }
                )

                if (showWidgetSelector) {
                    WidgetSelectorDialog(
                        selectedType = widgetType,
                        onTypeSelected = { 
                            onWidgetTypeChange(it)
                            showWidgetSelector = false 
                        },
                        onDismiss = { showWidgetSelector = false }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Icon Shape", 
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconShapeOption("Hex", IconShape.HEX, iconShape == IconShape.HEX) { onIconShapeChange(IconShape.HEX) }
                    IconShapeOption("Squircle", IconShape.SQUIRCLE, iconShape == IconShape.SQUIRCLE) { onIconShapeChange(IconShape.SQUIRCLE) }
                    IconShapeOption("Round", IconShape.ROUND, iconShape == IconShape.ROUND) { onIconShapeChange(IconShape.ROUND) }
                    IconShapeOption("Square", IconShape.SQUARE, iconShape == IconShape.SQUARE) { onIconShapeChange(IconShape.SQUARE) }
                    IconShapeOption("Octagon", IconShape.OCTAGON, iconShape == IconShape.OCTAGON) { onIconShapeChange(IconShape.OCTAGON) }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            SettingsSection(title = "Health") {
                SettingsToggleItem(
                    icon = Icons.Default.AccessibilityNew,
                    title = "Posture Alert",
                    description = "Alert when holding phone at an unhealthy angle",
                    checked = postureAlertEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            if (android.provider.Settings.canDrawOverlays(context)) {
                                onPostureAlertToggle(true)
                            } else {
                                val intent = Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                                overlayPermissionLauncher.launch(intent)
                            }
                        } else {
                            onPostureAlertToggle(false)
                        }
                    }
                )

                if (postureAlertEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Sensitivity",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SensitivityOption("Relaxed", 30, postureSensitivity == 30) { onPostureSensitivityChange(30) }
                        SensitivityOption("Normal", 45, postureSensitivity == 45) { onPostureSensitivityChange(45) }
                        SensitivityOption("Strict", 60, postureSensitivity == 60) { onPostureSensitivityChange(60) }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Duration Before Alert",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DurationOption("15s", 15, postureDuration == 15) { onPostureDurationChange(15) }
                        DurationOption("30s", 30, postureDuration == 30) { onPostureDurationChange(30) }
                        DurationOption("60s", 60, postureDuration == 60) { onPostureDurationChange(60) }
                        DurationOption("2m", 120, postureDuration == 120) { onPostureDurationChange(120) }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    SettingsClickableItem(
                        icon = Icons.Default.Bedtime,
                        title = "Quiet Hours",
                        value = "$postureQuietStart - $postureQuietEnd",
                        onClick = {
                            val startParts = postureQuietStart.split(":")
                            TimePickerDialog(context, { _, h, m ->
                                val newStart = "%02d:%02d".format(h, m)
                                val endParts = postureQuietEnd.split(":")
                                TimePickerDialog(context, { _, eh, em ->
                                    val newEnd = "%02d:%02d".format(eh, em)
                                    onPostureQuietHoursChange(newStart, newEnd)
                                }, endParts[0].toInt(), endParts[1].toInt(), true).show()
                            }, startParts[0].toInt(), startParts[1].toInt(), true).show()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                content = content
            )
        }
    }
}

@Composable
fun SettingsToggleItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            shape = CircleShape
        ) {
            Icon(
                icon, 
                contentDescription = null, 
                modifier = Modifier.padding(8.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked, 
            onCheckedChange = onCheckedChange,
            thumbContent = if (checked) {
                {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                    )
                }
            } else null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                checkedIconColor = MaterialTheme.colorScheme.onPrimary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                uncheckedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

@Composable
fun SettingsClickableItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
            shape = CircleShape
        ) {
            Icon(
                icon, 
                contentDescription = null, 
                modifier = Modifier.padding(8.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight, 
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ModeOption(title: String, description: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                Text(
                    title, 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Bold,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    description, 
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RadioButton(
                selected = selected, 
                onClick = null,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
fun WidgetSelectorDialog(
    selectedType: WidgetType,
    onTypeSelected: (WidgetType) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onTypeSelected(WidgetType.CALENDAR_EVENT)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Widget") },
        text = {
            Column {
                WidgetOption("Screen Time", selectedType == WidgetType.SCREEN_TIME) {
                    onTypeSelected(WidgetType.SCREEN_TIME)
                }
                WidgetOption("Next Alarm", selectedType == WidgetType.NEXT_ALARM) {
                    onTypeSelected(WidgetType.NEXT_ALARM)
                }
                WidgetOption("Battery & Temperature", selectedType == WidgetType.BATTERY_TEMP) {
                    onTypeSelected(WidgetType.BATTERY_TEMP)
                }
                WidgetOption("Calendar Event", selectedType == WidgetType.CALENDAR_EVENT) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
                        onTypeSelected(WidgetType.CALENDAR_EVENT)
                    } else {
                        calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun WidgetOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun IconShapeOption(label: String, shape: IconShape, selected: Boolean, onClick: () -> Unit) {
    val displayShape = getIconShape(shape)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }.padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(displayShape)
                .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 4.dp),
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun SensitivityOption(label: String, angle: Int, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) }
    )
}

@Composable
fun DurationOption(label: String, seconds: Int, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) }
    )
}
