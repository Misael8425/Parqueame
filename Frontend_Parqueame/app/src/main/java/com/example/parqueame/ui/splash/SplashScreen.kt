package com.example.parqueame.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.lerp
import com.example.parqueame.ui.theme.rtlRomman
import kotlinx.coroutines.delay
import androidx.compose.ui.res.stringResource
import com.example.parqueame.R

@Composable
fun SplashScreen(
    onBackgroundColorChange: (Color) -> Unit,
    onSplashFinished: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1500)
        startAnimation = true
        delay(2500)
        onSplashFinished()
    }

    val animationProgress = splashAnimation(startAnimation, onBackgroundColorChange)

    val topGradientColor = lerp(Color.White, Color(0xFF00A1FF), animationProgress)
    val backgroundColor = lerp(Color.White, Color(0xFF003099), animationProgress)

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(topGradientColor, backgroundColor),
        startY = 0f,
        endY = 1000f
    )

    val interpolatedBrush = remember(animationProgress) {
        Brush.verticalGradient(
            colors = listOf(
                lerp(Color(0xFF00A1FF), Color.White, animationProgress),
                lerp(Color(0xFF003099), Color.White, animationProgress)
            ),
            startY = 0f,
            endY = 200f
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = backgroundBrush),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.splash_logo_letter), // ← reemplazo literal
            fontSize = 140.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = rtlRomman,
            style = TextStyle(brush = interpolatedBrush)
        )
    }
}
