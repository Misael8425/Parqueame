package com.example.parqueame.ui.parqueos

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.airbnb.lottie.compose.*
import com.example.parqueame.R

@Composable
fun ParkingAnimation(
    modifier: Modifier = Modifier,
    iterations: Int = 1,
    speed: Float = 1.0f,
    onFinished: (() -> Unit)? = null
) {
    // Cargamos la animación desde res/raw
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.parking_car)
    )

    // Control de progreso/estado
    val animState = animateLottieCompositionAsState(
        composition = composition,
        iterations = iterations,
        speed = speed,
        restartOnPlay = false
    )

    // Callback opcional al terminar
    LaunchedEffect(animState.isAtEnd && animState.isPlaying.not()) {
        if (composition != null && animState.isAtEnd && !animState.isPlaying) {
            onFinished?.invoke()
        }
    }

    // Render
    if (composition != null) {
        LottieAnimation(
            composition = composition,
            progress = { animState.progress },
            modifier = modifier
        )
    } else {
        // Fallback si no carga (mismo look que tu placeholder)
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFF4F7FE)),
            contentAlignment = Alignment.Center
        ) {
            // Puedes dejarlo vacío o poner tu ícono parking
        }
    }
}