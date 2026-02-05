package com.example.parqueame.ui.admin.parqueosInfo

import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.parqueame.R
import com.example.parqueame.models.Parqueo
import com.example.parqueame.ui.theme.dmSans
import androidx.compose.ui.res.painterResource

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ParqueoDetalleScreen(
    navController: NavController,
    onClose: () -> Unit,
    parkingId: String,
    vm: ParqueoDetalleViewModel = viewModel()
) {
    var showSheet by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }

    LaunchedEffect(parkingId) { vm.cargarParqueo(parkingId) }

    val ui by vm.ui.collectAsState()

    when {
        ui.isLoading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }
        ui.error != null -> {
            ErrorState(
                message = ui.error ?: stringResource(R.string.error_default),
                onClose = onClose,
                onRetry = { vm.cargarParqueo(parkingId) }
            )
            return
        }
    }

    val parqueo: Parqueo = ui.data ?: run {
        ErrorState(
            message = stringResource(R.string.no_data_found),
            onClose = onClose,
            onRetry = { vm.cargarParqueo(parkingId) }
        )
        return
    }

    val images = parqueo.imagenesURL.ifEmpty {
        listOf(
            "https://picsum.photos/800/400?random=1",
            "https://picsum.photos/800/400?random=2"
        )
    }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { images.size })
    val isInactive = (ui.status == "Deshabilitado")

    ParqueoOptionsSheet(
        showTime = showTime,
        showSheet = showSheet,
        onDismiss = { showSheet = false; showTime = false },
        onShowTime = { showTime = true },
        navController = navController,
        parkingId = parkingId,
        daysLabel = ui.daysLabel,
        scheduleLabels = ui.scheduleLabels,
        isInactive = isInactive,
        onDisable = {
            vm.deshabilitarParqueo(parkingId) { ok ->
                if (ok) vm.cargarParqueo(parkingId)
            }
        },
        onEnable = {
            vm.habilitarParqueo(parkingId) { ok ->
                if (ok) vm.cargarParqueo(parkingId)
            }
        }
    )

    Column (modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.height(300.dp)) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                AsyncImage(
                    model = images[page],
                    contentDescription = stringResource(R.string.parking_image_cd),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 24.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.close_button_cd),
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                IconButton(onClick = { showSheet = true }) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = stringResource(R.string.more_options_cd),
                        modifier = Modifier.size(30.dp),
                        tint = Color.White
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(images.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 10.dp else 8.dp)
                            .background(
                                color = if (isSelected) Color.White else Color.Gray,
                                shape = RoundedCornerShape(percent = 50)
                            )
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = parqueo.nombre,
                        fontFamily = dmSans,
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)

                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = parqueo.direccion,
                        fontFamily = dmSans,
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "4.5", style = MaterialTheme.typography.bodyMedium, fontFamily = dmSans) // Example rating
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

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.hourly_rate_format, parqueo.tarifaHora.toInt()),
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF0B66FF)),
                    fontFamily = dmSans
                )
                Spacer(Modifier.width(8.dp))
                HorizontalDivider(
                    color = Color.Gray.copy(alpha = 0.4f),
                    modifier = Modifier
                        .height(14.dp)
                        .width(1.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.schedule_label),
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Black),
                    fontFamily = dmSans
                )
                TextButton(
                    onClick = { showTime = true },
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF0B66FF))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = stringResource(R.string.view_action), style = MaterialTheme.typography.bodyMedium, fontFamily = dmSans)
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.open_options_cd)
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${parqueo.capacidad} ",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF0B66FF)),
                    fontFamily = dmSans
                )
                Text(
                    text = stringResource(R.string.available_spots_suffix),
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Black),
                    fontFamily = dmSans
                )
            }

            Spacer(Modifier.height(28.dp))

            CaracteristicasSeccion(characteristics = ui.characteristics)

            Text(stringResource(R.string.status_label), style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray), fontFamily = dmSans)

            val statusText = when (ui.status) {
                "Activo" -> stringResource(R.string.status_active)
                "Rechazado" -> stringResource(R.string.status_rejected)
                "Pendiente" -> stringResource(R.string.status_pending)
                "Deshabilitado" -> stringResource(R.string.status_disabled)
                else -> ui.status ?: "—"
            }
            val statusColor = when (ui.status) {
                "Activo" -> Color(0xFF0B66FF)
                "Rechazado" -> Color(0xFFDC2626)
                "Pendiente" -> Color(0xFFFF9800)
                "Deshabilitado" -> Color.Gray
                else -> Color.Gray
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    fontFamily = dmSans
                )
            )

            // ✅ INICIO DE LA CORRECCIÓN
            val reason = ui.rejectionReason
            if (ui.status == "Rechazado" && !reason.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.rejection_reason_prefix, reason),
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFDC2626)),
                    fontFamily = dmSans
                )
            }
            // ✅ FIN DE LA CORRECCIÓN
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParqueoOptionsSheet(
    showTime: Boolean,
    showSheet: Boolean,
    onDismiss: () -> Unit,
    onShowTime: () -> Unit,
    navController: NavController,
    parkingId: String,
    daysLabel: String,
    scheduleLabels: List<String>,
    isInactive: Boolean,
    onDisable: () -> Unit,
    onEnable: () -> Unit
) {
    if (!showTime && !showSheet) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        dragHandle = {}
    ) {
        if (showTime) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.parking_schedule_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    fontFamily = dmSans
                )
                Spacer(Modifier.height(6.dp))
                HorizontalDivider(color = Color(0xFFE7EAF0))
                Spacer(Modifier.height(12.dp))

                if ((daysLabel == "—") && scheduleLabels.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_schedule_configured),
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color(0x99000000)),
                        fontFamily = dmSans
                    )
                } else {
                    if (daysLabel != "—") {
                        Text(
                            text = daysLabel,
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF0B66FF)),
                            fontFamily = dmSans
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        scheduleLabels.forEach { label ->
                            Text(
                                text = "• $label",
                                style = MaterialTheme.typography.bodyLarge,
                                fontFamily = dmSans
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFE7EAF0))
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.cancel), color = Color(0xFFDC2626), fontFamily = dmSans) }

                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                SheetAction(
                    title = stringResource(R.string.edit_parking_action),
                    onClick = {
                        onDismiss()
                        navController.navigate("admin/editar/$parkingId")
                    }
                )
                HorizontalDivider(color = Color(0xFFE7EAF0))

                SheetAction(
                    title = if (isInactive) stringResource(R.string.enable_parking_action) else stringResource(R.string.disable_parking_action),
                    onClick = {
                        if (isInactive) onEnable() else onDisable()
                        onDismiss()
                    }
                )
                HorizontalDivider(color = Color(0xFFE7EAF0))

                SheetAction(
                    title = stringResource(R.string.cancel),
                    isDestructive = true,
                    onClick = onDismiss
                )

                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        }
    }
}

@Composable
private fun SheetAction(
    title: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(contentColor = if (isDestructive) Color(0xFFDC2626) else Color.Unspecified),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isDestructive) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
            fontFamily = dmSans
        )
    }
}

private fun String.normalize(): String =
    lowercase()
        .replace("á","a").replace("é","e").replace("í","i").replace("ó","o").replace("ú","u")
        .replace("ä","a").replace("ë","e").replace("ï","i").replace("ö","o").replace("ü","u")
        .replace("ñ","n")

@DrawableRes
private fun featureIconFor(raw: String): Int {
    val s = raw.normalize()
    return when {
        "techad" in s                      -> R.drawable.techado
        "guardia" in s || "seguridad" in s -> R.drawable.guardia
        "discap" in s || "diversidad" in s || "embaraz" in s -> R.drawable.discapacitados
        "subter" in s                      -> R.drawable.subterraneo
        "premisa" in s                     -> R.drawable.premisa
        "aire libre" in s                  -> R.drawable.aire_libre
        "smart" in s                       -> R.drawable.smart
        "carga" in s || "elec" in s        -> R.drawable.electeric
        "pesad" in s                       -> R.drawable.carga      // si tienes otro de camión, cámbialo aquí
        "liger" in s || "moto" in s || "bicic" in s -> R.drawable.ligero
        else                               -> R.drawable.p_icon      // fallback
    }
}

@Composable
private fun CaracteristicasSeccion(characteristics: List<String>) {
    val list = remember(characteristics) {
        characteristics.map { it.trim() }.filter { it.isNotEmpty() }
    }

    if (list.isEmpty()) {
        Text(
            text = stringResource(R.string.features_not_specified),
            style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
            fontFamily = dmSans
        )
        Spacer(Modifier.height(18.dp))
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        list.forEach { label ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Usa TUS drawables
                Icon(
                    painter = painterResource(id = featureIconFor(label)),
                    contentDescription = null,
                    // Si tus assets ya son a color, quita el tint:
                    // tint = Color.Unspecified
                    tint = Color.Unspecified
                )
                Spacer(Modifier.width(8.dp))
                Text(label, fontFamily = dmSans)
            }
        }
    }

    Spacer(Modifier.height(18.dp))
}

@Composable
private fun ErrorState(message: String, onClose: () -> Unit, onRetry: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(stringResource(R.string.problem_occurred_title), style = MaterialTheme.typography.headlineSmall, fontFamily = dmSans)
        Spacer(Modifier.height(8.dp))
        Text(message, color = Color.Gray, fontFamily = dmSans)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onRetry) { Text(stringResource(R.string.retry_action)) }
            TextButton(onClick = onClose) { Text(stringResource(R.string.close_action)) }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewParqueoDetalle() {
    ParqueoDetalleScreen(
        navController = rememberNavController(),
        onClose = {},
        parkingId = "demo-id"
    )
}