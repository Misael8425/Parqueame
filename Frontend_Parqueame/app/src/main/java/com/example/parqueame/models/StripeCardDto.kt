package com.example.parqueame.models

data class StripeCardDto(
    val id: String,            // pm_xxx
    val brand: String,         // "visa", "mastercard", "amex", etc.
    val last4: String,
    val expMonth: Int,
    val expYear: Int,
    val holderName: String?
)
