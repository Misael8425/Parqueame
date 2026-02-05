package com.example.parqueame.repository

import com.example.parqueame.api.RetrofitInstance
import com.example.parqueame.models.CreateCommentRequest
import com.example.parqueame.models.CreateParkingLotRequest
import com.example.parqueame.models.CreateParkingLotRequestDto
import com.example.parqueame.models.ParkingCommentDto
import com.example.parqueame.models.ParkingLotDto
import com.example.parqueame.models.Parqueo
import com.example.parqueame.models.UpdateParkingStatusRequest
import com.example.parqueame.models.UsuarioPropietario
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ParkingRepository(
    private val api: com.example.parqueame.api.ApiService = RetrofitInstance.apiService
) {

    // -------------------- CREATE --------------------
    suspend fun createParking(
        body: CreateParkingLotRequestDto,
        xUserId: String? = null,
        ownerDocumento: String? = null,
        ownerTipo: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            // La API POST usa CreateParkingLotRequest (compat) con solicitudTipo
            val compat = CreateParkingLotRequest(
                localName = body.localName,
                address = body.address,
                capacity = body.capacity,
                priceHour = body.priceHour,
                daysOfWeek = body.daysOfWeek,
                schedules = body.schedules,
                characteristics = body.characteristics,
                photos = body.photos,
                infraDocUrl = body.infraDocUrl,
                lat = body.lat,
                lng = body.lng,
                solicitudTipo = body.solicitudTipo   // <--- se envía
            )

            val resp = api.crearParqueo(
                userId = xUserId ?: "",
                ownerDocumento = ownerDocumento,
                ownerTipo = ownerTipo,
                body = compat
            )
            if (resp.isSuccessful) {
                val id = resp.body()?.id
                if (!id.isNullOrBlank()) Result.success(id)
                else Result.failure(IllegalStateException("Respuesta sin id"))
            } else {
                Result.failure(RuntimeException(resp.errorBody()?.string() ?: "Error ${resp.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -------------------- READ (DTO) --------------------
    suspend fun getParkingByIdDto(id: String): Result<ParkingLotDto> = withContext(Dispatchers.IO) {
        return@withContext try {
            val resp = api.getParkingByIdDto(id)
            if (resp.isSuccessful) {
                val body = resp.body()
                if (body != null) Result.success(body) else Result.failure(IllegalStateException("Body nulo"))
            } else {
                Result.failure(RuntimeException(resp.errorBody()?.string() ?: "Error ${resp.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -------------------- READ (modelo interno Parqueo) --------------------
    suspend fun getParkingById(id: String): Result<Parqueo> = withContext(Dispatchers.IO) {
        return@withContext try {
            val resp = api.obtenerParqueoId(id)
            if (resp.isSuccessful) {
                val dto = resp.body()
                if (dto != null) {
                    Result.success(dtoToParqueo(dto))
                } else {
                    Result.failure(IllegalStateException("Body nulo"))
                }
            } else {
                Result.failure(RuntimeException(resp.errorBody()?.string() ?: "Error ${resp.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -------------------- COMMENTS --------------------
    suspend fun getParkingComments(id: String): Result<List<ParkingCommentDto>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val resp = api.obtenerComentarios(id)
            if (resp.isSuccessful) {
                Result.success(resp.body().orEmpty())
            } else {
                Result.failure(RuntimeException(resp.errorBody()?.string() ?: "Error ${resp.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addParkingComment(id: String, request: CreateCommentRequest): Result<Unit> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val resp = api.agregarComentario(id, request)
                if (resp.isSuccessful) Result.success(Unit)
                else Result.failure(RuntimeException(resp.errorBody()?.string() ?: "Error ${resp.code()}"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // -------------------- STATUS --------------------
    suspend fun setParkingStatus(
        id: String,
        status: String,
        reason: String? = null,
        solicitudTipo: String? = null          // <--- se acepta
    ): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val body = UpdateParkingStatusRequest(
                status = status,
                rejectionReason = reason,
                solicitudTipo = solicitudTipo     // <--- se envía
            )
            val resp = api.setParkingStatus(id = id, status = status, body = body)
            if (resp.isSuccessful) Result.success(Unit)
            else Result.failure(RuntimeException(resp.errorBody()?.string() ?: "Error ${resp.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -------------------- UPDATE (PUT) --------------------
    suspend fun updateParking(
        id: String,
        body: CreateParkingLotRequestDto,
        xUserId: String? = null,
        ownerDocumento: String? = null,
        ownerTipo: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            // En PUT usamos directamente el DTO con solicitudTipo="Edicion"
            val resp = api.updateParking(
                id = id,
                body = body,
                xUserId = xUserId,
                ownerDocumento = ownerDocumento,
                ownerTipo = ownerTipo
            )
            if (resp.isSuccessful) Result.success(Unit)
            else Result.failure(RuntimeException(resp.errorBody()?.string() ?: "Error ${resp.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -------------------- Mapper DTO -> Parqueo --------------------
    private fun dtoToParqueo(dto: ParkingLotDto): Parqueo {
        val estadoBool: Boolean = when (dto.status.lowercase()) {
            "approved", "activo" -> true
            else -> false
        }
        val ubicacionPair: Pair<Double, Double> = dto.location?.let {
            Pair(it.lat ?: 0.0, it.lng ?: 0.0) // (lat, lng)
        } ?: Pair(0.0, 0.0)

        val usuarioPropietario = UsuarioPropietario(
            usuarioId = dto.createdBy.orEmpty(),
            usuarioTipo = "propietarios"
        )

        return Parqueo(
            id = dto.id,
            nombre = dto.localName,
            direccion = dto.address,
            capacidad = dto.capacity,
            ubicacion = ubicacionPair,
            tarifaHora = dto.priceHour.toFloat(),
            imagenesURL = dto.photos,
            estado = estadoBool,
            usuario = usuarioPropietario
        )
    }
}