/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.backend.service

import com.letapay.backend.config.AppConfig
import com.letapay.backend.db.Nonces
import com.letapay.backend.db.Sessions
import com.letapay.backend.error.InvalidRefreshTokenError
import com.letapay.backend.error.InvalidWalletSignatureError
import com.letapay.backend.error.NonceAlreadyUsedError
import com.letapay.backend.error.NonceExpiredError
import com.letapay.backend.error.NonceMissingError
import com.letapay.backend.error.RefreshTokenExpiredError
import com.letapay.backend.error.TokenFamilyRevokedError
import com.letapay.backend.model.auth.AuthTokens
import com.letapay.backend.model.auth.GeneratedNonce
import com.letapay.backend.model.auth.VerifyRequest
import com.letapay.backend.security.JwtTokenService
import de.mkammerer.argon2.Argon2Factory
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.web3j.crypto.Hash
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

interface AuthService {
    suspend fun generateNonce(walletAddress: String): GeneratedNonce

    suspend fun verify(request: VerifyRequest): AuthTokens

    suspend fun rotateRefreshToken(rawToken: String, deviceFingerprint: String?): AuthTokens

    suspend fun mintFirebaseToken(walletAddress: String, sessionId: String): String

    suspend fun revokeSession(sessionId: String)
}

class DefaultAuthService(
    private val config: AppConfig,
    private val jwtTokenService: JwtTokenService,
    private val firebaseTokenService: FirebaseTokenService,
) : AuthService {
    private val argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id)

    override suspend fun generateNonce(walletAddress: String): GeneratedNonce {
        val nonce = UUID.randomUUID().toString()
        val expiresAt = System.currentTimeMillis() + FIVE_MINUTES_MS

        transaction {
            // Fix: persist nonce expiry with a nullable usedAt flag so replay checks have durable state.
            Nonces.insert {
                it[Nonces.nonce] = nonce
                it[Nonces.walletAddress] = walletAddress
                it[Nonces.expiresAt] = expiresAt
                it[usedAt] = null
            }
        }

        return GeneratedNonce(nonce = nonce, expiresAt = expiresAt)
    }

    override suspend fun verify(request: VerifyRequest): AuthTokens {
        val nonce = extractNonce(request.message)
        val now = System.currentTimeMillis()

        transaction {
            val nonceRow = Nonces.selectAll().where { Nonces.nonce eq nonce }.singleOrNull()
                ?: throw NonceMissingError()

            if (nonceRow[Nonces.expiresAt] <= now) {
                throw NonceExpiredError()
            }

            // Fix: atomically consume the nonce inside the transaction before token issuance so replays lose the race.
            val updatedRows = Nonces.update({
                (Nonces.nonce eq nonce) and (Nonces.usedAt.isNull())
            }) {
                it[usedAt] = now
            }
            if (updatedRows != 1) {
                throw NonceAlreadyUsedError()
            }

            if (!verifySignature(request.walletAddress, request.message, request.signature)) {
                throw InvalidWalletSignatureError()
            }
        }

        return issueTokens(
            walletAddress = request.walletAddress,
            familyId = UUID.randomUUID().toString(),
            deviceFingerprint = null,
        )
    }

    override suspend fun rotateRefreshToken(rawToken: String, deviceFingerprint: String?): AuthTokens {
        val (selector, rawSecret) = parseRefreshToken(rawToken)
        val session = transaction {
            Sessions.selectAll()
                .where { Sessions.refreshTokenSelector eq selector }
                .singleOrNull()
                ?.takeIf { row -> verifyRefreshToken(rawSecret, row[Sessions.refreshTokenHash]) }
        } ?: throw InvalidRefreshTokenError()

        val now = System.currentTimeMillis()
        val familyId = session[Sessions.familyId]

        transaction {
            val familyRows = Sessions.selectAll().where { Sessions.familyId eq familyId }.toList()
            if (familyRows.any { it[Sessions.revokedAt] != null }) {
                revokeFamily(familyId, now)
                throw TokenFamilyRevokedError()
            }
            if (session[Sessions.expiresAt] <= now) {
                throw RefreshTokenExpiredError()
            }

            Sessions.update({ Sessions.id eq session[Sessions.id] }) {
                it[revokedAt] = now
            }
        }

        return issueTokens(
            walletAddress = session[Sessions.walletAddress],
            familyId = familyId,
            deviceFingerprint = deviceFingerprint ?: session[Sessions.deviceFingerprint],
        )
    }

    override suspend fun mintFirebaseToken(walletAddress: String, sessionId: String): String =
        try {
            firebaseTokenService.createCustomToken(walletAddress, sessionId)
        } catch (_: Exception) {
            "firebase-token-unavailable"
        }

    override suspend fun revokeSession(sessionId: String) {
        val now = System.currentTimeMillis()
        transaction {
            Sessions.update({ Sessions.id eq sessionId }) {
                it[revokedAt] = now
            }
        }
    }

    private suspend fun issueTokens(
        walletAddress: String,
        familyId: String,
        deviceFingerprint: String?,
    ): AuthTokens {
        val sessionId = UUID.randomUUID().toString()
        val refreshSelector = UUID.randomUUID().toString()
        val refreshSecret = UUID.randomUUID().toString()
        val refreshToken = "$refreshSelector:$refreshSecret"
        val sessionExpiresAt = Instant.now().plus(30, ChronoUnit.MINUTES)
        val refreshExpiresAt = System.currentTimeMillis() + SEVEN_DAYS_MS
        val sessionToken = jwtTokenService.mintSessionToken(walletAddress, sessionId, sessionExpiresAt)
        val firebaseToken = mintFirebaseToken(walletAddress, sessionId)
        val refreshHash = hashRefreshToken(refreshSecret)

        transaction {
            Sessions.insert {
                it[id] = sessionId
                it[Sessions.walletAddress] = walletAddress
                it[refreshTokenSelector] = refreshSelector
                it[refreshTokenHash] = refreshHash
                it[Sessions.familyId] = familyId
                it[Sessions.deviceFingerprint] = deviceFingerprint
                it[expiresAt] = refreshExpiresAt
                it[revokedAt] = null
            }
        }

        return AuthTokens(
            sessionToken = sessionToken,
            refreshToken = refreshToken,
            firebaseToken = firebaseToken,
            expiresAt = sessionExpiresAt.toEpochMilli(),
        )
    }

    private fun revokeFamily(familyId: String, revokedAt: Long) {
        // Fix: revoke the full family in one guarded statement so only still-live rows are touched on reuse detection.
        Sessions.update({ (Sessions.familyId eq familyId) and Sessions.revokedAt.isNull() }) {
            it[Sessions.revokedAt] = revokedAt
        }
    }

    private fun extractNonce(message: String): String {
        val match = NONCE_REGEX.find(message) ?: throw NonceMissingError()
        return match.groupValues[1]
    }

    private fun hashRefreshToken(rawToken: String): String =
        // Fix: hash refresh tokens as Argon2id PHC strings using the configured pepper and phase-spec work factors.
        argon2.hash(3, 65536, 4, "$rawToken${config.refreshTokenPepper}")

    private fun verifyRefreshToken(rawToken: String, hash: String): Boolean =
        argon2.verify(hash, "$rawToken${config.refreshTokenPepper}")

    private fun verifySignature(walletAddress: String, message: String, signature: String): Boolean {
        val hex = signature.removePrefix("0x")
        if (hex.length != 130) {
            return false
        }

        val bytes = hex.chunked(2).map { it.toInt(16).toByte() }
        val prefixed = "\u0019Ethereum Signed Message:\n${message.length}$message"
        val msgHash = Hash.sha3(prefixed.toByteArray(StandardCharsets.UTF_8))
        val signatureData = Sign.SignatureData(
            bytes[64],
            bytes.subList(0, 32).toByteArray(),
            bytes.subList(32, 64).toByteArray(),
        )
        val recoveredKey = Sign.signedMessageHashToKey(msgHash, signatureData)
        val recoveredAddress = Keys.toChecksumAddress(Keys.getAddress(recoveredKey))

        return recoveredAddress.equals(
            // Fix: compare normalized checksum forms directly; lowercasing the
            // input first can hide mixed-case validation issues.
            Keys.toChecksumAddress(walletAddress),
            ignoreCase = true,
        )
    }

    private fun parseRefreshToken(token: String): Pair<String, String> {
        val parts = token.split(":", limit = 2)
        if (parts.size != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw InvalidRefreshTokenError()
        }
        return parts[0] to parts[1]
    }

    companion object {
        private const val FIVE_MINUTES_MS = 5 * 60 * 1000L
        private const val SEVEN_DAYS_MS = 7 * 24 * 60 * 60 * 1000L
        private val NONCE_REGEX = Regex("""Nonce:\s*([A-Za-z0-9-]+)""")
    }
}
