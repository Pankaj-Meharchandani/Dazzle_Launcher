package com.example.dazzlelauncher

import androidx.compose.ui.graphics.ImageBitmap

data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: ImageBitmap? = null
)
