package com.example.parqueame.models

data class StripeCustomerBootstrap(
    val publishableKey: String,
    val customerId: String,
    val ephemeralKey: String
)
