package com.parqueame.models

import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

@Serializable
data class CustomerBootstrapDto(
    val publishableKey: String,
    val customerId: String,
    val ephemeralKey: String
)

@Serializable
data class SetupIntentDto(
    val clientSecret: String
)

@Serializable
data class CreateSetupIntentRequest(
    val customerId: String
)

@Serializable
data class StripeCardOut(
    val id: String,
    val brand: String,
    val last4: String,
    val expMonth: Int,
    val expYear: Int,
    val holderName: String? = null
)

/** ➕ DOC persistente: vínculo userId ↔ customerId (Stripe) */
data class StripeCustomerDoc(
    @BsonId val id: ObjectId = ObjectId(),
    val userId: String,           // tu user interno
    val customerId: String,       // Stripe customer id (cus_xxx)
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)