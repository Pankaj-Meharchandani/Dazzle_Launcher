package com.example.dazzlelauncher

import android.widget.ImageView
import android.widget.ImageView.ScaleType
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.res.painterResource
import kotlin.math.abs

@Composable
fun OnboardingGuide(
    currentIconShape: IconShape,
    onIconShapeChange: (IconShape) -> Unit,
    onSetDefaultLauncher: () -> Unit,
    onComplete: () -> Unit
) {
    var page by remember { mutableIntStateOf(0) }
    val pages = listOf(
        "Welcome to Dazzle" to "A calmer launcher designed to help you spend less time on your phone and more time on what matters.",
        "Make distraction less automatic" to "Dazzle changes the usual home-screen routine, helping you pause before opening distracting apps.",
        "Your dock stays put" to "Dock apps never shuffle. Keep your essential tools there, while other apps can move around to break autopilot habits.",
        "Make it yours" to "Choose an icon shape now. You can change this, your wallpaper, and other details later in Settings.",
        "Ready to reduce screen time?" to "Set Dazzle as your default launcher to make these calmer choices part of your everyday routine."
    )
    val shapeOptions = listOf(
        IconShape.HEX,
        IconShape.SQUIRCLE,
        IconShape.ROUND,
        IconShape.SQUARE,
        IconShape.OCTAGON
    )
    fun movePage(delta: Int) {
        page = (page + delta).coerceIn(0, pages.lastIndex)
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var moved = false
                detectHorizontalDragGestures(
                    onDragStart = { moved = false },
                    onHorizontalDrag = { _, dragAmount ->
                        if (!moved && abs(dragAmount) > 24f) {
                            movePage(if (dragAmount < 0) 1 else -1)
                            moved = true
                        }
                    }
                )
            },
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            if (page == 0) {
                AndroidView(
                    factory = { viewContext ->
                        ImageView(viewContext).apply {
                            setImageResource(R.mipmap.ic_launcher)
                            scaleType = ScaleType.CENTER_INSIDE
                        }
                    },
                    modifier = Modifier.size(144.dp),
                    update = { it.setImageResource(R.mipmap.ic_launcher) }
                )
                Spacer(modifier = Modifier.height(28.dp))
            } else if (page == 2) {
                Image(
                    painter = painterResource(R.drawable.dock),
                    contentDescription = "Dazzle dock preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.height(28.dp))
            } else if (page == 3) {
                IconShapePreview(
                    shape = currentIconShape,
                    shapeOptions = shapeOptions,
                    onShapeChange = onIconShapeChange,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            Text(
                text = pages[page].first,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = pages[page].second,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (page == pages.lastIndex) {
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedButton(onClick = onSetDefaultLauncher) {
                    Text("Set as default launcher")
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                pages.indices.forEach { index ->
                    Spacer(
                        modifier = Modifier
                            .size(if (index == page) 22.dp else 8.dp, 8.dp)
                            .background(
                                if (index == page) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                RoundedCornerShape(8.dp)
                            )
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onComplete) {
                    Text("Skip")
                }
                Button(onClick = {
                    if (page == pages.lastIndex) onComplete() else movePage(1)
                }) {
                    Text(if (page == pages.lastIndex) "Finish" else "Next")
                }
            }
        }
    }
}

@Composable
private fun IconShapePreview(
    shape: IconShape,
    shapeOptions: List<IconShape>,
    onShapeChange: (IconShape) -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (androidx.compose.foundation.isSystemInDarkTheme()) Color.White else Color.Black

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(128.dp)
                .background(backgroundColor, RoundedCornerShape(20.dp))
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { context ->
                    ImageView(context).apply {
                        setImageResource(R.mipmap.ic_launcher)
                        scaleType = ScaleType.CENTER_INSIDE
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .clip(getIconShape(shape)),
                update = { it.setImageResource(R.mipmap.ic_launcher) }
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            shapeOptions.chunked(3).forEach { rowOptions ->
                Row(horizontalArrangement = Arrangement.Center) {
                    rowOptions.forEach { option ->
                        FilterChip(
                            selected = option == shape,
                            onClick = { onShapeChange(option) },
                            label = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
