package com.example.parqueame.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset

val GradientBrush = Brush.linearGradient(
    colors = listOf(GradientStart, GradientEnd),
    start = Offset.Zero,
    end = Offset(0f, 100f)
)
