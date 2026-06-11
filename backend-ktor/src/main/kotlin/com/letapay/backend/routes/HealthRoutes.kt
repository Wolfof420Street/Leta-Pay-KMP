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
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import org.koin.ktor.ext.inject

fun Route.configureHealthRoutes() {
    val healthService by inject<HealthService>()

    get("/health") {
        val snapshot = runCatching { healthService.status() }.getOrElse {
            com.letapay.backend.service.HealthSnapshot(
                status = "degraded",
                db = "degraded",
                redis = "degraded",
                sidecar = "degraded",
                firebase = if (RuntimeState.isFirebaseHealthy()) "ok" else "degraded",
            )
        }

        call.respond(
            if (snapshot.status == "ok" && !RuntimeState.isMisconfigured()) {
                HttpStatusCode.OK
            } else {
                HttpStatusCode.ServiceUnavailable
            },
            HealthResponse(
                status = if (RuntimeState.isMisconfigured()) "misconfigured" else snapshot.status,
                version = "1.0.0",
                db = snapshot.db,
                redis = snapshot.redis,
                sidecar = snapshot.sidecar,
                firebase = snapshot.firebase,
                timestamp = System.currentTimeMillis(),
            ),
        )
    }
}
