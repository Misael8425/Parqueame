package com.example.parqueame.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parqueame.api.RetrofitInstance
import com.example.parqueame.models.ParkingLotDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MapViewModel : ViewModel() {

    private val _parkingLots = MutableStateFlow<List<ParkingLotDto>>(emptyList())
    val parkingLots: StateFlow<List<ParkingLotDto>> = _parkingLots

    fun loadParkingLots() {
        // Evita recargar si ya tenemos datos
        if (_parkingLots.value.isNotEmpty()) return

        viewModelScope.launch {
            try {
                val resp = RetrofitInstance.apiService.obtenerParqueosAprobados()
                if (resp.isSuccessful) {
                    val data = resp.body().orEmpty()
                    _parkingLots.value = data
                    Log.d("MapViewModel", "Parqueos cargados: ${data.size}")
                } else {
                    Log.w("MapViewModel", "Error ${resp.code()} al obtener parqueos: ${resp.errorBody()?.string()}")
                    _parkingLots.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e("MapViewModel", "Fallo al cargar parqueos: ${e.localizedMessage}", e)
                _parkingLots.value = emptyList()
            }
        }
    }
}