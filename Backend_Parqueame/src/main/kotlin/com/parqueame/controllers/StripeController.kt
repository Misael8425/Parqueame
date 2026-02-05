package com.parqueame.controllers

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import com.stripe.Stripe
import com.stripe.model.Customer
import com.stripe.model.EphemeralKey
import com.stripe.model.PaymentIntent
import com.stripe.param.CustomerCreateParams
import com.stripe.param.EphemeralKeyCreateParams
import com.stripe.param.PaymentIntentCreateParams

@Serializable
data class StripePaymentResponse(
    val publishableKey: String,
    val customerId:     String,
    val ephemeralKey:   String,
    val clientSecret:   String
)

fun Route.stripeController() {
    val log = LoggerFactory.getLogger("StripeController")

    post("/create-payment-intent") {
        try {
            // 🔐 Claves hardcodeadas
            val secretKey = "sk_test_51RtGDcCyTXZ1US4XNBVbAzEAFVWeupBnnHsovw9PDXKPRZW2tR0JGgGJSeoo9fLUgib8E7xcd2HLrz86MOPOvElO006AbC2JVS"
            val publishableKey = "pk_test_51RtGDcCyTXZ1US4XMzxMKU3IUAo0Ix9A9jr9AhgihnaOYTqcK0EnR36L02e652FMum2RcIXG8yiChWXt6u2aXvR500HeJBWAtp"

            // Inicializa Stripe
            Stripe.apiKey = secretKey

            // 1️⃣ Crear Customer
            val customer = Customer.create(
                CustomerCreateParams.builder().build()
            )

            // 2️⃣ Crear Ephemeral Key con versión API válida
            val ephemeralKey = EphemeralKey.create(
                EphemeralKeyCreateParams.builder()
                    .setCustomer(customer.id)
                    .setStripeVersion("2024-06-20") // ← Versión API válida
                    .build()
            )

            // 3️⃣ Crear PaymentIntent con pagos automáticos habilitados
            val paymentIntent = PaymentIntent.create(
                PaymentIntentCreateParams.builder()
                    .setAmount(1099L) // 10.99 USD en centavos
                    .setCurrency("usd")
                    .setCustomer(customer.id)
                    .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                            .setEnabled(true)
                            .build()
                    )
                    .build()
            )

            // 4️⃣ Responder al cliente con los datos necesarios
            call.respond(
                HttpStatusCode.OK,
                StripePaymentResponse(
                    publishableKey = publishableKey,
                    customerId     = customer.id,
                    ephemeralKey   = ephemeralKey.secret,
                    clientSecret   = paymentIntent.clientSecret
                )
            )
        } catch (e: Exception) {
            log.error("Error en /create-payment-intent", e)
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to (e.message ?: "Error interno del servidor"))
            )
        }
    }
}
