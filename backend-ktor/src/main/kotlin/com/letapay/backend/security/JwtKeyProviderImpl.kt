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

import com.letapay.app.core.domain.JwtKeyProvider
import com.letapay.backend.config.AppConfig
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

class RsaJwtKeyProvider(config: AppConfig) : JwtKeyProvider {
    override val privateKey: PrivateKey = parsePrivateKey(
        requireNotNull(config.jwtPrivateKey) { "JWT_PRIVATE_KEY missing" },
    )
    override val publicKey: PublicKey = parsePublicKey(
        requireNotNull(config.jwtPublicKey) { "JWT_PUBLIC_KEY missing" },
    )

    private fun parsePrivateKey(pem: String): PrivateKey {
        val cleanPem = pem.replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")
        val encoded = Base64.getDecoder().decode(cleanPem)
        val keySpec = PKCS8EncodedKeySpec(encoded)
        return KeyFactory.getInstance("RSA").generatePrivate(keySpec)
    }

    private fun parsePublicKey(pem: String): PublicKey {
        val cleanPem = pem.replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\\s".toRegex(), "")
        val encoded = Base64.getDecoder().decode(cleanPem)
        val keySpec = X509EncodedKeySpec(encoded)
        return KeyFactory.getInstance("RSA").generatePublic(keySpec)
    }
}

class DevJwtKeyProvider : JwtKeyProvider {
    private val keyPair = sharedKeyPair

    override val privateKey: PrivateKey = keyPair.private
    override val publicKey: PublicKey = keyPair.public

    private companion object {
        private val sharedKeyPair = KeyPairGenerator.getInstance("RSA").apply {
            initialize(2048)
        }.generateKeyPair()
    }
}
