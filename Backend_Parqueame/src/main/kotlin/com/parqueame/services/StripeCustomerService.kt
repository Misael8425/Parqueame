package com.parqueame.services

import com.parqueame.DatabaseFactory
import com.parqueame.models.StripeCustomerDoc
import com.stripe.model.Customer
import com.stripe.param.CustomerCreateParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.litote.kmongo.eq

object StripeCustomerService {

    /** Devuelve el customerId de Stripe para un userId; si no existe, lo crea y persiste. */
    suspend fun getOrCreateCustomerId(userId: String): String {
        // buscar
        val existing = DatabaseFactory.stripeCustomersCollection
            .findOne(StripeCustomerDoc::userId eq userId)
        if (existing != null) return existing.customerId

        // crear en Stripe
        val created = withContext(Dispatchers.IO) {
            Customer.create(
                CustomerCreateParams.builder()
                    .putMetadata("app_user_id", userId)
                    .build()
            )
        }

        val doc = StripeCustomerDoc(
            userId = userId,
            customerId = created.id
        )
        DatabaseFactory.stripeCustomersCollection.insertOne(doc)
        return created.id
    }

    /** Obtiene (sin crear) el customerId o null. */
    suspend fun getCustomerIdOrNull(userId: String): String? {
        val existing = DatabaseFactory.stripeCustomersCollection
            .findOne(StripeCustomerDoc::userId eq userId)
        return existing?.customerId
    }
}
