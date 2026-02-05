// app/src/main/java/com/example/parqueame/ui/admin/AdminParqueosViewModel.kt
package com.example.parqueame.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parqueame.api.RetrofitInstance
import com.example.parqueame.models.ParkingLotDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AdminParqueosViewModel : ViewModel() {

    private val _items = MutableStateFlow<List<ParkingLotDto>>(emptyList())
    val items: StateFlow<List<ParkingLotDto>> = _items

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadForUser(userId: String, documento: String? = null, tipoDocumento: String? = null) {
        if (_loading.value) return
        _loading.value = true

        viewModelScope.launch {
            try {
                // 🔥 LOGS DE DIAGNÓSTICO
                println("📱 Cliente solicitando parqueos:")
                println("   - userId: $userId")
                println("   - documento: $documento")
                println("   - tipoDocumento: $tipoDocumento")

                // 🔒 Server-side filtering
                val resp = RetrofitInstance.apiService.listarParqueos(
                    createdBy = userId,                 // Filtro por usuario creador
                    ownerDocumento = documento,         // Si tu backend los usa, los mandamos también
                    ownerTipo = tipoDocumento
                )

                // 🔥 LOGS DE RESPUESTA
                println("📡 Respuesta del servidor:")
                println("   - Código HTTP: ${resp.code()}")
                println("   - Es exitoso: ${resp.isSuccessful}")
                println("   - Headers: ${resp.headers()}")

                if (resp.isSuccessful) {
                    val parqueos = resp.body().orEmpty()
                    println("   - Parqueos recibidos: ${parqueos.size}")
                    parqueos.forEachIndexed { index, parqueo ->
                        println("     ${index + 1}. ${parqueo.localName} (ID: ${parqueo.id}, createdBy: ${parqueo.createdBy})")
                    }

                    _items.value = parqueos
                    _error.value = null
                } else if (resp.code() == 204) {
                    println("   - Sin parqueos (204 No Content)")
                    _items.value = emptyList()
                    _error.value = null
                } else {
                    println("   - Error HTTP: ${resp.code()}")
                    println("   - Mensaje error: ${resp.message()}")

                    // Intentar leer el cuerpo del error
                    val errorBody = resp.errorBody()?.string()
                    if (errorBody != null) {
                        println("   - Cuerpo del error: $errorBody")
                    }

                    _items.value = emptyList()
                    _error.value = "Error al cargar parqueos: ${resp.code()}"
                }
            } catch (e: Exception) {
                println("❌ Excepción en loadForUser:")
                println("   - Mensaje: ${e.message}")
                println("   - Clase: ${e::class.simpleName}")
                e.printStackTrace()

                _items.value = emptyList()
                _error.value = "Error de conexión: ${e.message}"
            } finally {
                _loading.value = false
                println("🏁 Carga finalizada. Items: ${_items.value.size}, Error: ${_error.value}")
            }
        }
    }

    fun refresh(userId: String, documento: String? = null, tipoDocumento: String? = null) {
        println("🔄 Refrescando parqueos para userId: $userId")
        loadForUser(userId, documento, tipoDocumento)
    }
}
