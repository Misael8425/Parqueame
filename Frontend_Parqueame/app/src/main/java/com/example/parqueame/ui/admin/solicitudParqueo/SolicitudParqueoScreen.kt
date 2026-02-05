package com.example.parqueame.ui.admin.solicitudParqueo

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.example.parqueame.R
import com.example.parqueame.ui.admin.DaysOTWeek
import com.example.parqueame.ui.common_components.GradientButton
import com.example.parqueame.ui.common_components.LabelledTextField
import com.example.parqueame.ui.common_components.TopErrorBanner
import com.example.parqueame.ui.common_components.rememberTopErrorBanner
import com.example.parqueame.ui.navigation.Screen
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolicitudParqueoScreen(
    navController: NavController,
    userId: String,                          // ID del usuario logueado
    userDocumento: String,                   // RNC o Cédula
    userTipoDocumento: String,               // "RNC" o "CEDULA"
    onSubmitClick: () -> Unit = {},
    viewModel: SolicitudParqueoViewModel = viewModel(),
    showUserInfoDebug: Boolean = false       // <-- evita depender de BuildConfig en este módulo
) {

    val context = LocalContext.current
    val showError = rememberTopErrorBanner()
    var showSheet by remember { mutableStateOf(false) }

    DocInfraestructuraSheet(
        showSheet = showSheet,
        onDismiss = { showSheet = false },
    )
    // --- Estados originales ---
    var localName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("") }
    var priceHour by remember { mutableStateOf("") }
    var characteristics by remember { mutableStateOf(setOf<String>()) }

    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val ctx = LocalContext.current

    // --- Días de semana ---
    val selectedDays = remember { mutableStateListOf<DaysOTWeek>() }
    var showDaysDialog by remember { mutableStateOf(false) }
    // ✅ PRESELECCIÓN MÍNIMA (para que el botón pueda habilitarse)
    if (selectedDays.isEmpty()) {
        selectedDays.addAll(DaysOTWeek.values())
    }

    val hours: List<String> = (0..23).map { String.format(Locale.US, "%02d:00", it) }
    val schedules = remember { mutableStateListOf<SolicitudParqueoState>() }
    if (schedules.isEmpty()) schedules += SolicitudParqueoState(start = hours.first(), end = hours.last())

    // --- Horas + lista de horarios ---
//    val horarios = remember { mutableStateListOf<Pair<String, String>>() }
//    if (horarios.isEmpty()) {
//        horarios.add(hours.first() to hours.last())
//    }

    // --- Fotos ---
    val photos = remember { mutableStateListOf<Uri>() }
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { picked: List<Uri>? ->
        if (picked != null) {
            val remaining = (8 - photos.size).coerceAtLeast(0)
            photos.addAll(picked.take(remaining))
        }
    }

    // --- Documento de infraestructura ---
    var infraDoc: Uri? by remember { mutableStateOf(null) }
    val docPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> infraDoc = uri }

    // --- Características ---
    val characteristicsOptions = listOf(
        R.string.feature_covered_parking,
        R.string.feature_security_guard,
        R.string.feature_accessible_parking,
        R.string.feature_underground_parking,
        R.string.feature_on_premise_parking,
        R.string.feature_open_air_parking,
        R.string.feature_smart_parking,
        R.string.feature_ev_charging,
        R.string.feature_heavy_vehicles,
        R.string.feature_light_vehicles
    )

    val autoVm: AutocompleteAddressViewModel =
        viewModel(
            factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory(
                LocalContext.current.applicationContext as android.app.Application
            )
        )


    // NUEVO: estados para coords resueltas por Autocomplete
    var lat by remember { mutableStateOf<Double?>(null) }
    var lng by remember { mutableStateOf<Double?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            // ======= Header =======
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close_action),
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.create_parking_title),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.labelLarge.copy(color = Color(0xFF003099))
                )
            }

            // Mostrar info del usuario (debug opcional)
            if (showUserInfoDebug) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE6F0FF))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Info del usuario:",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2563EB)
                        )
                        Text(
                            "ID: $userId",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF1E40AF)
                        )
                        Text(
                            "$userTipoDocumento: $userDocumento",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF1E40AF)
                        )
                    }
                }
            }

            // ======= Campos =======
            Spacer(modifier = Modifier.height(16.dp))
            LabelledTextField(stringResource(R.string.local_name), localName, { localName = it }, stringResource(R.string.local_name), KeyboardType.Text)
            Spacer(Modifier.height(16.dp))

            AddressAutocompleteField(
                label = stringResource(R.string.address),
                vm = autoVm,
                value = address,
                onValueChange = { address = it },
                onAddressResolved = { resolvedAddress, resolvedLat, resolvedLng ->
                    address = resolvedAddress
                    lat = resolvedLat
                    lng = resolvedLng
                },
                maxResults = 4
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    LabelledTextField(
                        label = stringResource(R.string.capacity),
                        value = capacity,
                        onValueChange = { capacity = it },
                        placeholder = stringResource(R.string.number_of_spots_placeholder),
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    LabelledTextField(
                        label = stringResource(R.string.price_per_hour),
                        value = priceHour,
                        onValueChange = { priceHour = it },
                        placeholder = stringResource(R.string.price_x_h_placeholder),
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            Text(
                                text = stringResource(R.string.currency_dop),
                                color = Color(0xFF2563EB),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            ScheduleSection(
                schedules = schedules,
                hours = hours,
                modifier = Modifier.fillMaxWidth()
            )

            // ======= Fotos =======
            Text(
                text = stringResource(R.string.photos),
                fontWeight = FontWeight.Normal,
                color = Color.Black,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 12.dp, bottom = 10.dp)
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                items(photos) { uri: Uri ->
                    Box {
                        Image(
                            painter = rememberAsyncImagePainter(model = uri),
                            contentDescription = stringResource(R.string.photo_cd),
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        // Botón para eliminar foto
                        IconButton(
                            onClick = { photos.remove(uri) },
                            modifier = Modifier
                                .size(20.dp)
                                .align(Alignment.TopEnd)
                                .background(Color.Red, CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.delete_action),                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
                item {
                    Box(
                        modifier = Modifier
                            .padding(start = 5.dp)
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF3F4F6))
                            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
                            .clickable {
                                if (photos.size < 8) {
                                    photoPicker.launch("image/*")
                                } else {
                                    Toast.makeText(
                                        ctx,
                                        ctx.getString(R.string.max_8_photos_toast),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_photo_cd), tint = Color(0xFF2563EB))
                    }
                }
            }
            Text(
                text = stringResource(R.string.photos_count_format, photos.size),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF6B7280),
                modifier = Modifier.padding(top = 4.dp, start = 12.dp)
            )

            Spacer(Modifier.height(16.dp))

            // ======= Características =======
            Text(
                text = stringResource(R.string.features),
                fontWeight = FontWeight.Normal,
                color = Color.Black,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 12.dp, bottom = 10.dp)
            )
            var characteristics by remember { mutableStateOf(setOf<String>()) }
            characteristicsOptions.forEach { id ->
                val optionText = stringResource(id)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = characteristics.contains(optionText),
                        onCheckedChange = { checked ->
                            characteristics = if (checked) characteristics + optionText else characteristics - optionText
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF2563EB),
                            uncheckedColor = Color.Gray,
                            checkmarkColor = Color.White
                        )
                    )
                    Text(optionText, modifier = Modifier.padding(start = 4.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

            // ======= Documento de Infraestructura =======
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.infrastructure_document),
                    fontWeight = FontWeight.Normal,
                    color = Color.Black,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 12.dp)
                )
                IconButton(onClick = { showSheet = true }) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = stringResource(R.string.info_cd),
                        tint = Color(0xFF2563EB)
                    )
                }
            }
            Text(
                text = stringResource(R.string.upload_document_description),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6B7280),
                modifier = Modifier.padding(start = 12.dp)
            )
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFFF5F5F5))
                        .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(18.dp))
                        .clickable { docPicker.launch("*/*") }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = infraDoc?.lastPathSegment ?: stringResource(R.string.upload_document_placeholder),
                        color = if (infraDoc != null) Color(0xFF374151) else Color(0xFF6B7280),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = { docPicker.launch("*/*") },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFFE6F0FF))
                        .border(1.dp, Color(0xFF2563EB), RoundedCornerShape(18.dp))
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.upload_cd), tint = Color(0xFF2563EB))
                }
            }

            Spacer(Modifier.height(20.dp))

            val isFormComplete: Boolean =
                localName.isNotBlank() &&
                        address.isNotBlank() &&
                        capacity.isNotBlank() &&
                        priceHour.isNotBlank() &&
                        selectedDays.isNotEmpty() &&
                        infraDoc != null

            // ======= Botón =======
            val coroutineScope = rememberCoroutineScope()
            val ctx = LocalContext.current
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                GradientButton(
                    text = if (isLoading) stringResource(R.string.saving_state)
                    else stringResource(R.string.register_parking_action),
                    onClick = {
//                        if (!isFormComplete) {
//                            showError(context.getString(R.string.login_user_type_unknown))
//                            return@GradientButton
//                        }

                        isLoading = true

                        // 2) Submit real
                        coroutineScope.launch {
                            try {
                                val daysForVm = selectedDays.map {
                                    com.example.parqueame.models.WeekDay.valueOf(it.name)
                                }

                                viewModel.submitSolicitud(
                                    context = ctx,
                                    userId = userId,
                                    localName = localName,
                                    address = address,
                                    capacity = capacity,
                                    priceHour = priceHour,
                                    days = daysForVm,
                                    horarios = schedules.map { it.start to it.end },
                                    characteristics = characteristics,
                                    photoUris = photos.toList(),
                                    infraDocUri = infraDoc,
                                    lat = lat,
                                    lng = lng,
                                    ownerDocumento = userDocumento,
                                    ownerTipo = userTipoDocumento,
                                    onSuccess = {
                                        isLoading = false
                                        onSubmitClick()
                                        navController.navigate(Screen.SolicitudCreadaScreen.route)
                                    },
                                    onError = { msg ->
                                        isLoading = false
                                        val finalMsg = msg.takeIf { it.isNotBlank() }
                                            ?: context.getString(R.string.request_creation_failed)
                                        showError(finalMsg)
                                    }
                                )
                            } catch (e: Exception) {
                                isLoading = false
                                showError(context.getString(R.string.request_creation_failed))
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    enabled = !isLoading
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showDaysDialog) {
        DaysOfWeekDialog(
            initial = selectedDays.toSet(),
            onDismiss = { showDaysDialog = false },
            onConfirm = { chosen ->
                selectedDays.clear()
                selectedDays.addAll(chosen)
                showDaysDialog = false
            }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SolicitudParqueoScreen() {
    SolicitudParqueoScreen(
        navController = rememberNavController(),
        userId = "demo-user-123",
        userDocumento = "402-1234567-8",
        userTipoDocumento = "CEDULA",
        onSubmitClick = { /* no-op en preview */ },
        showUserInfoDebug = true
    )
}
