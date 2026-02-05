package com.parqueame.services

import com.parqueame.models.*
import com.parqueame.repositories.WalletRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class WalletService(private val repo: WalletRepository) {

    private fun iso(date: Instant) = DateTimeFormatter.ISO_INSTANT.format(date)

    suspend fun getSummary(userId: String): WalletSummaryDto {
        val total = repo.sumSettledIncome(userId)
        val tx = repo.listTransactions(userId, null, null).take(10).map {
            TransactionDto(
                id = it.id,
                parkingLotId = it.parkingLotId,
                parkingLotName = "Parqueo",
                parkingLotAddress = "",
                amount = it.amount,
                date = iso(it.createdAt),
                status = it.status,
                paymentMethod = it.paymentMethod,
                stripePaymentIntentId = it.stripePaymentIntentId
            )
        }
        return WalletSummaryDto(totalIncome = total, recentTransactions = tx)
    }

    suspend fun getTransactions(
        userId: String,
        startDate: String?,
        endDate: String?
    ): List<TransactionDto> {
        val start = startDate?.let { LocalDate.parse(it).atStartOfDay().toInstant(ZoneOffset.UTC) }
        val end = endDate?.let { LocalDate.parse(it).plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC) }

        return repo.listTransactions(userId, start, end).map {
            TransactionDto(
                id = it.id,
                parkingLotId = it.parkingLotId,
                parkingLotName = "Parqueo",
                parkingLotAddress = "",
                amount = it.amount,
                date = iso(it.createdAt),
                status = it.status,
                paymentMethod = it.paymentMethod,
                stripePaymentIntentId = it.stripePaymentIntentId
            )
        }
    }

    suspend fun updateBankAccount(
        userId: String,
        req: UpdateBankAccountRequest
    ): BankAccountDto {
        // TODO: valida contraseña real (a futuro)
        require(req.password.isNotBlank()) { "Contraseña requerida" }

        val saved = repo.upsertBankAccount(userId, req)
        return BankAccountDto(
            accountNumber = saved.accountNumber,
            bankName = saved.bankName,
            accountType = saved.accountType,
            holderName = saved.holderName,
            isDefault = saved.isDefault
        )
    }

    suspend fun createWithdrawal(userId: String, amount: Double): WithdrawalDto {
        require(amount > 0) { "El monto debe ser mayor a 0" }

        val balance = repo.sumSettledIncome(userId) - repo.sumApprovedWithdrawals(userId)
        require(amount <= balance + 1e-6) { "Fondos insuficientes" }

        val bank = repo.getBankAccount(userId)
            ?: error("Primero configura una cuenta bancaria")

        val w = repo.createWithdrawal(userId, amount, bank.accountNumber)
        return WithdrawalDto(
            id = w.id,
            userId = w.userId,
            accountNumber = w.accountNumber,
            amount = w.amount,
            status = w.status,
            createdAt = iso(w.createdAt)
        )
    }

    suspend fun listWithdrawals(userId: String): List<WithdrawalDto> =
        repo.listWithdrawals(userId).map {
            WithdrawalDto(
                id = it.id,
                userId = it.userId,
                accountNumber = it.accountNumber,
                amount = it.amount,
                status = it.status,
                createdAt = iso(it.createdAt)
            )
        }
}
