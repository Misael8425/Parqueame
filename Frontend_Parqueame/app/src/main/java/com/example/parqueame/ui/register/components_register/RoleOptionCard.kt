package com.example.parqueame.ui.register.components_register

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.parqueame.ui.theme.DmSans
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember


@Composable
fun RoleOptionCard(
    icon: Painter,
    title: String,
    description: String,
    onClick: () -> Unit,
    iconSize: Dp = 40.dp,
    iconTintColor: Color,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false
) {
    val gradientColors = listOf(Color(0xFF00A1FF), Color(0xFF003099))
    val shape = RoundedCornerShape(25.dp)

    val interactionSource = remember { MutableInteractionSource() }

    Card(
        modifier = modifier
            //.width(340.dp)
            .height(105.dp) // Más alto para botones más grandes
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = if (isSelected) Brush.verticalGradient(gradientColors)
                    else Brush.linearGradient(
                        listOf(Color.Black.copy(alpha = 0.04f), Color.Black.copy(alpha = 0.04f))
                    ),
                    shape = shape
                )
                .padding(horizontal = 32.dp, vertical = 20.dp), // Más padding para separación
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxHeight()
            ) {
                Image(
                    painter = icon,
                    contentDescription = title,
                    colorFilter = ColorFilter.tint(if (isSelected) Color.White else iconTintColor),
                    modifier = Modifier.size(iconSize)
                )
                Spacer(modifier = Modifier.width(32.dp))
                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Text(
                        text = title,
                        fontFamily = DmSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp,
                        color = if (isSelected) Color.White else Color.Black
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = description,
                        fontFamily = DmSans,
                        fontWeight = FontWeight.Normal,
                        fontSize = 15.sp,
                        color = if (isSelected) Color.White.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}