package com.example.dazzlelauncher

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import kotlin.math.abs
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeWidgets(
    widgets: List<HomeWidget>,
    widgetHost: AppWidgetHost,
    widgetManager: AppWidgetManager,
    modifier: Modifier = Modifier,
    editMode: Boolean,
    onEditModeChange: (Boolean) -> Unit,
    onReplaceWidget: (Int) -> Unit,
    onRemoveWidget: (Int) -> Unit,
    onResizeWidget: (Int, Int, Int) -> Unit,
    onMoveWidget: (Int, Int) -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        widgets.forEach { widget ->
            val info = widgetManager.getAppWidgetInfo(widget.appWidgetId)
            val widgetHeight = (88 * widget.spanY + 16).dp

            Surface(
                modifier = Modifier
                    .fillMaxWidth(widget.spanX / 4f)
                    .height(widgetHeight)
                    .combinedClickable(
                        enabled = !editMode,
                        onClick = {},
                        onLongClick = { onEditModeChange(true) }
                    )
                    .then(
                        if (editMode) {
                            Modifier.pointerInput(widget.appWidgetId) {
                                var accumulatedDrag = 0f
                                detectDragGestures(
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        accumulatedDrag += dragAmount.y
                                        if (abs(accumulatedDrag) >= 64f) {
                                            onMoveWidget(widget.appWidgetId, if (accumulatedDrag < 0) -1 else 1)
                                            accumulatedDrag = 0f
                                        }
                                    },
                                    onDragEnd = { accumulatedDrag = 0f },
                                    onDragCancel = { accumulatedDrag = 0f }
                                )
                            }
                        } else Modifier
                    ),
                color = Color.Transparent,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (info != null) {
                        AndroidView(
                            factory = {
                                widgetHost.createView(context, widget.appWidgetId, info)
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }

                    if (editMode) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp)
                                .border(
                                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                                    RoundedCornerShape(16.dp)
                                )
                        )

                        ResizeHandle(
                            modifier = Modifier.align(Alignment.TopCenter),
                            axis = ResizeAxis.VERTICAL,
                            outwardDirection = -1f,
                            onResize = { delta -> onResizeWidget(widget.appWidgetId, 0, delta) }
                        )
                        ResizeHandle(
                            modifier = Modifier.align(Alignment.BottomCenter),
                            axis = ResizeAxis.VERTICAL,
                            outwardDirection = 1f,
                            onResize = { delta -> onResizeWidget(widget.appWidgetId, 0, delta) }
                        )
                        ResizeHandle(
                            modifier = Modifier.align(Alignment.CenterStart),
                            axis = ResizeAxis.HORIZONTAL,
                            outwardDirection = -1f,
                            onResize = { delta -> onResizeWidget(widget.appWidgetId, delta, 0) }
                        )
                        ResizeHandle(
                            modifier = Modifier.align(Alignment.CenterEnd),
                            axis = ResizeAxis.HORIZONTAL,
                            outwardDirection = 1f,
                            onResize = { delta -> onResizeWidget(widget.appWidgetId, delta, 0) }
                        )

                        IconButton(
                            onClick = { onRemoveWidget(widget.appWidgetId) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(2.dp)
                                .size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove widget",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

private enum class ResizeAxis { HORIZONTAL, VERTICAL }

@Composable
private fun ResizeHandle(
    modifier: Modifier,
    axis: ResizeAxis,
    outwardDirection: Float,
    onResize: (Int) -> Unit
) {
    Box(
        modifier = modifier
            .size(16.dp)
            .clip(CircleShape)
            .pointerInput(axis, outwardDirection) {
                var accumulatedDrag = 0f
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val movement = if (axis == ResizeAxis.HORIZONTAL) dragAmount.x else dragAmount.y
                        accumulatedDrag += movement
                        if (abs(accumulatedDrag) >= 32f) {
                            val delta = if (accumulatedDrag * outwardDirection > 0) 1 else -1
                            onResize(delta)
                            accumulatedDrag = 0f
                        }
                    },
                    onDragEnd = { accumulatedDrag = 0f },
                    onDragCancel = { accumulatedDrag = 0f }
                )
            }
            .background(MaterialTheme.colorScheme.primary)
    )
}
