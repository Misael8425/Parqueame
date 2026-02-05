package com.example.parqueame.ui.parqueos

import androidx.compose.ui.res.painterResource
import com.example.parqueame.R
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Accessible
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.parqueame.api.RetrofitInstance
import com.example.parqueame.models.ParkingAvailabilityResponse
import com.example.parqueame.models.ParkingLotDto
import com.example.parqueame.models.ScheduleRange
import com.example.parqueame.models.WeekDay
import com.example.parqueame.models.UsuarioPerfil
import com.example.parqueame.ui.common_components.GradientButton
import com.example.parqueame.ui.theme.GradientEnd
import com.example.parqueame.ui.theme.GradientStart
import com.example.parqueame.ui.theme.dmSans
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale


@DrawableRes
private fun featureIconRes(label: String): Int {
    val s = label.lowercase()

    return when {
        "techad" in s -> R.drawable.techado
        "guardia" in s || "seguridad" in s -> R.drawable.guardia
        "discap" in s || "diversidad" in s || "embaraz" in s -> R.drawable.discapacitados
        "subter" in s -> R.drawable.subterraneo
        "premisa" in s -> R.drawable.premisa
        "aire libre" in s -> R.drawable.aire_libre
        "smart" in s -> R.drawable.smart
        "carga" in s || "eléctr" in s || "electr" in s -> R.drawable.electeric
        "pesad" in s -> R.drawable.carga  // cámbialo por uno si tienes “camion” o “truck”
        "liger" in s || "moto" in s || "bicic" in s -> R.drawable.ligero
        else -> R.drawable.p_icon // ícono por defecto
    }
}

private fun formatDistanceShort(meters: Long): String {
    if (meters < 0) return "—"
    return if (meters < 1000) "$meters m" else String.format(Locale.US, "%.1f km", meters / 1000.0)
}

@Composable
private fun FeatureRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(featureIconRes(text)),
            contentDescription = null,
            tint = Color(0xFF0B66FF),
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(text, fontFamily = dmSans, fontSize = 18.sp, style = LocalTextStyle.current)
    }
}

/* ---------- Helpers de horario ---------- */

private fun javaDayToWeekDay(d: DayOfWeek): WeekDay = when (d) {
    DayOfWeek.MONDAY -> WeekDay.MON
    DayOfWeek.TUESDAY -> WeekDay.TUE
    DayOfWeek.WEDNESDAY -> WeekDay.WED
    DayOfWeek.THURSDAY -> WeekDay.THU
    DayOfWeek.FRIDAY -> WeekDay.FRI
    DayOfWeek.SATURDAY -> WeekDay.SAT
    DayOfWeek.SUNDAY -> WeekDay.SUN
}

private fun parseTimeOrNull(s: String?): LocalTime? = try {
    if (s.isNullOrBlank()) null else LocalTime.parse(s.trim())
} catch (_: Exception) { null }

/** Devuelve (abiertoAhora, proximoAperturaHoy) */
private fun computeOpenStateToday(
    daysOfWeek: List<WeekDay>,
    schedules: List<ScheduleRange>,
    zone: ZoneId
): Pair<Boolean, LocalTime?> {
    val nowLocal = LocalTime.now(zone)
    val today = javaDayToWeekDay(LocalDate.now(zone).dayOfWeek)

    if (daysOfWeek.isNotEmpty() && today !in daysOfWeek) return false to null

    var anyOpen = false
    var nextOpen: LocalTime? = null

    for (r in schedules) {
        val open = parseTimeOrNull(r.open)
        val close = parseTimeOrNull(r.close)
        if (open == null || close == null) continue

        if (!nowLocal.isBefore(open) && nowLocal.isBefore(close)) {
            anyOpen = true
            break
        }
        if (nowLocal.isBefore(open)) {
            nextOpen = if (nextOpen == null || open.isBefore(nextOpen)) open else nextOpen
        }
    }
    return anyOpen to nextOpen
}

/* ---------- Indicador de puntos para el carrusel ---------- */

@Composable
private fun PagerDotsIndicator(
    total: Int,
    currentIndex: Int,
    modifier: Modifier = Modifier,
    activeSize: Dp = 8.dp,
    inactiveSize: Dp = 6.dp,
    spacing: Dp = 8.dp
) {
    if (total <= 1) return
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = Color(0x66000000)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(total) { i ->
                    val isActive = i == currentIndex
                    Box(
                        modifier = Modifier
                            .size(if (isActive) activeSize else inactiveSize)
                            .clip(CircleShape)
                            .background(if (isActive) Color.White else Color(0x80FFFFFF))
                    )
                    if (i != total - 1) Spacer(Modifier.width(spacing))
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = "${currentIndex + 1}/$total",
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = dmSans
            )
        }
    }
}

/* ---------- Sección de Horario ---------- */

private val dayLabelEs = mapOf(
    WeekDay.MON to "L", WeekDay.TUE to "M", WeekDay.WED to "X",
    WeekDay.THU to "J", WeekDay.FRI to "V", WeekDay.SAT to "S", WeekDay.SUN to "D"
)

@Composable
private fun ScheduleSection(
    daysOfWeek: List<WeekDay>,
    schedules: List<ScheduleRange>,
    zone: ZoneId
) {
    val todayJava = LocalDate.now(zone).dayOfWeek
    val today = javaDayToWeekDay(todayJava)
    val formatter = remember { DateTimeFormatter.ofPattern("h:mm a", Locale("es", "ES")) }

    Text(
        text = "Horario",
        style = MaterialTheme.typography.titleMedium.copy(fontFamily = dmSans),
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(Modifier.height(8.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        val allDays = listOf(
            WeekDay.MON, WeekDay.TUE, WeekDay.WED, WeekDay.THU,
            WeekDay.FRI, WeekDay.SAT, WeekDay.SUN
        )
        allDays.forEach { d ->
            val isOpenDay = daysOfWeek.isEmpty() || d in daysOfWeek
            val isToday = d == today
            AssistChip(
                onClick = { /* no-op */ },
                label = { Text(dayLabelEs[d] ?: d.name.take(1)) },
                enabled = isOpenDay,
                border = if (isToday) BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary
                ) else null,
                colors = AssistChipDefaults.assistChipColors(
                    disabledContainerColor = Color(0xFFF0F0F0),
                    disabledLabelColor = Color(0xFF9E9E9E)
                )
            )
        }
    }

    Spacer(Modifier.height(10.dp))

    if (schedules.isEmpty()) {
        Text(
            text = "No hay horario configurado.",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = dmSans),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            schedules.forEach { r ->
                val o = parseTimeOrNull(r.open)
                val c = parseTimeOrNull(r.close)
                val txt = if (o != null && c != null) {
                    "${o.format(formatter)} – ${c.format(formatter)}"
                } else {
                    "Rango inválido"
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        modifier = Modifier.size(16.dp),
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = txt,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = dmSans),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParkingDetailScreen(
    navController: NavController,
    parqueo: ParkingLotDto,
    distanceMeters: Long,
    onClose: () -> Unit,
    onPrimaryAction: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var isChecking by remember { mutableStateOf(false) }
    var lastAvailability by remember { mutableStateOf<ParkingAvailabilityResponse?>(null) }
    var buttonEnabled by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var operator by remember { mutableStateOf<UsuarioPerfil?>(null) }
    var operatorError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(parqueo.createdBy) {
        operator = null
        operatorError = null
        val ownerId = parqueo.createdBy?.trim().orEmpty()
        if (ownerId.isBlank()) return@LaunchedEffect
        runCatching { RetrofitInstance.apiService.getUsuarioPublico(ownerId) }
            .onSuccess { resp ->
                if (resp.isSuccessful) operator = resp.body()
                else operatorError = "No se pudo cargar el propietario (HTTP ${resp.code()})"
            }
            .onFailure { t -> operatorError = t.localizedMessage ?: "Error de red" }
    }

    val localZone = remember { ZoneId.systemDefault() }
    val (isOpenNow, nextOpenToday) = remember(parqueo.daysOfWeek, parqueo.schedules, localZone) {
        computeOpenStateToday(parqueo.daysOfWeek, parqueo.schedules, localZone)
    }

    val abiertoHasta: String = remember(parqueo, localZone) {
        val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale("es", "ES"))
        if (parqueo.schedules.isEmpty()) {
            context.getString(R.string.schedule_not_available)
        } else {
            val close = parqueo.schedules.mapNotNull { parseTimeOrNull(it.close) }.maxOrNull()
            if (close == null) context.getString(R.string.schedule_not_available)
            else context.getString(R.string.open_today_until_format, close.format(formatter))
        }
    }

    val utcZone = remember { ZoneId.of("UTC") }
    val nowUtc = ZonedDateTime.now(utcZone)
    val startEpochMin = nowUtc.toEpochSecond() / 60
    val endEpochMin = nowUtc.plusHours(1).toEpochSecond() / 60

    LaunchedEffect(parqueo.id, startEpochMin, endEpochMin, isOpenNow, nextOpenToday) {
        if (!isOpenNow) {
            val f = DateTimeFormatter.ofPattern("h:mm a", Locale("es", "ES"))
            errorMessage = if (nextOpenToday != null) {
                context.getString(R.string.out_of_hours_with_time, nextOpenToday.format(f))
            } else {
                context.getString(R.string.out_of_hours_now)
            }
            buttonEnabled = false
            lastAvailability = null
            return@LaunchedEffect
        }

        isChecking = true
        errorMessage = null
        buttonEnabled = true

        runCatching {
            RetrofitInstance.apiService.checkAvailability(
                parkingId = parqueo.id,
                startMin = startEpochMin,
                endMin = endEpochMin
            )
        }.onSuccess { resp ->
            if (resp.isSuccessful) {
                val body = resp.body()
                lastAvailability = body
                buttonEnabled = body?.available ?: true
                if (body?.available == false) {
                    errorMessage = context.getString(R.string.no_spaces_available_now)
                }
            } else {
                val errorBody = resp.errorBody()?.string()?.trim()
                val isOutOfHours = errorBody?.let {
                    it.contains("fuera de horario", true) ||
                            it.contains("fuera del horario", true) ||
                            it.contains("cerrado", true) ||
                            it.contains("closed", true) ||
                            it.contains("outside hours", true)
                } == true

                errorMessage = when {
                    isOutOfHours -> {
                        val f = DateTimeFormatter.ofPattern("h:mm a", Locale("es", "ES"))
                        if (nextOpenToday != null)
                            context.getString(R.string.out_of_hours_with_time, nextOpenToday.format(f))
                        else
                            context.getString(R.string.out_of_hours_now)
                    }
                    errorBody?.contains("endMin debe ser mayor que startMin") == true ->
                        context.getString(R.string.error_time_range)
                    errorBody?.contains("Parqueo no encontrado") == true || resp.code() == 404 ->
                        context.getString(R.string.parking_not_found)
                    !errorBody.isNullOrBlank() -> errorBody
                    resp.code() == 400 -> context.getString(R.string.availability_validation_error)
                    else -> context.getString(R.string.availability_query_error_http, resp.code())
                }

                buttonEnabled = if (isOutOfHours) false else resp.code() != 404
            }
        }.onFailure { e ->
            errorMessage = context.getString(R.string.network_error_with_reason, e.message ?: "")
            buttonEnabled = true
        }

        isChecking = false
    }

    val activos = lastAvailability?.activeReservations ?: 0
    val libresAhora = (parqueo.capacity - activos).coerceAtLeast(0)
    val photos = remember(parqueo.photos) { parqueo.photos.filterNotNull() }

    val scrollState = rememberScrollState()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { photos.size.coerceAtLeast(1) })

    Scaffold(
        containerColor = Color.White,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            // ⬇️ Bottom bar compacto que respeta insets del sistema
            Surface(color = Color.White) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .navigationBarsPadding()
                        .imePadding(),
                    contentAlignment = Alignment.Center
                ) {
                    GradientButton(
                        text = when {
                            isChecking -> stringResource(R.string.checking_availability_ellipsis)
                            !buttonEnabled -> stringResource(R.string.unavailable_label)
                            else -> stringResource(R.string.park_me_action)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp),
                        enabled = buttonEnabled && !isChecking,
                        onClick = {
                            onPrimaryAction()
                            navController.currentBackStackEntry
                                ?.savedStateHandle
                                ?.set("selected_parking_full", parqueo)
                            navController.navigate("reservaForm/${parqueo.id}")
                        }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Zona desplazable sobre el botón
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
            ) {
                // Header con pager
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                ) {
                    if (photos.isNotEmpty()) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            AsyncImage(
                                model = photos[page],
                                contentDescription = stringResource(R.string.parking_image_cd),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        val lastIndex = (photos.size - 1).coerceAtLeast(0)
                        val current = pagerState.currentPage.coerceIn(0, lastIndex)
                        PagerDotsIndicator(
                            total = photos.size,
                            currentIndex = current,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 10.dp)
                        )
                    } else {
                        // Placeholder sin fotos
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFEAEAEA)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                modifier = Modifier.size(42.dp),
                                imageVector = Icons.Outlined.Image,
                                contentDescription = stringResource(R.string.parking_image_cd),
                                tint = Color.Gray
                            )
                        }
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .size(45.dp)
                            .align(Alignment.TopStart)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.close_content_description),
                            tint = Color.White
                        )
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(horizontal = 25.dp, vertical = 12.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.verticalGradient(listOf(GradientStart, GradientEnd)))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            modifier = Modifier.size(16.dp),
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.distance_from_you_fmt, formatDistanceShort(distanceMeters)),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontFamily = dmSans
                        )
                    }
                }

                // Contenido
                Column(Modifier.padding(horizontal = 25.dp, vertical = 20.dp)) {
                    Text(
                        text = parqueo.localName,
                        fontFamily = dmSans,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall.copy(fontSize = 28.sp)
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = parqueo.address,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = dmSans)
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.hourly_rate_format, parqueo.priceHour),
                        fontFamily = dmSans,
                        color = Color(0xFF0B66FF)
                    )

                    Spacer(Modifier.height(12.dp))

                    when {
                        isChecking -> {
                            Text(
                                text = stringResource(R.string.checking_availability_ellipsis),
                                fontFamily = dmSans,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        errorMessage != null -> {
                            Text(
                                text = errorMessage!!,
                                fontFamily = dmSans,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        else -> {
                            Text(
                                text = stringResource(R.string.available_now_count_format, libresAhora),
                                fontFamily = dmSans,
                                color = if (buttonEnabled) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = abiertoHasta,
                        fontFamily = dmSans,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(16.dp))

                    ScheduleSection(
                        daysOfWeek = parqueo.daysOfWeek,
                        schedules = parqueo.schedules,
                        zone = localZone
                    )

                    Spacer(Modifier.height(20.dp))
                    if (parqueo.characteristics.isEmpty()) {
                        Text(stringResource(R.string.no_characteristics_reported), color = Color.Gray)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            parqueo.characteristics.forEach { FeatureRow(it) }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    if (operator != null) {
                        OperatorInfoRow(
                            imageUrl = operator!!.fotoUrl,
                            title = operator!!.nombre,
                            subtitle = operator!!.rol,
                            rating = 4.5
                        )
                    } else if (operatorError != null) {
                        Text(
                            text = operatorError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun OperatorInfoRow(
    imageUrl: String?,
    title: String,
    subtitle: String?,
    rating: Double?,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    HorizontalDivider(
        modifier = Modifier.padding(top = 14.dp, bottom = 14.dp),
        color = Color(0x1A000000)
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = stringResource(R.string.profile_photo_cd),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.cuenta_icon),
                    contentDescription = stringResource(R.string.profile_photo_cd),
                    modifier = Modifier.size(60.dp),
                    tint = Color.LightGray
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Normal
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF2563EB)
                )
            }
        }

        if (rating != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = String.format(Locale.US, "%.1f", rating),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFC107),
                    modifier = Modifier
                        .size(18.dp)
                        .padding(start = 2.dp)
                )
            }
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(top = 14.dp, bottom = 14.dp),
        color = Color(0x1A000000)
    )
}

@Preview(showBackground = true, showSystemUi = true, name = "ParkingDetail Preview")
@Composable
fun ParkingDetailScreen_Preview() {
    val navController = rememberNavController()

    val sample = ParkingLotDto(
        id = "pk_demo_001",
        localName = "Downtown Center Parking",
        address = "Avenida José Núñez de Cáceres, SD",
        capacity = 120,
        priceHour = 85,
        daysOfWeek = listOf(WeekDay.MON, WeekDay.TUE, WeekDay.WED, WeekDay.THU, WeekDay.FRI),
        schedules = listOf(
            ScheduleRange(open = "08:00", close = "12:00"),
            ScheduleRange(open = "13:00", close = "18:00")
        ),
        characteristics = listOf("Covered parking", "Security guard"),
        photos = listOf(
            "https://picsum.photos/seed/parking1/1280/720",
            "https://picsum.photos/seed/parking2/1280/720",
            "https://picsum.photos/seed/parking3/1280/720"
        ),
        infraDocUrl = null,
        location = null,
        status = "approved",
        createdBy = "user_123",
        createdAt = 1_698_796_800_000L,
        updatedAt = 1_698_883_200_000L,
        rejectionReason = null,
        comments = emptyList()
    )

    ParkingDetailScreen(
        navController = navController,
        parqueo = sample,
        distanceMeters = 250L,
        onClose = {},
        onPrimaryAction = {}
    )
}