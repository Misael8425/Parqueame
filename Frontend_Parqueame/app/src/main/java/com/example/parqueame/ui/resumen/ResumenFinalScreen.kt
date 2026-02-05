package com.example.parqueame.ui.resumen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ResumenFinalScreen(
    reservationId: String,
    avgRatingText: String
) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Resumen de tu reserva", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Divider()
        Spacer(Modifier.height(8.dp))
        Text("ID de reserva: $reservationId", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(6.dp))
        Text("Rating promedio del parqueo: $avgRatingText ★", style = MaterialTheme.typography.bodyLarge)
    }
}
