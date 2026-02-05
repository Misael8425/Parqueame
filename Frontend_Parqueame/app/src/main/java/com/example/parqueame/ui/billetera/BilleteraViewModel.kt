// BilleteraViewModel.kt
package com.example.parqueame.ui.billetera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parqueame.api.RetrofitInstance
import com.example.parqueame.models.CreateSetupIntentRequest
import com.example.parqueame.models.SetupIntentResponse
import com.example.parqueame.models.StripeCardDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class WalletState(
    val isLoading: Boolean = true,
    val initialLoaded: Boolean = false,
    val errorMessage: String? = null,
    val publishableKey: String? = null,
    val customerId: String? = null,
    val ephemeralKey: String? = null,
    val status: String? = null,
    val cards: List<UiCard> = emptyList()
)

class BilleteraViewModel : ViewModel() {

    private val _state = MutableStateFlow(WalletState())
    val state: StateFlow<WalletState> = _state

    fun bootstrap(userId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            try {
                val resp = RetrofitInstance.apiService.getCustomerBootstrap(userId)
                if (!resp.isSuccessful || resp.body() == null) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = "No se pudo inicializar la billetera (HTTP ${resp.code()})"
                    )
                    return@launch
                }
                val b = resp.body()!!
                _state.value = _state.value.copy(
                    publishableKey = b.publishableKey,
                    customerId = b.customerId,
                    ephemeralKey = b.ephemeralKey,
                    errorMessage = null
                )
                refreshCards(userId)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    initialLoaded = true,
                    errorMessage = t.localizedMessage ?: "Error de red"
                )
            }
        }
    }

    suspend fun createSetupIntent(customerId: String): retrofit2.Response<SetupIntentResponse> {
        return RetrofitInstance.apiService.createSetupIntent(CreateSetupIntentRequest(customerId))
    }

    fun refreshCards(userId: String) {
        viewModelScope.launch {
            val keepLoading = !_state.value.initialLoaded
            _state.value = _state.value.copy(
                isLoading = keepLoading,
                errorMessage = null
            )
            try {
                val resp = RetrofitInstance.apiService.getStripePaymentMethods(userId)
                if (resp.isSuccessful && resp.body() != null) {
                    val uiCards = resp.body()!!.map { it.toUi() }
                    _state.value = _state.value.copy(
                        cards = uiCards,
                        isLoading = false,
                        initialLoaded = true,
                        errorMessage = null
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        initialLoaded = true,
                        errorMessage = "No se pudieron obtener las tarjetas (HTTP ${resp.code()})"
                    )
                }
            } catch (t: Throwable) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    initialLoaded = true,
                    errorMessage = t.localizedMessage ?: "Error de red"
                )
            }
        }
    }

    fun detachPaymentMethod(pmId: String, userId: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val resp = RetrofitInstance.apiService.detachStripePaymentMethod(pmId, userId)
                val ok = resp.isSuccessful
                if (ok) refreshCards(userId)
                onDone(ok)
            } catch (_: Throwable) {
                onDone(false)
            }
        }
    }

    private fun StripeCardDto.toUi(): UiCard {
        val mm = expMonth.toString().padStart(2, '0')
        val yy = (expYear % 100).toString().padStart(2, '0')
        val holderNameSafe = holderName ?: "Titular"
        return UiCard(
            brand = brand.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
            last4 = last4,
            holder = holderNameSafe,
            expiry = "$mm/$yy",
            nickname = null,
            pmId = id
        )
    }
}