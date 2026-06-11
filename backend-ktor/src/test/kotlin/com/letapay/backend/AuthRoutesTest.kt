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

import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import org.web3j.crypto.Credentials
import org.web3j.crypto.Sign
import java.nio.charset.StandardCharsets
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthRoutesTest {
    @Test
    fun `POST auth request-nonce returns nonce and expiry`() = testApplication {
        application { configureApp(backendTestOverrides()) }

        val response = client.post("/auth/request-nonce") {
            contentType(ContentType.Application.Json)
            setBody("""{"walletAddress":"0x742d35Cc6634C0532925a3b844Bc454e4438f44e"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("\"nonce\""))
        assertTrue(response.bodyAsText().contains("\"expiresAt\""))
    }

    @Test
    fun `POST auth verify-signature replay attack returns NONCE_ALREADY_USED`() = testApplication {
        application { configureApp(backendTestOverrides()) }

        val walletAddress = TEST_CREDENTIALS.address
        val nonceBody = client.post("/auth/request-nonce") {
            contentType(ContentType.Application.Json)
            setBody("""{"walletAddress":"$walletAddress"}""")
        }.bodyAsText()
        val nonce = Regex(""""nonce":"([^"]+)"""").find(nonceBody)?.groupValues?.get(1)
            ?: error("Nonce missing from response: $nonceBody")

        val message = buildSiweMessage(walletAddress, nonce)

        val requestBody = """
            {"walletAddress":"$walletAddress","message":${message.asJsonString()},"signature":"${sign(message)}"}
        """.trimIndent()

        val first = client.post("/auth/verify-signature") {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }
        assertEquals(HttpStatusCode.OK, first.status)

        val replay = client.post("/auth/verify-signature") {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }
        assertEquals(HttpStatusCode.Unauthorized, replay.status)
        assertTrue(replay.bodyAsText().contains("NONCE_ALREADY_USED"))
    }

    @Test
    fun `POST auth refresh-token with invalid token returns INVALID_REFRESH_TOKEN`() = testApplication {
        application { configureApp(backendTestOverrides()) }

        val response = client.post("/auth/refresh-token") {
            contentType(ContentType.Application.Json)
            setBody("""{"refreshToken":"not-a-real-token"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertTrue(response.bodyAsText().contains("INVALID_REFRESH_TOKEN"))
    }

    @Test
    fun `revoked session cannot reuse same jwt after revoke-session`() = testApplication {
        application { configureApp(backendTestOverrides()) }
        val walletAddress = TEST_CREDENTIALS.address
        val nonceBody = client.post("/auth/request-nonce") {
            contentType(ContentType.Application.Json)
            setBody("""{"walletAddress":"$walletAddress"}""")
        }.bodyAsText()
        val nonce = Regex(""""nonce":"([^"]+)"""").find(nonceBody)?.groupValues?.get(1)
            ?: error("Nonce missing from response: $nonceBody")
        val message = buildSiweMessage(walletAddress, nonce)
        val verifyBody = """
            {"walletAddress":"$walletAddress","message":${message.asJsonString()},"signature":"${sign(message)}"}
        """.trimIndent()
        val token = Regex(""""sessionToken":"([^"]+)"""")
            .find(
                client.post("/auth/verify-signature") {
                    contentType(ContentType.Application.Json)
                    setBody(verifyBody)
                }.bodyAsText(),
            )
            ?.groupValues
            ?.get(1)
            ?: error("sessionToken missing")

        val revokeResponse = client.post("/auth/revoke-session") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.NoContent, revokeResponse.status)

        val responseAfterRevoke = client.post("/auth/firebase-token") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.Unauthorized, responseAfterRevoke.status)
    }

    @Test
    fun `request-nonce cannot evade pre-auth rate limit by rotating forwarded for header`() = testApplication {
        application { configureApp(backendTestOverrides()) }
        val wallet = "0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        repeat(10) { attempt ->
            val response = client.post("/auth/request-nonce") {
                contentType(ContentType.Application.Json)
                header("X-Forwarded-For", "198.51.100.$attempt")
                setBody("""{"walletAddress":"$wallet"}""")
            }
            assertEquals(HttpStatusCode.OK, response.status)
        }

        val blocked = client.post("/auth/request-nonce") {
            contentType(ContentType.Application.Json)
            header("X-Forwarded-For", "203.0.113.199")
            setBody("""{"walletAddress":"$wallet"}""")
        }
        assertEquals(HttpStatusCode.TooManyRequests, blocked.status)
        assertTrue(blocked.bodyAsText().contains("RATE_LIMIT_EXCEEDED"))
    }

    private fun sign(message: String): String {
        val signature = Sign.signPrefixedMessage(
            message.toByteArray(StandardCharsets.UTF_8),
            TEST_CREDENTIALS.ecKeyPair,
        )
        return buildString {
            append("0x")
            append(signature.r.joinToString("") { "%02x".format(it.toInt() and 0xff) })
            append(signature.s.joinToString("") { "%02x".format(it.toInt() and 0xff) })
            append("%02x".format(signature.v.first().toInt() and 0xff))
        }
    }

    private fun String.asJsonString(): String =
        "\"" + replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""

    private fun buildSiweMessage(walletAddress: String, nonce: String): String {
        val issuedAt = Instant.now()
        val expirationTime = issuedAt.plusSeconds(5 * 60)
        return """
            letapay.app wants you to sign in with your Ethereum account:
            $walletAddress

            Sign in to Leta Pay

            URI: https://letapay.app
            Version: 1
            Chain ID: 1
            Nonce: $nonce
            Issued At: $issuedAt
            Expiration Time: $expirationTime
        """.trimIndent()
    }

    companion object {
        private val TEST_CREDENTIALS =
            Credentials.create("0x4c0883a69102937d6231471b5dbb6204fe512961708279da7af81c33a9ad2e16")
    }
}
