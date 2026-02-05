package com.example.parqueame.ui.parqueos

// Jetpack Compose base
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.res.stringResource

// Coil para cargar imágenes desde URL
import coil.compose.AsyncImage

// Tu modelo y data
import com.example.parqueame.models.ParkingLotDto

// Tu tema (colores y fuentes)
import com.example.parqueame.ui.theme.GradientStart
import com.example.parqueame.ui.theme.GradientEnd
import com.example.parqueame.ui.common_components.GradientButton
import com.example.parqueame.ui.theme.GradientBrush
import com.example.parqueame.ui.theme.dmSans

//Iconos
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import com.example.parqueame.R

@DrawableRes
private fun featureIconRes(label: String): Int {
    val s = label.lowercase()

    return when {
        "techad" in s -> R.drawable.techado
        "guardia" in s || "seguridad" in s -> R.drawable.guardia
        "discap" in s || "diversidad" in s || "embaraz" in s -> R.drawable.discapacitados
        "subter" in s -> R.drawable.subterraneo
        "premisa" in s -> R.drawable.premisa
        "aire libre" in s -> R.drawable.parqueo_icon
        "smart" in s -> R.drawable.smart
        "carga" in s || "eléctr" in s || "electr" in s -> R.drawable.carga
        "pesad" in s -> R.drawable.qr_icon  // cámbialo por uno si tienes “camion” o “truck”
        "liger" in s || "moto" in s || "bicic" in s -> R.drawable.ligero
        else -> R.drawable.p_icon // ícono por defecto
    }
}

@Composable
private fun FeatureRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(featureIconRes(text)),
            contentDescription = null,
            tint = Color(0xFF0B66FF),
            modifier = Modifier.size(30.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(text, fontFamily = dmSans, fontSize = 18.sp, style = LocalTextStyle.current)
    }
}

@Composable
fun ParkingDetailScreen(
    navController: NavController? = null,
    parqueo: ParkingLotDto,
    distanceMeters: Long,
    onClose: () -> Unit,
    onPrimaryAction: () -> Unit
) {
    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(vertical = 75.dp),
                contentAlignment = Alignment.Center
            ) {
                GradientButton(text = stringResource(R.string.park_me_action), modifier = Modifier.width(200.dp),onClick = onPrimaryAction)
            }
        }
    ) {

        padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header con imagen
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                AsyncImage( // usa Coil
                    model = parqueo.photos.firstOrNull(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )


                // Botón cerrar
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .size(45.dp)
                        .align(Alignment.TopStart)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.close_action),
                        tint = Color.White,
                    )
                }

                // Chip distancia (ejemplo fijo, luego calculamos con haversine)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(horizontal = 25.dp, vertical = 12.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.verticalGradient(listOf(GradientStart, GradientEnd))
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.distance_from_you_m, distanceMeters),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = dmSans
                    )
                }
            }

            // Contenido principal
            Column(modifier = Modifier.padding(horizontal = 25.dp, vertical = 20.dp)) {
                Text(
                    text = parqueo.localName,
                    fontFamily = dmSans,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = parqueo.address,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.Black,
                        fontFamily = dmSans,
                        fontSize = 16.sp
                    )
                )

                Spacer(Modifier.height(12.dp))

                // Tarifa
                Text(
                    text = stringResource(R.string.hourly_rate_format, parqueo.priceHour.toInt()),
                    fontFamily = dmSans,
                    fontSize = 16.sp,
                    color = Color(0xFF0B66FF),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )

                Spacer(Modifier.height(12.dp))

                // Disponibles (ejemplo: capacidad total como disponibles)
                Text(
                    text = stringResource(R.string.available_spots_count_format, parqueo.capacity),
                    fontFamily = dmSans,
                    fontSize = 16.sp,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(Modifier.height(20.dp))

                val features: List<String> = parqueo.characteristics

                if (features.isEmpty()) {
                    Text(stringResource(R.string.features_not_specified), color = Color.Gray, fontSize = 16.sp)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        features.forEach { f ->
                            FeatureRow(f)
                        }
                    }
                }

//                // Features (mockeados)
//                Row(verticalAlignment = Alignment.CenterVertically) {
//                    Icon(Icons.Outlined.Person, contentDescription = null, tint = Color(0xFF0B66FF))
//                    Spacer(Modifier.width(8.dp))
//                    Text("Guardia de seguridad en la premisa")
//                }
//
//                Spacer(Modifier.height(8.dp))
//
//                Row(verticalAlignment = Alignment.CenterVertically) {
//                    Icon(
//                        Icons.Outlined.ThumbUp,
//                        contentDescription = null,
//                        tint = Color(0xFF0B66FF)
//                    )
//                    Spacer(Modifier.width(8.dp))
//                    Text("Plaza / Centro Comercial")
//                }
//
//                Spacer(Modifier.height(8.dp))
//
//                Row(verticalAlignment = Alignment.CenterVertically) {
//                    Icon(Icons.Outlined.Home, contentDescription = null, tint = Color(0xFF0B66FF))
//                    Spacer(Modifier.width(8.dp))
//                    Text("Parqueos techados disponibles")
//                }
            }
        }
    }
}
