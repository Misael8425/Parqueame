package com.example.parqueame.ui.admin.parqueosInfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parqueame.models.CreateCommentRequest
import com.example.parqueame.models.ParkingCommentDto
import com.example.parqueame.models.Parqueo
import com.example.parqueame.models.ScheduleRange
import com.example.parqueame.models.WeekDay
import com.example.parqueame.repository.ParkingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ParqueoDetalleUiState(
    val isLoading: Boolean = false,
    val data: Parqueo? = null,

    // Status ya mapeado a etiquetas de UI
    val status: String? = null,                      // "Activo" | "Pendiente" | "Rechazado" | "Deshabilitado"
    val characteristics: List<String> = emptyList(),
    val comments: List<ParkingCommentDto> = emptyList(),
    val rejectionReason: String? = null,

    // Guardamos crudos por si los necesitas después
    val daysOfWeek: List<WeekDay> = emptyList(),
    val schedules: List<ScheduleRange> = emptyList(),

    // ✅ Textos listos para pintar
    val daysLabel: String = "—",
    val scheduleLabels: List<String> = emptyList(),

    val error: String? = null
)

class ParqueoDetalleViewModel(
    private val repository: ParkingRepository = ParkingRepository()
) : ViewModel() {

    private val _ui = MutableStateFlow(ParqueoDetalleUiState(isLoading = true))
    val ui: StateFlow<ParqueoDetalleUiState> = _ui.asStateFlow()

    fun cargarParqueo(id: String) {
        _ui.value = _ui.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            runCatching {
                val dto   = repository.getParkingByIdDto(id).getOrThrow()
                val model = repository.getParkingById(id).getOrThrow()
                val cmts  = repository.getParkingComments(id).getOrDefault(emptyList())
                Triple(dto, model, cmts)
            }.fold(
                onSuccess = { (dto, model, cmts) ->
                    val mappedStatus = when (dto.status.lowercase()) {
                        "approved" -> "Activo"
                        "pending"  -> "Pendiente"
                        "rejected" -> "Rechazado"
                        "inactive" -> "Deshabilitado"
                        else       -> dto.status
                    }

                    val days = dto.daysOfWeek.orEmpty()
                    val schedules = dto.schedules.orEmpty()

                    val daysLabel = formatDaysLabel(days.map { it.toString() })
                    val scheduleLabels = schedules.map { formatRangeLabel(it.toString()) }

                    _ui.value = ParqueoDetalleUiState(
                        isLoading = false,
                        data = model,
                        status = mappedStatus,
                        characteristics = dto.characteristics.orEmpty(),
                        comments = cmts,
                        rejectionReason = dto.rejectionReason,

                        daysOfWeek = days,
                        schedules  = schedules,

                        daysLabel = daysLabel,
                        scheduleLabels = scheduleLabels
                    )
                },
                onFailure = { e ->
                    _ui.value = ParqueoDetalleUiState(
                        isLoading = false,
                        error = e.message ?: "Error desconocido"
                    )
                }
            )
        }
    }

    fun agregarComentario(
        id: String,
        texto: String,
        tipo: String = "note",
        authorId: String? = null,
        authorEmail: String? = null
    ) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isLoading = true, error = null)
            runCatching {
                repository.addParkingComment(
                    id = id,
                    request = CreateCommentRequest(
                        type = tipo,
                        text = texto,
                        authorId = authorId,
                        authorEmail = authorEmail
                    )
                ).getOrThrow()
            }.onSuccess { cargarParqueo(id) }
                .onFailure { e ->
                    _ui.value = _ui.value.copy(
                        isLoading = false,
                        error = e.message ?: "No se pudo agregar el comentario"
                    )
                }
        }
    }

    /** Deshabilita el parqueo -> backend status "inactive" */
    fun deshabilitarParqueo(
        id: String,
        motivo: String? = null,
        done: (Boolean) -> Unit = {}
    ) {
        _ui.value = _ui.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val result = runCatching {
                repository.setParkingStatus(
                    id = id,
                    status = "inactive",
                    reason = motivo
                ).getOrThrow()
                Unit
            }
            result.onSuccess {
                cargarParqueo(id)
                done(true)
            }.onFailure { e ->
                _ui.value = _ui.value.copy(isLoading = false, error = e.message ?: "No se pudo deshabilitar")
                done(false)
            }
        }
    }

    /** Habilita el parqueo -> backend status "approved" */
    fun habilitarParqueo(
        id: String,
        done: (Boolean) -> Unit = {}
    ) {
        _ui.value = _ui.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val result = runCatching {
                repository.setParkingStatus(
                    id = id,
                    status = "approved",   // el backend acepta pending/approved/rejected/inactive
                    reason = null
                ).getOrThrow()
                Unit
            }
            result.onSuccess {
                cargarParqueo(id)
                done(true)
            }.onFailure { e ->
                _ui.value = _ui.value.copy(isLoading = false, error = e.message ?: "No se pudo habilitar")
                done(false)
            }
        }
    }

    // =================== Formateadores ===================

    private fun formatDaysLabel(dayTokens: List<String>): String {
        if (dayTokens.isEmpty()) return "Días: —"
        val names = dayTokens.map { mapWeekDayToEs(it) }
        return "Días: " + names.joinToString(", ")
    }

    private fun formatRangeLabel(raw: String): String {
        val openCloseRegex = Regex("""open\s*=\s*([0-2]?\d:\d{2}).*?close\s*=\s*([0-2]?\d:\d{2})""", RegexOption.IGNORE_CASE)
        openCloseRegex.find(raw)?.let {
            val (o, c) = it.destructured
            return "${o.trim()} – ${c.trim()}"
        }
        val times = Regex("""([0-2]?\d:\d{2})""").findAll(raw).map { it.value }.toList()
        if (times.size >= 2) return "${times[0]} – ${times[1]}"
        return raw
    }

    private fun mapWeekDayToEs(token: String): String {
        return when (token.trim().lowercase()) {
            "mon" -> "Lunes"; "tue" -> "Martes"; "wed" -> "Miércoles"
            "thu" -> "Jueves"; "fri" -> "Viernes"; "sat" -> "Sábado"; "sun" -> "Domingo"
            "monday" -> "Lunes"; "tuesday" -> "Martes"; "wednesday" -> "Miércoles"
            "thursday" -> "Jueves"; "friday" -> "Viernes"; "saturday" -> "Sábado"; "sunday" -> "Domingo"
            "lunes" -> "Lunes"; "martes" -> "Martes"; "miércoles", "miercoles" -> "Miércoles"
            "jueves" -> "Jueves"; "viernes" -> "Viernes"; "sábado", "sabado" -> "Sábado"; "domingo" -> "Domingo"
            "1" -> "Lunes"; "2" -> "Martes"; "3" -> "Miércoles"; "4" -> "Jueves"
            "5" -> "Viernes"; "6" -> "Sábado"; "7" -> "Domingo"
            else -> token.replaceFirstChar { it.titlecase() }
        }
    }
}