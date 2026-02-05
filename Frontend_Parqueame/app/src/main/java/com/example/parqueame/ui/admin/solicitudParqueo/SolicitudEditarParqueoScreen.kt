package com.example.parqueame.ui.admin.solicitudParqueo

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.parqueame.R
import com.example.parqueame.data.CloudUploader
import com.example.parqueame.models.CreateParkingLotRequestDto
import com.example.parqueame.models.ScheduleRange
import com.example.parqueame.models.WeekDay
import com.example.parqueame.repository.ParkingRepository
import com.example.parqueame.session.SessionStore
import com.example.parqueame.ui.admin.DaysOTWeek
import com.example.parqueame.ui.common_components.GradientButton
import com.example.parqueame.ui.common_components.LabelledTextField
import com.example.parqueame.ui.common_components.rememberTopErrorBanner
import kotlinx.coroutines.launch
import java.util.Locale
import androidx.core.net.toUri
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.parqueame.ui.navigation.Screen
import com.example.parqueame.ui.theme.dmSans
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolicitudEditarParqueoScreen(
    navController: NavController,
    onSubmitClick: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val mostrarError = rememberTopErrorBanner()

    val repository = remember { ParkingRepository() }
    val parkingId: String? = remember { navController.currentBackStackEntry?.arguments?.getString("parkingId") }

    val session = remember { SessionStore(ctx) }
    val sessUserId by session.userId.collectAsState(initial = null)
    val sessDoc by session.userDoc.collectAsState(initial = null)
    val sessTipo by session.userTipo.collectAsState(initial = null)

    /* ===== Estados ===== */
    var localName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("") }
    var priceHour by remember { mutableStateOf("") }
    var characteristics by remember { mutableStateOf(setOf<String>()) }
    var isLoading by remember { mutableStateOf(false) }

    // Días
    val selectedDays = remember { mutableStateListOf<DaysOTWeek>() }
    var showDaysDialog by remember { mutableStateOf(false) }

    // Horarios
    val hours: List<String> = (0..23).map { String.format(Locale.US, "%02d:00", it) }
    val horarios = remember { mutableStateListOf<Pair<String, String>>() }
    if (horarios.isEmpty()) horarios += (hours.first() to hours.last())

    // Fotos
    val photos = remember { mutableStateListOf<Uri>() }
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { picked: List<Uri>? ->
        if (picked != null) {
            val remaining = (8 - photos.size).coerceAtLeast(0)
            photos.addAll(picked.take(remaining))
        }
    }

    // Documento de infraestructura
    var infraDoc: Uri? by remember { mutableStateOf(null) }
    val docPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> infraDoc = uri }

    val characteristicsOptions = stringArrayResource(R.array.parking_features_list)

    /* ========= Precarga (GET) ========= */
    LaunchedEffect(parkingId) {
        if (parkingId.isNullOrBlank()) return@LaunchedEffect

        isLoading = true
        repository.getParkingByIdDto(parkingId)
            .onSuccess { dto ->
                localName = dto.localName
                address = dto.address
                capacity = dto.capacity.toString()
                priceHour = dto.priceHour.toString()

                selectedDays.clear()
                selectedDays.addAll(dto.daysOfWeek.mapNotNull { it.toUiDaySafe() })

                horarios.clear()
                horarios.addAll(
                    dto.schedules.map { it.open to it.close }.ifEmpty {
                        listOf("00:00" to "23:00")
                    }
                )

                characteristics = dto.characteristics.toSet()

                photos.clear()
                photos.addAll(dto.photos.map { it.toUri() })
                infraDoc = dto.infraDocUrl?.toUri()

                isLoading = false
            }
            .onFailure { e ->
                isLoading = false
                mostrarError(e.message ?: ctx.getString(R.string.could_not_load_data))
            }
    }

    /* ========= UI ========= */
    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            // Header
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.close_action),
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    stringResource(R.string.edit_parking_lot_title),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    fontFamily = dmSans,
                    style = MaterialTheme.typography.labelLarge.copy(color = Color(0xFF003099))
                )
            }

            Spacer(Modifier.height(16.dp))
            LabelledTextField(stringResource(R.string.local_name), localName, { localName = it }, stringResource(R.string.local_name), KeyboardType.Text)
            Spacer(Modifier.height(16.dp))
            LabelledTextField(stringResource(R.string.address), address, { address = it }, stringResource(R.string.address), KeyboardType.Text)
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    LabelledTextField(
                        label = stringResource(R.string.capacity),
                        value = capacity,
                        onValueChange = { capacity = it },
                        placeholder = stringResource(R.string.number_of_spots_placeholder),
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Column(Modifier.weight(1f)) {
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

            Spacer(Modifier.height(16.dp))

            /* ======= Horario ======= */
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.schedule_label),
                    fontWeight = FontWeight.Normal,
                    color = Color.Black,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 12.dp, bottom = 10.dp),
                    fontFamily = dmSans
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { horarios.add(hours.first() to hours.last()) }
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = Color(0xFF2563EB)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.add_new_schedule),
                        color = Color(0xFF2563EB),
                        fontWeight = FontWeight.Medium,
                        fontFamily = dmSans
                    )
                }
            }

            if (selectedDays.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                val dayNames = prettyDayList(selectedDays.sortedBy { it.order })
                Text(
                    text = dayNames.joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 14.dp),
                    fontFamily = dmSans
                )
            }
            Spacer(Modifier.height(8.dp))

            horarios.forEachIndexed { index, _ ->
                key(index) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (index > 0) {
                            Text(
                                text = stringResource(R.string.delete_action_label),
                                color = Color(0xFFDC2626),
                                fontWeight = FontWeight.Medium,
                                fontFamily = dmSans,
                                modifier = Modifier
                                    .padding(start = 14.dp, top = 8.dp, bottom = 6.dp)
                                    .clickable { horarios.removeAt(index) }
                            )
                        } else {
                            Spacer(Modifier.height(0.dp))
                        }

                        Text(
                            stringResource(R.string.select_days),
                            color = Color(0xFF2563EB),
                            fontWeight = FontWeight.Medium,
                            fontFamily = dmSans,
                            modifier = Modifier
                                .padding(end = 14.dp, top = 8.dp)
                                .clickable { showDaysDialog = true }
                        )
                    }
                    Spacer(Modifier.height(4.dp))

                    Row(
                        Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Inicio
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.start_time), modifier = Modifier.padding(start = 14.dp), fontFamily = dmSans,)
                            var expandedOpen by remember(index) { mutableStateOf(false) }
                            val valueOpen = horarios[index].first
                            Box {
                                TextField(
                                    value = valueOpen,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = Color(0xFF2563EB)
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .clickable { expandedOpen = true },
                                    shape = RoundedCornerShape(18.dp),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFFF5F5F5),
                                        unfocusedContainerColor = Color(0xFFF5F5F5),
                                        disabledContainerColor = Color(0xFFF5F5F5),
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        disabledIndicatorColor = Color.Transparent
                                    )
                                )
                                DropdownMenu(expanded = expandedOpen, onDismissRequest = { expandedOpen = false }) {
                                    hours.forEach { h ->
                                        DropdownMenuItem(
                                            text = { Text(h) },
                                            onClick = {
                                                horarios[index] = h to horarios[index].second
                                                expandedOpen = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        // Fin
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.end_time), modifier = Modifier.padding(start = 14.dp), fontFamily = dmSans,)
                            var expandedClose by remember(index) { mutableStateOf(false) }
                            val valueClose = horarios[index].second
                            Box {
                                TextField(
                                    value = valueClose,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = Color(0xFF2563EB)
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .clickable { expandedClose = true },
                                    shape = RoundedCornerShape(18.dp),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFFF5F5F5),
                                        unfocusedContainerColor = Color(0xFFF5F5F5),
                                        disabledContainerColor = Color(0xFFF5F5F5),
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        disabledIndicatorColor = Color.Transparent
                                    )
                                )
                                DropdownMenu(expanded = expandedClose, onDismissRequest = { expandedClose = false }) {
                                    hours.forEach { h ->
                                        DropdownMenuItem(
                                            text = { Text(h) },
                                            onClick = {
                                                horarios[index] = horarios[index].first to h
                                                expandedClose = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            // Fotos
            Text(
                text = stringResource(R.string.photos),
                fontWeight = FontWeight.Normal,
                fontFamily = dmSans,
                color = Color.Black,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 12.dp, bottom = 10.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(photos) { uri: Uri ->
                    Box {
                        Image(
                            painter = rememberAsyncImagePainter(model = uri),
                            contentDescription = stringResource(R.string.photo_cd),
                            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { photos.remove(uri) },
                            modifier = Modifier
                                .size(20.dp)
                                .align(Alignment.TopEnd)
                                .background(Color.Red, CircleShape)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.delete_photo_cd), tint = Color.White, modifier = Modifier.size(12.dp))
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
                                    Toast.makeText(ctx, R.string.max_8_photos_toast, Toast.LENGTH_SHORT).show()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_photo_cd), tint = Color(0xFF2563EB))
                    }
                }
            }
            Text(
                stringResource(R.string.photos_count_format, photos.size),
                fontFamily = dmSans,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF6B7280),
                modifier = Modifier.padding(top = 4.dp, start = 12.dp)
            )

            Spacer(Modifier.height(16.dp))

            // Características
            Text(
                text = stringResource(R.string.features),
                fontWeight = FontWeight.Normal,
                fontFamily = dmSans,
                color = Color.Black,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 12.dp, bottom = 10.dp)
            )
            characteristicsOptions.forEach { option ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = characteristics.contains(option),
                        onCheckedChange = { checked ->
                            characteristics = if (checked) characteristics + option else characteristics - option
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF2563EB),
                            uncheckedColor = Color.Gray,
                            checkmarkColor = Color.White
                        )
                    )
                    Text(option, modifier = Modifier.padding(start = 4.dp), fontFamily = dmSans)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Documento de Infraestructura
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.infrastructure_document),
                    fontWeight = FontWeight.Normal,
                    fontFamily = dmSans,
                    color = Color.Black,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 12.dp)
                )
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = stringResource(R.string.info_cd),
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
            Text(
                stringResource(R.string.upload_document_description),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = dmSans,
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
                        fontFamily = dmSans,
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
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.upload_cd), tint = Color(0xFF2563EB))
                }
            }

            Spacer(Modifier.height(20.dp))

            // Guardar
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                GradientButton(
                    text = if (isLoading) stringResource(R.string.saving_state) else stringResource(R.string.request_edit_parking_action),
                    onClick = {
                        scope.launch {
                            try {
                                if (localName.isBlank() || address.isBlank() || capacity.isBlank() || priceHour.isBlank()) {
                                    mostrarError(ctx.getString(R.string.all_fields_required_error))
                                    return@launch
                                }
                                val cap = capacity.toIntOrNull() ?: run { mostrarError(ctx.getString(R.string.invalid_capacity_error)); return@launch }
                                val price = priceHour.toIntOrNull() ?: run { mostrarError(ctx.getString(R.string.invalid_price_error)); return@launch }
                                val pid = parkingId ?: run { mostrarError(ctx.getString(R.string.missing_parking_id_error)); return@launch }
                                if (selectedDays.isEmpty()) {
                                    mostrarError(ctx.getString(R.string.select_at_least_one_day_error))
                                    return@launch
                                }
                                if (horarios.isEmpty()) {
                                    mostrarError(ctx.getString(R.string.add_at_least_one_schedule_error))
                                    return@launch
                                }
                                val xUserIdHeader = sessUserId?.takeIf { it.isNotBlank() }
                                val ownerDocHeader = sessDoc?.takeIf { it.isNotBlank() && xUserIdHeader == null }
                                val ownerTipoHeader = sessTipo?.takeIf { it.isNotBlank() && xUserIdHeader == null }
                                if (xUserIdHeader == null && (ownerDocHeader == null || ownerTipoHeader == null)) {
                                    mostrarError(ctx.getString(R.string.missing_owner_identity_error))
                                    return@launch
                                }

                                isLoading = true

                                val photoUrls = mutableListOf<String>()
                                for (u in photos) {
                                    if (u.isRemoteUrl()) photoUrls.add(u.toString())
                                    else photoUrls.add(CloudUploader.uploadImage(ctx, u))
                                }
                                val infraUrl: String? = infraDoc?.let { uri ->
                                    if (uri.isRemoteUrl()) uri.toString()
                                    else CloudUploader.uploadDocument(ctx, uri)
                                }

                                val daysBackend: List<WeekDay> = selectedDays.map { it.toBackendWeekDay() }
                                val schedulesBackend: List<ScheduleRange> = horarios.map { (o, c) -> ScheduleRange(open = o, close = c) }
                                val body = CreateParkingLotRequestDto(
                                    localName = localName.trim(),
                                    address = address.trim(),
                                    capacity = cap,
                                    priceHour = price,
                                    daysOfWeek = daysBackend,
                                    schedules = schedulesBackend,
                                    characteristics = characteristics.toList(),
                                    photos = photoUrls,
                                    infraDocUrl = infraUrl,
                                    lat = null,
                                    lng = null,
                                    status = "pending"
                                )

                                repository.updateParking(
                                    id = pid,
                                    body = body,
                                    xUserId = xUserIdHeader,
                                    ownerDocumento = ownerDocHeader,
                                    ownerTipo = ownerTipoHeader
                                ).onSuccess {
                                    isLoading = false
                                    onSubmitClick()
                                    navController.navigate(Screen.SolicitudCreadaScreen.route)
                                }.onFailure { e ->
                                    isLoading = false
                                    mostrarError(e.message ?: ctx.getString(R.string.could_not_update_parking_error))
                                }
                            } catch (e: Exception) {
                                isLoading = false
                                mostrarError(e.message ?: ctx.getString(R.string.unexpected_error))
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    enabled = !isLoading
                )
            }

            Spacer(Modifier.height(20.dp))
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

/* ================= Selector de días ================= */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DaysPickerSheet(
    preselected: List<DaysOTWeek>,
    onDismiss: () -> Unit,
    onApply: (List<DaysOTWeek>) -> Unit
) {
    val current = remember(preselected) {
        mutableStateListOf<DaysOTWeek>().apply { addAll(preselected) }
    }
    val allDays = remember { DaysOTWeek::class.java.enumConstants?.sortedBy { it.order }.orEmpty() }
    val hasSelection = current.isNotEmpty()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        dragHandle = {}
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(stringResource(R.string.select_weekdays_title), style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.SemiBold))
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.select_availability_days_prompt), style = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFF6B7280)))
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFE7EAF0))
            Spacer(Modifier.height(4.dp))

            val dayNames = stringArrayResource(R.array.weekdays_full)
            allDays.forEachIndexed { index, day ->
                val checked = current.contains(day)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { if (checked) current.remove(day) else current.add(day) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { isChecked -> if (isChecked) current.add(day) else current.remove(day) }
                    )
                    Text(dayNames.getOrElse(index) { day.short }, style = MaterialTheme.typography.titleMedium)
                }
            }

            if (!hasSelection) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.must_select_one_day_error),
                    color = Color(0xFFDC2626),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFE7EAF0))
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.cancel), color = Color(0xFF6C63FF)) }
                Button(
                    onClick = { onApply(current.toList()) },
                    enabled = hasSelection,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB),
                        disabledContainerColor = Color(0xFFE5E7EB),
                        disabledContentColor = Color(0xFF9CA3AF)
                    )
                ) {
                    Text(stringResource(R.string.confirm))
                }
            }
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun prettyDayList(days: List<DaysOTWeek>): List<String> {
    val dayNames = stringArrayResource(R.array.weekdays_full)
    return days.map { day -> dayNames.getOrElse(day.order - 1) { day.short } }
}

private fun WeekDay.toUiDaySafe(): DaysOTWeek? {
    val targetShort = when (this) {
        WeekDay.MON -> "lun"
        WeekDay.TUE -> "mar"
        WeekDay.WED -> "mié"
        WeekDay.THU -> "jue"
        WeekDay.FRI -> "vie"
        WeekDay.SAT -> "sáb"
        WeekDay.SUN -> "dom"
    }
    return runCatching { DaysOTWeek::class.java.enumConstants?.firstOrNull { it.short.equals(targetShort, ignoreCase = true) } }.getOrNull()
}

private fun DaysOTWeek.toBackendWeekDay(): WeekDay = when (this) {
    DaysOTWeek.MON -> WeekDay.MON
    DaysOTWeek.TUE -> WeekDay.TUE
    DaysOTWeek.WED -> WeekDay.WED
    DaysOTWeek.THU -> WeekDay.THU
    DaysOTWeek.FRI -> WeekDay.FRI
    DaysOTWeek.SAT -> WeekDay.SAT
    DaysOTWeek.SUN -> WeekDay.SUN
}

private fun Uri.isRemoteUrl(): Boolean {
    val s = this.scheme?.lowercase()
    return s == "http" || s == "https"
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewSolicitudEditarParqueoScreen() {
    val navController = rememberNavController()

    // Llamamos a la pantalla real pasando lambdas vacías
    SolicitudEditarParqueoScreen(
        navController = navController,
        onSubmitClick = {}
    )
}