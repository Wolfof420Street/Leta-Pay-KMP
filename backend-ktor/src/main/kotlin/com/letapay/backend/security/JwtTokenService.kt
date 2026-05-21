/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.backend.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.JWTVerifier
import com.letapay.app.core.domain.JwtKeyProvider
import com.letapay.backend.config.AppConfig
import com.letapay.backend.model.auth.JwkKey
import com.letapay.backend.model.auth.JwksResponse
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Instant
import java.util.Base64
import java.util.Date

class JwtTokenService(
    private val config: AppConfig,
    private val keyProvider: JwtKeyProvider,
) {
    private val algorithm = Algorithm.RSA256(
        keyProvider.publicKey as RSAPublicKey,
        keyProvider.privateKey as RSAPrivateKey,
    )

    val verifier: JWTVerifier = JWT.require(algorithm)
        .withIssuer(config.jwtIssuer)
        .withAudience(config.jwtAudience)
        .build()

    fun mintSessionToken(walletAddress: String, sessionId: String, expiresAt: Instant): String =
        JWT.create()
            .withIssuer(config.jwtIssuer)
            .withAudience(config.jwtAudience)
            .withSubject(walletAddress)
            .withClaim("sessionId", sessionId)
            .withExpiresAt(Date.from(expiresAt))
            .sign(algorithm)

    fun getJwks(): JwksResponse {
        val publicKey = keyProvider.publicKey as RSAPublicKey
        return JwksResponse(
            keys = listOf(
                JwkKey(
                    kty = "RSA",
                    kid = "default",
                    use = "sig",
                    alg = "RS256",
                    n = Base64.getUrlEncoder().withoutPadding().encodeToString(publicKey.modulus.toByteArray()),
                    e = Base64.getUrlEncoder().withoutPadding().encodeToString(publicKey.publicExponent.toByteArray()),
                ),
            ),
        )
    }
}
