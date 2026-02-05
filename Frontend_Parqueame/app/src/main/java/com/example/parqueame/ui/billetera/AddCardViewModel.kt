// AddCardViewModel.kt
package com.example.parqueame.ui.billetera

import androidx.lifecycle.ViewModel
import com.example.parqueame.api.RetrofitInstance
import com.example.parqueame.models.CreateSetupIntentRequest
import com.example.parqueame.models.SetupIntentResponse
import com.example.parqueame.models.StripeCustomerBootstrap
import retrofit2.Response

class AddCardViewModel : ViewModel() {

    suspend fun bootstrap(userId: String): Response<StripeCustomerBootstrap> {
        return RetrofitInstance.apiService.getCustomerBootstrap(userId)
    }

    suspend fun createSetupIntent(customerId: String): Response<SetupIntentResponse> {
        return RetrofitInstance.apiService.createSetupIntent(
            CreateSetupIntentRequest(customerId = customerId)
        )
    }
}