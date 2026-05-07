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

import com.letapay.backend.security.WalletPrincipal
import com.letapay.backend.service.ContactService
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.configureContactRoutes() {
    val contactService by inject<ContactService>()

    authenticate("session-auth") {
        route("/contacts") {
            get {
                val principal = requireNotNull(call.principal<WalletPrincipal>())
                call.respond(contactService.list(ownerWallet = principal.walletAddress))
            }
        }
    }
}
