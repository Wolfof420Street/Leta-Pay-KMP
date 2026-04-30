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

import com.letapay.backend.error.UpstreamTimeoutError
import com.letapay.backend.service.PricingService
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.koin.ktor.ext.inject

fun Route.configurePriceRoutes() {
    val pricingService by inject<PricingService>()

    route("/prices") {
        get("/{chain}/{asset}") {
            val chain = requireNotNull(call.parameters["chain"])
            val asset = requireNotNull(call.parameters["asset"])
            try {
                call.respond(withTimeout(8_000L) { pricingService.quote(chain = chain, asset = asset) })
            } catch (_: TimeoutCancellationException) {
                throw UpstreamTimeoutError()
            }
        }
    }
}
