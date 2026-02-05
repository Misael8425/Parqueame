package com.parqueame.repositories

import com.mongodb.client.model.ReplaceOptions
import com.parqueame.models.*
import org.litote.kmongo.and
import org.litote.kmongo.ascending
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.descending
import org.litote.kmongo.eq
import org.litote.kmongo.`in`
import org.litote.kmongo.gte
import org.litote.kmongo.lt
import java.time.Instant

class WalletRepository(
    private val txCol: CoroutineCollection<TransactionDoc>,
    private val bankCol: CoroutineCollection<BankAccountDoc>,
    private val wdCol: CoroutineCollection<WithdrawalDoc>
) {

    suspend fun getBankAccount(userId: String): BankAccountDoc? =
        bankCol.find(BankAccountDoc::userId eq userId)
            .sort(ascending(BankAccountDoc::createdAt))
            .limit(1)
            .toList()
            .firstOrNull()

    suspend fun upsertBankAccount(userId: String, req: UpdateBankAccountRequest): BankAccountDoc {
        val existing = getBankAccount(userId)

        val doc = existing?.copy(
            accountNumber = req.accountNumber,
            bankName = req.bankName ?: existing.bankName,
            accountType = req.accountType ?: existing.accountType,
            holderName = req.holderName ?: existing.holderName,
            isDefault = true
        ) ?: BankAccountDoc(
            id = java.util.UUID.randomUUID().toString(),
            userId = userId,
            accountNumber = req.accountNumber,
            bankName = req.bankName ?: "N/D",
            accountType = req.accountType ?: "corriente",
            holderName = req.holderName ?: "N/D",
            isDefault = true
        )

        // KMongo coroutine: usar ReplaceOptions para upsert
        bankCol.replaceOne(
            filter = BankAccountDoc::userId eq userId,
            replacement = doc,
            options = ReplaceOptions().upsert(true)
        )

        return doc
    }

    suspend fun listTransactions(userId: String, start: Instant?, end: Instant?): List<TransactionDoc> {
        val base = TransactionDoc::userId eq userId
        val filter = when {
            start != null && end != null ->
                and(base, TransactionDoc::createdAt gte start, TransactionDoc::createdAt lt end)
            start != null ->
                and(base, TransactionDoc::createdAt gte start)
            end != null ->
                and(base, TransactionDoc::createdAt lt end)
            else -> base
        }

        return txCol.find(filter)
            .sort(descending(TransactionDoc::createdAt))
            .toList()
    }

    // MVP: suma en memoria (simple y robusto)
    suspend fun sumSettledIncome(userId: String): Double =
        txCol.find(and(TransactionDoc::userId eq userId, TransactionDoc::status eq "SUCCEEDED"))
            .toList()
            .sumOf { it.amount }

    suspend fun sumApprovedWithdrawals(userId: String): Double =
        wdCol.find(and(WithdrawalDoc::userId eq userId, WithdrawalDoc::status `in` listOf("APPROVED", "PAID")))
            .toList()
            .sumOf { it.amount }

    suspend fun createWithdrawal(userId: String, amount: Double, accountNumber: String): WithdrawalDoc {
        val doc = WithdrawalDoc(
            id = java.util.UUID.randomUUID().toString(),
            userId = userId,
            amount = amount,
            accountNumber = accountNumber
        )
        wdCol.insertOne(doc)
        return doc
    }

    suspend fun listWithdrawals(userId: String): List<WithdrawalDoc> =
        wdCol.find(WithdrawalDoc::userId eq userId)
            .sort(descending(WithdrawalDoc::createdAt))
            .toList()
}
