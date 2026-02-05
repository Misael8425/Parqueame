package com.example.parqueame.models

data class UpdateBankAccountRequest(
    val accountNumber: String,
    val bankName: String? = null,
    val accountType: String? = null,
    val holderName: String? = null,
    val password: String
)
