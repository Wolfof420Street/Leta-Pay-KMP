/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.backend

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.letapay.backend.security.SessionBootstrapRegistry

object TestAuthSessionFactory {
    fun issueToken(wallet: String, sessionId: String): String {
        SessionBootstrapRegistry.register(sessionId = sessionId, walletAddress = wallet)
        return JWT.create()
            .withIssuer("leta-pay-backend")
            .withAudience("leta-pay-clients")
            .withSubject(wallet)
            .withClaim("sessionId", sessionId)
            .sign(Algorithm.HMAC256("dev-jwt-secret-change-me"))
    }
}
