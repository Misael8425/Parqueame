package com.example.parqueame.ui.parqueos

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.airbnb.lottie.compose.LottieConstants
import com.example.parqueame.R
import com.example.parqueame.api.RetrofitInstance
import com.example.parqueame.models.RatingSummaryDto
import com.example.parqueame.models.UsuarioPerfil
import com.example.parqueame.session.SessionStore
import com.example.parqueame.ui.common_components.GradientIcon
import com.example.parqueame.ui.navigation.Screen            // 👈 IMPORTA Screen para navegar al Home
import com.example.parqueame.ui.theme.DmSans
import com.example.parqueame.ui.theme.GradientBrush
import com.example.parqueame.ui.theme.RtlRomman
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class OperatorInfo(val nombre: String, val rol: String, val fotoUrl: String?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservaEnCursoScreen(
    navController: NavController,
    summary: ReservationSummaryUi,
    reservationId: String,
) {
    var showQr by remember { mutableStateOf(false) }

    // ====== Sesión ======
    val context = LocalContext.current
    val session = remember { SessionStore(context) }
    val userId by session.userId.collectAsState(initial = null)
    val effectiveUserId = remember(userId) { userId.orEmpty() }

    // ====== Horas ======
    val zoneId = remember { ZoneId.systemDefault() }
    val localeEs = remember { Locale("es", "ES") }

    val start = remember(summary.startEpochMinutes, zoneId) {
        Instant.ofEpochSecond(summary.startEpochMinutes * 60).atZone(zoneId).toLocalTime()
    }
    val end = remember(summary.endEpochMinutes, zoneId) {
        Instant.ofEpochSecond(summary.endEpochMinutes * 60).atZone(zoneId).toLocalTime()
    }
    val horaFinFmt = remember(end, localeEs) { formatHoraEs(end, localeEs) }
    val rango = remember(start, end, localeEs) {
        "${formatHoraEs(start, localeEs)} – ${formatHoraEs(end, localeEs)}"
    }

    // ====== Backend ======
    val api = remember { RetrofitInstance.apiService }

    val operatorInfo by produceState<OperatorInfo?>(initialValue = null, key1 = summary.parqueo.createdBy) {
        val ownerId = summary.parqueo.createdBy.orEmpty()
        value = if (ownerId.isBlank()) null else try {
            val resp = withContext(Dispatchers.IO) { api.getUsuarioPublico(ownerId) }
            if (resp.isSuccessful) {
                val u: UsuarioPerfil? = resp.body()
                OperatorInfo(u?.nombre ?: "Operador", u?.rol ?: "Encargado", u?.fotoUrl)
            } else null
        } catch (_: Exception) { null }
    }

    // rating real desde backend (nullable) — se usa como respaldo
    val ratingSummary by produceState<RatingSummaryDto?>(initialValue = null, key1 = summary.parqueo.id) {
        value = try {
            val resp = withContext(Dispatchers.IO) { api.getParkingRatingSummary(summary.parqueo.id) }
            if (resp.isSuccessful) resp.body() else null
        } catch (_: Exception) { null }
    }

    // ====== UI ======
    Box(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 24.dp, end = 24.dp, top = 45.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.parked_until_prefix),
                fontFamily = DmSans,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0A0A0A),
                lineHeight = 30.sp
            )
            Text(
                text = horaFinFmt,
                fontFamily = DmSans,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .graphicsLayer(alpha = 0.99f)
                    .drawWithContent {
                        drawContent()
                        drawRect(brush = GradientBrush, blendMode = BlendMode.SrcIn)
                    },
                color = Color.Unspecified
            )

            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.resolve_things_hint),
                fontFamily = DmSans,
                fontSize = 16.sp,
            )

            Spacer(Modifier.height(16.dp))

            // Animación
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF4F7FE)),
                contentAlignment = Alignment.Center
            ) {
                ParkingAnimation(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .padding(12.dp),
                    iterations = LottieConstants.IterateForever,
                    speed = 1.0f
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.brand_title),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = RtlRomman,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier
                    .graphicsLayer(alpha = 0.99f)
                    .drawWithContent {
                        drawContent()
                        drawRect(brush = GradientBrush, blendMode = BlendMode.SrcIn)
                    },
                color = Color.Unspecified
            )

            Spacer(Modifier.height(15.dp))
            HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 4.dp)
            Spacer(Modifier.height(12.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = summary.parqueo.localName,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = DmSans,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = summary.parqueo.address,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = DmSans,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0x99000000)
                        )
                    }
                    IconButton(onClick = { showQr = true }) {
                        GradientIcon(
                            imageVector = Icons.Outlined.QrCode2,
                            contentDescription = stringResource(R.string.qr_cd),
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                OperatorInfoRow(
                    imageUrl = operatorInfo?.fotoUrl,
                    title = operatorInfo?.nombre ?: "Operador",
                    subtitle = operatorInfo?.rol ?: "Encargado",
                    rating = ratingSummary?.average,          // ✅ ya no es 4.5 fijo
                    ratingCount = ratingSummary?.count ?: 0,
                    modifier = Modifier.padding(top = 6.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccessTime,
                        contentDescription = null,
                        tint = Color(0xFF4F7DF9),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = rango,
                        fontFamily = DmSans,
                        fontSize = 14.sp,
                        color = Color(0xFF4F7DF9)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
        }
    }

    // QR
    QrCodeSheet(
        visible = showQr,
        onDismiss = { showQr = false },
        titulo = "Muestra este ",
        link = "código QR",
        restoTitulo = " al personal para registrar tu salida.",
        reservationId = reservationId,
        parkingId = summary.parqueo.id,
        userId = effectiveUserId,
        onValidated = {
            showQr = false

            val p = summary.parqueo
            val photoUrl: String? = p.photos.firstOrNull()   // <- toma la primera foto si hay

            val destino =
                "home_calificacion" +
                        "?pid=${Uri.encode(p.id)}" +
                        "&pname=${Uri.encode(p.localName)}" +
                        "&paddr=${Uri.encode(p.address)}" +
                        "&pimg=${Uri.encode(photoUrl ?: "")}" +      // <- pasa la imagen (o vacío)
                        "&op=${Uri.encode(operatorInfo?.nombre ?: "Operador")}"

            navController.navigate(destino) {
                launchSingleTop = true
            }
        }
    )
}

// ====== Helper ======
private fun formatHoraEs(t: LocalTime, locale: Locale): String {
    val base = t.format(DateTimeFormatter.ofPattern("hh:mm a", locale)).lowercase(locale)
    return base.replace("am", "a. m.").replace("pm", "p. m.")
}

// ====== UI Row ======
@Composable
private fun OperatorInfoRow(
    imageUrl: String?,
    title: String,
    subtitle: String,
    rating: Double?,     // ✅ nullable
    ratingCount: Int,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center
        ) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize()
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontFamily = DmSans)
            Text(text = subtitle, fontFamily = DmSans, fontSize = 13.sp, color = Color(0x99000000))
        }
        if (rating != null) {
            Text(
                text = "★ %.1f".format(Locale.US, rating) +
                        if (ratingCount > 0) " ($ratingCount)" else "",
                fontFamily = DmSans,
                fontSize = 13.sp,
                color = Color(0xFF4F7DF9)
            )
        }
    }
}
