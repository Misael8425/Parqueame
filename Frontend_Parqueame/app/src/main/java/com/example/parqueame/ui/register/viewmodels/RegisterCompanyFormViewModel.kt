@file:OptIn(FlowPreview::class)

package com.example.parqueame.ui.register.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parqueame.api.RetrofitInstance
import com.example.parqueame.models.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import retrofit2.HttpException
import java.io.IOException

class RegisterCompanyFormViewModel : ViewModel() {

    companion object {
        private const val TAG = "RegisterCompanyForm"
        private const val VALIDATION_DEBOUNCE = 800L
        private const val API_TIMEOUT = 15000L
    }

    private val _companyName = MutableStateFlow("")
    val companyName: StateFlow<String> = _companyName.asStateFlow()

    private val _rnc = MutableStateFlow("")
    val rnc: StateFlow<String> = _rnc.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _confirmPassword = MutableStateFlow("")
    val confirmPassword: StateFlow<String> = _confirmPassword.asStateFlow()

    private val _termsAccepted = MutableStateFlow(false)
    val termsAccepted: StateFlow<Boolean> = _termsAccepted.asStateFlow()

    private val _registerStatus = MutableStateFlow("")
    val registerStatus: StateFlow<String> = _registerStatus.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _rncErrorMessage = MutableStateFlow("")
    val rncErrorMessage: StateFlow<String> = _rncErrorMessage.asStateFlow()

    // Cache de validación mejorado
    private val validatedRncs = mutableMapOf<String, ValidationResult>()
    private var validationJob: Job? = null
    private var shouldShowValidationErrors = false

    // Estados de validación
    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val reason: String) : ValidationResult()
        object Pending : ValidationResult()
    }

    init {
        viewModelScope.launch {
            _rnc
                .debounce(VALIDATION_DEBOUNCE)
                .filter { it.isNotBlank() }
                .collectLatest { value ->
                    Log.d(TAG, "Iniciando validación automática para: $value")
                    if (isDocumentFormatValid(value)) {
                        validarConDgiiOptimizado(value) {}
                    } else {
                        Log.d(TAG, "Formato inválido, no se valida: $value")
                    }
                }
        }
    }

    fun onCompanyNameChange(value: String) {
        _companyName.value = value
        Log.d(TAG, "Nombre empresa cambiado: $value")
    }

    fun onRncChange(value: String) {
        val filteredValue = value.filter { it.isDigit() }.take(11)
        _rnc.value = filteredValue

        // Limpiar errores solo si no estamos en modo validación
        if (!shouldShowValidationErrors) {
            _rncErrorMessage.value = ""
        }

        Log.d(TAG, "RNC cambiado: '$value' -> '$filteredValue'")
    }

    fun validarRncParaRegistro(mostrarError: (String) -> Unit): Boolean {
        shouldShowValidationErrors = true
        val value = _rnc.value.trim()

        Log.d(TAG, "Validando RNC para registro: '$value'")

        if (!isDocumentFormatValid(value)) {
            val error = "El RNC debe tener 9 dígitos o la cédula 11 dígitos numéricos."
            mostrarError(error)
            Log.d(TAG, "Formato inválido: $error")
            return false
        }

        val normalized = normalizeRnc(value)
        val validationResult = validatedRncs[normalized]

        return when (validationResult) {
            is ValidationResult.Valid -> {
                Log.d(TAG, "RNC válido en cache: $normalized")
                true
            }
            is ValidationResult.Invalid -> {
                mostrarError(validationResult.reason)
                Log.d(TAG, "RNC inválido en cache: ${validationResult.reason}")
                false
            }
            is ValidationResult.Pending, null -> {
                val error = "Validando RNC en segundo plano. Intenta de nuevo en unos segundos."
                mostrarError(error)
                Log.d(TAG, "RNC pendiente de validación: $normalized")
                // Forzar validación si no está en proceso
                if (validationResult == null) {
                    validarConDgiiOptimizado(value) { errorMsg ->
                        Log.d(TAG, "Error en validación forzada: $errorMsg")
                    }
                }
                false
            }
        }
    }

    private fun validarConDgiiOptimizado(rnc: String, onError: (String) -> Unit) {
        // Cancelar validación anterior
        validationJob?.cancel()

        validationJob = viewModelScope.launch {
            val normalizedRnc = normalizeRnc(rnc)

            Log.d(TAG, "=== INICIANDO VALIDACIÓN DGII ===")
            Log.d(TAG, "RNC original: '$rnc'")
            Log.d(TAG, "RNC normalizado: '$normalizedRnc'")

            // Marcar como pendiente
            validatedRncs[normalizedRnc] = ValidationResult.Pending
            _rncErrorMessage.value = ""

            try {
                // Mostrar mensaje de carga después de 2 segundos
                val loadingJob = launch {
                    delay(2000)
                    if (isActive && _rncErrorMessage.value.isBlank()) {
                        _rncErrorMessage.value = "Consultando DGII... Por favor espere"
                        Log.d(TAG, "Mostrando mensaje de carga")
                    }
                }

                val response = withContext(Dispatchers.IO) {
                    withTimeout(API_TIMEOUT) {
                        Log.d(TAG, "Haciendo llamada a la API...")
                        RetrofitInstance.apiService.consultarRnc(normalizedRnc)
                    }
                }

                loadingJob.cancel() // Cancelar mensaje de carga

                Log.d(TAG, "Response code: ${response.code()}")
                Log.d(TAG, "Response successful: ${response.isSuccessful}")

                if (response.isSuccessful) {
                    val result = response.body()
                    Log.d(TAG, "Response body completo: $result")

                    if (result != null) {
                        Log.d(TAG, "RNC encontrado: ${result.rnc}")
                        Log.d(TAG, "Razón social: '${result.razonSocial}'")
                        Log.d(TAG, "Estado: ${result.estado}")
                    }

                    if (result != null && !result.razonSocial.isNullOrBlank()) {
                        // RNC válido
                        validatedRncs[normalizedRnc] = ValidationResult.Valid
                        _rncErrorMessage.value = ""

                        // Auto-llenar nombre de empresa si está vacío
                        if (_companyName.value.isBlank()) {
                            _companyName.value = result.razonSocial
                            Log.d(TAG, "Auto-llenando nombre empresa: ${result.razonSocial}")
                        }

                        Log.d(TAG, "✅ RNC VÁLIDO: $normalizedRnc")
                    } else {
                        // RNC no encontrado
                        val reason = "RNC no encontrado en la base de datos de la DGII"
                        validatedRncs[normalizedRnc] = ValidationResult.Invalid(reason)
                        _rncErrorMessage.value = reason
                        if (shouldShowValidationErrors) onError(reason)
                        Log.d(TAG, "❌ RNC NO ENCONTRADO: $normalizedRnc")
                    }
                } else {
                    // Error HTTP
                    val errorBody = try {
                        response.errorBody()?.string()
                    } catch (e: Exception) {
                        "Error al leer respuesta"
                    }

                    Log.d(TAG, "❌ ERROR HTTP: ${response.code()}")
                    Log.d(TAG, "Error body: $errorBody")

                    val errorMsg = when (response.code()) {
                        404 -> "RNC no encontrado en la base de datos de la DGII"
                        408, 504 -> "La consulta DGII tardó demasiado tiempo. Intente nuevamente."
                        500, 502, 503 -> "Servicio DGII no disponible temporalmente"
                        429 -> "Demasiadas consultas. Espere un momento e intente de nuevo."
                        else -> "Error al consultar DGII (Código: ${response.code()})"
                    }

                    if (response.code() == 404) {
                        validatedRncs[normalizedRnc] = ValidationResult.Invalid(errorMsg)
                    } else {
                        // Para otros errores, no guardar en cache
                        validatedRncs.remove(normalizedRnc)
                    }

                    _rncErrorMessage.value = errorMsg
                    if (shouldShowValidationErrors) onError(errorMsg)
                }

            } catch (e: TimeoutCancellationException) {
                val msg = "Consulta DGII excedió tiempo límite. Verifique su conexión."
                Log.d(TAG, "❌ TIMEOUT: ${e.message}")
                validatedRncs.remove(normalizedRnc) // No guardar timeouts en cache
                _rncErrorMessage.value = msg
                if (shouldShowValidationErrors) onError(msg)
            } catch (e: IOException) {
                val msg = "Error de conexión. Verifique su conexión a internet."
                Log.d(TAG, "❌ IO ERROR: ${e.message}")
                validatedRncs.remove(normalizedRnc) // No guardar errores de red en cache
                _rncErrorMessage.value = msg
                if (shouldShowValidationErrors) onError(msg)
            } catch (e: HttpException) {
                val msg = "Error del servidor: ${e.localizedMessage ?: "Desconocido"}"
                Log.d(TAG, "❌ HTTP EXCEPTION: ${e.message}")
                validatedRncs.remove(normalizedRnc)
                _rncErrorMessage.value = msg
                if (shouldShowValidationErrors) onError(msg)
            } catch (e: Exception) {
                val msg = "Error inesperado: ${e.localizedMessage ?: "Desconocido"}"
                Log.d(TAG, "❌ EXCEPTION: ${e.message}", e)
                validatedRncs.remove(normalizedRnc)
                _rncErrorMessage.value = msg
                if (shouldShowValidationErrors) onError(msg)
            }

            Log.d(TAG, "=== FIN VALIDACIÓN DGII ===")
        }
    }

    fun onEmailChange(value: String) {
        _email.value = value
    }

    fun onPasswordChange(value: String) {
        _password.value = value
    }

    fun onConfirmPasswordChange(value: String) {
        _confirmPassword.value = value
    }

    fun onTermsAcceptedChange(value: Boolean) {
        _termsAccepted.value = value
    }

    fun clearRegisterStatus() {
        _registerStatus.value = ""
        _rncErrorMessage.value = ""
        shouldShowValidationErrors = false
        validationJob?.cancel()
        Log.d(TAG, "Estado de registro limpiado")
    }

    private fun isDocumentFormatValid(document: String): Boolean {
        val isValid = (document.length == 9 || document.length == 11) &&
                document.all { it.isDigit() } &&
                document.toLongOrNull() != null

        Log.d(TAG, "Validación de formato para '$document': $isValid")
        return isValid
    }

    private fun normalizeRnc(rnc: String): String {
        val normalized = rnc.padStart(11, '0')
        Log.d(TAG, "Normalizando '$rnc' -> '$normalized'")
        return normalized
    }

    fun registerCompany(usuario: Usuario) {
        shouldShowValidationErrors = true
        Log.d(TAG, "Iniciando registro de empresa: ${usuario.nombre}")

        viewModelScope.launch {
            _isLoading.value = true

            try {
                val response = withContext(Dispatchers.IO) {
                    withTimeout(30000) {
                        RetrofitInstance.apiService.registerUsuario(usuario)
                    }
                }

                Log.d(TAG, "Registro response code: ${response.code()}")

                _registerStatus.value = if (response.isSuccessful) {
                    val message = response.body()?.message ?: "Registro exitoso"
                    Log.d(TAG, "✅ Registro exitoso: $message")
                    message
                } else {
                    val errorBody = response.errorBody()?.string()
                    val error = "Error: ${errorBody ?: "No se pudo registrar"}"
                    Log.d(TAG, "❌ Error en registro: $error")
                    error
                }

            } catch (e: TimeoutCancellationException) {
                val error = "El registro tardó demasiado tiempo. Intente nuevamente."
                Log.d(TAG, "❌ Timeout en registro: ${e.message}")
                _registerStatus.value = error
            } catch (e: HttpException) {
                val error = "Error HTTP: ${e.localizedMessage ?: "Desconocido"}"
                Log.d(TAG, "❌ HTTP error en registro: ${e.message}")
                _registerStatus.value = error
            } catch (e: Exception) {
                val error = "Error de red: ${e.localizedMessage ?: "Desconocido"}"
                Log.d(TAG, "❌ Error en registro: ${e.message}", e)
                _registerStatus.value = error
            }

            _isLoading.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        validationJob?.cancel()
        Log.d(TAG, "ViewModel limpiado")
    }
}