package com.parqueame

import com.parqueame.models.Reservation
import com.parqueame.repositories.QrSessionRepository
import com.parqueame.repositories.ReservationRepository
import com.parqueame.routes.qrRoutes
import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch

fun Application.qrWiring() {
    // ✅ QrSessionRepository usa coroutine collection
    val qrRepo = QrSessionRepository(DatabaseFactory.qrSessionsCollection)

    // ✅ ReservationRepository espera MongoCollection<Reservation> (sync),
    // por eso utilizamos la colección síncrona:
    val reservationRepo = ReservationRepository(DatabaseFactory.reservationsCollectionSync)

    environment.monitor.subscribe(ApplicationStarted) {
        launch { qrRepo.ensureIndexes() } // si es suspend en qrRepo
        // Si tu ReservationRepository tiene ensureIndexes() no-suspend y sync, lo llamas sin launch.
        // reservationRepo.ensureIndexes()
    }

    val publicBaseUrl = System.getenv("PUBLIC_BASE_URL")
        ?: "https://parqueame-backend-production.up.railway.app"

    routing {
        qrRoutes(qrRepo, reservationRepo, publicBaseUrl)
    }
}