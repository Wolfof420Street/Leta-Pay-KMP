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

import com.letapay.backend.model.device.DeviceTokenDeleteRequest
import com.letapay.backend.model.device.DeviceTokenRequest
import com.letapay.backend.security.WalletPrincipal
import com.letapay.backend.service.DeviceTokenService
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.put
import org.koin.ktor.ext.inject

fun Route.configureDeviceTokenRoutes() {
    val deviceTokenService by inject<DeviceTokenService>()

    authenticate("session-auth") {
        put("/device-token") {
            val principal = requireNotNull(call.principal<WalletPrincipal>())
            val body = call.receive<DeviceTokenRequest>()
            deviceTokenService.upsert(principal.walletAddress, body.fcmToken, body.platform)
            call.respond(HttpStatusCode.NoContent)
        }

        delete("/device-token") {
            val principal = requireNotNull(call.principal<WalletPrincipal>())
            val body = call.receive<DeviceTokenDeleteRequest>()
            deviceTokenService.deactivate(principal.walletAddress, body.fcmToken)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
