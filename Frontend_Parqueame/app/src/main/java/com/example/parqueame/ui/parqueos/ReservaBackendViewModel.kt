package com.example.parqueame.ui.parqueos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.parqueame.models.CreateReservationRequest
import com.example.parqueame.models.ReservationDto
import com.example.parqueame.repository.ReservationRepository
import com.example.parqueame.session.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

data class ReservaBackendState(
    val loading: Boolean = false,
    val success: ReservationDto? = null,
    val error: String? = null
)

class ReservaBackendViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ReservationRepository()
    private val session = SessionStore(app.applicationContext)

    private val _state = MutableStateFlow(ReservaBackendState())
    val state: StateFlow<ReservaBackendState> = _state

    fun reset() { _state.value = ReservaBackendState() }

    fun confirmar(summary: ReservationSummaryUi) {
        viewModelScope.launch {
            _state.value = ReservaBackendState(loading = true)
            try {
                // 👇 leer el Flow una sola vez (en vez de .value)
                val userId = session.userId.firstOrNull().orEmpty()
                require(userId.isNotBlank()) { "No se encontró el usuario. Inicia sesión." }

                val zoneId = runCatching { ZoneId.of(summary.timezone) }.getOrElse { ZoneId.systemDefault() }
                val start = Instant.ofEpochSecond(summary.startEpochMinutes * 60).atZone(zoneId)
                val end   = Instant.ofEpochSecond(summary.endEpochMinutes * 60).atZone(zoneId)

                val body = CreateReservationRequest(
                    parkingId   = summary.parqueo.id,
                    startHour24 = start.hour,
                    startMin    = start.minute,
                    endHour24   = end.hour,
                    endMin      = end.minute,
                    localDate   = start.toLocalDate().toString(), // "YYYY-MM-DD"
                    timezone    = zoneId.id
                )

                // (Opcional) validar disponibilidad antes:
                // val avail = repo.checkAvailability(summary.parqueo.id, summary.startEpochMinutes, summary.endEpochMinutes).getOrThrow()
                // if (!avail.available) throw IllegalStateException(avail.message ?: "Capacidad completa")

                val created = repo.createReservation(userId, body).getOrThrow()
                _state.value = ReservaBackendState(success = created)
            } catch (e: Exception) {
                _state.value = ReservaBackendState(error = e.message ?: "Error creando la reserva")
            }
        }
    }
}