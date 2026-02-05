package com.parqueame

import com.parqueame.repositories.ParkingRepository
import com.parqueame.repositories.ReservationRepository
import com.parqueame.routes.reservationRoutes
import com.parqueame.services.ReservationService
import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch

fun Application.reservationsWiring() {
    // ✅ Reusar las colecciones síncronas centralizadas en DatabaseFactory
    val reservationsCol = DatabaseFactory.reservationsCollectionSync
    val parkingsCol     = DatabaseFactory.parkingsCollectionSync

    val resRepo  = ReservationRepository(reservationsCol)
    val parkRepo = ParkingRepository(parkingsCol)

    environment.monitor.subscribe(ApplicationStarted) {
        launch { resRepo.ensureIndexes() }
    }

    val service = ReservationService(resRepo, parkRepo)

    routing {
        reservationRoutes(service)
    }
}