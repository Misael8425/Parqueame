package com.parqueame.services

import com.parqueame.models.Contribuyente
import com.parqueame.models.DgiiRncResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.litote.kmongo.`in`
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.or

class DgiiService(
    private val contribuyentes: CoroutineCollection<Contribuyente>
) {

    /**
     * Busca un RNC/Cédula con soporte a:
     * - 9 dígitos (RNC) y 11 dígitos (cédula)
     * - padding a 11 con ceros
     * - variante sin ceros iniciales
     */
    suspend fun consultarRnc(rncInput: String): DgiiRncResponse? = withContext(Dispatchers.IO) {
        try {
            val digits = rncInput.filter(Char::isDigit)

            println("=== BÚSQUEDA DGII ===")
            println("Input: '$rncInput' | Solo dígitos: '$digits'")

            if (!isValidRncFormat(digits)) {
                println("❌ Formato inválido")
                return@withContext null
            }

            val candidates = buildSearchCandidates(digits)
            println("Candidatos: $candidates")

            val doc = contribuyentes.findOne(
                or(Contribuyente::_id `in` candidates)
            )

            if (doc != null) {
                println("✅ ENCONTRADO: ${doc._id} - ${doc.razonSocial}")
                return@withContext DgiiRncResponse(
                    rnc = doc._id,
                    razonSocial = doc.razonSocial,
                    actividadEconomica = doc.actividadEconomica,
                    estado = doc.estado,
                    fechaInicioOperaciones = doc.fechaInicioOperaciones,
                    regimenPago = doc.regimenPago
                )
            }

            println("❌ NO ENCONTRADO")
            null
        } catch (e: Exception) {
            println("❌ ERROR en consultarRnc: ${e.message}")
            null
        }
    }

    /** Genera variantes para cubrir ambos formatos coexistentes en tu BD. */
    private fun buildSearchCandidates(digits: String): List<String> {
        val set = mutableSetOf<String>()

        // Valor exacto digitado
        set.add(digits)

        // Si son 9 dígitos → probar versión a 11 con ceros a la izquierda
        if (digits.length == 9) {
            set.add(digits.padStart(11, '0'))
        }

        // Si son 11 dígitos → probar versión sin ceros iniciales
        if (digits.length == 11) {
            set.add(digits.trimStart('0'))
        }

        return set.toList()
    }

    /** 9 o 11 dígitos, no todo ceros, numérico válido. */
    private fun isValidRncFormat(d: String): Boolean {
        return (d.length == 9 || d.length == 11) &&
                d.all { it.isDigit() } &&
                d.toLongOrNull() != null &&
                d != "0".repeat(d.length)
    }

    /**
     * Estadísticas útiles para debug.
     * Mantiene el endpoint /dgii/stats funcionando.
     */
    suspend fun getDatabaseStats(): Map<String, Any> = withContext(Dispatchers.IO) {
        try {
            val total = contribuyentes.countDocuments()
            val sample = contribuyentes.find().limit(10).toList()

            val idLengthDist = mutableMapOf<Int, Int>()
            sample.forEach { s ->
                val len = s._id.length
                idLengthDist[len] = (idLengthDist[len] ?: 0) + 1
            }

            mapOf(
                "total_documents" to total,
                "sample_ids" to sample.map { it._id },
                "id_length_distribution" to idLengthDist
            )
        } catch (e: Exception) {
            mapOf("error" to (e.message ?: e.toString()))
        }
    }
}
