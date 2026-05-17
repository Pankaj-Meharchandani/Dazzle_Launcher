package com.example.dazzlelauncher

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun getIconShape(shapeType: IconShape): Shape {
    return when (shapeType) {
        IconShape.HEX -> HexagonShape()
        IconShape.SQUIRCLE -> SquircleShape()
        IconShape.ROUND -> CircleShape
        IconShape.SQUARE -> RoundedCornerShape(12.dp)
        IconShape.OCTAGON -> OctagonShape()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppItem(
    app: AppInfo, 
    onClick: (String) -> Unit, 
    onLongClick: () -> Unit = {},
    showLabel: Boolean = true,
    labelColor: Color = if (isSystemInDarkTheme()) Color.White else MaterialTheme.colorScheme.onSurface,
    iconShape: IconShape = IconShape.HEX
) {
    val shape = getIconShape(iconShape)
    Column(
        modifier = Modifier
            .combinedClickable(
                onClick = { onClick(app.packageName) },
                onLongClick = onLongClick
            )
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(shape)
                .background(app.dominantColor?.let { Color(it) } ?: MaterialTheme.colorScheme.surfaceVariant)
        ) {
            app.icon?.let {
                Image(
                    bitmap = it,
                    contentDescription = app.label,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        if (showLabel) {
            Text(
                text = app.label,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
                lineHeight = 14.sp,
                color = labelColor,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.5f),
                        offset = Offset(1f, 1f),
                        blurRadius = 3f
                    )
                )
            )
        }
    }
}
