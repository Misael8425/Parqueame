// routes/RatingRoutes.kt
package com.parqueame.routes

import com.parqueame.controllers.RatingController
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.response.*

fun Route.registerRatingRoutes() {
    route("/api/parkings") {
        post("/{id}/ratings") { RatingController.postRate(call) }
    }
}
