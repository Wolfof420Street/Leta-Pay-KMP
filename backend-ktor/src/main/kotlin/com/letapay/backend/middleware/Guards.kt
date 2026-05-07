/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.backend.middleware

import com.letapay.backend.error.KillSwitchActiveError
import com.letapay.backend.error.MissingIdempotencyKeyError
import com.letapay.backend.security.WalletPrincipal
import com.letapay.backend.service.IdempotencyReplay
import com.letapay.backend.service.IdempotencyService
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.application
import io.ktor.server.auth.principal
import io.ktor.server.request.path
import io.ktor.server.request.receiveText
import org.koin.ktor.ext.get
import java.security.MessageDigest

suspend fun ApplicationCall.killSwitchGuard() {
    val active = (
        System.getProperty("KILL_SWITCH_VALUE_MOVES")
            ?: System.getenv("KILL_SWITCH_VALUE_MOVES")
        )?.toBooleanStrictOrNull() ?: false
    if (active) {
        throw KillSwitchActiveError()
    }
}

suspend fun ApplicationCall.idempotencyGuard(): IdempotencyReplay? {
    val key = request.headers["Idempotency-Key"] ?: throw MissingIdempotencyKeyError()
    val principal = principal<WalletPrincipal>() ?: throw MissingIdempotencyKeyError()
    val endpoint = request.path()
    val payloadHash = sha256(receiveText())
    val idempotencyService = application.get<IdempotencyService>()
    val replay = idempotencyService.checkReplay(key, principal.walletAddress, endpoint, payloadHash)
    if (replay != null) {
        return replay
    }

    // Fix: register the key before side effects so concurrent retries collapse onto the first request.
    idempotencyService.registerKey(key, principal.walletAddress, endpoint, payloadHash)
    return null
}

private fun sha256(value: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray())
        .joinToString("") { byte -> "%02x".format(byte) }
