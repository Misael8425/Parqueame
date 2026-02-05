package com.parqueame.services

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*              // HttpTimeout
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*                   // ByteReadChannel
import kotlinx.serialization.Serializable
import kotlinx.datetime.Clock
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.InsertManyOptions
import org.bson.Document
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

@Serializable
data class SyncReport(
    val ok: Boolean,
    val message: String,
    val inserted: Long = 0,
    val elapsedMs: Long = 0,
    val usedLimit: Int? = null
)

class DgiiSyncService(
    private val mongoClient: com.mongodb.client.MongoClient,
    private val dbName: String,
    private val sourceUrl: String,
    private val defaultLimit: Int = 1000
) {
    private val http = HttpClient(CIO) {
        // No explotes si el server responde 206/302/etc.
        expectSuccess = false
        followRedirects = true

        // Solo timeouts aquí (sin DefaultRequest)
        install(HttpTimeout) {
            requestTimeoutMillis = 5 * 60_000
            socketTimeoutMillis  = 5 * 60_000
            connectTimeoutMillis = 60_000
        }
    }

    // Headers por defecto para cada request
    private fun HttpRequestBuilder.applyCommonHeaders() {
        header(HttpHeaders.UserAgent, "Mozilla/5.0")
        header("Referer", "https://dgii.gov.do/app/WebApps/Consultas/RNC/")
        header(HttpHeaders.AcceptEncoding, "identity")
    }

    /** Ejecuta una corrida de sync (staging + swap). */
    suspend fun runOnce(limitOverride: Int? = null): SyncReport {
        val limit = limitOverride ?: defaultLimit
        val db = mongoClient.getDatabase(dbName)
        val liveName = "Contribuyentes"
        val stagingName = "Contribuyentes_staging"
        val started = System.currentTimeMillis()

        // 1) Descargar ZIP de forma resumible (streaming)
        val zipBytes = downloadZipResumable(sourceUrl)

        // 2) Abrir el primer CSV dentro del ZIP
        val csvStream = unzipFirstCsv(zipBytes)

        // 3) (Re)crear STAGING
        runCatching { db.getCollection(stagingName).drop() }
        db.createCollection(stagingName)
        val staging = db.getCollection(stagingName)

        // 4) Índices en STAGING
        staging.createIndex(Indexes.ascending("razonSocial"))

        // 5) Cargar CSV → STAGING
        val inserted = loadCsv(staging, csvStream, limit)

        // 6) SWAP: STAGING → LIVE (renameCollection)
        val okSwap = runCatching {
            renameCollection("${dbName}.$stagingName", "${dbName}.$liveName", dropTarget = true)
        }.isSuccess

        if (!okSwap) {
            // Plan B si Atlas no deja usar dropTarget
            runCatching { db.getCollection(liveName).drop() }
            renameCollection("${dbName}.$stagingName", "${dbName}.$liveName", dropTarget = false)
        }

        val elapsed = System.currentTimeMillis() - started
        return SyncReport(true, "Sync OK", inserted, elapsed, limit)
    }

    /** Descarga el ZIP en trozos de 8 MB usando Range, evita Content-Length mismatch. */
    private suspend fun downloadZipResumable(url: String): ByteArray {
        // intentar conocer longitud total
        var totalLength: Long? = null
        runCatching {
            val probe = http.get(url) {
                applyCommonHeaders()
                header(HttpHeaders.Range, "bytes=0-0")
            }
            val cr = probe.headers[HttpHeaders.ContentRange] // ej: bytes 0-0/24872022
            totalLength = if (!cr.isNullOrBlank() && cr.contains("/")) {
                cr.substringAfter("/").toLongOrNull()
            } else {
                probe.headers[HttpHeaders.ContentLength]?.toLongOrNull()
            }
        }

        val out = ByteArrayOutputStream(256 * 1024)
        var downloaded = 0L
        val chunkSize = 8L * 1024 * 1024 // 8 MB

        while (true) {
            val end = totalLength?.let { minOf(downloaded + chunkSize - 1, it - 1) }
            val rangeHeader = "bytes=$downloaded-${end ?: ""}"

            val resp = http.get(url) {
                applyCommonHeaders()
                header(HttpHeaders.Range, rangeHeader)
            }
            val ok = resp.status == HttpStatusCode.PartialContent || resp.status == HttpStatusCode.OK
            if (!ok) error("Descarga falló (HTTP ${resp.status}) con Range: $rangeHeader")

            val bytes = resp.bodyAsChannel().toByteArraySafe()
            if (bytes.isEmpty()) break

            out.write(bytes)
            downloaded += bytes.size

            if (totalLength != null && downloaded >= totalLength!!) break
            if (resp.status == HttpStatusCode.OK) break
        }

        return out.toByteArray()
    }

    /** Convierte un ByteReadChannel en ByteArray sin validar Content-Length. */
    private suspend fun ByteReadChannel.toByteArraySafe(): ByteArray {
        val out = ByteArrayOutputStream(64 * 1024)
        val buf = ByteArray(8192)
        while (!isClosedForRead) {
            val n = readAvailable(buf, 0, buf.size)
            if (n == -1) break
            if (n > 0) out.write(buf, 0, n)
        }
        return out.toByteArray()
    }

    /** Devuelve un InputStream posicionado en el primer .csv dentro del ZIP. */
    private fun unzipFirstCsv(zipBytes: ByteArray): java.io.InputStream {
        val zis = ZipInputStream(zipBytes.inputStream())
        generateSequence { zis.nextEntry }.forEach { e ->
            if (!e.isDirectory && e.name.lowercase().endsWith(".csv")) return zis
        }
        error("CSV no encontrado en el ZIP")
    }

    /** Inserta por lotes; mantiene _id = RNC (string). */
    private fun loadCsv(
        col: com.mongodb.client.MongoCollection<Document>,
        csvStream: java.io.InputStream,
        limit: Int
    ): Long {
        var inserted = 0L
        val fmt = CSVFormat.DEFAULT
            .withFirstRecordAsHeader()
            .withIgnoreHeaderCase()
            .withTrim()
            .withQuote('"')

        val parser = CSVParser(InputStreamReader(csvStream, Charsets.ISO_8859_1), fmt)
        val opts = InsertManyOptions().ordered(false)
        val batch = ArrayList<Document>(2000)
        var count = 0

        parser.use {
            for (rec in it) {
                val rnc = rec.get("RNC")?.filter(Char::isDigit).orEmpty()
                if (rnc.isEmpty()) continue

                val doc = Document("_id", rnc)
                    .append("razonSocial", rec.get("RAZÓN SOCIAL")?.trim().orEmpty())
                    .append("actividad", rec.get("ACTIVIDAD ECONÓMICA")?.trim().orEmpty())
                    .append("estatus", rec.get("ESTADO")?.trim().orEmpty())
                    .append("regimen", rec.get("RÉGIMEN DE PAGO")?.trim().orEmpty())
                    .append("updatedAt", Clock.System.now().toEpochMilliseconds())

                batch += doc
                count++

                if (limit > 0 && count >= limit) break
                if (batch.size >= 2000) {
                    col.insertMany(batch, opts); inserted += batch.size; batch.clear()
                }
            }
            if (batch.isNotEmpty()) {
                col.insertMany(batch, opts); inserted += batch.size; batch.clear()
            }
        }
        return inserted
    }

    /** renameCollection vía DB admin (driver sync). */
    private fun renameCollection(from: String, to: String, dropTarget: Boolean) {
        val admin = mongoClient.getDatabase("admin")
        val cmd = Document("renameCollection", from)
            .append("to", to)
            .append("dropTarget", dropTarget)
        admin.runCommand(cmd)
    }
}