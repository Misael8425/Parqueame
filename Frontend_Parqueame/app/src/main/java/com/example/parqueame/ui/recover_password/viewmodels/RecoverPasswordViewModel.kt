package com.example.parqueame.ui.recover_password.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parqueame.api.RetrofitInstance
import com.example.parqueame.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import androidx.annotation.StringRes
import com.example.parqueame.R
import kotlinx.coroutines.launch

class RecoverPasswordViewModel : ViewModel() {

    // Mensajes para UI (resource o texto crudo)
    sealed class UiMessage {
        data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiMessage()
        data class Text(val value: String) : UiMessage()
    }

    private val _uiMessage = MutableStateFlow<UiMessage?>(null)
    val uiMessage: StateFlow<UiMessage?> = _uiMessage

    // Mantiene email/código entre pantallas
    companion object {
        private var userEmail: String = ""
        private var recoveryCode: String = ""
    }

    fun clearMessage() { _uiMessage.value = null }
    fun clearRecoveryData() { userEmail = ""; recoveryCode = "" }

    private fun setEmail(email: String) { userEmail = email }
    private fun setCode(code: String) { recoveryCode = code }

    // --- Parser de error tolerante (JSON o texto/HTML) ---
    private fun extractErrorMessage(errorBody: String?): String {
        return try {
            val jsonObject = JSONObject(errorBody ?: "")
            when {
                jsonObject.has("message") -> jsonObject.getString("message")
                jsonObject.has("error") -> jsonObject.getString("error")
                else -> "Unknown error" // usamos tu clave existente 'unknown_error' en la UI si queremos mapear
            }
        } catch (e: Exception) {
            val raw = errorBody ?: "Network error: ${e.message ?: "unknown"}"
            raw.replace(Regex("<[^>]*>"), " ").replace(Regex("\\s+"), " ").take(300)
        }
    }

    fun solicitarCodigo(email: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.apiService.solicitarCodigo(EmailRequest(email))
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.success == true) {
                        setEmail(email)
                        onSuccess()
                    } else {
                        _uiMessage.value = apiResponse?.message?.let { UiMessage.Text(it) }
                            ?: UiMessage.Res(R.string.unknown_error)
                    }
                } else {
                    val error = extractErrorMessage(response.errorBody()?.string())
                    _uiMessage.value = UiMessage.Text(error)
                }
            } catch (e: Exception) {
                _uiMessage.value = UiMessage.Res(
                    R.string.network_error_with_reason,
                    args = listOf(e.localizedMessage ?: "desconocido")
                )
            }
        }
    }

    fun reenviarCodigo(onSuccess: () -> Unit) {
        if (userEmail.isEmpty()) { _uiMessage.value = UiMessage.Res(R.string.email_not_found_error); return }
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.apiService.solicitarCodigo(EmailRequest(userEmail))
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.success == true) {
                        onSuccess()
                    } else {
                        _uiMessage.value = apiResponse?.message?.let { UiMessage.Text(it) }
                            ?: UiMessage.Res(R.string.unknown_error)
                    }
                } else {
                    val error = extractErrorMessage(response.errorBody()?.string())
                    _uiMessage.value = UiMessage.Text(error)
                }
            } catch (e: Exception) {
                _uiMessage.value = UiMessage.Res(
                    R.string.network_error_with_reason,
                    args = listOf(e.localizedMessage ?: "desconocido")
                )
            }
        }
    }

    fun verificarCodigo(code: String, onSuccess: () -> Unit) {
        if (userEmail.isEmpty()) { _uiMessage.value = UiMessage.Res(R.string.email_not_found_error); return }
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.apiService.verificarCodigo(
                    CodeVerificationRequest(userEmail, code)
                )
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.success == true) {
                        setCode(code)
                        onSuccess()
                    } else {
                        _uiMessage.value = apiResponse?.message?.let { UiMessage.Text(it) }
                            ?: UiMessage.Res(R.string.invalid_or_expired_code_error)
                    }
                } else {
                    val error = extractErrorMessage(response.errorBody()?.string())
                    _uiMessage.value = UiMessage.Text(error)
                }
            } catch (e: Exception) {
                _uiMessage.value = UiMessage.Res(
                    R.string.network_error_with_reason,
                    args = listOf(e.localizedMessage ?: "desconocido")
                )
            }
        }
    }

    fun cambiarContrasena(nueva: String, confirmar: String, onSuccess: () -> Unit) {
        if (nueva != confirmar) { _uiMessage.value = UiMessage.Res(R.string.passwords_not_equal_error); return }
        if (nueva.length < 8) { _uiMessage.value = UiMessage.Res(R.string.password_min_length_error, args = listOf(8)); return }
        if (userEmail.isEmpty()) { _uiMessage.value = UiMessage.Res(R.string.email_not_found_error); return }
        if (recoveryCode.isEmpty()) { _uiMessage.value = UiMessage.Res(R.string.code_not_found_error); return }

        viewModelScope.launch {
            try {
                val response = RetrofitInstance.apiService.cambiarContrasena(
                    ResetPasswordRequest(userEmail, recoveryCode, nueva)
                )
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.success == true) {
                        _uiMessage.value = null
                        onSuccess()
                    } else {
                        _uiMessage.value = apiResponse?.message?.let { UiMessage.Text(it) }
                            ?: UiMessage.Res(R.string.password_update_error)
                    }
                } else {
                    val error = extractErrorMessage(response.errorBody()?.string())
                    _uiMessage.value = UiMessage.Text(error)
                }
            } catch (e: Exception) {
                _uiMessage.value = UiMessage.Res(
                    R.string.network_error_with_reason,
                    args = listOf(e.localizedMessage ?: "desconocido")
                )
            }
        }
    }
}