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
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
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

    init {
        require((privateKey as RSAPrivateKey).modulus.bitLength() >= 2048) {
            "JWT_PRIVATE_KEY must be an RSA key with a modulus of at least 2048 bits."
        }
        require((publicKey as RSAPublicKey).modulus.bitLength() >= 2048) {
            "JWT_PUBLIC_KEY must be an RSA key with a modulus of at least 2048 bits."
        }
    }

    private fun parsePrivateKey(pem: String): PrivateKey {
        val cleanPem = pem.requirePem("JWT_PRIVATE_KEY", "PRIVATE KEY")
        return try {
            val encoded = Base64.getDecoder().decode(cleanPem)
            KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(encoded))
        } catch (exception: Exception) {
            throw IllegalStateException("JWT_PRIVATE_KEY is not a valid PKCS#8 RSA PEM key.", exception)
        }
    }

    private fun parsePublicKey(pem: String): PublicKey {
        val cleanPem = pem.requirePem("JWT_PUBLIC_KEY", "PUBLIC KEY")
        return try {
            val encoded = Base64.getDecoder().decode(cleanPem)
            KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(encoded))
        } catch (exception: Exception) {
            throw IllegalStateException("JWT_PUBLIC_KEY is not a valid X.509 RSA PEM key.", exception)
        }
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

private fun String.requirePem(envName: String, marker: String): String {
    val begin = "-----BEGIN $marker-----"
    val end = "-----END $marker-----"
    require(contains(begin) && contains(end)) {
        "$envName must be a PEM string containing $begin and $end."
    }
    return replace(begin, "")
        .replace(end, "")
        .replace("\\s".toRegex(), "")
}
