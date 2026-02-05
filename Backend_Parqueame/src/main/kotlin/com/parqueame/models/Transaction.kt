/*
// models/Transaction.kt
package com.parqueame.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

@Serializable
data class TransactionDto(
    val id: String,
    val parkingLotId: String,
    val parkingLotName: String,
    val parkingLotAddress: String,
    val amount: Double,
    val currency: String = "DOP",
    val date: String,
    val status: String, // "completed", "pending", "failed"
    val paymentMethod: String, // "stripe", "bank_transfer"
    val stripePaymentIntentId: String? = null
)

@Serializable
data class BankAccountDto(
    val accountNumber: String,
    val bankName: String,
    val accountType: String, // "checking", "savings"
    val holderName: String,
    val isDefault: Boolean = false
)

@Serializable
data class UpdateBankAccountRequest(
    val accountNumber: String,
    val password: String
)

@Serializable
data class WalletSummaryDto(
    val totalIncome: Double,
    val currency: String = "DOP",
    val recentTransactions: List<TransactionDto>
)

@Serializable
data class TransactionFilterRequest(
    val month: Int? = null,
    val year: Int? = null,
    val startDate: String? = null,
    val endDate: String? = null
)

// Tables para Exposed ORM
object Transactions : Table("transactions") {
    val id = varchar("id", 50)
    val parkingLotId = varchar("parking_lot_id", 50)
    val userId = varchar("user_id", 50)
    val amount = double("amount")
    val currency = varchar("currency", 10).default("DOP")
    val date = datetime("date")
    val status = varchar("status", 20)
    val paymentMethod = varchar("payment_method", 50)
    val stripePaymentIntentId = varchar("stripe_payment_intent_id", 100).nullable()
    override val primaryKey = PrimaryKey(id)
}

object BankAccounts : Table("bank_accounts") {
    val id = varchar("id", 50)
    val userId = varchar("user_id", 50)
    val accountNumber = varchar("account_number", 50)
    val bankName = varchar("bank_name", 100)
    val accountType = varchar("account_type", 20)
    val holderName = varchar("holder_name", 100)
    val isDefault = bool("is_default").default(false)
    val createdAt = datetime("created_at")
    override val primaryKey = PrimaryKey(id)
}*/
