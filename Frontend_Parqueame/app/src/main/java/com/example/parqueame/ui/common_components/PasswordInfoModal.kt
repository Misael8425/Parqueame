package com.example.parqueame.ui.common_components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import com.example.parqueame.R
import com.example.parqueame.ui.theme.DmSans

@Composable
fun PasswordInfoBox(
    showPasswordInfo: Boolean,
    onDismiss: () -> Unit
) {
    if (showPasswordInfo) {
        Box(
            modifier = Modifier
                .fillMaxHeight() // Ajusta la altura para que no sobresalga
                .padding(top = 80.dp) // Ajuste del padding superior para evitar solaparse con la barra de navegación
                .background(Color.Black.copy(alpha = 0.6f)), // Fondo overlay más visible
            contentAlignment = Alignment.BottomCenter // Asegura que el modal esté en la parte inferior
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 300.dp, max = 400.dp) // Limita la altura del modal
                    .background(
                        color = Color.White,
                        shape = RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp)
                    )
                    .padding(horizontal = 32.dp, vertical = 16.dp) // Reducimos el padding interno para mover los elementos más arriba
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp), // Reducimos el espaciado entre los elementos
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Contraseña",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = DmSans,
                        color = Color(0xFF115ED0)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "La contraseña debe contener 8 caracteres o más, mayúsculas y al menos un carácter especial (*!#$%&+-).",
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal,
                                fontFamily = DmSans,
                                color = Color.Black,
                                lineHeight = 22.sp
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            painter = painterResource(id = R.drawable.symbols_password),
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = Color.Unspecified
                        )
                    }

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        GradientButton(
                            text = "Siguiente",
                            onClick = onDismiss,
                            modifier = Modifier
                                .width(240.dp)
                                .height(60.dp)
                        )
                    }
                }
            }
        }
    }
}
