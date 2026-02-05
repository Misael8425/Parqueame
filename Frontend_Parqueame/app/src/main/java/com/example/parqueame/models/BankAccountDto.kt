package com.example.parqueame.models

data class BankAccountDto(
    val accountNumber: String,
    val bankName: String,
    val accountType: String,
    val holderName: String,
    val isDefault: Boolean = false
)
