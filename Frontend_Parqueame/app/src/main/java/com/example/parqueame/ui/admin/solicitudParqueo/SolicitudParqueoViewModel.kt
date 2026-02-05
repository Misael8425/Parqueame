package com.example.parqueame.ui.admin.solicitudParqueo

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parqueame.api.CloudinaryInstance
import com.example.parqueame.api.RetrofitInstance
import com.example.parqueame.models.CreateParkingLotRequest
import com.example.parqueame.models.ScheduleRange
import com.example.parqueame.models.WeekDay
import com.example.parqueame.utils.strPart
import com.example.parqueame.utils.uriToMultipart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SolicitudParqueoViewModel : ViewModel() {

    private val uploadPreset = "parqueame_preset"
    private val photosFolder = "parqueame/parking_lots"
    private val infraFolder  = "parqueame/parking_lots/infra"

    private suspend fun uploadPhotos(context: Context, uris: List<Uri>): List<String> = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val urls = mutableListOf<String>()
        for (uri in uris) {
            val part = uriToMultipart(resolver, uri, "file")
            val resp = CloudinaryInstance.api.subirImagen(
                file = part,
                uploadPreset = strPart(uploadPreset),
                folder = strPart(photosFolder)
            )
            if (resp.isSuccessful && resp.body()?.secure_url != null) {
                urls += resp.body()!!.secure_url!!
            } else {
                throw IllegalStateException("Error subiendo imagen a Cloudinary: ${resp.code()} ${resp.errorBody()?.string()}")
            }
        }
        urls
    }

    private suspend fun uploadInfraDoc(context: Context, uri: Uri?): String? = withContext(Dispatchers.IO) {
        if (uri == null) return@withContext null
        val resolver = context.contentResolver
        val part = uriToMultipart(resolver, uri, "file")
        val resp = CloudinaryInstance.api.subirArchivoRaw(
            file = part,
            uploadPreset = strPart(uploadPreset),
            folder = strPart(infraFolder)
        )
        if (resp.isSuccessful && resp.body()?.secure_url != null) {
            resp.body()!!.secure_url
        } else {
            throw IllegalStateException("Error subiendo documento a Cloudinary: ${resp.code()} ${resp.errorBody()?.string()}")
        }
    }

    /**
     * Envía la solicitud al backend con headers de identidad del dueño.
     * Aquí se fija solicitudTipo = "Apertura" para auditar la creación.
     */
    fun submitSolicitud(
        context: Context,
        userId: String,
        localName: String,
        address: String,
        capacity: String,
        priceHour: String,
        days: List<WeekDay>,
        horarios: List<Pair<String, String>>,
        characteristics: Set<String>,
        photoUris: List<Uri>,
        infraDocUri: Uri?,
        lat: Double? = null,
        lng: Double? = null,
        ownerDocumento: String,        // RNC o Cédula
        ownerTipo: String,             // "RNC" o "CEDULA"
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val photoUrls = uploadPhotos(context, photoUris)
                val infraUrl  = uploadInfraDoc(context, infraDocUri)

                val ranges = horarios.map { (open, close) -> ScheduleRange(open = open, close = close) }

                val body = CreateParkingLotRequest(
                    localName = localName.trim(),
                    address = address.trim(),
                    capacity = capacity.toInt(),
                    priceHour = priceHour.toInt(),
                    daysOfWeek = days,
                    schedules = ranges,
                    characteristics = characteristics.toList(),
                    photos = photoUrls,
                    infraDocUrl = infraUrl,
                    lat = lat,
                    lng = lng,
                    solicitudTipo = "Apertura" // 👈 IMPORTANTE: se guarda en la BD, no se muestra en UI
                )

                val resp = RetrofitInstance.apiService.crearParqueo(
                    userId = userId,
                    ownerDocumento = ownerDocumento,
                    ownerTipo = ownerTipo,
                    body = body
                )

                if (resp.isSuccessful) {
                    Toast.makeText(context, "Parqueo registrado", Toast.LENGTH_SHORT).show()
                    onSuccess()
                } else {
                    onError("Error backend: ${resp.code()} - ${resp.errorBody()?.string() ?: "desconocido"}")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Error inesperado")
            }
        }
    }
}