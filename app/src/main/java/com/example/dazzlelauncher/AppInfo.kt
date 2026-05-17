package com.example.dazzlelauncher

import androidx.compose.ui.graphics.ImageBitmap

data class AppInfo(
    val label: String,
    val packageName: String,
    val className: String,
    val icon: ImageBitmap? = null,
    val dominantColor: Int? = null
) {
    val key: String get() = "$packageName/$className"
}
