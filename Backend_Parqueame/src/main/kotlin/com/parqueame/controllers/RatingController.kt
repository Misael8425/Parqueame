// controllers/RatingController.kt
package com.parqueame.controllers

import com.parqueame.dto.RateParkingRequest
import com.parqueame.dto.RateParkingResponse
import com.parqueame.models.ApiResponse
import com.parqueame.models.ApiResponseWithData
import com.parqueame.services.RatingService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*

object RatingController {
    suspend fun postRate(call: ApplicationCall) {
        val parkingId = call.parameters["id"]
        if (parkingId.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "Missing parking id"))
            return
        }

        val body = try { call.receive<RateParkingRequest>() }
        catch (_: Exception) {
            call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "Invalid body"))
            return
        }

        try {
            val (count, _, avg) = RatingService.rateParkingUsingReservationOnce(parkingId, body)

            val response = ApiResponseWithData(
                success = true,
                message = "Rating registrado correctamente",
                data = RateParkingResponse(
                    parkingId = parkingId,
                    ratingCount = count,
                    avgRating = avg
                )
            )

            call.respond(HttpStatusCode.OK, response)

        } catch (e: IllegalStateException) {
            // Ya calificado
            call.respond(HttpStatusCode.Conflict, ApiResponse(false, "Reservation already rated"))

        } catch (e: Exception) {
            e.printStackTrace()
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiResponse(false, e.message ?: "Server error")
            )
        }
    }
}
