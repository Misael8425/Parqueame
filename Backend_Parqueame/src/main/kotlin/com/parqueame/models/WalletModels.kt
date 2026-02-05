package com.parqueame.models

import kotlinx.serialization.Serializable
import org.litote.kmongo.Id
import org.litote.kmongo.newId
import java.time.Instant

// ======== DTOs expuestos a la app (API) ========

@Serializable
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

@Serializable
data class BankAccountDto(
    val accountNumber: String,
    val bankName: String,
    val accountType: String,
    val holderName: String,
    val isDefault: Boolean = false
)

@Serializable
data class UpdateBankAccountRequest(
    val accountNumber: String,
    val bankName: String? = null,
    val accountType: String? = null,
    val holderName: String? = null,
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

// ======== Documentos Mongo ========

data class TransactionDoc(
    val _id: Id<TransactionDoc> = newId(),
    val id: String,
    val parkingLotId: String,
    val userId: String,
    val amount: Double,
    val currency: String = "DOP",
    val createdAt: Instant,
    val status: String,
    val paymentMethod: String,
    val stripePaymentIntentId: String? = null
)

data class BankAccountDoc(
    val _id: Id<BankAccountDoc> = newId(),
    val id: String,
    val userId: String,
    val accountNumber: String,
    val bankName: String,
    val accountType: String,
    val holderName: String,
    val isDefault: Boolean = false,
    val createdAt: Instant = Instant.now()
)

// ======== NUEVO: Retiros ========

@Serializable
data class CreateWithdrawalRequest(
    val amount: Double,
    val accountNumber: String,
    val password: String
)

@Serializable
data class WithdrawalDto(
    val id: String,
    val userId: String,
    val accountNumber: String,
    val amount: Double,
    val currency: String = "DOP",
    val status: String, // PENDING, APPROVED, REJECTED, PAID
    val createdAt: String
)

data class WithdrawalDoc(
    val _id: Id<WithdrawalDoc> = newId(),
    val id: String,
    val userId: String,
    val accountNumber: String,
    val amount: Double,
    val currency: String = "DOP",
    val status: String = "PENDING",
    val createdAt: Instant = Instant.now()
)
