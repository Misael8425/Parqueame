package com.example.parqueame.ui.ayudaConductor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.parqueame.ui.common_components.GradientIcon
import com.example.parqueame.ui.admin.ayuda.FaqItem
import com.example.parqueame.ui.admin.ayuda.FaqRow

@Composable
fun AyudaConductorScreen(
    navController: NavController
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    GradientIcon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    "Ayuda para Conductores",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.labelLarge.copy(color = Color(0xFF003099))
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Todos los temas",
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color(0xFF003099),
                    fontWeight = FontWeight.SemiBold
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            val faqs = listOf(
                FaqItem(
                    "¿Cómo encuentro parqueos cercanos?",
                    "Desde Inicio usa la barra de búsqueda. Te mostramos parqueos disponibles ordenados por cercanía y precio."
                ),
                FaqItem(
                    "¿Cómo reservo un espacio?",
                    "Toca el parqueo, revisa fotos, horario y tarifa. Pulsa 'Reservar', confirma el tiempo y el método de pago."
                ),
                FaqItem(
                    "¿Cómo pago mi reserva?",
                    "Puedes pagar con tu Billetera (métodos guardados). En la confirmación elige o agrega un método y finaliza."
                ),
                FaqItem(
                    "¿Puedo extender el tiempo?",
                    "Sí. Ve a Actividad > Reserva activa > 'Extender tiempo' y selecciona el nuevo tiempo. Se ajustará el costo."
                ),
                FaqItem(
                    "El lugar estaba ocupado al llegar",
                    "Abre la reserva activa y presiona 'Reportar problema'. Adjunta una foto si puedes. Te ayudaremos con reubicación o reembolso según el caso."
                ),
                FaqItem(
                    "¿Cómo cancelo una reserva?",
                    "Desde Actividad selecciona la reserva y pulsa 'Cancelar'. Las políticas de cancelación pueden variar por parqueo."
                ),
                FaqItem(
                    "¿Dónde veo mi historial y recibos?",
                    "Actividad > Historial. Allí puedes ver detalles, descargar recibos y reportar inconvenientes pasados."
                ),
                FaqItem(
                    "¿Cómo califico un parqueo?",
                    "Al finalizar recibirás un prompt para calificar. También puedes ir a Historial > Detalle > 'Calificar'."
                ),
                FaqItem(
                    "Métodos de pago admitidos",
                    "Tarjetas y cuentas compatibles a través de Billetera. Ve a Cuenta > Billetera para agregar o editar."
                ),
                FaqItem(
                    "¿Cómo activo las notificaciones?",
                    "Desde Cuenta > Idioma/Notificaciones (según tu versión). Activa alertas de reserva, vencimiento y promociones."
                ),
                FaqItem(
                    "Problemas con la ubicación/GPS",
                    "Asegúrate de tener la ubicación activada en el sistema y permisos para la app. Si persiste, reinicia el GPS del dispositivo."
                ),
                FaqItem(
                    "Soporte",
                    "Desde Actividad o Cuenta > Ayuda, usa 'Reportar un problema'. Nuestro equipo te responderá por correo."
                )
            )

            faqs.forEach { item ->
                FaqRow(item = item, modifier = Modifier.padding(horizontal = 8.dp))
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AyudaConductorScreenPreview() {
    AyudaConductorScreen(navController = rememberNavController())
}
