package com.example.dazzlelauncher

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.cos
import kotlin.math.sin

class HexagonShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        return Outline.Generic(
            path = Path().apply {
                val radius = size.width / 2f
                val centerX = size.width / 2f
                val centerY = size.height / 2f
                for (i in 0..5) {
                    val angle = Math.toRadians(60.0 * i - 90.0)
                    val x = centerX + radius * cos(angle).toFloat()
                    val y = centerY + radius * sin(angle).toFloat()
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
        )
    }
}

class SquircleShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        return Outline.Generic(
            path = Path().apply {
                val width = size.width
                val height = size.height
                val radius = width / 2f
                
                moveTo(0f, radius)
                cubicTo(0f, 0f, 0f, 0f, radius, 0f)
                cubicTo(width, 0f, width, 0f, width, radius)
                cubicTo(width, height, width, height, radius, height)
                cubicTo(0f, height, 0f, height, 0f, radius)
                close()
            }
        )
    }
}

class FlowerShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        return Outline.Generic(
            path = Path().apply {
                val radius = size.width / 2f
                val centerX = size.width / 2f
                val centerY = size.height / 2f
                
                for (i in 0..7) {
                    val angle = Math.toRadians(45.0 * i)
                    val r = if (i % 2 == 0) radius else radius * 0.75f
                    val x = centerX + r * cos(angle).toFloat()
                    val y = centerY + r * sin(angle).toFloat()
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
        )
    }
}
