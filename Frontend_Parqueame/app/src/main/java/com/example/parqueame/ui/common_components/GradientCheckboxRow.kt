package com.example.parqueame.ui.common_components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.parqueame.ui.theme.DmSans
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.filterIsInstance
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString

@Composable
fun GradientCheckboxRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    text: String,
    // Recibimos la función onTermsClick para abrir los términos
    onTermsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradientBrush = Brush.linearGradient(
        colors = listOf(Color(0xFF00A1FF), Color(0xFF003099))
    )

    val pressedBrush = Brush.linearGradient(
        colors = listOf(Color(0xFF005BBB), Color(0xFF002766))
    )

    val interactionSource = remember { MutableInteractionSource() }
    var isPressed by remember { mutableStateOf(false) }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions
            .filterIsInstance<PressInteraction>()
            .collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> isPressed = true
                    is PressInteraction.Release, is PressInteraction.Cancel -> isPressed = false
                }
            }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    brush = when {
                        isPressed -> pressedBrush
                        checked -> gradientBrush
                        else -> Brush.linearGradient(
                            colors = listOf(Color.Transparent, Color.Transparent)
                        )
                    },
                    shape = RoundedCornerShape(4.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier
                    .size(20.dp)
                    .indication(interactionSource, null), // sin ripple
                interactionSource = interactionSource,
                colors = CheckboxDefaults.colors(
                    checkedColor = Color.Transparent,  // Transparent para que se vea el fondo del Box
                    uncheckedColor = Color.Gray,
                    checkmarkColor = Color.White,
                    disabledCheckedColor = Color.Gray,
                    disabledUncheckedColor = Color.Transparent,
                    disabledIndeterminateColor = Color.Transparent
                ),
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // El texto que incluye un enlace para abrir los Términos y Condiciones
        Text(
            text = text,
            color = Color.Black,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = DmSans
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Texto normal (no clickeable)
        Text(
            text = "Acepto todos los ",
            color = Color.Black,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = DmSans
        )

        // Hacer solo la parte de "Términos y Condiciones" clickeable
        BasicText(
            text = buildAnnotatedString {
                pushStringAnnotation(tag = "terms", annotation = "terms")
                withStyle(style = SpanStyle(color = Color(0xFF115ED0), textDecoration = TextDecoration.Underline)) {
                    append("Términos y Condiciones")
                }
                pop()
            },
            modifier = Modifier.clickable {
                onTermsClick() // Llamar a la función que maneja la apertura del modal
            }
        )
    }
}
