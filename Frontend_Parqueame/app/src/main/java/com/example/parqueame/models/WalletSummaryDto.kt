package com.example.parqueame.models

data class WalletSummaryDto(
    val totalIncome: Double,
    val currency: String = "DOP",
    val recentTransactions: List<TransactionDto>
)
