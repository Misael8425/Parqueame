package com.example.parqueame.ui.admin.solicitudParqueo

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.parqueame.api.RetrofitInstance
import com.example.parqueame.models.ParkingCommentDto
import com.example.parqueame.models.ParkingLotDto
import com.example.parqueame.ui.common_components.LabelledTextField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import androidx.core.net.toUri // para toUri()
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.CheckboxDefaults
import com.example.parqueame.R


/**
 * ViewModel simple y autocontenido para cargar detalle + comentarios.
 */
class VerSolicitudViewModel : ViewModel() {

    var detalle by mutableStateOf<ParkingLotDto?>(null)
        private set

    var comentarios by mutableStateOf<List<ParkingCommentDto>>(emptyList())
        private set

    var errorMsg by mutableStateOf<String?>(null)
        private set

    var loading by mutableStateOf(false)
        private set

    suspend fun cargar(parkingId: String) {
        loading = true
        errorMsg = null
        try {
            // 1) Detalle
            val det = withContext(Dispatchers.IO) {
                RetrofitInstance.apiService.obtenerParqueoId(parkingId)
            }
            if (det.isSuccessful) {
                detalle = det.body()
            } else {
                errorMsg = "Error detalle: ${det.code()}"
            }

            // 2) Comentarios
            val com = withContext(Dispatchers.IO) {
                RetrofitInstance.apiService.obtenerComentarios(parkingId)
            }
            if (com.isSuccessful) {
                comentarios = com.body().orEmpty()
            }
        } catch (e: Exception) {
            errorMsg = e.message ?: "Error inesperado"
        } finally {
            loading = false
        }
    }

    fun latestRejectionReason(): String? =
        comentarios.filter { it.type == "rejection" }
            .maxByOrNull { it.createdAt }?.text
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerSolicitudScreen(
    navController: NavController,
    parkingIdArg: String? = null,
    vm: VerSolicitudViewModel = viewModel()
) {
    // Estados UI (rellenados desde backend)
    var localName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("") }
    var priceHour by remember { mutableStateOf("") }
    var openingHour by remember { mutableStateOf("") }
    var closingHour by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var characteristics by remember { mutableStateOf(setOf<String>()) }
    val photos = remember { mutableStateListOf<Uri>() }
    var infraDoc: Uri? by remember { mutableStateOf(null) }

    val hours = (0..23).map { String.format(Locale.US, "%02d:00", it) }

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
    val characteristicsNormalized = remember(characteristics) {
        characteristics.map { it.trim().lowercase(Locale.ROOT) }.toSet()
    }

// Aliases por cada resId (ES / EN) para comparar con lo que envía el backend
    val backendAliasesByResId = mapOf(
        R.string.feature_covered_parking to listOf(
            "Parqueos techados", "Covered parking"
        ),
        R.string.feature_security_guard to listOf(
            "Guardia de seguridad", "Security guard"
        ),
        R.string.feature_accessible_parking to listOf(
            "Parqueos para personas con diversidad funcional y embarazadas",
            "Accessible parking (people with disabilities & pregnant)"
        ),
        R.string.feature_underground_parking to listOf(
            "Parqueos subterráneos", "Underground parking"
        ),
        R.string.feature_on_premise_parking to listOf(
            "Parqueos en premisa", "On-premise parking"
        ),
        R.string.feature_open_air_parking to listOf(
            "Parqueos al aire libre", "Open-air parking"
        ),
        R.string.feature_smart_parking to listOf(
            "Smartparking", "Smart parking"
        ),
        R.string.feature_ev_charging to listOf(
            "Parqueos con estaciones de carga para vehículos eléctricos",
            "EV charging stations"
        ),
        R.string.feature_heavy_vehicles to listOf(
            "Parqueos para vehículos pesados", "Heavy vehicles parking"
        ),
        R.string.feature_light_vehicles to listOf(
            "Parqueos para vehículos ligeros (motocicletas, bicicletas, etc.)",
            "Light vehicles parking (motorcycles, bicycles, etc.)"
        )
    )

    val horarios = remember { mutableStateListOf<Pair<String, String>>() }
    if (horarios.isEmpty()) {
        horarios.add(hours.first() to hours.last())
    }

    // parkingId desde args si aplica
    val parkingIdFromArgs: String? =
        navController.currentBackStackEntry?.arguments?.getString("parkingId")
    val parkingId = parkingIdArg ?: parkingIdFromArgs

    LaunchedEffect(parkingId) {
        val id = parkingId
        if (!id.isNullOrBlank()) {
            vm.cargar(id)

            vm.detalle?.let { dto ->
                localName = dto.localName
                address = dto.address
                capacity = dto.capacity.toString()
                priceHour = dto.priceHour.toString()

                val firstRange = dto.schedules.firstOrNull()
                openingHour = firstRange?.open ?: ""
                closingHour = firstRange?.close ?: ""

                photos.clear()
                photos.addAll(dto.photos.mapNotNull { runCatching { it.toUri() }.getOrNull() })
                characteristics = dto.characteristics
                    ?.map { it.trim() }
                    ?.toSet()
                    ?: emptySet()

                infraDoc = dto.infraDocUrl?.toUri()

                // comentario: prioridad a rejectionReason
                comment = dto.rejectionReason
                    ?: vm.comentarios.filter { it.type == "note" }.maxByOrNull { it.createdAt }?.text
                            ?: ""
            }

            vm.latestRejectionReason()?.let { latest ->
                if (latest.isNotBlank()) comment = latest
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
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
                        contentDescription = stringResource(R.string.new_parking_lot_cd),
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    stringResource(R.string.request_create_parking_title),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.labelLarge.copy(color = Color(0xFF6B7280))
                )
            }
            Spacer(Modifier.height(10.dp))

            Spacer(modifier = Modifier.height(16.dp))

            Text(stringResource(R.string.local_name))
            BasicTextField(
                value = localName,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(18.dp))
                    .padding(18.dp)
            )
            Spacer(Modifier.height(16.dp))

            Text(stringResource(R.string.address))
            BasicTextField(
                value = address,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(18.dp))
                    .padding(18.dp)
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.opening_time_label))
                    BasicTextField(
                        value = openingHour,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(18.dp))
                            .padding(18.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.closing_time_label))
                    BasicTextField(
                        value = closingHour,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(18.dp))
                            .padding(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text(stringResource(R.string.photos) + ":", fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(photos) { uri: Uri ->
                    Image(
                        painter = rememberAsyncImagePainter(model = uri),
                        contentDescription = stringResource(R.string.photo_cd),
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            LabelledMultilineTextField(
                label = stringResource(R.string.comment_label),
                value = comment,
                onValueChange = {},
                placeholder = stringResource(R.string.comment_placeholder),
                maxLines = 5
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(stringResource(R.string.features), fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))

// Estilo de fila similar al de la izquierda: más aire vertical
            characteristicsOptions.forEach { resId ->
                val label = stringResource(resId) // lo mostrado se traduce por locale
                val aliases = backendAliasesByResId[resId] ?: listOf(label)
                val isChecked = aliases.any { alias ->
                    characteristicsNormalized.contains(alias.lowercase(Locale.ROOT))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = null,       // null => no interactivo
                        enabled = false,              // solo lectura
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF2563EB),
                            uncheckedColor = Color(0xFF9CA3AF),
                            checkmarkColor = Color.White,
                            disabledCheckedColor = Color(0xFF2563EB),
                            disabledUncheckedColor = Color(0xFF9CA3AF),
                            disabledIndeterminateColor = Color(0xFF2563EB)
                        )
                    )
                    Text(
                        label,
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(stringResource(R.string.infrastructure_document), fontWeight = FontWeight.Medium)
                Icon(Icons.Outlined.Info, contentDescription = stringResource(R.string.info_cd), tint = Color(0xFF2563EB))
            }
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
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF5F5F5))
                        .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = infraDoc?.lastPathSegment ?: stringResource(R.string.no_document_available),
                        color = Color(0xFF6B7280),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            val rawStatus = vm.detalle?.status.orEmpty()
            val (estadoTexto, estadoColor) = when (rawStatus.lowercase()) {
                "approved" -> stringResource(R.string.status_approved_label) to Color(0xFF10B981)
                "rejected" -> stringResource(R.string.status_rejected_label) to Color(0xFFDC2626)
                else       -> stringResource(R.string.status_pending_label)  to Color(0xFFF59E0B)
            }

            Text(
                buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            color = Color.Black,
                            fontWeight = FontWeight.SemiBold
                        )
                    ) { append(stringResource(R.string.request_status_prefix)) }
                    withStyle(
                        style = SpanStyle(
                            color = estadoColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    ) { append(estadoTexto) }
                },
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (vm.loading) {
                Text(stringResource(R.string.loading_ellipsis), color = Color(0xFF6B7280))
            }
            vm.errorMsg?.let { Text(stringResource(R.string.error_status_prefix, it), color = Color(0xFFDC2626)) }
        }
    }
}

@Composable
fun LabelledMultilineTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit = {},
    placeholder: String = "",
    modifier: Modifier = Modifier,
    minHeight: Dp = 120.dp,
    maxLines: Int = 5
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { if (placeholder.isNotEmpty()) Text(placeholder) },
            singleLine = false,
            maxLines = maxLines,
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .clickable(interactionSource = interactionSource, indication = null) {},
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF5F5F5),
                unfocusedContainerColor = Color(0xFFF5F5F5),
                disabledContainerColor = Color(0xFFF5F5F5),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            )
        )
    }
}
