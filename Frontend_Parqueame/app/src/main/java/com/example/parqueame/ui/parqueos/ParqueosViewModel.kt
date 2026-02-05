package com.example.parqueame.ui.parqueos

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.parqueame.api.RetrofitInstance
import com.example.parqueame.models.ParkingLotDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


// Estado de UI en el mismo archivo
sealed class UiState<out T> {
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

class ParqueosViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _parqueos = mutableStateListOf<ParkingLotDto>()
    val parqueos: List<ParkingLotDto> get() = _parqueos

    private val _state = MutableStateFlow<UiState<ParkingLotDto>>(UiState.Loading)
    val state: StateFlow<UiState<ParkingLotDto>> = _state

    init {
        val id = savedStateHandle.get<String>("parqueoId").orEmpty()
        if (id.isBlank()) {
            _state.value = UiState.Error("ID inválido")
        } else {
            load(id)
        }
    }

    fun reload() {
        val id = savedStateHandle.get<String>("parqueoId").orEmpty()
        if (id.isNotBlank()) load(id)
    }

    private fun load(id: String) = viewModelScope.launch {
        _state.value = UiState.Loading
        try {
            val resp = RetrofitInstance.apiService.obtenerParqueoId(id)
            _state.value = if (resp.isSuccessful) {
                resp.body()?.let { UiState.Success(it) } ?: UiState.Error("Respuesta vacía")
            } else {
                UiState.Error("HTTP ${resp.code()}: ${resp.errorBody()?.string().orEmpty()}")
            }
        } catch (e: Exception) {
            _state.value = UiState.Error("Red: ${e.localizedMessage}")
        }
    }

    // ---- Factory para inyectar SavedStateHandle ----
    companion object {
        fun provideFactory(
            savedStateHandle: SavedStateHandle
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ParqueosViewModel(savedStateHandle) as T
            }
        }
    }
}
