/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.backend.plugins

import com.letapay.backend.config.AppConfig
import com.letapay.backend.db.Sessions
import com.letapay.backend.security.JwtTokenService
import com.letapay.backend.security.SessionBootstrapRegistry
import com.letapay.backend.security.WalletPrincipal
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.ktor.ext.get

fun Application.configureSecurity() {
    val appConfig = get<AppConfig>()
    val tokenService = get<JwtTokenService>()

    install(Authentication) {
        jwt("session-auth") {
            verifier(tokenService.verifier)
            validate { credential ->
                val walletAddress = credential.payload.subject ?: return@validate null
                val sessionId = credential.payload.getClaim("sessionId").asString() ?: return@validate null
                if (!credential.payload.audience.contains(appConfig.jwtAudience)) {
                    return@validate null
                }
                if (!isActiveSession(sessionId, walletAddress)) {
                    null
                } else {
                    WalletPrincipal(walletAddress = walletAddress, sessionId = sessionId)
                }
            }
        }
    }
}

private fun isActiveSession(sessionId: String, walletAddress: String): Boolean {
    val now = System.currentTimeMillis()
    return transaction {
        val existing = Sessions.selectAll()
            .where {
                (Sessions.id eq sessionId) and
                    (Sessions.walletAddress eq walletAddress)
            }
            .singleOrNull()
        if (existing != null) {
            return@transaction existing[Sessions.revokedAt] == null && existing[Sessions.expiresAt] > now
        }

        if (!SessionBootstrapRegistry.consume(sessionId, walletAddress)) {
            return@transaction false
        }

        Sessions.insertIgnore {
            it[id] = sessionId
            it[Sessions.walletAddress] = walletAddress
            it[refreshTokenSelector] = "bootstrap-${sessionId.takeLast(26)}"
            it[refreshTokenHash] = "bootstrap-hash-$sessionId"
            it[familyId] = "bootstrap-family-${sessionId.takeLast(21)}"
            it[deviceFingerprint] = null
            it[expiresAt] = now + (30 * 60 * 1000L)
            it[revokedAt] = null
        }
        Sessions.selectAll()
            .where {
                (Sessions.id eq sessionId) and
                    (Sessions.walletAddress eq walletAddress)
            }
            .singleOrNull()
            ?.let { row -> row[Sessions.revokedAt] == null && row[Sessions.expiresAt] > now }
            ?: false
    }
}
