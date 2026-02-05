package com.example.parqueame.ui.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import com.example.parqueame.R
import com.example.parqueame.models.ParkingLotDto
import com.example.parqueame.ui.common_components.BottomNavBar
import com.example.parqueame.ui.common_components.BottomNavItem
import com.example.parqueame.ui.common_components.GradientIcon
import com.example.parqueame.ui.theme.GradientBrush
import com.example.parqueame.ui.theme.RtlRomman
import com.example.parqueame.ui.theme.dmSans
import com.example.parqueame.utils.isLight
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.example.parqueame.ui.common_components.GradientIcon
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.luminance
import com.example.parqueame.ui.theme.loginBackgroundGradient
import com.example.parqueame.ui.theme.loginTopGradientColor
import com.example.parqueame.utils.isLight
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.maps.android.compose.*
import kotlin.math.*
import com.example.parqueame.api.RetrofitInstance
import com.example.parqueame.ui.navigation.Screen
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.Divider
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.parqueame.models.ReservationDto
import com.example.parqueame.session.SessionStore
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import com.example.parqueame.ui.components.ParkingRatingModal
import com.example.parqueame.ui.parqueos.ReservationSummaryUi
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.example.parqueame.api.ApiService


data class BusquedaReciente(
    val id: String,
    val nombre: String,
    val direccion: String,
    val distancia: String
)



@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeConductorScreen(
    navController: NavController? = null,
    userLat: Double? = null,
    userLng: Double? = null
) {
    val context = LocalContext.current
    val whiteBackground = Color.White
    val useDarkIconsStatusBar = whiteBackground.luminance() > 0.5f
    val systemUiController = rememberSystemUiController()
    val useDarkIcons = whiteBackground.luminance() > 0.5f

    SideEffect {
        systemUiController.setStatusBarColor(whiteBackground, darkIcons = useDarkIcons)
        systemUiController.setNavigationBarColor(whiteBackground, darkIcons = useDarkIcons)
    }

    // 🔹 Solo para pruebas: que el modal aparezca al abrir Home
    var showRating by rememberSaveable { mutableStateOf(false) }

    // 🔹 Datos de ejemplo (luego vendrán del summary de la reserva)
    val parkingName = "Estacionamiento de Downtown Center"
    val parkingAddress = "Av. José Núñez de Cáceres, Santo Domingo"
    val parkingImageUrl: String? = null
    val operatorName = "Downtown Center"



    // ViewModel del mapa (como ya lo tenías)
    val mapViewModel = remember { MapViewModel() }
    val parkingLots by mapViewModel.parkingLots.collectAsState()

    // Permisos
    val multiplePermissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
    LaunchedEffect(Unit) {
        if (!multiplePermissionsState.allPermissionsGranted) {
            multiplePermissionsState.launchMultiplePermissionRequest()
        }
    }

    // Cargar parqueos
    LaunchedEffect(Unit) { mapViewModel.loadParkingLots() }

    // Ubicación usuario
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    LaunchedEffect(multiplePermissionsState.allPermissionsGranted) {
        if (multiplePermissionsState.allPermissionsGranted) {
            val fused: FusedLocationProviderClient =
                LocationServices.getFusedLocationProviderClient(context)
            if (
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                fused.lastLocation.addOnSuccessListener { loc ->
                    loc?.let { userLocation = LatLng(it.latitude, it.longitude) }
                }
            }
        }
    }

    // Cámara
    val defaultLocation = LatLng(18.4861, -69.9312)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(userLocation ?: defaultLocation, 14f)
    }

    // -------- Estado búsqueda + filtros ----------
    var query by rememberSaveable { mutableStateOf("") }
    val isSearching = query.isNotBlank()

    val characteristicOptions = remember {
        listOf(
            "Parqueos techados",
            "Guardia de seguridad",
            "Parqueos para personas con diversidad funcional y embarazadas",
            "Parqueos subterráneos",
            "Parqueos en premisa",
            "Parqueos al aire libre",
            "Smartparking",
            "Parqueos con estaciones de carga para vehículos eléctricos",
            "Parqueos para vehículos pesados",
            "Parqueos para vehículos ligeros (motocicletas, bicicletas, etc.)"
        )
    }

    var selectedCharacteristics by rememberSaveable { mutableStateOf(setOf<String>()) }
    var priceMin by rememberSaveable { mutableFloatStateOf(0f) }
    var priceMax by rememberSaveable { mutableFloatStateOf(500f) }

    val hasActiveChars = selectedCharacteristics.isNotEmpty()
    val hasPriceFilter = (priceMin > 0f) || (priceMax < 500f)

    val filteredList by remember(parkingLots, query, selectedCharacteristics, priceMin, priceMax, hasPriceFilter) {
        derivedStateOf {
            parkingLots.filter { lot ->
                val matchesText = if (query.isBlank()) true else matchesQuery(lot, query)

                // características del lote (ajusta el nombre si tu DTO difiere)
                val lotChars = (lot.characteristics ?: emptyList())
                val matchesChars =
                    if (selectedCharacteristics.isEmpty()) true
                    else selectedCharacteristics.all { it in lotChars }

                // precio por hora
                val matchesPrice = if (hasPriceFilter) {
                    val p = lot.priceHour ?: return@filter false
                    p.toFloat() in priceMin..priceMax
                } else true

                matchesText && matchesChars && matchesPrice
            }
        }
    }

    // Qué lista dibuja el mapa
    val lotsForMap by remember(isSearching, hasActiveChars, hasPriceFilter, parkingLots, filteredList) {
        derivedStateOf { if (isSearching || hasActiveChars || hasPriceFilter) filteredList else parkingLots }
    }

    var showFilters by remember { mutableStateOf(false) }

    // -------- Bottom nav ----------
    var currentRoute by rememberSaveable { mutableStateOf("inicio") }
    val navItems = listOf(
        BottomNavItem("inicio", R.drawable.inicio_icon, stringResource(R.string.bottom_home)),
        BottomNavItem("actividad", R.drawable.actividad_icon, stringResource(R.string.activity_screen_title)),
        BottomNavItem("cuenta", R.drawable.cuenta_icon, stringResource(R.string.account_title))
    )

    // userId actual
    val session = remember { SessionStore(context) }
    val userId by session.userId.collectAsState(initial = null)

// estado de reserva activa
    var activeReservation by remember { mutableStateOf<ReservationDto?>(null) }

    // Parking correspondiente a la reserva activa
    val activeParking: ParkingLotDto? = remember(activeReservation, parkingLots) {
        val pid = activeReservation?.parkingId
        parkingLots.firstOrNull { it.id == pid }
    }

// Summary para RutaReservaScreen
    val activeSummary: ReservationSummaryUi? = remember(activeReservation, activeParking) {
        if (activeReservation != null && activeParking != null) {
            ReservationSummaryUi(
                parqueo = activeParking,
                startEpochMinutes = activeReservation!!.startEpochMin,
                endEpochMinutes = activeReservation!!.endEpochMin,
                timezone = ZoneId.systemDefault().id,
                hoursBilled = activeReservation!!.hoursBilled,
                pricePerHour = activeReservation!!.pricePerHour,
                totalAmount = activeReservation!!.totalAmount,
                distanceMeters = null,
                paymentMethodLabel = null
            )
        } else null
    }

// cuando entres a "inicio", consulta el backend
    LaunchedEffect(userId, currentRoute) {
        val uid = userId.orEmpty()
        if (uid.isBlank()) {
            activeReservation = null
            return@LaunchedEffect
        }

            runCatching {
                val resp = RetrofitInstance.apiService
                    .listReservationsByUser(uid)
                if (resp.isSuccessful) {
                    activeReservation = resp.body().orEmpty().firstOrNull()
                } else {
                    activeReservation = null
                }
            }.onFailure { activeReservation = null }

    }


    Scaffold(
        bottomBar = {
            BottomNavBar(
                items = navItems,
                currentRoute = currentRoute,
                onItemClick = { route ->
                    currentRoute = route
                    when (route) {
                        "inicio" -> Unit
                        "actividad" -> navController?.navigate("actividad")
                        "cuenta" -> navController?.navigate("cuenta")
                    }
                }
            )
        }
    )
    { padding ->
        when (currentRoute) {
            "inicio" -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(Color.White)
                ) {
//                    if (activeSummary != null) {
//                    Card(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(horizontal = 16.dp, vertical = 8.dp)
//                            .clickable {
//                                navController?.currentBackStackEntry
//                                    ?.savedStateHandle
//                                    ?.set("resumen_reserva", activeSummary)
//                                navController?.navigate("reserva/ruta")
//                            },
//                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF4FF)),
//                        shape = RoundedCornerShape(14.dp)
//                    ) {
//                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
//                            Icon(
//                                imageVector = Icons.Filled.AccessTime,
//                                contentDescription = "Reserva activa",
//                                tint = Color(0xFF0B66FF)
//                            )
//                            Spacer(Modifier.width(10.dp))
//                            Column(Modifier.weight(1f)) {
//                                Text(
//                                    "Tienes una reserva en curso",
//                                    color = Color(0xFF0B66FF),
//                                    fontWeight = FontWeight.Bold
//                                )
//                                val endLocal = Instant.ofEpochSecond(activeSummary.endEpochMinutes * 60)
//                                    .atZone(ZoneId.systemDefault())
//                                    .toLocalTime()
//                                val horaFin = endLocal.format(
//                                    DateTimeFormatter.ofPattern("h:mm a", Locale("es", "ES"))
//                                )
//                                Text("Estacionado hasta $horaFin", color = Color(0xFF4F7DF9))
//                            }
//                            Text("Ver", color = Color(0xFF0B66FF))
//                        }
//                    }
//                }


                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp)
                    ) {
                        // Título
                        Box(modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)) {
                            Text(
                                text = stringResource(R.string.nav_item_home),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = RtlRomman,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier
                                    .graphicsLayer(alpha = 0.99f)
                                    .drawWithContent {
                                        drawContent()
                                        drawRect(brush = GradientBrush, blendMode = BlendMode.SrcIn)
                                    },
                                color = Color.Unspecified
                            )
                        }

                        // Buscador + pill Filtrar
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 3.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                        ) {
                            var expanded by remember { mutableStateOf(false) }
                            var selectedTime by rememberSaveable { mutableStateOf("") }
                            val nowLabel = stringResource(R.string.now_label)
                            LaunchedEffect(nowLabel) {
                                if (selectedTime.isBlank()) selectedTime = nowLabel
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.lupa_icon),
                                    contentDescription = stringResource(R.string.search_cd),
                                    tint = Color.Black,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(8.dp))

                                TextField(
                                    value = query,
                                    onValueChange = { query = it },
                                    textStyle = TextStyle(
                                        fontFamily = dmSans,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 16.sp
                                    ),
                                    placeholder = {
                                        Text(
                                            stringResource(R.string.search_placeholder_where),
                                            color = Color.Gray.copy(alpha = 0.7f),
                                            fontFamily = dmSans,
                                            fontSize = 16.sp
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 44.dp), // altura estable
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent
                                    )
                                )

                                Spacer(Modifier.width(10.dp))

                                val activeCount = selectedCharacteristics.size + if (hasPriceFilter) 1 else 0
                                Row(
                                    modifier = Modifier
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(GradientBrush)
                                        .clickable { showFilters = true }
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FilterList,
                                        contentDescription = stringResource(R.string.filters_cd),
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = if (activeCount > 0)
                                            stringResource(R.string.filter_with_count_format, activeCount)
                                        else
                                            stringResource(R.string.filter_title),                                   fontFamily = dmSans,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        if (isSearching || hasActiveChars || hasPriceFilter) {
                            // -------- RESULTADOS ARRIBA + MAPA ABAJO --------
                            Text(
                                text = stringResource(R.string.results_title),
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = dmSans,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 18.sp
                                )
                            )

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                items(filteredList, key = { it.id }) { lot ->
                                    val lat = lot.location?.lat
                                    val lng = lot.location?.lng
                                    val dm: Double? = if (userLocation != null && lat != null && lng != null) {
                                        haversineMeters(userLocation!!.latitude, userLocation!!.longitude, lat, lng)
                                    } else null

                                    val distanceText = dm?.let { formatDistance(it) } ?: "—"
                                    ParkingItem(
                                        id = lot.id,
                                        name = lot.localName,
                                        address = lot.address,
                                        distance = distanceText,
                                        distMeters = dm,
                                        navController = navController
                                    )
                                }
                                if (filteredList.isEmpty()) {
                                    item {
                                        Text(
                                            text = stringResource(R.string.no_results_message),
                                            color = Color.Gray,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }

                            MapCard(
                                cameraPositionState = cameraPositionState,
                                lots = lotsForMap,
                                userLocation = userLocation,
                                permissionsGranted = multiplePermissionsState.allPermissionsGranted,
                                navController = navController
                            )
                        } else {
                            // -------- MAPA ARRIBA + CERCA DE TI DEBAJO --------
                            MapCard(
                                cameraPositionState = cameraPositionState,
                                lots = parkingLots,
                                userLocation = userLocation,
                                permissionsGranted = multiplePermissionsState.allPermissionsGranted,
                                navController = navController
                            )

                            Text(
                                text = stringResource(R.string.near_you),
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = dmSans,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 18.sp
                                )
                            )

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                items(parkingLots, key = { it.id }) { lot ->
                                    val lat = lot.location?.lat
                                    val lng = lot.location?.lng
                                    val dm: Double? = if (userLocation != null && lat != null && lng != null) {
                                        haversineMeters(userLocation!!.latitude, userLocation!!.longitude, lat, lng)
                                    } else null

                                    val distanceText = dm?.let { formatDistance(it) } ?: "—"

                                    ParkingItem(
                                        id = lot.id,
                                        name = lot.localName,
                                        address = lot.address,
                                        distance = distanceText,
                                        distMeters = dm,
                                        navController = navController
                                    )
                                }
                            }
                        }
                    }
                }
            }

            "actividad" -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { Text(stringResource(R.string.activity_screen_title)) }
            "actividad" -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = painterResource(R.drawable.actividad_icon),
                            contentDescription = stringResource(R.string.activity_screen_title),
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.activity_screen_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            "cuenta" -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { Text(stringResource(R.string.account_title)) }
            "cuenta" -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = painterResource(R.drawable.cuenta_icon),
                            contentDescription = stringResource(R.string.account_title),
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.account_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        // 🔹 Modal de calificación (AL FINAL para que quede por encima)
        ParkingRatingModal(
            visible = showRating, // por ahora true para probar
            parkingName = parkingName,
            address = parkingAddress,
            imageUrl = parkingImageUrl, // puede ser null
            operatorName = operatorName,
            onSubmit = { rating ->
                showRating = false
                // TODO luego: viewModel.rateParking(parkingId, rating)
            },
            onIgnore = { showRating = false },
            onClose = { showRating = false }
        )
    }

    // ---------- MODAL DE FILTROS ----------
// ---------- MODAL DE FILTROS ----------
    if (showFilters) {
        FiltersDialog(
            allCharacteristics = characteristicOptions,
            selected = selectedCharacteristics,
            initialPriceRange = priceMin..priceMax,
            onToggleChar = { opt ->
                selectedCharacteristics =
                    if (opt in selectedCharacteristics) selectedCharacteristics - opt
                    else selectedCharacteristics + opt
            },
            onPriceChange = { range ->
                priceMin = range.start
                priceMax = range.endInclusive
            },
            onClear = {
                selectedCharacteristics = emptySet()
                priceMin = 0f
                priceMax = 500f
            },
            onDismiss = { showFilters = false },
            onApply = { showFilters = false }
        )
    }

}

// ---------- MapCard reutilizable ----------
@Composable
private fun MapCard(
    cameraPositionState: CameraPositionState,
    lots: List<ParkingLotDto>,
    userLocation: LatLng?,
    permissionsGranted: Boolean,
    navController: NavController?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = permissionsGranted),
            uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = true)
        ) {
            lots.forEach { lot ->
                val lat = lot.location?.lat
                val lng = lot.location?.lng
                if (lat != null && lng != null) {
                    val pos = LatLng(lat, lng)
                    Marker(
                        state = rememberMarkerState(position = pos),
                        title = lot.localName,
                        snippet = lot.address,
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                        onClick = {
                            val dist = if (userLocation != null) {
                                haversineMeters(userLocation.latitude, userLocation.longitude, lat, lng).toLong()
                            } else -1L
                            navController?.navigate("parkingDetail/${lot.id}?distMeters=$dist")
                            true
                        }
                    )
                }
            }
        }
    }
}

// ---------- Modal estilo diálogo (más compacto y con precio al inicio) ----------
// ---------- Modal estilo diálogo con footer fijo y precio fijo ----------
@Composable
private fun FiltersDialog(
    allCharacteristics: List<String>,
    selected: Set<String>,
    initialPriceRange: ClosedFloatingPointRange<Float>,
    onToggleChar: (String) -> Unit,
    onPriceChange: (ClosedFloatingPointRange<Float>) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
    onApply: () -> Unit
) {
    // Estados locales para editar sin mutar al padre hasta Confirmar
    var localSelected by remember { mutableStateOf(selected) }
    var localPrice by remember { mutableStateOf(initialPriceRange) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(6.dp),
                modifier = Modifier
                    .fillMaxWidth(0.92f)   // ancho controlado
                    .fillMaxHeight(0.80f),  // alto controlado
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    // ===== Header =====
                    Text(
                        stringResource(R.string.filter_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        stringResource(R.string.filter_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6B7280)
                    )

                    // ===== Precio por hora (FIJO) =====
                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.price_per_hour_dop_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.min_label_fmt, localPrice.start.toInt()))
                        Text(stringResource(R.string.max_label_fmt, localPrice.endInclusive.toInt()))
                    }
                    Spacer(Modifier.height(8.dp))

                    RangeSlider(
                        value = localPrice,
                        onValueChange = { range ->
                            val clamped = range.start.coerceAtLeast(0f)..range.endInclusive.coerceAtMost(500f)
                            localPrice = clamped
                        },
                        valueRange = 0f..500f,
                        steps = 10
                    )

                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(
                            R.string.range_dop_format,
                            localPrice.start.toInt(),
                            localPrice.endInclusive.toInt()
                        ),
                        color = Color(0xFF2563EB),
                        fontWeight = FontWeight.SemiBold
                    )

                    Divider(Modifier.padding(vertical = 16.dp))

                    // ===== Características (SOLO ESTA PARTE HACE SCROLL) =====
                    Text(
                        stringResource(R.string.features), // ya existente
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f) // ocupa el espacio disponible entre header y footer
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Column {
                            allCharacteristics.forEach { opt ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            localSelected =
                                                if (opt in localSelected) localSelected - opt
                                                else localSelected + opt
                                        }
                                        .padding(vertical = 6.dp)
                                ) {
                                    Checkbox(
                                        checked = opt in localSelected,
                                        onCheckedChange = {
                                            localSelected =
                                                if (opt in localSelected) localSelected - opt
                                                else localSelected + opt
                                        }
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(opt)
                                }
                            }
                        }
                    }

                    Divider()

                    // ===== Footer (FIJO) =====
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                // Limpia estados locales y del padre
                                onClear()
                                localSelected = emptySet()
                                localPrice = 0f..500f
                            }
                        ) { Text(stringResource(R.string.clear_action)) }

                        Row {
                            TextButton(onClick = onDismiss) { Text(stringResource(R.string.button_cancel)) }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    // Sincroniza con el padre
                                    onClear()
                                    localSelected.forEach { onToggleChar(it) }
                                    onPriceChange(localPrice)
                                    onApply()
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) { Text(stringResource(R.string.button_confirm)) }
                        }
                    }
                }
            }
        }
    }
}


// ---------- Item de lista ----------
@Composable
fun ParkingItem(
    navController: NavController? = null,
    id: String,
    name: String,
    address: String,
    distance: String,
    distMeters: Double? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                val dist = distMeters?.toLong() ?: -1L
                navController?.navigate("parkingDetail/$id?distMeters=$dist")
              },
        colors = CardDefaults.cardColors(
            containerColor = if (isPressed) Color(0x143498DB) else Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GradientIcon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(30.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = name,
                    fontWeight = FontWeight.Bold,
                    fontFamily = dmSans,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = address,
                    fontFamily = dmSans,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray.copy(alpha = 0.8f)
                )
            }
            Text(
                text = distance,
                modifier = Modifier
                    .graphicsLayer(alpha = 0.99f)
                    .drawWithContent {
                        drawContent()
                        drawRect(brush = GradientBrush, blendMode = BlendMode.SrcIn)
                    },
                color = Color.Unspecified,
                fontFamily = dmSans,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ---------- Utils ----------
private fun String.normalize(): String =
    lowercase()
        .replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
        .replace("ñ", "n")

private fun matchesQuery(lot: ParkingLotDto, q: String): Boolean {
    if (q.isBlank()) return true
    val nq = q.normalize()
    val name = lot.localName.orEmpty().normalize()
    val addr = lot.address.orEmpty().normalize()
    return name.contains(nq) || addr.contains(nq)
}

private fun haversineMeters(
    lat1: Double, lon1: Double,
    lat2: Double, lon2: Double
): Double {
    val R = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2.0) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2).pow(2.0)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return R * c
}

private fun formatDistance(distanceMeters: Double): String {
    return if (distanceMeters < 1000) {
        "${distanceMeters.roundToInt()} m"
    } else {
        val km = distanceMeters / 1000.0
        if (km < 10) String.format("%.1f km", km) else String.format("%.0f km", km)
    }
}
