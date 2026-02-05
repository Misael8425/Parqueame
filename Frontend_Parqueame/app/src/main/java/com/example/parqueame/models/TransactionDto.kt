package com.example.parqueame.models

data class TransactionDto(
    val id: String,
    val parkingLotId: String,
    val parkingLotName: String,
    val parkingLotAddress: String,
    val amount: Double,
    val currency: String = "DOP",
    val date: String,
    val status: String,
    val paymentMethod: String,
    val stripePaymentIntentId: String? = null
)
