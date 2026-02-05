package com.example.parqueame.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.parqueame.R
import com.example.parqueame.ui.common_components.GradientButton
import com.example.parqueame.ui.theme.dmSans

@Composable
fun ParkingRatingModal(
    visible: Boolean,
    parkingName: String,
    address: String,
    imageUrl: String?,
    operatorName: String,
    onSubmit: (Int) -> Unit,
    onIgnore: () -> Unit,
    onClose: () -> Unit
) {
    if (!visible) return

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Scrim más oscuro para que la tarjeta “flote” como en el diseño
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x99000000)),
            contentAlignment = Alignment.Center
        ) {
            // Tarjeta principal
            Surface(
                shape = RoundedCornerShape(26.dp),                    // radio más “píldora”
                tonalElevation = 0.dp,
                shadowElevation = 12.dp,                               // sombra marcada
                color = Color.White,                                   // 🔒 blanco puro
                modifier = Modifier
                    .fillMaxWidth(0.88f)                               // ancho como el mockup
                    .wrapContentHeight()
            ) {
                var rating by remember { mutableStateOf(0) }

                Column {
                    // ====== Imagen superior ======
                    val headerHeight = 140.dp                           // un poco más baja
                    Box {
                        if (!imageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Imagen del parqueo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(headerHeight)
                                    .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                            )
                        } else {
                            // Fondo neutro si no hay imagen (sin tintes azulados)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(headerHeight)
                                    .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                                    .background(Color(0xFFEDEDED)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Sin imagen", color = Color(0xFF6B6B6B), fontFamily = dmSans)
                            }
                        }

                        // X arriba a la izquierda, blanca
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color.White,
                            modifier = Modifier
                                .padding(10.dp)
                                .size(24.dp)
                                .clip(CircleShape)
                                .clickable { onClose() }
                        )
                    }

                    // ====== Cuerpo ======
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp) // espaciados
                    ) {
                        Text(
                            text = parkingName,
                            fontFamily = dmSans,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFF111111)
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = address,
                            fontFamily = dmSans,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0x99000000)
                        )

                        Spacer(Modifier.height(12.dp))

                        // Estrellas (gris clarito como en el mockup)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            (1..5).forEach { i ->
                                val filled = i <= rating
                                Icon(
                                    imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.Star,
                                    contentDescription = "Star $i",
                                    tint = if (filled) Color(0xFFFFB300) else Color(0x1A000000),
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable { rating = i }
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Operador
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEFF6FF))
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = operatorName,
                                    fontFamily = dmSans,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1A1A1A)
                                )
                                Text(
                                    text = "Anfitrión Estrella",
                                    fontFamily = dmSans,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF1E88E5)
                                )
                            }
                        }

                        Spacer(Modifier.height(18.dp))

                        // Botón más estrecho (como en el diseño)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            GradientButton(
                                text = "Calificar",
                                onClick = { onSubmit(rating) },
                                enabled = rating > 0,
                                modifier = Modifier
                                    .width(190.dp)                         // ancho similar al mock
                                    .clip(RoundedCornerShape(28.dp))
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        TextButton(
                            onClick = onIgnore,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("Ignorar", fontFamily = dmSans, color = Color(0xFF1E88E5))
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun StarRatingRow(
    current: Int,
    onRate: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        (1..5).forEach { i ->
            val filled = i <= current
            Icon(
                imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = "Star $i",
                tint = if (filled) Color(0xFFFFB300) else Color(0x33000000),
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onRate(i) }
            )
        }
    }
}
