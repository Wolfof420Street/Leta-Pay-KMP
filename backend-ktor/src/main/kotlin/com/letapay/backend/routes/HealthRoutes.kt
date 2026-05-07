/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.backend.routes

import com.letapay.backend.config.RuntimeState
import com.letapay.backend.model.HealthResponse
import com.letapay.backend.service.HealthService
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.ktor.ext.inject

fun Route.configureHealthRoutes() {
    val healthService by inject<HealthService>()

    get("/health") {
        val dbStatus = withTimeoutOrNull(1_000L) {
            healthService.dbStatus()
        } ?: "degraded"

        val firebaseStatus = withTimeoutOrNull(1_000L) {
            healthService.firebaseStatus()
        } ?: "degraded"

        call.respond(
            HealthResponse(
                status = if (RuntimeState.isMisconfigured()) "misconfigured" else "ok",
                version = "1.0.0",
                db = dbStatus,
                firebase = firebaseStatus,
                timestamp = System.currentTimeMillis(),
            ),
        )
    }
}
