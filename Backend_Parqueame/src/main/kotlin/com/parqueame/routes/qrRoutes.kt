package com.parqueame.routes

import com.parqueame.dto.*
import com.parqueame.repositories.QrSessionRepository
import com.parqueame.repositories.ReservationRepository
import com.parqueame.models.QrSession
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.UUID

fun Route.qrRoutes(
    qrRepo: QrSessionRepository,
    reservationRepo: ReservationRepository,
    publicBaseUrl: String // p. ej. "https://parqueame-backend-production.up.railway.app"
) {

    route("/qr") {
        // 1) Crea el token para una reserva y devuelve la URL a incrustar en el QR
        post("/create") {
            val req = call.receive<QrCreateRequest>()

            // (opcional) validar que exista esa reserva
            val r = reservationRepo.findById(req.reservationId)
                ?: return@post call.respond(HttpStatusCode.NotFound, mapOf("message" to "Reserva no encontrada"))

            // TTL y expiración
            val ttlMs = (req.ttlMinutes.coerceIn(1, 120)) * 60_000L
            val expiresAt = Instant.now().toEpochMilli() + ttlMs

            val token = UUID.randomUUID().toString()
            qrRepo.insert(
                QrSession(
                    token = token,
                    reservationId = r._id.toHexString(),
                    parkingId = r.parkingId,
                    userId = r.userId,
                    expiresAt = expiresAt
                )
            )

            val url = "$publicBaseUrl/qr/$token"
            call.respond(QrCreateResponse(token, url))
        }

        // 2) Página HTML con botón "Validar entrada"
        get("/{token}") {
            val token = call.parameters["token"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Token requerido")
            val s = qrRepo.findByToken(token) ?: return@get call.respond(HttpStatusCode.NotFound, "QR no encontrado")

            val now = Instant.now().toEpochMilli()
            val allowed = now <= s.expiresAt
            val preMsg = if (!allowed) "QR expirado" else if (s.validatedAt != null) "QR ya validado" else "QR listo para validar"

            val html = """
                <!doctype html>
                <html lang="es">
                <head>
                  <meta charset="utf-8"/>
                  <meta name="viewport" content="width=device-width, initial-scale=1"/>
                  <title>Validar entrada</title>
                  <style>
                    body { font-family: system-ui, -apple-system, Segoe UI, Roboto, Ubuntu, Cantarell, "Fira Sans", "Droid Sans", "Helvetica Neue", Arial; 
                           background:#f7f9fc; margin:0; display:flex; align-items:center; justify-content:center; min-height:100vh; }
                    .card { background:#fff; border-radius:16px; box-shadow:0 6px 20px rgba(0,0,0,.06); padding:24px; max-width:420px; width:90%; text-align:center; }
                    h1 { font-size:20px; margin:0 0 8px; }
                    p { color:#475569; margin:8px 0 16px; }
                    button { appearance:none; border:none; background:#0B66FF; color:#fff; padding:12px 16px; border-radius:12px; font-weight:600; cursor:pointer; width:100%; }
                    button[disabled] { background:#94a3b8; cursor:not-allowed; }
                    .ok { color:#0B66FF; font-weight:600; }
                    .err { color:#ef4444; font-weight:600; }
                    .link { margin-top:14px; display:inline-block; color:#0B66FF; text-decoration:none; }
                  </style>
                </head>
                <body>
                  <div class="card">
                    <h1>Validar entrada</h1>
                    <p id="pre">$preMsg</p>
                    <button id="btn" ${if (!allowed || s.validatedAt != null) "disabled" else ""}>Validar entrada</button>
                    <div id="result" style="margin-top:12px;"></div>
                    <a id="continue" class="link" href="#" style="display:none;">Continuar en la app</a>
                  </div>
                  <script>
  btn?.addEventListener('click', async () => {
    btn.disabled = true;
    resDiv.textContent = 'Validando...';
    try {
      const resp = await fetch('/qr/confirm/' + encodeURIComponent(token), { method: 'POST' });

      let data;
      try { data = await resp.json(); }
      catch { data = { valid:false, message: await resp.text().catch(()=> 'Error') }; }

      if (resp.ok && data.valid) {
        resDiv.innerHTML = '<span class="ok">' + (data.message || 'Validado') + '</span>';
        const dl = 'parqueame://app/reservation/validated?token=' + encodeURIComponent(token);
        cont.href = dl; cont.style.display = 'inline-block';
      } else {
        // Muestra el motivo (incluye 403: expirado)
        const msg = data?.message || ('Error ' + resp.status);
        resDiv.innerHTML = '<span class="err">' + msg + '</span>';
        btn.disabled = false;
      }
    } catch (e) {
      resDiv.innerHTML = '<span class="err">Error de red</span>';
      btn.disabled = false;
    }
  });
</script>

                </body>
                </html>
            """.trimIndent()

            call.respondText(html, ContentType.Text.Html)
        }

        // 3) Confirmar (validar) el QR
        post("/confirm/{token}") {
            val token = call.parameters["token"] ?: return@post call.respond(HttpStatusCode.BadRequest, QrValidateResponse(false, "Token requerido"))
            val s = qrRepo.findByToken(token) ?: return@post call.respond(HttpStatusCode.NotFound, QrValidateResponse(false, "QR no encontrado"))

            val now = Instant.now().toEpochMilli()
            if (now > s.expiresAt) return@post call.respond(HttpStatusCode.Forbidden, QrValidateResponse(false, "QR expirado"))
            if (s.validatedAt != null) return@post call.respond(HttpStatusCode.Conflict, QrValidateResponse(false, "QR ya validado"))

            qrRepo.markValidated(token)
            call.respond(QrValidateResponse(true, "Reserva validada. Abrir puerta."))
        }

        // 4) Estado para polling desde la app
        // GET /qr/status/{token}
        get("/status/{token}") {
            val token = call.parameters["token"] ?: return@get call.respond(
                HttpStatusCode.BadRequest, QrStatusResponse("", false, "Token requerido")
            )
            val s = qrRepo.findByToken(token) ?: return@get call.respond(
                HttpStatusCode.NotFound, QrStatusResponse(token, false, "QR no encontrado")
            )

            val now = Instant.now().toEpochMilli()
            if (now > s.expiresAt) {
                return@get call.respond(QrStatusResponse(token, false, "QR expirado"))
            }
            val validated = s.validatedAt != null
            val msg = if (validated) "Validado" else "En espera"
            call.respond(
                QrStatusResponse(
                    token = token,
                    validated = validated,
                    message = msg,
                    reservationId = if (validated) s.reservationId else null
                )
            )
        }
    }
}
