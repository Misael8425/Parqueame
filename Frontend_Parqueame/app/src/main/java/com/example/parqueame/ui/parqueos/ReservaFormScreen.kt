package com.example.parqueame.ui.parqueos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.parqueame.R
import com.example.parqueame.api.RetrofitInstance
import com.example.parqueame.models.ParkingLotDto
import com.example.parqueame.session.SessionStore
import com.example.parqueame.ui.common_components.GradientButton
import com.example.parqueame.ui.common_components.GradientIcon
import com.example.parqueame.ui.theme.dmSans
import kotlinx.coroutines.launch
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.draw.rotate
import com.example.parqueame.ui.common_components.CalendarSheet
import com.example.parqueame.models.CreateReservationRequest

// 🔹 Icons para características
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Accessible
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Domain
import androidx.compose.material.icons.outlined.ElectricCar
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.PedalBike
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Public
import androidx.compose.ui.res.stringResource
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservaFormScreen(
    navController: NavController,
    parqueo: ParkingLotDto,
    parkingId: String,
    onContinuar: (Int, Int, Int, Int) -> Unit = { _, _, _, _ -> }
) {
    // ------- Estado de hora/min (24h)  |  PERSISTENTES ENTRE RECOMPOSICIONES -------
    var startHour by rememberSaveable { mutableIntStateOf(0) }   // 0..23
    var startMin  by rememberSaveable { mutableIntStateOf(0) }   // 0,15,30,45
    var endHour   by rememberSaveable { mutableIntStateOf(1) }   // 0..23
    var endMin    by rememberSaveable { mutableIntStateOf(0) }

    // ------- Utilidades de fecha -------
    val zoneId = ZoneId.systemDefault()
    var selectedDate by rememberSaveable { mutableStateOf(LocalDate.now(zoneId)) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Texto mostrado en la UI (depende de selectedDate)
    val fechaTexto = remember(selectedDate) {
        selectedDate.format(
            DateTimeFormatter.ofPattern("EEE, d 'de' MMM", Locale("es", "ES"))
        ).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es","ES")) else it.toString() }
    }

    // “Abierto hoy hasta …” usando schedules del backend si existen (versión simple)
    val horarioHoy: Pair<LocalTime, LocalTime>? = remember(parqueo) {
        if (parqueo.schedules.isEmpty()) null else {
            val best = parqueo.schedules.maxByOrNull { it.close }
            best?.let { LocalTime.parse(it.open) to LocalTime.parse(it.close) }
        }
    }
    val abiertoTexto = horarioHoy?.second?.format(
        DateTimeFormatter.ofPattern("h:mm a", Locale("es", "ES"))
    )?.let { stringResource(R.string.open_today_until_format, it) }
        ?: stringResource(R.string.closed_today)

    // ------- Total estimado -------
    fun toMinutes(h24: Int, m: Int): Int = h24 * 60 + m

    val startAbs = toMinutes(startHour, startMin)
    var endAbs   = toMinutes(endHour, endMin)
    if (endAbs <= startAbs) endAbs += 24 * 60
    val durMin = endAbs - startAbs
    val durHours = ceil(durMin / 60.0).toInt().coerceAtLeast(1)
    val total = durHours * parqueo.priceHour.toDouble()

    val context = LocalContext.current
    val session = remember { SessionStore(context) }
    val userId: String? by session.userId.collectAsState(initial = null)

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isLoading by remember { mutableStateOf(false) }

    fun epochMinutesFor(date: LocalDate, h24: Int, m: Int): Long {
        val dt = LocalDateTime.of(date, LocalTime.of(h24, m))
        return dt.atZone(zoneId).toEpochSecond() / 60L
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Column(modifier = Modifier.padding(horizontal = 25.dp)) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                    title = {},
                    navigationIcon = {
                        GradientIcon(
                            imageVector = ImageVector.vectorResource(R.drawable.back_icon),
                            contentDescription = stringResource(R.string.close_action),
                            modifier = Modifier
                                .size(25.dp)
                                .clickable { navController.popBackStack() }
                        )
                    }
                )
            }
        },
        bottomBar = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 24.dp, vertical = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                GradientButton(
                    text = if (isLoading) stringResource(R.string.creating_ellipsis) else stringResource(R.string.continue_action),
                    modifier = Modifier.width(220.dp),
                    onClick = {
                        if (userId.isNullOrBlank()) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.login_again_toast),
                                    withDismissAction = true
                                )
                            }
                            return@GradientButton
                        }

                        // Validación y fecha de fin si cruza medianoche
                        val startM = startHour * 60 + startMin
                        val endM   = endHour * 60 + endMin
                        val endDateForEnd = if (endM <= startM) selectedDate.plusDays(1) else selectedDate

                        val startEpochMin = epochMinutesFor(selectedDate, startHour, startMin)
                        val endEpochMin   = epochMinutesFor(endDateForEnd, endHour, endMin)

                        // 🔎 Log para verificar qué valores se envían realmente
                        Log.d(
                            "ReservaForm",
                            "date=$selectedDate start=$startHour:$startMin end=$endHour:$endMin tz=${zoneId.id} " +
                                    "startEpochMin=$startEpochMin endEpochMin=$endEpochMin"
                        )

                        isLoading = true
                        scope.launch {
                            try {
                                // Disponibilidad (backend, sin hardcode)
                                val avail = RetrofitInstance.apiService
                                    .checkAvailability(parkingId, startEpochMin, endEpochMin)

                                if (!avail.isSuccessful) {
                                    val msg = avail.errorBody()?.string().orEmpty()
                                        .ifBlank { context.getString(R.string.could_not_verify_availability) }
                                    snackbarHostState.showSnackbar(msg)
                                    isLoading = false
                                    return@launch
                                }
                                if (avail.body()?.available != true) {
                                    val msg = avail.body()?.message
                                        ?: context.getString(R.string.no_availability_for_range)
                                    snackbarHostState.showSnackbar(msg)
                                    isLoading = false
                                    return@launch
                                }

                                // Request de reserva (24h)
                                val req = CreateReservationRequest(
                                    parkingId = parkingId,
                                    startHour24 = startHour,
                                    startMin = startMin,
                                    endHour24 = endHour,
                                    endMin = endMin,
                                    localDate = selectedDate.toString(),
                                    timezone = zoneId.id
                                )

                                val resp = RetrofitInstance.apiService.createReservation(userId!!, req)
                                if (resp.isSuccessful) {
                                    val r = resp.body()!!

                                    // Callback de compatibilidad
                                    onContinuar(
                                        startHour, startMin,
                                        endHour, endMin
                                    )

                                    // Armar resumen con datos del backend
                                    val resumen = ReservationSummaryUi(
                                        parqueo = parqueo,
                                        startEpochMinutes = r.startEpochMin,
                                        endEpochMinutes = r.endEpochMin,
                                        timezone = zoneId.id,
                                        hoursBilled = r.hoursBilled,
                                        pricePerHour = r.pricePerHour,
                                        totalAmount = r.totalAmount,
                                        distanceMeters = null,
                                        paymentMethodLabel = null
                                    )

                                    // Navegar a confirmación
                                    navController.currentBackStackEntry
                                        ?.savedStateHandle
                                        ?.set("resumen_reserva", resumen)
                                    navController.navigate("reserva/confirmacion")
                                } else {
                                    val msg = resp.errorBody()?.string()
                                        .orEmpty()
                                        .ifBlank { context.getString(R.string.could_not_create_reservation) }
                                    snackbarHostState.showSnackbar(msg)
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.network_error_with_reason, e.localizedMessage ?: "")
                                )
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
        ) {
            Text(
                text = parqueo.localName,
                fontFamily = dmSans,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(8.dp))
            // Dirección
            Text(
                text = parqueo.address,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = dmSans, color = Color(0xFF2A2A2A), fontSize = 15.sp
                )
            )
            Spacer(Modifier.height(8.dp))

            // Precio por hora + abierto hasta
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(
                        R.string.price_per_hour_dop_format,
                        "%,.0f".format(Locale.US, parqueo.priceHour.toFloat())
                    ),
                    color = Color(0xFF0B66FF),
                    fontFamily = dmSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Spacer(Modifier.width(10.dp))
                Text(" | ", color = Color(0x33000000))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = abiertoTexto,
                    fontFamily = dmSans,
                    color = Color(0xFF2A2A2A),
                    fontSize = 15.sp
                )
            }
            Spacer(Modifier.height(14.dp))

            // Características (si vinieran)
            if (parqueo.characteristics.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        parqueo.characteristics.forEach { label ->
                            FeatureRow(label)
                        }
                    }
                }
            }

            Spacer(Modifier.height(25.dp))
            HorizontalDivider(color = Color(0xFFEAEAEA))
            Spacer(Modifier.height(25.dp))

            // Encabezado “Tiempo de estadía” + fecha
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    stringResource(R.string.stay_time_label),
                    fontFamily = dmSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )

                Text(
                    fechaTexto,
                    fontFamily = dmSans,
                    color = Color(0xFF0B66FF),
                    fontSize = 16.sp,
                    modifier = Modifier.clickable { showDatePicker = true }
                )
            }

            Spacer(Modifier.height(5.dp))

            // Hora inicio / fin con estilo “píldora”
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                TimeField24(
                    hour = startHour, minute = startMin,
                    onHour = { startHour = it }, onMinute = { startMin = it },
                    modifier = Modifier.weight(1f)
                )
                TimeField24(
                    hour = endHour, minute = endMin,
                    onHour = { endHour = it }, onMinute = { endMin = it },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(25.dp))

            // Total
            Text(
                text = "${stringResource(R.string.total_label)}: ${"%,.2f".format(Locale.US, total)} DOP",
                fontFamily = dmSans,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            CalendarSheet(
                show = showDatePicker,
                initialDate = selectedDate,
                onDismiss = { showDatePicker = false },
                onConfirm = { date ->
                    selectedDate = date
                    showDatePicker = false
                }
            )
        }
    }
}

@Composable
private fun TimeField24(
    hour: Int,
    minute: Int,
    onHour: (Int) -> Unit,
    onMinute: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TimeDropdown24(
                hour = hour,
                minute = minute,
                onChange = { h, m -> onHour(h); onMinute(m) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDropdown24(
    hour: Int,
    minute: Int,
    onChange: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf(false) }
    val selectedTotal = hour * 60 + minute
    val options = remember { (0 until 24 * 60 step 15).toList() } // 00:00..23:45
    val pill = RoundedCornerShape(24.dp) // 🔹 Misma forma para campo y menú

    fun label(totalMin: Int): String =
        "%02d:%02d".format(totalMin / 60, totalMin % 60)

    ExposedDropdownMenuBox(
        expanded = open,
        onExpandedChange = { open = !open }
    ) {
        OutlinedTextField(
            value = label(selectedTotal),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            shape = pill,
            textStyle = LocalTextStyle.current.copy(
                fontFamily = dmSans,
                textAlign = TextAlign.Center,
                fontSize = 16.sp
            ),
            trailingIcon = {
                val arrow = ImageVector.vectorResource(R.drawable.flecha_abajo)
                GradientIcon(
                    imageVector = arrow,
                    contentDescription = null,
                    modifier = Modifier
                        .size(15.dp)
                        .rotate(if (open) 180f else 0f)
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF4F5F7),
                unfocusedContainerColor = Color(0xFFF4F5F7),
                disabledContainerColor = Color(0xFFF4F5F7),
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                disabledTextColor = Color.Black,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                disabledBorderColor = Color.Transparent
            ),
            modifier = modifier
                .menuAnchor()
                .height(50.dp)
                .clip(pill)
        )

        // 🔹 Menú desplegable con mismo fondo gris y forma coherente
        ExposedDropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            modifier = Modifier
                .exposedDropdownSize()
                .clip(pill)
                .background(Color(0xFFF4F5F7))
        ) {
            options.forEach { total ->
                DropdownMenuItem(
                    text = {
                        Text(
                            label(total),
                            fontFamily = dmSans,
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                    },
                    onClick = {
                        onChange(total / 60, total % 60)
                        open = false
                    },
                    colors = MenuDefaults.itemColors(
                        textColor = Color.Black
                    ),
                    modifier = Modifier.background(Color(0xFFF4F5F7))
                )
            }
        }
    }
}

@Composable
private fun FeatureRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = featureIcon(text),
            contentDescription = null,
            tint = Color(0xFF0B66FF),
            modifier = Modifier.size(22.dp)
        )
        Text(text, fontFamily = dmSans, fontSize = 16.sp)
    }
}

private fun featureIcon(label: String): ImageVector {
    val s = label.lowercase(Locale.getDefault())
    return when {
        "guardia" in s || "seguridad" in s -> Icons.Outlined.Person
        "discap" in s || "embaraz" in s || "diversidad" in s -> Icons.Outlined.Accessible
        "subter" in s -> Icons.Outlined.Domain
        "premisa" in s || "lugar" in s -> Icons.Outlined.Place
        "aire libre" in s || "exterior" in s -> Icons.Outlined.Public
        "smart" in s || "sensor" in s -> Icons.Outlined.Memory
        "carga" in s || "eléctr" in s || "electr" in s -> Icons.Outlined.ElectricCar
        "pesad" in s || "camión" in s -> Icons.Outlined.LocalShipping
        "moto" in s || "bicic" in s || "liger" in s -> Icons.Outlined.PedalBike
        else -> Icons.Outlined.CheckCircle
    }
}