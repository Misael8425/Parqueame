package com.example.parqueame.ui.common_components

import androidx.compose.foundation.Indication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.parqueame.ui.theme.GradientEnd
import com.example.parqueame.ui.theme.GradientStart
import androidx.compose.ui.unit.Dp
import com.example.parqueame.ui.theme.dmSans

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    textColor: Color = Color.White,
    gradientColors: List<Color> = listOf(GradientStart, GradientEnd),
    shape: RoundedCornerShape = RoundedCornerShape(30.dp),
    paddingHorizontal: Dp = 16.dp,
    paddingVertical: Dp = 14.dp,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    indication: Indication? = null // Cambiado de LocalIndication.current a null

) {
    Box(
        modifier = modifier
            .background(
                brush = if (enabled) Brush.verticalGradient(gradientColors)
                else Brush.verticalGradient(listOf(Color.Gray.copy(alpha = 0.6f), Color.Gray.copy(alpha = 0.6f))),
                shape = shape
            )
            .then(
                if (enabled) Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = indication,
                    onClick = onClick
                )
                else Modifier
            )
            .padding(vertical = paddingVertical, horizontal = paddingHorizontal),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            fontFamily = dmSans,

        )
    }
}