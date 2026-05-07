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

import com.letapay.backend.service.RateLimiterService
import io.ktor.server.application.ApplicationCall
import io.ktor.util.AttributeKey

private val preAuthWalletAddressKey = AttributeKey<String>("preAuthWalletAddress")

fun ApplicationCall.attachPreAuthWallet(walletAddress: String) {
    attributes.put(preAuthWalletAddressKey, walletAddress.lowercase())
}

fun ApplicationCall.enforcePreAuthRateLimit(rateLimiter: RateLimiterService) {
    val ip = request.local.remoteHost
        .trim()
        .ifBlank { "unknown-ip" }
    val wallet = attributes.getOrNull(preAuthWalletAddressKey) ?: "unknown-wallet"
    rateLimiter.enforce("preauth:ip:$ip", limit = 30, windowMs = 60_000)
    rateLimiter.enforce("preauth:wallet:$wallet", limit = 10, windowMs = 60_000)
}

fun ApplicationCall.enforceGlobalAndWalletRateLimit(rateLimiter: RateLimiterService, walletAddress: String) {
    rateLimiter.enforce("global:all", limit = 10, windowMs = 1_000)
    rateLimiter.enforce("wallet:${walletAddress.lowercase()}", limit = 100, windowMs = 60_000)
}
