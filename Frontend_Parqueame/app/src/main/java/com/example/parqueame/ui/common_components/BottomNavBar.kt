// BottomNavBar.kt
package com.example.parqueame.ui.common_components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.parqueame.ui.theme.GradientBrush
import com.example.parqueame.ui.theme.dmSans

data class BottomNavItem(
    val route: String,
    @DrawableRes val iconRes: Int,
    val label: String
)

@Composable
fun BottomNavBar(
    items: List<BottomNavItem>,
    currentRoute: String,
    onItemClick: (route: String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        NavigationBar(
            containerColor = Color.White,
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items.forEach { item ->
                val selected = item.route == currentRoute

                NavigationBarItem(
                    selected = selected,
                    onClick = { onItemClick(item.route) },
                    icon = {
                        Icon(
                            painter = painterResource(item.iconRes),
                            contentDescription = item.label,
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer(alpha = if (selected) 0.99f else 1f)
                                .then(
                                    if (selected) {
                                        Modifier.drawWithContent {
                                            // 1) dibuja el icono original
                                            drawContent()
                                            // 2) pinta tu degradado encima
                                            drawRect(
                                                brush = GradientBrush,
                                                blendMode = BlendMode.SrcIn
                                            )
                                        }
                                    } else Modifier
                                ),
                            tint = if (selected) Color.Unspecified
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    },
                    label = {
                        Text(
                            text = item.label,
                            fontSize = 15.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            fontFamily = dmSans,
                            modifier = Modifier
                                .graphicsLayer(alpha = if (selected) 0.99f else 1f)
                                .then(
                                    if (selected) {
                                        Modifier.drawWithContent {
                                            drawContent()
                                            drawRect(
                                                brush = GradientBrush,
                                                blendMode = BlendMode.SrcIn
                                            )
                                        }
                                    } else Modifier
                                ),
                            color = if (selected) Color.Unspecified
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor      = Color.Transparent,
                        selectedIconColor   = Color.Unspecified,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        selectedTextColor   = Color.Unspecified,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
            }
        }
    }
}
