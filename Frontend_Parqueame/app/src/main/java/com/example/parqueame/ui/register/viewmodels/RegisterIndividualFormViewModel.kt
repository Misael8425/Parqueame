package com.example.parqueame.ui.register.viewmodels

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parqueame.api.RetrofitInstance
import com.example.parqueame.models.Usuario
import kotlinx.coroutines.launch

class RegisterIndividualFormViewModel : ViewModel() {

    private val _registerStatus = mutableStateOf("")
    val registerStatus = _registerStatus

    private val _isLoading = mutableStateOf(false)
    val isLoading = _isLoading

    fun registerUser(usuario: Usuario, onError: (String) -> Unit) {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val response = RetrofitInstance.apiService.registerUsuario(usuario)

                if (response.isSuccessful) {
                    _registerStatus.value = "Usuario registrado correctamente"
                } else {
                    val errorBody = response.errorBody()?.string()

                    when {
                        errorBody?.contains("correo", ignoreCase = true) == true -> {
                            onError("El correo ya está registrado.")
                        }
                        errorBody?.contains("cédula", ignoreCase = true) == true ||
                                errorBody?.contains("cedula", ignoreCase = true) == true -> {
                            onError("La cédula ya está registrada.")
                        }
                        else -> {
                            onError("Error al registrar: ${response.code()}")
                        }
                    }
                }
            } catch (e: Exception) {
                onError("Error al realizar el registro: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearRegisterStatus() {
        _registerStatus.value = ""
    }
}
