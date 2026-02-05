package com.example.parqueame.utils

import androidx.compose.ui.graphics.Color

fun Color.isLight(): Boolean {
    val darkness = 1 - (0.299 * red + 0.587 * green + 0.114 * blue)
    return darkness < 0.5
}
