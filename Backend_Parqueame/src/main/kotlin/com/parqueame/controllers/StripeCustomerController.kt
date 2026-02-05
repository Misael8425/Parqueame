package com.parqueame.controllers

import com.parqueame.models.*
import com.parqueame.services.StripeCustomerService
import com.stripe.Stripe
import com.stripe.model.EphemeralKey
import com.stripe.model.PaymentMethod
import com.stripe.model.SetupIntent
import com.stripe.param.EphemeralKeyCreateParams
import com.stripe.param.PaymentMethodListParams
import com.stripe.param.SetupIntentCreateParams
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

fun Route.stripeCustomerController() {
    val log = LoggerFactory.getLogger("StripeCustomerController")

    val secretKey = System.getenv("STRIPE_SECRET_KEY") ?: error("STRIPE_SECRET_KEY no definido")
    val publishableKey = System.getenv("STRIPE_PUBLISHABLE_KEY") ?: error("STRIPE_PUBLISHABLE_KEY no definido")
    Stripe.apiKey = secretKey

    /** POST /stripe/customer-bootstrap
     * Resuelve el customer por X-User-Id, crea si no existe y devuelve eph key + pk
     */
    post("/stripe/customer-bootstrap") {
        val userId = call.request.header("X-User-Id")
            ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "X-User-Id requerido"))

        try {
            val customerId = StripeCustomerService.getOrCreateCustomerId(userId)

            val eph = EphemeralKey.create(
                EphemeralKeyCreateParams.builder()
                    .setCustomer(customerId)
                    .setStripeVersion("2024-06-20")
                    .build()
            )

            call.respond(
                CustomerBootstrapDto(
                    publishableKey = publishableKey,
                    customerId = customerId,
                    ephemeralKey = eph.secret
                )
            )
        } catch (e: Exception) {
            log.error("Error en bootstrap", e)
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Error")))
        }
    }

    /** POST /stripe/create-setup-intent
     * Crea SetupIntent para el customer del usuario
     */
    post("/stripe/create-setup-intent") {
        try {
            val body = call.receive<CreateSetupIntentRequest>()
            val si = SetupIntent.create(
                SetupIntentCreateParams.builder()
                    .setCustomer(body.customerId)
                    .build()
            )
            call.respond(SetupIntentDto(clientSecret = si.clientSecret))
        } catch (e: Exception) {
            log.error("Error SetupIntent", e)
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Error")))
        }
    }

    /** GET /stripe/payment-methods
     * Lista PMs del customer correspondiente al X-User-Id
     */
    get("/stripe/payment-methods") {
        val userId = call.request.header("X-User-Id")
            ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "X-User-Id requerido"))
        try {
            val customerId = StripeCustomerService.getOrCreateCustomerId(userId)

            val params = PaymentMethodListParams.builder()
                .setCustomer(customerId)
                .setType(PaymentMethodListParams.Type.CARD)
                .build()

            val list = PaymentMethod.list(params)

            val out = list.data.map { pm ->
                val card = pm.card
                StripeCardOut(
                    id = pm.id,
                    brand = card?.brand ?: "card",
                    last4 = card?.last4 ?: "0000",
                    // 👇 Conversión de Long -> Int para que coincida con tu DTO
                    expMonth = card?.expMonth?.toInt() ?: 1,
                    expYear  = card?.expYear ?.toInt() ?: 2099,
                    holderName = pm.billingDetails?.name
                )
            }
            call.respond(out)
        } catch (e: Exception) {
            log.error("Error listando PMs", e)
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Error")))
        }
    }

    /** DELETE /stripe/payment-methods/{pmId}
     * Detach de un PM con validación de pertenencia al usuario.
     */
    delete("/stripe/payment-methods/{pmId}") {
        val userId = call.request.header("X-User-Id")
            ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "X-User-Id requerido"))
        val pmId = call.parameters["pmId"]
            ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "pmId requerido"))

        try {
            // Validamos que el PM pertenezca al customer del user (recomendado en producción)
            val userCustomerId = StripeCustomerService.getCustomerIdOrNull(userId)
                ?: return@delete call.respond(HttpStatusCode.NotFound, mapOf("error" to "Customer no encontrado para el usuario"))

            val pm = PaymentMethod.retrieve(pmId)
            if (pm.customer != userCustomerId) {
                return@delete call.respond(HttpStatusCode.Forbidden, mapOf("error" to "El método de pago no pertenece al usuario"))
            }

            pm.detach()
            call.respond(mapOf("message" to "detached"))
        } catch (e: Exception) {
            log.error("Error detaching PM", e)
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Error")))
        }
    }
}