package com.example.parqueame.ui.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.parqueame.R
import com.example.parqueame.ui.common_components.BottomNavBar
import com.example.parqueame.ui.common_components.BottomNavItem
import com.example.parqueame.ui.common_components.GradientIcon
import com.example.parqueame.ui.navigation.BottomNavDestination
import com.example.parqueame.ui.navigation.Screen
import com.example.parqueame.ui.theme.GradientBrush
import com.example.parqueame.ui.theme.RtlRomman
import com.example.parqueame.ui.theme.dmSans

@Composable
fun AdminParqueosScreen(
    navController: NavController,
    userId: String,
    userDocumento: String? = null,
    userTipoDocumento: String? = null,
    vm: AdminParqueosViewModel = viewModel()
) {
    var currentRoute by remember { mutableStateOf(Screen.AdminParqueosScreen.route) }
    val navItems = BottomNavDestination.entries.toList()

    val items by vm.items.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()

    LaunchedEffect(userId, userDocumento, userTipoDocumento) {
        vm.loadForUser(userId, userDocumento, userTipoDocumento)
    }

    Scaffold(
        bottomBar = {
            BottomNavBar(
                items = navItems.map { BottomNavItem(it.route, it.icon, it.label) },
                currentRoute = currentRoute,
                onItemClick = { route ->
                    navController.navigate(route) { launchSingleTop = true }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(innerPadding)
        ) {
            // Header
            Column (modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.parking_lots_title),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = RtlRomman,
                        ),
                        modifier = Modifier
                            .graphicsLayer(alpha = 0.99f)
                            .drawWithContent {
                                drawContent()
                                drawRect(brush = GradientBrush, blendMode = BlendMode.SrcIn)
                            },
                        color = Color.Unspecified
                    )

                    // Acciones (solo Refresh y Add)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                vm.refresh(userId, userDocumento, userTipoDocumento)
                                Toast.makeText(
                                    navController.context,
                                    navController.context.getString(R.string.refreshing_parkings_toast),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.cd_refresh),
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                val route =
                                    "${Screen.SolicitudParqueoScreen.route}?userId=$userId" +
                                            if (userDocumento != null) "&documento=$userDocumento" else "" +
                                                    if (userTipoDocumento != null) "&tipoDocumento=$userTipoDocumento" else ""
                                navController.navigate(route) { launchSingleTop = true }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.cd_new_parking),
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                // Estados
                when {
                    loading -> LoaderSection()
                    error != null -> ErrorSection(error ?: "Error") {
                        vm.refresh(userId, userDocumento, userTipoDocumento)
                    }

                    else -> {
                        val misParqueos = remember(items) {
                            items.filter {
                                it.status.equals(
                                    "approved",
                                    true
                                ) || it.status.equals("inactive", true)
                            }
                        }
                        val solicitudes = remember(items) {
                            items.filterNot {
                                it.status.equals(
                                    "approved",
                                    true
                                ) || it.status.equals("inactive", true)
                            }
                        }

                        Text(
                            stringResource(R.string.my_parkings),
                            modifier = Modifier.padding(horizontal = 16.dp),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = Color(0xFF003099),
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = dmSans
                            )
                        )
                        Spacer(Modifier.height(10.dp))

                        if (misParqueos.isEmpty()) {
                            EmptyLight(stringResource(R.string.no_approved_or_disabled))
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(misParqueos, key = { it.id }) { p ->
                                    if (p.status.equals("inactive", true)) {
                                        InactiveCard(
                                            name = p.localName,
                                            address = p.address,
                                            onClick = { navController.navigate("parqueo_detalle/${p.id}") }
                                        )
                                    } else {
                                        PCard(
                                            name = p.localName,
                                            address = p.address,
                                            capacity = p.capacity,
                                            status = p.status,
                                            onClick = { navController.navigate("parqueo_detalle/${p.id}") }
                                        )
                                    }
                                }
                                item { Spacer(Modifier.height(12.dp)) }
                            }
                        }

                        Text(
                            stringResource(R.string.my_requests_header),
                            modifier = Modifier.padding(horizontal = 16.dp),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = Color(0xFF003099),
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = dmSans
                            )
                        )
                        Spacer(Modifier.height(10.dp))

                        if (solicitudes.isEmpty()) {
                            EmptyLight(stringResource(R.string.no_pending_or_rejected))
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(solicitudes, key = { it.id }) { p ->
                                    SolicitudCard(
                                        name = p.localName,
                                        address = p.address,
                                        status = p.status ?: "pending",
                                        onClick = {
                                            navController.navigate("ver_solicitud_parqueo/${p.id}")
                                        }
                                    )
                                }
                                item { Spacer(Modifier.height(20.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun LoaderSection() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorSection(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                message,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFDC2626)
            )
            FilledTonalButton(onClick = onRetry, modifier = Modifier.padding(top = 8.dp)) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}

@Composable
private fun EmptyLight(text: String) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF6B7280)
        )
    }
}

/* -------------------- Cards -------------------- */

@Composable
fun SolicitudCard(
    name: String,
    address: String,
    status: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontFamily = dmSans
            )
            Spacer(Modifier.height(4.dp))
            Text(
                address,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontFamily = dmSans
            )
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val statusColor = when (status.lowercase()) {
                    "approved" -> Color(0xFF10B981)
                    "rejected" -> Color(0xFFDC2626)
                    else -> Color(0xFFF59E0B)
                }
                val statusText = when (status.lowercase()) {
                    "approved" -> stringResource(R.string.status_approved)
                    "rejected" -> stringResource(R.string.status_rejected)
                    else -> stringResource(R.string.status_pending)
                }

                Text(
                    buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                color = Color.Black,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = dmSans
                            )
                        ) { append(stringResource(R.string.request_status_prefix)) }
                        append(" ")
                        withStyle(
                            style = SpanStyle(
                                color = statusColor,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = dmSans
                            )
                        ) { append(statusText) }
                    },
                    style = MaterialTheme.typography.titleSmall
                )
                IconButton(onClick = onClick) {
                    GradientIcon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(25.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun InactiveCard(
    name: String,
    address: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontFamily = dmSans
            )
            Spacer(Modifier.height(4.dp))
            Text(
                address,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontFamily = dmSans
            )
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                color = Color.Black,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = dmSans
                            )
                        ) { append(stringResource(R.string.parking_status_prefix)) }
                        withStyle(
                            style = SpanStyle(
                                color = Color(0xFF6B7280),
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = dmSans
                            )
                        ) { append(stringResource(R.string.parking_disabled)) }
                    },
                    style = MaterialTheme.typography.titleSmall
                )
                IconButton(onClick = onClick) {
                    GradientIcon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(25.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PCard(
    name: String,
    address: String,
    capacity: Int?,
    status: String? = null,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = dmSans
                )
                Spacer(Modifier.width(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "4.5", style = MaterialTheme.typography.bodyMedium, fontFamily = dmSans)
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFC107),
                        modifier = Modifier
                            .size(18.dp)
                            .padding(start = 2.dp)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                address,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontFamily = dmSans
            )

            Spacer(Modifier.height(8.dp))

            if (status?.equals("approved", ignoreCase = true) == true) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val total = capacity ?: 0
                    val statusText = buildAnnotatedString {
                        append(stringResource(R.string.available_spots_prefix_card))
                        append(" ")
                        withStyle(style = SpanStyle(color = Color.Black)) { append(total.toString()) }
                        withStyle(
                            style = SpanStyle(
                                color = Color(0xFF115ED0),
                                fontWeight = FontWeight.SemiBold
                            )
                        ) { append(" / $total") }
                    }
                    Text(text = statusText, style = MaterialTheme.typography.titleSmall, fontFamily = dmSans)
                    IconButton(onClick = onClick) {
                        GradientIcon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(25.dp)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onClick) {
                        GradientIcon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(25.dp)
                        )
                    }
                }
            }
        }
    }
}

/* -------------------- Preview -------------------- */

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AdminParqueosScreenPreview() {
    AdminParqueosScreen(
        navController = rememberNavController(),
        userId = "12345",
        userDocumento = "00123456789",
        userTipoDocumento = "CEDULA",
        vm = viewModel()
    )
}