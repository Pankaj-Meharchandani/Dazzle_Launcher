package com.example.dazzlelauncher

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ForceAllowFlow(
    condition: ForceAllowCondition,
    onOpenAnyway: () -> Unit,
    onDismiss: () -> Unit
) {
    var stage by remember(condition) { mutableStateOf(ForceAllowStage.WARNING) }

    when {
        condition == ForceAllowCondition.NONE -> BlockedWarningDialog(
            condition = condition,
            onOpenAnyway = onOpenAnyway,
            onContinue = {},
            onDismiss = onDismiss
        )
        stage == ForceAllowStage.WARNING -> BlockedWarningDialog(
            condition = condition,
            onOpenAnyway = {},
            onContinue = { stage = ForceAllowStage.EXERCISE },
            onDismiss = onDismiss
        )
        condition == ForceAllowCondition.BREATHING -> BreathingDialog(onOpenAnyway, onDismiss)
        condition == ForceAllowCondition.WAIT -> WaitDialog(onOpenAnyway, onDismiss)
        else -> NeckExerciseDialog(onOpenAnyway, onDismiss)
    }
}

@Composable
fun ExercisePickerDialog(
    onSelect: (ForceAllowCondition) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose an exercise") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onSelect(ForceAllowCondition.BREATHING) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Breathing exercise")
                }
                Button(
                    onClick = { onSelect(ForceAllowCondition.NECK) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Neck exercise")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun StandaloneExerciseFlow(
    condition: ForceAllowCondition,
    onDismiss: () -> Unit
) {
    when (condition) {
        ForceAllowCondition.BREATHING -> BreathingDialog(onDismiss, onDismiss)
        ForceAllowCondition.NECK -> NeckExerciseDialog(onDismiss, onDismiss)
        else -> onDismiss()
    }
}

private enum class ForceAllowStage { WARNING, EXERCISE }

@Composable
private fun BlockedWarningDialog(
    condition: ForceAllowCondition,
    onOpenAnyway: () -> Unit,
    onContinue: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("This app is blocked") },
        text = { Text("You shouldn't open this app during your scheduled focus hours.") },
        dismissButton = {
            TextButton(onClick = if (condition == ForceAllowCondition.NONE) onOpenAnyway else onDismiss) {
                Text(if (condition == ForceAllowCondition.NONE) "Open anyway" else "Cancel")
            }
        },
        confirmButton = {
            Button(onClick = if (condition == ForceAllowCondition.NONE) onDismiss else onContinue) {
                Text(if (condition == ForceAllowCondition.NONE) "Don't open" else "Continue")
            }
        }
    )
}

@Composable
private fun BreathingDialog(onComplete: () -> Unit, onCancel: () -> Unit) {
    var remaining by remember { mutableIntStateOf(60) }
    LaunchedEffect(Unit) {
        while (remaining > 0) {
            delay(1000)
            remaining--
        }
        onComplete()
    }

    val elapsed = 60 - remaining
    val isInhale = (elapsed / 4) % 2 == 0
    val hapticFeedback = LocalHapticFeedback.current
    LaunchedEffect(isInhale) {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    val flowerScale by animateFloatAsState(
        targetValue = if (isInhale) 1f else 0.62f,
        animationSpec = tween(1800),
        label = "breathingFlowerScale"
    )
    val rotation by rememberInfiniteTransition(label = "flowerRotation").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6000), RepeatMode.Restart),
        label = "flowerRotation"
    )

    ExerciseDialog(title = "Breathe", onCancel = onCancel) {
        Text(if (isInhale) "Inhale" else "Exhale", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        Flower(
            modifier = Modifier.size(180.dp),
            flowerScale = flowerScale,
            rotation = rotation
        )
        Spacer(Modifier.height(12.dp))
        Text(formatSeconds(remaining), style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun WaitDialog(onComplete: () -> Unit, onCancel: () -> Unit) {
    var remaining by remember { mutableIntStateOf(60) }
    LaunchedEffect(Unit) {
        while (remaining > 0) {
            delay(1000)
            remaining--
        }
        onComplete()
    }

    ExerciseDialog(title = "Take a pause", onCancel = onCancel) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp)) {
            CircularProgressIndicator(
                progress = { remaining / 60f },
                modifier = Modifier.size(170.dp),
                strokeWidth = 12.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(formatSeconds(remaining), style = MaterialTheme.typography.headlineMedium)
        }
        Spacer(Modifier.height(16.dp))
        Text("Wait before opening this app")
    }
}

@Composable
private fun NeckExerciseDialog(onComplete: () -> Unit, onCancel: () -> Unit) {
    var remaining by remember { mutableIntStateOf(30) }
    LaunchedEffect(Unit) {
        while (remaining > 0) {
            delay(1000)
            remaining--
        }
        onComplete()
    }

    val steps = listOf(
        "Look left" to Icons.Default.ArrowBack,
        "Look right" to Icons.Default.ArrowForward,
        "Look up" to Icons.Default.ArrowUpward,
        "Look down" to Icons.Default.ArrowDownward
    )
    val stepIndex = ((30 - remaining) / 8).coerceAtMost(3)
    val progress = (30 - remaining) / 30f
    val hapticFeedback = LocalHapticFeedback.current
    LaunchedEffect(stepIndex) {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    ExerciseDialog(title = "Neck exercise", onCancel = onCancel) {
        Icon(Icons.Default.Face, contentDescription = null, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(8.dp))
        Icon(steps[stepIndex].second, contentDescription = null, modifier = Modifier.size(74.dp))
        Text(steps[stepIndex].first, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        CircularProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text(formatSeconds(remaining))
    }
}

@Composable
private fun ExerciseDialog(
    title: String,
    onCancel: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 6.dp) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(title, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(20.dp))
                content()
                Spacer(Modifier.height(20.dp))
                OutlinedButton(onClick = onCancel) { Text("Cancel") }
            }
        }
    }
}

@Composable
private fun Flower(modifier: Modifier, flowerScale: Float, rotation: Float) {
    val flowerColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        rotate(rotation, center) {
            scale(flowerScale, center) {
                val radius = size.minDimension * 0.22f
                val petalRadius = size.minDimension * 0.17f
                repeat(8) { index ->
                    val angle = Math.toRadians(index * 45.0)
                    drawCircle(
                        color = flowerColor,
                        radius = petalRadius,
                        center = Offset(
                            center.x + cos(angle).toFloat() * radius,
                            center.y + sin(angle).toFloat() * radius
                        )
                    )
                }
                drawCircle(flowerColor, size.minDimension * 0.16f, center)
            }
        }
    }
}

private fun formatSeconds(value: Int): String = "%02d:%02d".format(value / 60, value % 60)
