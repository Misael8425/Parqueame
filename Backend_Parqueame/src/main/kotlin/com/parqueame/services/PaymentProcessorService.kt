// services/PaymentProcessorService.kt
package com.parqueame.services

import com.parqueame.DatabaseFactory.transactionsCollection
import com.parqueame.models.TransactionDoc
import com.stripe.Stripe
import com.stripe.model.PaymentIntent
import com.stripe.model.Refund
import com.stripe.param.PaymentIntentCreateParams
import com.stripe.param.RefundCreateParams
import org.litote.kmongo.*
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

class PaymentProcessorService {
    private val log = LoggerFactory.getLogger(PaymentProcessorService::class.java)

    init {
        val secretKey = System.getenv("STRIPE_SECRET_KEY")
            ?: error("STRIPE_SECRET_KEY no configurada en el entorno")
        Stripe.apiKey = secretKey
    }

    /**
     * Crea un PaymentIntent y registra la transacción en Mongo
     */
    suspend fun createPayment(
        userId: String,
        parkingLotId: String,
        amount: Double,
        customerId: String,
        currency: String = "DOP"
    ): PaymentIntent {
        try {
            val amountInMinor = (amount * 100).toLong()

            val paymentIntent = PaymentIntent.create(
                PaymentIntentCreateParams.builder()
                    .setAmount(amountInMinor)
                    .setCurrency(currency.lowercase())
                    .setCustomer(customerId)
                    .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                            .setEnabled(true)
                            .build()
                    )
                    .putMetadata("userId", userId)
                    .putMetadata("parkingLotId", parkingLotId)
                    .build()
            )

            val transactionId = UUID.randomUUID().toString()
            val doc = TransactionDoc(
                id = transactionId,
                parkingLotId = parkingLotId,
                userId = userId,
                amount = amount,
                currency = currency,
                createdAt = Instant.now(),
                status = "pending",
                paymentMethod = "stripe",
                stripePaymentIntentId = paymentIntent.id
            )
            transactionsCollection.insertOne(doc)

            log.info("Payment created in Stripe=${paymentIntent.id} MongoTxn=${transactionId} user=$userId")
            return paymentIntent
        } catch (e: Exception) {
            log.error("Error creating payment", e)
            throw e
        }
    }

    /**
     * Confirma un pago si Stripe lo marcó como succeeded
     */
    suspend fun confirmPayment(paymentIntentId: String): Boolean {
        return try {
            val paymentIntent = PaymentIntent.retrieve(paymentIntentId)
            if (paymentIntent.status == "succeeded") {
                transactionsCollection.updateOne(
                    TransactionDoc::stripePaymentIntentId eq paymentIntentId,
                    setValue(TransactionDoc::status, "completed")
                )
                log.info("Payment confirmed: $paymentIntentId")
                true
            } else {
                log.warn("Payment not succeeded: $paymentIntentId status=${paymentIntent.status}")
                false
            }
        } catch (e: Exception) {
            log.error("Error confirming payment", e)
            false
        }
    }

    /**
     * Procesa un reembolso y actualiza la transacción en Mongo
     */
    suspend fun processRefund(paymentIntentId: String, reason: String? = null): Refund {
        try {
            val builder = RefundCreateParams.builder().setPaymentIntent(paymentIntentId)
            // Stripe solo acepta enums específicas; por simplicidad marcamos REQUESTED_BY_CUSTOMER si llega cualquier razón
            if (!reason.isNullOrBlank()) {
                builder.setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER)
            }
            val refund = Refund.create(builder.build())

            transactionsCollection.updateOne(
                TransactionDoc::stripePaymentIntentId eq paymentIntentId,
                setValue(TransactionDoc::status, "refunded")
            )

            log.info("Refund processed: ${refund.id} for $paymentIntentId")
            return refund
        } catch (e: Exception) {
            log.error("Error processing refund", e)
            throw e
        }
    }

    /**
     * Maneja webhook events (llámalo desde tu controller)
     */
    suspend fun handleWebhookEvent(type: String, paymentIntentId: String?) {
        when (type) {
            "payment_intent.succeeded" -> {
                if (paymentIntentId != null) confirmPayment(paymentIntentId)
            }
            "payment_intent.payment_failed" -> {
                if (paymentIntentId != null) {
                    transactionsCollection.updateOne(
                        TransactionDoc::stripePaymentIntentId eq paymentIntentId,
                        setValue(TransactionDoc::status, "failed")
                    )
                }
            }
            "charge.refunded" -> {
                if (paymentIntentId != null) {
                    transactionsCollection.updateOne(
                        TransactionDoc::stripePaymentIntentId eq paymentIntentId,
                        setValue(TransactionDoc::status, "refunded")
                    )
                }
            }
            else -> {
                log.debug("Unhandled webhook event: $type")
            }
        }
    }
}
