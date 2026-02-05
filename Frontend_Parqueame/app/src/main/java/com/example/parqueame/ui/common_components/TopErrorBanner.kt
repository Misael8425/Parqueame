package com.example.parqueame.ui.common_components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.parqueame.ui.theme.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay


@Composable
fun TopErrorBanner(
    show: Boolean,
    message: String
) {
    key(message) {
        AnimatedVisibility(
            visible = show,
            enter = slideInVertically(initialOffsetY = { -100 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -100 }) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .widthIn(min = 260.dp, max = 343.dp)        // como en prototipo
                    .defaultMinSize(minHeight = 240.dp)          // altura mínima de burbuja
                    .wrapContentHeight()
                    .background(
                        color = SoftPink,
                        shape = RoundedCornerShape(18.727.dp)
                    )
                    .padding(horizontal = 40.dp, vertical = 25.dp), // buen espacio interno
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = message,
                    color = IntenseRed,
                    fontFamily = DmSans,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center // centrado en todo caso
                )
            }
        }
    }
}

@Composable
fun rememberTopErrorBanner(): (String) -> Unit {
    val message = remember { mutableStateOf("") }
    val isVisible = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Dibujar el banner solo si hay mensaje visible
    if (isVisible.value) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f),
            contentAlignment = Alignment.TopCenter
        ) {
            TopErrorBanner(
                show = isVisible.value,
                message = message.value
            )
        }
    }

    // Función que puedes reutilizar
    return remember {
        { msg: String ->
            scope.launch {
                message.value = ""
                delay(50) // pequeño delay para reiniciar animación si es el mismo texto
                message.value = msg
                isVisible.value = true
                delay(3000)
                isVisible.value = false
            }
        }
    }
}

