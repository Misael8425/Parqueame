package com.example.parqueame.ui.parqueos

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri                         // ✅ para Uri.encode
import android.widget.Toast                  // ✅ para mensaje si el id viene vacío
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.parqueame.R
import com.example.parqueame.api.RetrofitInstance
import com.example.parqueame.models.UsuarioPerfil
import com.example.parqueame.models.RatingSummaryDto
import com.example.parqueame.session.SessionStore
import com.example.parqueame.ui.common_components.GradientIcon
import com.example.parqueame.ui.theme.GradientBrush
import com.example.parqueame.ui.theme.RtlRomman
import com.example.parqueame.ui.theme.dmSans
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RutaReservaScreen(
    navController: NavController,
    summary: ReservationSummaryUi,
    reservationIdCreated: String,
    directionsApiKey: String
) {
    val context = LocalContext.current
    val session = remember { SessionStore(context) }
    val userId by session.userId.collectAsState(initial = null)
    val effectiveUserId = remember(userId) { userId.orEmpty() }

    var showQr by remember { mutableStateOf(false) }
    var showOptions by remember { mutableStateOf(false) }

    // ---- permisos ubicación ----
    val perms = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
    LaunchedEffect(Unit) {
        if (!perms.allPermissionsGranted) perms.launchMultiplePermissionRequest()
    }

    // ---- origen/destino ----
    val destLatLng: LatLng? = remember(summary.parqueo.location) {
        summary.parqueo.location?.let { gp ->
            val lat = gp.lat
            val lng = gp.lng
            if (lat != null && lng != null) LatLng(lat, lng) else null
        }
    }

    var origin by remember { mutableStateOf<LatLng?>(null) }
    LaunchedEffect(perms.allPermissionsGranted) {
        if (!perms.allPermissionsGranted) return@LaunchedEffect
        val fused = LocationServices.getFusedLocationProviderClient(context)
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            fused.lastLocation.addOnSuccessListener { loc ->
                loc?.let { origin = LatLng(it.latitude, it.longitude) }
            }
        }
    }

    val camera = rememberCameraPositionState()
    var polyline by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var legDistance by remember { mutableStateOf<String?>(null) }
    var legDuration by remember { mutableStateOf<String?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(origin, destLatLng, directionsApiKey) {
        errorMsg = null
        val o = origin
        val d = destLatLng
        if (o == null || d == null || directionsApiKey.isBlank()) return@LaunchedEffect
        try {
            val res = withContext(Dispatchers.IO) { fetchDirections(o, d, directionsApiKey) }
            if (res.points.isNotEmpty()) {
                polyline = res.points
                legDistance = res.distanceText
                legDuration = res.durationText

                val b = LatLngBounds.builder()
                b.include(o)
                b.include(d)
                polyline.forEach { b.include(it) }
                camera.animate(
                    update = CameraUpdateFactory.newLatLngBounds(b.build(), 80),
                    durationMs = 700
                )
            } else {
                errorMsg = context.getString(R.string.could_not_calculate_route)
            }
        } catch (t: Throwable) {
            errorMsg = context.getString(
                R.string.network_error_with_reason,
                t.localizedMessage ?: ""
            )
        }
    }

    // ===================== RATING: fetch y valores para propagar =====================
    val api = remember { RetrofitInstance.apiService }
    val ratingSummary by produceState<RatingSummaryDto?>(initialValue = null, key1 = summary.parqueo.id) {
        value = try {
            val resp = withContext(Dispatchers.IO) { api.getParkingRatingSummary(summary.parqueo.id) }
            if (resp.isSuccessful) resp.body() else null
        } catch (_: Exception) { null }
    }
    val avgForRoute by remember(ratingSummary) {
        mutableStateOf(String.format(Locale.US, "%.1f", ratingSummary?.average ?: 0.0))
    }
    val ratingCountForRoute by remember(ratingSummary) {
        mutableStateOf(ratingSummary?.count ?: 0)
    }
    // ================================================================================

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            stringResource(R.string.brand_title),
                            fontFamily = RtlRomman,
                            fontSize = 30.sp,
                            modifier = Modifier
                                .graphicsLayer(alpha = 0.99f)
                                .drawWithContent {
                                    drawContent()
                                    drawRect(brush = GradientBrush, blendMode = BlendMode.SrcIn)
                                },
                            color = Color.Unspecified
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
        ) {
            // ---- mapa ----
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(515.dp)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = camera,
                    properties = MapProperties(isMyLocationEnabled = perms.allPermissionsGranted),
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = false,
                        myLocationButtonEnabled = false
                    )
                ) {
                    origin?.let {
                        Marker(
                            state = rememberMarkerState(position = it),
                            title = stringResource(R.string.you_label),
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                        )
                    }
                    destLatLng?.let {
                        Marker(
                            state = rememberMarkerState(position = it),
                            title = summary.parqueo.localName
                        )
                    }
                    if (polyline.isNotEmpty()) {
                        Polyline(
                            points = polyline,
                            width = 8f,
                            color = Color(0xFF0078FF)
                        )
                    }
                }
            }

            // ---- info inferior ----
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 25.dp)
                    .background(Color.White)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = summary.parqueo.localName,
                        fontFamily = dmSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        GradientIcon(
                            imageVector = ImageVector.vectorResource(R.drawable.qr_icon),
                            contentDescription = "QR Entrada",
                            modifier = Modifier
                                .size(25.dp)
                                .clickable { showQr = true }
                        )
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.tres_puntos_icon),
                            contentDescription = "Opciones Reserva",
                            modifier = Modifier
                                .size(25.dp)
                                .clickable { showOptions = true }
                        )
                    }
                }
                Text(
                    text = summary.parqueo.address,
                    fontFamily = dmSans,
                    fontSize = 16.sp,
                    color = Color(0x99000000),
                    modifier = Modifier.width(200.dp)
                )
                Spacer(Modifier.height(12.dp))

                // header del propietario/operador
                var headerNombre by remember { mutableStateOf<String?>(null) }
                var headerRol by remember { mutableStateOf<String?>(null) }
                var headerFoto by remember { mutableStateOf<String?>(null) }
                LaunchedEffect(summary.parqueo.createdBy) {
                    runCatching {
                        val ownerId = summary.parqueo.createdBy.orEmpty()
                        if (ownerId.isNotBlank()) {
                            val resp = withContext(Dispatchers.IO) { api.getUsuarioPublico(ownerId) }
                            if (resp.isSuccessful) {
                                val u: UsuarioPerfil? = resp.body()
                                headerNombre = u?.nombre
                                headerRol = u?.rol
                                headerFoto = u?.fotoUrl
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(55.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF5F5F5)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!headerFoto.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(headerFoto)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Foto",
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.cuenta_icon),
                                contentDescription = stringResource(R.string.profile_photo_cd),
                                modifier = Modifier.size(30.dp),
                                tint = Color.LightGray
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = headerNombre ?: "Usuario",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(start = 5.dp)
                        )
                        Text(
                            text = (headerRol ?: "Administrador"),
                            fontSize = 16.sp,
                            modifier = Modifier.padding(start = 5.dp),
                            color = Color(0xFF0B66FF)
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))

                // rango horario + duración/distancia
                val zoneId = ZoneId.systemDefault()
                val s = Instant.ofEpochSecond(summary.startEpochMinutes * 60).atZone(zoneId).toLocalTime()
                val e = Instant.ofEpochSecond(summary.endEpochMinutes * 60).atZone(zoneId).toLocalTime()
                val es = Locale("es", "ES")
                val rango = "${formatHoraEs(s, es)} – ${formatHoraEs(e, es)}"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(rango, fontFamily = dmSans, color = Color(0xFF0B66FF))
                    Text(
                        listOfNotNull(legDuration, legDistance).joinToString(" • ").ifBlank { "—" },
                        fontFamily = dmSans,
                        color = Color(0xFF0B66FF)
                    )
                }
            }

            if (errorMsg != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = errorMsg!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    fontFamily = dmSans
                )
            }

            // ===================== QR / NAVEGACIÓN =====================
            QrCodeSheet(
                visible = showQr,
                onDismiss = { showQr = false },
                titulo = "Muestra el ",
                link = "código QR",
                restoTitulo = " para confirmar tu reserva al momento de llegada.",
                reservationId = reservationIdCreated,
                parkingId = summary.parqueo.id,
                userId = effectiveUserId,
                onValidated = { validatedReservationId ->
                    showQr = false

                    // Guardamos el summary y rating para la siguiente pantalla
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set("resumen_reserva_en_curso", summary)
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set("rating_avg_for_route", avgForRoute)
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set("rating_count_for_route", ratingCountForRoute)

                    // ✅ Defensa: validar/encode del id antes de navegar (evita crash)
                    val safeId = validatedReservationId?.trim().orEmpty()
                    if (safeId.isBlank()) {
                        Toast.makeText(context, "ID de reserva vacío", Toast.LENGTH_SHORT).show()
                        return@QrCodeSheet
                    }
                    navController.navigate("reserva/en_curso/${Uri.encode(safeId)}") {
                        launchSingleTop = true
                    }
                }
            )
            // ===========================================================

            OptionsSheetCancelarReserva(
                visible = showOptions,
                onCancelarReserva = {
                    showOptions = false
                    // TODO: invocar cancelReservation(reservationIdCreated)
                },
                onDismiss = { showOptions = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionsSheetCancelarReserva(
    visible: Boolean,
    onCancelarReserva: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Cancelar reserva",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = dmSans,
                    color = Color(0xFFD32F2F),
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCancelarReserva() }
                    .padding(vertical = 14.dp)
            )
            HorizontalDivider(thickness = 0.6.dp, color = Color(0xFFE6E8EC))
            Text(
                text = "Volver",
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = dmSans),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDismiss() }
                    .padding(vertical = 14.dp)
            )
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrCodeSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    titulo: String,
    link: String,
    restoTitulo: String,
    reservationId: String,
    parkingId: String,   // compat
    userId: String,      // compat
    onValidated: ((String) -> Unit)? = null
) {
    if (!visible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val titleStyled = buildAnnotatedString {
                append(titulo)
                withStyle(SpanStyle(color = Color(0xFF1E88E5), fontWeight = FontWeight.SemiBold)) {
                    append(link)
                }
                append(restoTitulo)
            }
            Text(
                text = titleStyled,
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = dmSans),
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(Modifier.height(16.dp))

            // 🔵 Tap para continuar (sin escanear)
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF1F5F9))
                    .clickable {
                        onValidated?.invoke(reservationId)
                    }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    GradientIcon(
                        imageVector = ImageVector.vectorResource(R.drawable.qr_icon),
                        contentDescription = "Continuar",
                        modifier = Modifier.size(120.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    // Texto opcional
                    // Text("Toca para continuar", fontFamily = dmSans, color = Color(0xFF1E88E5))
                }
            }

            Spacer(Modifier.height(12.dp))
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

// ===================== Helpers de direcciones =====================
private data class DirectionsResult(
    val points: List<LatLng>,
    val distanceText: String?,
    val durationText: String?
)

private fun formatHoraEs(t: LocalTime, locale: Locale): String {
    val base = t.format(DateTimeFormatter.ofPattern("hh:mm a", locale)).lowercase(locale)
    return base.replace("am", "a. m.").replace("pm", "p. m.")
}

private fun fetchDirections(origin: LatLng, dest: LatLng, key: String): DirectionsResult {
    val originLat = origin.latitude
    val originLng = origin.longitude
    val destLat = dest.latitude
    val destLng = dest.longitude
    val url =
        "https://maps.googleapis.com/maps/api/directions/json" +
                "?origin=$originLat,$originLng" +
                "&destination=$destLat,$destLng" +
                "&mode=driving&key=$key"
    val conn = (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 10000
        readTimeout = 15000
        doInput = true
    }
    val body = try {
        conn.connect()
        conn.inputStream.bufferedReader().use { it.readText() }
    } finally {
        conn.disconnect()
    }
    val json = JSONObject(body)
    val routes = json.optJSONArray("routes") ?: return DirectionsResult(emptyList(), null, null)
    if (routes.length() == 0) return DirectionsResult(emptyList(), null, null)
    val route0 = routes.getJSONObject(0)
    val overview = route0.optJSONObject("overview_polyline")
    val line = overview?.optString("points").orEmpty()
    val legs = route0.optJSONArray("legs")
    var distance: String? = null
    var duration: String? = null
    if (legs != null && legs.length() > 0) {
        val leg0 = legs.getJSONObject(0)
        distance = leg0.optJSONObject("distance")?.optString("text")
        duration = leg0.optJSONObject("duration")?.optString("text")
    }
    val pts = if (line.isNotBlank()) decodePolyline(line) else emptyList()
    return DirectionsResult(points = pts, distanceText = distance, durationText = duration)
}

private fun decodePolyline(encoded: String): List<LatLng> {
    val len = encoded.length
    var index = 0
    var lat = 0
    var lng = 0
    val path = ArrayList<LatLng>()
    while (index < len) {
        var b: Int
        var shift = 0
        var result = 0
        do {
            b = encoded[index++].code - 63
            result = result or ((b and 0x1f) shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlat = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
        lat += dlat
        shift = 0
        result = 0
        do {
            b = encoded[index++].code - 63
            result = result or ((b and 0x1f) shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlng = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
        lng += dlng
        path.add(LatLng(lat / 1E5, lng / 1E5))
    }
    return path
}

// (opcional) QR local
private fun generateQRCode(content: String): Bitmap? = try {
    BarcodeEncoder().encodeBitmap(content, BarcodeFormat.QR_CODE, 900, 900)
} catch (e: Exception) {
    e.printStackTrace(); null
}