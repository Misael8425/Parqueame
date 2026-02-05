package com.example.parqueame.ui.actividad

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.parqueame.R
import com.example.parqueame.api.RetrofitInstance
import com.example.parqueame.models.ParkingLotDto
import com.example.parqueame.models.ReservationDto
import com.example.parqueame.ui.common_components.BottomNavBar
import com.example.parqueame.ui.common_components.BottomNavItem
import com.example.parqueame.ui.common_components.GradientIcon
import com.example.parqueame.ui.navigation.Screen
import com.example.parqueame.ui.theme.GradientBrush
import com.example.parqueame.ui.theme.RtlRomman
import com.example.parqueame.ui.theme.dmSans
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import retrofit2.Response
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/* =========================== UI MODEL ============================ */

data class ActividadItem(
    val id: String,
    val titulo: String,
    val direccion: String,
    val sucursal: String?,
    val fechaCorta: String
)

/* ============================== REPO ============================= */

class ActividadRepository {
    private val api = RetrofitInstance.apiService
    private val localeEs = Locale("es")
    private val fmt = DateTimeFormatter.ofPattern("EEE, d 'de' MMM", localeEs)

    private fun fechaDesdeEpochMin(epochMin: Long): String {
        val localDate = Instant.ofEpochMilli(epochMin * 60_000L)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val txt = localDate.format(fmt)
        return txt.replaceFirstChar { if (it.isLowerCase()) it.titlecase(localeEs) else it.toString() }
    }

    suspend fun getActividadPorUsuario(userId: String): List<ActividadItem> {
        // 1) Reservas del usuario
        val resvResp: Response<List<ReservationDto>> = api.listReservationsByUser(userId)
        val reservas = resvResp.body().orEmpty()
        if (reservas.isEmpty()) return emptyList()

        // 2) Traer parqueos en paralelo
        val parqueoResponses: List<Response<ParkingLotDto>> = reservas.map { r ->
            kotlinx.coroutines.GlobalScope.async {
                api.getParkingByIdDto(r.parkingId)
            }
        }.awaitAll()

        // 3) Combinar
        return reservas.mapIndexed { index, r ->
            val p = parqueoResponses.getOrNull(index)?.body()
            ActividadItem(
                id = r.id,
                titulo = p?.localName ?: "Parqueo",
                direccion = p?.address ?: "",
                sucursal = p?.localName,
                fechaCorta = fechaDesdeEpochMin(r.startEpochMin)
            )
        }
    }
}

/* ============================ VIEWMODEL ========================== */

sealed interface ActividadUiState {
    data object Loading : ActividadUiState
    data class Success(val data: List<ActividadItem>) : ActividadUiState
    data class Error(val message: String) : ActividadUiState
    data object Empty : ActividadUiState
}

class ActividadViewModel(
    private val repo: ActividadRepository = ActividadRepository()
) : ViewModel() {
    var uiState by mutableStateOf<ActividadUiState>(ActividadUiState.Loading)
        private set

    fun loadForUser(userId: String) {
        viewModelScope.launch {
            uiState = ActividadUiState.Loading
            try {
                val items = repo.getActividadPorUsuario(userId)
                uiState = if (items.isEmpty()) ActividadUiState.Empty
                else ActividadUiState.Success(items)
            } catch (e: Exception) {
                uiState = ActividadUiState.Error(e.message ?: "Error al cargar la actividad")
            }
        }
    }
}

/* ============================== SCREEN =========================== */

@Composable
fun ActividadConductorScreen(
    navController: NavController,
    currentUserId: String, // <-- pásalo desde donde tengas el usuario logueado
    viewModel: ActividadViewModel = viewModel()
) {
    // cargar al entrar (o cuando cambie el userId)
    LaunchedEffect(currentUserId) { viewModel.loadForUser(currentUserId) }

    var currentRoute by remember { mutableStateOf("actividad") }

    val navItems = listOf(
        BottomNavItem("inicio",    R.drawable.inicio_icon,    "Inicio"),
        BottomNavItem("actividad", R.drawable.actividad_icon, "Actividad"),
        BottomNavItem("cuenta",    R.drawable.cuenta_icon,    "Cuenta")
    )

    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            BottomNavBar(
                items = navItems,
                currentRoute = currentRoute,
                onItemClick = { route ->
                    currentRoute = route
                    when (route) {
                        "inicio"    -> navController.navigate(Screen.HomeConductor.route)
                        "actividad" -> Unit
                        "cuenta"    -> navController.navigate("cuenta")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
        ) {
            Column(Modifier.padding(horizontal = 25.dp)) {
                // TÍTULO
                Text(
                    text = "Actividad",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = RtlRomman,
                        fontSize = 34.sp
                    ),
                    modifier = Modifier
                        .padding(start = 20.dp, top = 18.dp, bottom = 8.dp)
                        .graphicsLayer(alpha = 0.99f)
                        .drawWithContent {
                            drawContent()
                            drawRect(brush = GradientBrush, blendMode = BlendMode.SrcIn)
                        },
                    color = Color.Unspecified
                )

                // Píldora (decorativa, puedes conectar un date picker)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(18.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    GradientIcon(
                        imageVector = ImageVector.vectorResource(R.drawable.calendar_icon),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Tus reservas",
                        fontFamily = dmSans,
                        fontSize = 14.sp,
                        color = Color(0xFF111827)
                    )
                }

                Spacer(Modifier.height(12.dp))

                when (val state = viewModel.uiState) {
                    is ActividadUiState.Loading -> LoadingListPlaceholder()
                    is ActividadUiState.Error -> ErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadForUser(currentUserId) }
                    )
                    is ActividadUiState.Empty -> EmptyState(onReload = { viewModel.loadForUser(currentUserId) })
                    is ActividadUiState.Success -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(state.data, key = { it.id }) { item ->
                                ActividadCard(
                                    data = item,
                                    onClick = {
                                        // navController.navigate("reservation/${item.id}")
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ============================ UI HELPERS ========================== */

@Composable
private fun LoadingListPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        repeat(4) {
            Surface(
                color = Color(0xFFF6F7FB),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .padding(vertical = 6.dp)
            ) {}
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No pudimos cargar tu actividad",
            fontFamily = dmSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = Color(0xFF111827)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = message,
            fontFamily = dmSans,
            fontSize = 13.sp,
            color = Color(0xFF6B7280)
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRetry) { Text("Reintentar") }
    }
}

@Composable
private fun EmptyState(onReload: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No tienes actividad aún",
            fontFamily = dmSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = Color(0xFF111827)
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = onReload) { Text("Actualizar") }
    }
}

@Composable
private fun ActividadCard(
    data: ActividadItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F7FB))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = data.titulo,
                    fontFamily = dmSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = Color(0xFF111827),
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF1D4ED8)
                )
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = data.direccion,
                fontFamily = dmSans,
                fontSize = 13.sp,
                color = Color(0xFF6B7280),
                lineHeight = 18.sp
            )

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFE5E7EB))
            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color(0xFFEFF2FF), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF06B6D4)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = data.sucursal ?: "—",
                        fontFamily = dmSans,
                        fontSize = 12.sp,
                        color = Color(0xFF111827)
                    )
                }

                Spacer(Modifier.weight(1f))

                Text(
                    text = data.fechaCorta,
                    fontFamily = dmSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color(0xFF1D4ED8)
                )
            }
        }
    }
}

/* ============================== PREVIEW =========================== */

@Preview
@Composable
private fun ActividadConductorScreenPreview() {
    ActividadConductorScreen(
        navController = rememberNavController(),
        currentUserId = "demo-user"
    )
}
