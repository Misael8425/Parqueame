//// controllers/StripeWebhookController.kt
//package com.parqueame.controllers
//
//import com.parqueame.services.PaymentProcessorService
//import com.stripe.exception.SignatureVerificationException
//import com.stripe.model.Event
//import com.stripe.model.PaymentIntent
//import com.stripe.net.Webhook
//import io.ktor.http.*
//import io.ktor.server.application.*
//import io.ktor.server.request.*
//import io.ktor.server.response.*
//import io.ktor.server.routing.*
//import kotlinx.coroutines.runBlocking
//import org.slf4j.LoggerFactory
//
//fun Route.stripeWebhookController() {
//    val log = LoggerFactory.getLogger("StripeWebhookController")
//    val paymentService = PaymentProcessorService()
//
//    post("/stripe/webhook") {
//        try {
//            val payload = call.receiveText()
//            val sigHeader = call.request.header("Stripe-Signature")
//                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing Stripe-Signature header"))
//
//            val webhookSecret = System.getenv("STRIPE_WEBHOOK_SECRET")
//                ?: error("STRIPE_WEBHOOK_SECRET no configurado")
//
//            val event: Event = try {
//                Webhook.constructEvent(payload, sigHeader, webhookSecret)
//            } catch (e: SignatureVerificationException) {
//                log.error("Invalid webhook signature", e)
//                return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid signature"))
//            }
//
//            when (event.type) {
//                "payment_intent.succeeded",
//                "payment_intent.payment_failed",
//                "payment_intent.canceled" -> {
//                    val pi = event.dataObjectDeserializer.`object`.get() as PaymentIntent
//                    runBlocking { paymentService.handleWebhookEvent(event.type, pi.id) }
//                }
//                "charge.refunded" -> {
//                    // data.object = Charge; obtener payment_intent id si lo necesitas
//                    val obj = event.dataObjectDeserializer.`object`.get()
//                    val paymentIntentId = obj?.let { (it as com.stripe.model.Charge).paymentIntent }
//                    runBlocking { paymentService.handleWebhookEvent(event.type, paymentIntentId) }
//                }
//                else -> log.debug("Unhandled event type: ${event.type}")
//            }
//
//            call.respond(HttpStatusCode.OK, mapOf("received" to true))
//        } catch (e: Exception) {
//            log.error("Error processing webhook", e)
//            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Webhook processing failed"))
//        }
//    }
//
//    // Tu endpoint para crear pagos permanece igual, pero recuerda que ahora el servicio es suspend
//    post("/payments/create") {
//        try {
//            val userId = call.request.header("X-User-Id")
//                ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("message" to "Usuario no autenticado"))
//
//            val body = call.receiveParameters()
//            val parkingLotId = body["parkingLotId"]
//                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("message" to "parkingLotId requerido"))
//            val amount = body["amount"]?.toDoubleOrNull()
//                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("message" to "amount inválido"))
//            val customerId = body["customerId"]
//                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("message" to "customerId requerido"))
//
//            val paymentIntent = paymentService.createPayment(
//                userId = userId,
//                parkingLotId = parkingLotId,
//                amount = amount,
//                customerId = customerId
//            )
//
//            call.respond(HttpStatusCode.OK, mapOf(
//                "clientSecret" to paymentIntent.clientSecret,
//                "paymentIntentId" to paymentIntent.id
//            ))
//        } catch (e: Exception) {
//            val log = LoggerFactory.getLogger("StripeWebhookController")
//            log.error("Error creating payment", e)
//            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to (e.message ?: "Error creando pago")))
//        }
//    }
//
//    post("/payments/refund") {
//        try {
//            val userId = call.request.header("X-User-Id")
//                ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("message" to "Usuario no autenticado"))
//
//            val body = call.receiveParameters()
//            val paymentIntentId = body["paymentIntentId"]
//                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("message" to "paymentIntentId requerido"))
//            val reason = body["reason"]
//
//            val refund = paymentService.processRefund(paymentIntentId, reason)
//            call.respond(HttpStatusCode.OK, mapOf("refundId" to refund.id, "status" to refund.status))
//        } catch (e: Exception) {
//            val log = LoggerFactory.getLogger("StripeWebhookController")
//            log.error("Error processing refund", e)
//            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to (e.message ?: "Error procesando reembolso")))
//        }
//    }
//}
