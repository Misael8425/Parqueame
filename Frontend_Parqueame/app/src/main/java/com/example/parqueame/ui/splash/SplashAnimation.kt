package com.example.parqueame.ui.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

@Composable
fun splashAnimation(
    startAnimation: Boolean,
    onBackgroundColorChange: (Color) -> Unit
): Float {
    val animationProgress by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 2000)
    )

    val backgroundColor = lerp(Color.White, Color(0xFF003099), animationProgress)
    onBackgroundColorChange(backgroundColor)

    return animationProgress
}
