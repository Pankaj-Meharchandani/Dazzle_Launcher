package com.example.dazzlelauncher

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import kotlin.math.abs
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
        widgets.forEachIndexed { index, widget ->
            val info = widgetManager.getAppWidgetInfo(widget.appWidgetId)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        enabled = !editMode,
                        onClick = {},
                        onLongClick = { onEditModeChange(true) }
                    )
                    .then(
                        if (editMode) {
                            Modifier.pointerInput(widget.appWidgetId) {
                                var accumulatedDrag = 0f
                                detectDragGesturesAfterLongPress(
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
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(max = 360.dp)
                    ) {
                        if (info != null) {
                            AndroidView(
                                factory = {
                                    widgetHost.createView(context, widget.appWidgetId, info)
                                },
                                modifier = Modifier
                                    .fillMaxWidth(widget.spanX / 4f)
                                    .height((88 * widget.spanY).dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        } else {
                            Text(
                                text = "Widget unavailable",
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (editMode) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row {
                                IconButton(
                                    onClick = { onMoveWidget(widget.appWidgetId, -1) },
                                    enabled = index > 0
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = "Move widget up")
                                }
                                IconButton(
                                    onClick = { onMoveWidget(widget.appWidgetId, 1) },
                                    enabled = index < widgets.lastIndex
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = "Move widget down")
                                }
                            }
                            Row {
                                IconButton(
                                    onClick = { onResizeWidget(widget.appWidgetId, -1, 0) },
                                    enabled = widget.spanX > 1
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Make widget narrower")
                                }
                                IconButton(
                                    onClick = { onResizeWidget(widget.appWidgetId, 1, 0) },
                                    enabled = widget.spanX < 4
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Make widget wider")
                                }
                            }
                            Row {
                                IconButton(
                                    onClick = { onResizeWidget(widget.appWidgetId, 0, -1) },
                                    enabled = widget.spanY > 1
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Make widget shorter")
                                }
                                IconButton(
                                    onClick = { onResizeWidget(widget.appWidgetId, 0, 1) },
                                    enabled = widget.spanY < 3
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Make widget taller")
                                }
                            }
                            IconButton(onClick = { onRemoveWidget(widget.appWidgetId) }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove widget")
                            }
                            IconButton(onClick = { onReplaceWidget(widget.appWidgetId) }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Replace widget")
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}
