package com.example.parqueame.ui.common_components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close

@Composable
fun TermsAndConditionsModal(
    onClose: () -> Unit
) {
    val scrollState = rememberScrollState()

    val termsContent = """
        Última actualización: 26 de Mayo 2025

        1. Aceptación de los Términos
        Al descargar, instalar o utilizar la aplicación Parquéame, el usuario acepta cumplir con los presentes Términos y Condiciones. Si no está de acuerdo, no debe utilizar la aplicación.

        2. Descripción del Servicio
        Parquéame es una aplicación móvil diseñada para facilitar la búsqueda, reserva y pago digital de parqueos en tiempo real en Santo Domingo, República Dominicana. A través de geolocalización, filtros personalizados, validación con códigos QR y herramientas de administración, la app busca mejorar la experiencia de estacionamiento tanto para conductores como para propietarios de parqueos.

        3. Registro y Uso de la Cuenta
        El usuario deberá crear una cuenta personal con datos verídicos.
        Es responsable de mantener la confidencialidad de su contraseña y actividades dentro de la app.
        Parquéame se reserva el derecho de suspender o eliminar cuentas que violen estos términos.

        4. Reservas y Pagos
        El usuario puede realizar reservas de parqueo a través de la app, sujetas a disponibilidad.
        Los pagos se procesan mediante Stripe, garantizando transacciones seguras.
        Algunas reservas pueden incluir una comisión adicional por disponibilidad garantizada.

        7. Privacidad y Protección de Datos
        Parquéame recopila datos mínimos necesarios para el funcionamiento (ubicación, información de usuario y método de pago).
        Toda información será tratada bajo políticas de confidencialidad y según las normativas de protección de datos vigentes.
        Se emplean medidas de seguridad como cifrado SSL y autenticación de dos factores.

        8. Responsabilidad
        Parquéame no garantiza la precisión de la geolocalización en todos los casos, dependiendo de la conectividad del dispositivo y otros factores externos.
        No se hace responsable por pérdidas, robos o daños en los parqueos aliados.
        El acceso mediante QR depende de la infraestructura del parqueo aliado.

        9. Cambios en los Términos
        Estos términos pueden ser modificados en cualquier momento. Los usuarios serán notificados y la continuidad en el uso de la app implicará aceptación de los cambios.

        10. Legislación Aplicable
        Estos términos se rigen por las leyes de la República Dominicana. Cualquier disputa será resuelta en los tribunales competentes del país.
    """.trimIndent()

    // Fondo negro semitransparente que no es interactivo
    Box(
        modifier = Modifier
            .fillMaxSize() // Cubrir toda la pantalla
            .background(Color.Black.copy(alpha = 0.6f)) // Fondo semitransparente
    ) {
        // Modal en el centro de la pantalla
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f) // Limitar la altura para no cubrir la barra de navegación
                .background(Color.White, shape = RoundedCornerShape(16.dp))
                .padding(16.dp)
                .align(Alignment.Center) // Centra el modal en la pantalla
        ) {
            // Título y X para cerrar
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,  // Definir el ícono como un ImageVector
                        contentDescription = "Cerrar",  // Descripción para accesibilidad
                        modifier = Modifier,  // Modificador si es necesario
                        tint = Color(0xFF003099)  // Usar Color directamente en el tint
                    )

                }
                Text(
                    text = "Términos y Condiciones",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF003099) // Aplicando el color del GradientEnd
                )
            }

            // Caja de contenido desplazable
            Box(
                modifier = Modifier
                    .weight(1f) // Esto hace que el contenido ocupe el espacio disponible
                    .padding(top = 8.dp)
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = termsContent,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 14.sp,
                    color = Color.Black
                )
            }

            // Botón Siguiente
            GradientButton(
                text = "Siguiente",
                onClick = onClose, // Cierra el modal
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
    }
}
