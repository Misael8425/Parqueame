package com.example.parqueame.ui.login.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parqueame.api.RetrofitInstance
import com.example.parqueame.models.LoginRequest
import com.example.parqueame.models.LoginResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.annotation.StringRes
import com.example.parqueame.R
import org.json.JSONObject

class LoginViewModel : ViewModel() {

    private val api = RetrofitInstance.apiService

    // Mensaje localizable para la UI
    sealed class UiMessage {
        data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiMessage()
        data class Text(val value: String) : UiMessage()
    }

    private val _loginState = MutableStateFlow<LoginResult>(LoginResult.Idle)
    val loginState: StateFlow<LoginResult> = _loginState

    fun login(user: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginResult.Loading
            try {
                val request = LoginRequest(email = user.trim(), password = password)
                val response = api.login(request)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        _loginState.value = LoginResult.Success(body)
                    } else {
                        _loginState.value = LoginResult.Error(UiMessage.Res(R.string.server_empty_response_error))
                    }
                } else {
                    // Mapeo explícito de credenciales inválidas
                    when (response.code()) {
                        400, 401, 404 -> {
                            _loginState.value = LoginResult.Error(UiMessage.Res(R.string.invalid_credentials_error))
                        }
                        else -> {
                            val raw = response.errorBody()?.string().orEmpty()
                            val parsed = parseServerMessage(raw)
                            _loginState.value = LoginResult.Error(parsed ?: UiMessage.Res(R.string.credentials_error_generic))
                        }
                    }
                }
            } catch (e: Exception) {
                _loginState.value = LoginResult.Error(
                    UiMessage.Res(R.string.network_error_with_reason, args = listOf(e.localizedMessage ?: "desconocido"))
                )
            }
        }
    }

    /**
     * Intenta extraer un mensaje útil del body del error.
     * - Si viene JSON con "message" o "error" → UiMessage.Text
     * - Si viene HTML/texto → limpia etiquetas y espacios → UiMessage.Text
     * - Si está vacío → null (para que el caller ponga un Res genérico)
     */
    private fun parseServerMessage(errorBody: String?): UiMessage? {
        val body = errorBody?.trim().orEmpty()
        if (body.isBlank()) return null
        return try {
            val json = JSONObject(body)
            when {
                json.has("message") -> UiMessage.Text(json.getString("message"))
                json.has("error")   -> UiMessage.Text(json.getString("error"))
                else                -> UiMessage.Text(body.take(300))
            }
        } catch (_: Exception) {
            val cleaned = body.replace(Regex("<[^>]*>"), " ")
                .replace(Regex("\\s+"), " ")
                .take(300)
            UiMessage.Text(cleaned.ifBlank { return null })
        }
    }

    sealed class LoginResult {
        data object Idle : LoginResult()
        data object Loading : LoginResult()
        data class Success(val data: LoginResponse) : LoginResult()
        data class Error(val message: UiMessage) : LoginResult()
    }
}