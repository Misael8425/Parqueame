package com.example.parqueame.ui.parqueos

import android.os.Parcelable
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.parqueame.R
import com.example.parqueame.api.RetrofitInstance
import com.example.parqueame.models.ParkingLotDto
import com.example.parqueame.models.UsuarioPerfil
import com.example.parqueame.session.SessionStore
import com.example.parqueame.ui.billetera.BilleteraViewModel
import com.example.parqueame.ui.billetera.CardNicknameStore
import com.example.parqueame.ui.billetera.UiCard
import com.example.parqueame.ui.common_components.GradientButton
import com.example.parqueame.ui.common_components.GradientIcon
import com.example.parqueame.ui.theme.dmSans
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Parcelize
data class ReservationSummaryUi(
    val parqueo: ParkingLotDto,
    val startEpochMinutes: Long,
    val endEpochMinutes: Long,
    val timezone: String,
    val hoursBilled: Int,
    val pricePerHour: Int,
    val totalAmount: Int,
    val distanceMeters: Long? = null,
    val paymentMethodLabel: String? = null
) : Parcelable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservaConfirmacionScreen(
    navController: NavController,
    summary: ReservationSummaryUi
) {
    val zoneId = remember(summary.timezone) {
        runCatching { ZoneId.of(summary.timezone) }.getOrElse { ZoneId.systemDefault() }
    }
    val esES = remember { Locale("es", "ES") }

    val fechaTexto = remember(summary.startEpochMinutes) {
        Instant.ofEpochSecond(summary.startEpochMinutes * 60)
            .atZone(zoneId).toLocalDate()
            .format(DateTimeFormatter.ofPattern("EEE, d 'de' MMM", esES))
            .lowercase(esES)
    }
    val rangoTexto = remember(summary.startEpochMinutes, summary.endEpochMinutes) {
        val s = Instant.ofEpochSecond(summary.startEpochMinutes * 60).atZone(zoneId).toLocalTime()
        val e = Instant.ofEpochSecond(summary.endEpochMinutes * 60).atZone(zoneId).toLocalTime()
        "${formatHoraEs(s, esES)} – ${formatHoraEs(e, esES)}"
    }

    // Estado local para bloquear el botón inmediatamente
    var isProcessing by remember { mutableStateOf(false) }

    var showPicker by remember { mutableStateOf(false) }
    var selectedIdx by rememberSaveable { mutableIntStateOf(0) }

    val subtotal = remember(summary.hoursBilled, summary.pricePerHour) {
        summary.hoursBilled.toDouble() * summary.pricePerHour.toDouble()
    }
    val discounts = 0.0
    val itbisRate = 0.18
    val itbis = remember(subtotal) { subtotal * itbisRate }
    val totalUi = remember(subtotal, itbis, discounts) { (subtotal - discounts + itbis).coerceAtLeast(0.0) }

    val abiertoHasta = remember(summary.parqueo.schedules) {
        val today = LocalDate.now(zoneId)
        val dow = when (today.dayOfWeek) {
            DayOfWeek.MONDAY -> "MON"
            DayOfWeek.TUESDAY -> "TUE"
            DayOfWeek.WEDNESDAY -> "WED"
            DayOfWeek.THURSDAY -> "THU"
            DayOfWeek.FRIDAY -> "FRI"
            DayOfWeek.SATURDAY -> "SAT"
            DayOfWeek.SUNDAY -> "SUN"
        }
        val oc = if (summary.parqueo.daysOfWeek.map { it.name }.contains(dow)
            && summary.parqueo.schedules.isNotEmpty()
        ) summary.parqueo.schedules.maxByOrNull { it.close } else null

        oc?.let {
            val close = LocalTime.parse(it.close)
            buildAnnotatedString {
                append("Abierto hasta las ")
                withStyle(SpanStyle(color = Color(0xFF0B66FF))) {
                    append(close.format(DateTimeFormatter.ofPattern("h:mm a", esES)))
                }
            }
        }
    }

    val context = LocalContext.current
    val session = remember { SessionStore(context) }
    val userId by session.userId.collectAsState(initial = null)

    val walletVm: BilleteraViewModel = viewModel()
    val walletState by walletVm.state.collectAsState()

    LaunchedEffect(userId) {
        val uid = userId.orEmpty()
        if (uid.isNotBlank()) walletVm.bootstrap(uid)
    }

    val cardsWithNicknames: List<UiCard> by remember {
        derivedStateOf {
            walletState.cards.map { c ->
                c.copy(
                    nickname = CardNicknameStore.get(context, c.pmId)
                        ?: CardNicknameStore.getByBrandLast4(context, c.brand, c.last4)
                )
            }
        }
    }

    val selectedCard: UiCard? = cardsWithNicknames.getOrNull(selectedIdx)
    val paymentLabel: String? = selectedCard?.let { card ->
        val displayName = card.nickname ?: card.holder
        "${card.brand} ${card.last4} ($displayName)"
    }

    val api = remember { RetrofitInstance.apiService }
    data class HeaderUser(val nombre: String, val rol: String, val fotoUrl: String?)

    val headerUser by produceState<HeaderUser?>(
        initialValue = null,
        key1 = summary.parqueo.createdBy,
        key2 = userId
    ) {
        value = try {
            val ownerId = summary.parqueo.createdBy.orEmpty()
            if (ownerId.isNotBlank()) {
                val resp = withContext(Dispatchers.IO) { api.getUsuarioPublico(ownerId) }
                if (resp.isSuccessful) {
                    val u: UsuarioPerfil? = resp.body()
                    HeaderUser(
                        nombre = u?.nombre ?: "Usuario",
                        rol = u?.rol ?: "Usuario",
                        fotoUrl = u?.fotoUrl
                    )
                } else null
            } else {
                val uid = userId.orEmpty()
                if (uid.isNotBlank()) {
                    val resp = withContext(Dispatchers.IO) { api.obtenerPerfilPorId(uid) }
                    if (resp.isSuccessful) {
                        val u: UsuarioPerfil? = resp.body()
                        HeaderUser(
                            nombre = u?.nombre ?: "Usuario",
                            rol = u?.rol ?: "Usuario",
                            fotoUrl = u?.fotoUrl
                        )
                    } else null
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    val backendVm: ReservaBackendViewModel = viewModel()
    val backendState by backendVm.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(backendState.error) {
        backendState.error?.let {
            Log.e("RESERVA_FLOW", "Error recibido del ViewModel: $it")
            snackbarHostState.showSnackbar(it)
            isProcessing = false // Desbloquear el botón si hay un error
        }
    }
    LaunchedEffect(backendState.success) {
        backendState.success?.let { created ->
            Log.i("RESERVA_FLOW", "✅ Éxito: Reserva creada con ID: ${created.id}. Navegando...")
            navController.currentBackStackEntry?.savedStateHandle?.set("resumen_reserva", summary)
            navController.navigate("reserva/ruta/${created.id}")
            backendVm.reset()
        }
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
                            imageVector = ImageVector.vectorResource(id = R.drawable.back_icon),
                            contentDescription = "Volver",
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
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 24.dp, vertical = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                GradientButton(
                    text = if (isProcessing || backendState.loading) "Procesando..." else "Confirmar y Pagar",
                    enabled = !isProcessing,
                    modifier = Modifier.width(220.dp),
                    onClick = {
                        if (isProcessing) {
                            Log.w("RESERVA_FLOW", "Click ignorado: ya se está procesando una reserva.")
                            return@GradientButton
                        }
                        if (selectedCard == null) {
                            Log.e("RESERVA_FLOW", "Error: Intento de confirmar sin método de pago.")
                            return@GradientButton
                        }

                        Log.d("RESERVA_FLOW", "=======================================")
                        Log.d("RESERVA_FLOW", "Botón 'Confirmar' presionado. Bloqueando UI.")
                        isProcessing = true

                        val finalSummary = summary.copy(paymentMethodLabel = paymentLabel)

                        Log.d("RESERVA_FLOW", "Llamando a backendVm.confirmar() con resumen: $finalSummary")
                        backendVm.confirmar(finalSummary)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            summary.distanceMeters?.takeIf { it > 0 }?.let { dist ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    DistancePill(text = "A ${"%,.0f".format(Locale.US, dist)} m de ti")
                }
            }
            Text(
                text = "Confirmación de reserva",
                fontFamily = dmSans,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    summary.parqueo.localName,
                    fontFamily = dmSans,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = summary.parqueo.address,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = dmSans, color = Color(0xFF2A2A2A), fontSize = 15.sp
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${"%(,.0f".format(Locale.US, summary.parqueo.priceHour.toFloat())} DOP por hora",
                        color = Color(0xFF0B66FF),
                        fontFamily = dmSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(" | ", color = Color(0x33000000))
                    Spacer(Modifier.width(10.dp))
                    if (abiertoHasta != null) {
                        Text(
                            abiertoHasta,
                            fontFamily = dmSans,
                            color = Color(0xFF2A2A2A),
                            fontSize = 15.sp
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserAvatar(
                        name = headerUser?.nombre ?: "U",
                        photoUrl = headerUser?.fotoUrl,
                        size = 55.dp
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = headerUser?.nombre ?: "Usuario",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(start = 5.dp)
                        )
                        Text(
                            text = headerUser?.rol ?: "—",
                            fontSize = 16.sp,
                            modifier = Modifier.padding(start = 5.dp),
                            color = Color(0xFF0B66FF)
                        )
                    }
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFFFFF),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = BorderStroke(1.dp, Color(0x11000000))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    HorizontalDivider(color = Color(0x00000000))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Tiempo de estadía", fontFamily = dmSans, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(rangoTexto, fontFamily = dmSans, color = Color(0xFF0B66FF), fontSize = 16.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(fechaTexto, fontFamily = dmSans, color = Color(0xFF4F7DF9), fontSize = 14.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "${summary.hoursBilled} ${if (summary.hoursBilled == 1) "hora" else "horas"}",
                                fontFamily = dmSans, color = Color(0xFF6B6B6B), fontSize = 14.sp
                            )
                        }
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryRow("Subtotal", dop(subtotal))
                SummaryRow("ITBIS (18%)", dop(itbis))
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total", fontFamily = dmSans, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(dop(totalUi), fontFamily = dmSans, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showPicker = true },
                color = Color(0xFFF8F9FB)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (selectedCard != null) {
                            Icon(
                                painter = painterResource(id = brandMenuIconRes(selectedCard.brand)),
                                contentDescription = selectedCard.brand,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = paymentLabel ?: "Selecciona un método de pago",
                                fontFamily = dmSans,
                                fontSize = 16.sp,
                                color = Color(0xFF1A1A1A)
                            )
                        } else {
                            Icon(
                                painter = painterResource(id = R.drawable.card_icon),
                                contentDescription = null,
                                tint = Color(0xFF6B6B6B),
                                modifier = Modifier.size(25.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Selecciona un método de pago",
                                fontFamily = dmSans,
                                fontSize = 16.sp,
                                color = Color(0xFF6B6B6B)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Cambiar",
                        tint = Color(0xFF1A1A1A)
                    )
                }
            }
            if (showPicker) {
                CardPickerSheetLite(
                    cards = cardsWithNicknames,
                    onSelect = { index ->
                        selectedIdx = index
                        showPicker = false
                    },
                    onAddNew = {
                        showPicker = false
                        navController.navigate("wallet/addCard")
                    },
                    onDismiss = { showPicker = false }
                )
            }
        }
    }
}

@Composable
private fun DistancePill(text: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFE9F2FF))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.LocationOn,
            contentDescription = null,
            tint = Color(0xFF0B66FF),
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            fontFamily = dmSans,
            fontSize = 12.sp,
            color = Color(0xFF0B66FF),
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardPickerSheetLite(
    cards: List<UiCard>,
    onSelect: (Int) -> Unit,
    onAddNew: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        dragHandle = {}
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 25.dp)
        ) {
            cards.forEachIndexed { index, card ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(index) }
                        .padding(vertical = 14.dp, horizontal = 25.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = brandMenuIconRes(card.brand)),
                        contentDescription = card.brand,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "${card.brand} ${card.last4} (${card.nickname ?: card.holder})",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = dmSans,
                            fontWeight = FontWeight.Medium,
                        )
                    )
                }
                HorizontalDivider(thickness = 0.6.dp, color = Color(0xFFE7EAF0))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAddNew() }
                    .padding(vertical = 14.dp, horizontal = 25.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GradientIcon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.plus_icon),
                    contentDescription = null,
                    modifier = Modifier.size(25.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Añadir otro método de pago",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = dmSans,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontFamily = dmSans, color = Color(0xFF9AA0A6))
        Text(value, fontFamily = dmSans, color = Color(0xFF9AA0A6))
    }
}

private fun dop(amount: Double): String = "DOP ${"%,.2f".format(Locale.US, amount)}"

private fun formatHoraEs(t: LocalTime, locale: Locale): String {
    val base = t.format(DateTimeFormatter.ofPattern("hh:mm a", locale)).lowercase(locale)
    return base.replace("am", "a. m.").replace("pm", "p. m.")
}

private fun brandMenuIconRes(brand: String): Int = when (brand.trim().lowercase()) {
    "visa" -> R.drawable.visa_card_icon
    "mastercard", "master card", "mc" -> R.drawable.mc_card_icon
    "american express", "american_express", "amex" -> R.drawable.amex_card_icon
    else -> R.drawable.card_icon
}

@Composable
private fun UserAvatar(
    name: String,
    photoUrl: String?,
    size: Dp
) {
    val initials = remember(name) {
        name.trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifBlank { "U" }
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFFF5F5F5)),
        contentAlignment = Alignment.Center
    ) {
        if (!photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(photoUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Foto de $name",
                modifier = Modifier.matchParentSize().clip(CircleShape)
            )
        } else {
            Text(
                text = initials,
                fontFamily = dmSans,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                color = Color(0xFF6B6B6B)
            )
        }
    }
}