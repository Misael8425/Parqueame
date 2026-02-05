package com.example.parqueame.repository

import com.example.parqueame.api.RetrofitInstance
import com.example.parqueame.models.CreateReservationRequest
import com.example.parqueame.models.ParkingAvailabilityResponse
import com.example.parqueame.models.ReservationDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReservationRepository(
    private val api: com.example.parqueame.api.ApiService = RetrofitInstance.apiService
) {

    suspend fun createReservation(
        xUserId: String,
        body: CreateReservationRequest
    ): Result<ReservationDto> = withContext(Dispatchers.IO) {
        return@withContext try {
            val resp: retrofit2.Response<ReservationDto> =
                api.createReservation(userId = xUserId, body = body)
            resp.toKotlinResult()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkAvailability(
        parkingId: String,
        startMin: Long,
        endMin: Long
    ): kotlin.Result<ParkingAvailabilityResponse> = withContext(Dispatchers.IO) {
        return@withContext try {
            val resp: retrofit2.Response<ParkingAvailabilityResponse> =
                api.checkAvailability(parkingId, startMin, endMin)
            resp.toKotlinResult()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getActiveByUser(userId: String): Result<List<ReservationDto>> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val resp: retrofit2.Response<List<ReservationDto>> =
                    api.getActiveReservationsByUser(userId)
                resp.toKotlinResult()
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getActiveByParking(parkingId: String): Result<List<ReservationDto>> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val resp: retrofit2.Response<List<ReservationDto>> =
                    api.getActiveReservationsByParking(parkingId)
                resp.toKotlinResult()
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}

/** Extension con tipos completamente calificados para eliminar cualquier ambigüedad. */
private fun <T> retrofit2.Response<T>.toKotlinResult(): Result<T> {
    return if (this.isSuccessful) {
        val body = this.body()
        if (body != null) {
            Result.success(body)
        } else {
            Result.failure(IllegalStateException("Respuesta vacía"))
        }
    } else {
        val msg = this.errorBody()?.string() ?: "HTTP ${this.code()}"
        Result.failure(RuntimeException(msg))
    }
}
