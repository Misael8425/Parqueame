package com.example.parqueame.models

data class StripePaymentResponse(
    val publishableKey: String,
    val customerId: String,
    val ephemeralKey: String,
    val clientSecret: String
)
