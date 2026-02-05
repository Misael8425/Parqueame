package com.example.parqueame.ui.common_components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.parqueame.ui.theme.GradientBrush // tu brush (ThemeExtensions.kt)
import com.example.parqueame.ui.theme.GradientEnd
import com.example.parqueame.ui.theme.GradientStart

@Composable
fun GradientIcon(
    imageVector: ImageVector,
    contentDescription: String? = null,
    modifier: Modifier = Modifier
) {
    Icon(
        painter = rememberVectorPainter(image = imageVector),
        contentDescription = contentDescription,
        tint = Color.Unspecified,              // SIN color sólido
        modifier = modifier
            .size(24.dp)
            .graphicsLayer(alpha = 0.99f)     // fuerza layer para el shader
            .drawWithCache {
                val gradient = Brush.verticalGradient(
                    colors = listOf(GradientStart, GradientEnd),
                    startY = 0f,
                    endY = size.height // 👈 limita el gradiente a la altura real del ícono
                )
                onDrawWithContent {
                    drawContent()              // dibuja el vector
                    drawRect(                  // aplica el degradado sobre su forma
                        brush = gradient, // (definido en ThemeExtensions.kt)
                        blendMode = BlendMode.SrcAtop
                    )
                }
            }
    )
}
