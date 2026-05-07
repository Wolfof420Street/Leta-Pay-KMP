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

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import org.web3j.crypto.Credentials
import org.web3j.crypto.Sign
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApplicationTest {
    @Test
    fun `health endpoint returns ok`() = testApplication {
        application {
            configureApp()
        }

        val response = client.get("/health")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("\"status\""))
    }

    @Test
    fun `request nonce returns nonce and expiry`() = testApplication {
        application {
            configureApp()
        }

        val response = client.post("/auth/request-nonce") {
            contentType(ContentType.Application.Json)
            setBody("""{"walletAddress":"0x742d35Cc6634C0532925a3b844Bc454e4438f44e"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("\"nonce\""))
        assertTrue(response.bodyAsText().contains("\"expiresAt\""))
    }

    @Test
    fun `ai parse returns deterministic send result`() = testApplication {
        application {
            configureApp()
        }

        val response = client.post("/ai/parse") {
            header(HttpHeaders.Authorization, "Bearer ${testJwt()}")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {"message":"send 0.1 ETH to 0x742d35Cc6634C0532925a3b844Bc454e4438f44e"}
                """.trimIndent(),
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("\"intent\":\"SendPayment\""))
        assertTrue(response.bodyAsText().contains("\"confidence\":0.95"))
    }

    @Test
    fun `ai parse rate limit exceeded returns 429`() = testApplication {
        application {
            configureApp()
        }

        var limited: io.ktor.client.statement.HttpResponse? = null
        repeat(180) {
            val response = client.post("/ai/parse") {
                header(HttpHeaders.Authorization, "Bearer ${testJwt()}")
                contentType(ContentType.Application.Json)
                setBody("""{"message":"balance"}""")
            }
            if (response.status == HttpStatusCode.TooManyRequests) {
                limited = response
                return@repeat
            }
        }

        assertTrue(limited != null, "Expected to hit rate limit within 180 requests")
        assertTrue(limited!!.headers.contains("Retry-After"))
    }

    @Test
    fun `transactions send without idempotency key returns 400`() = testApplication {
        application {
            configureApp()
        }

        val response = client.post("/transactions/send") {
            header(HttpHeaders.Authorization, "Bearer ${testJwt()}")
            contentType(ContentType.Application.Json)
            setBody("""{"signedTx":"0xdeadbeef"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("MISSING_IDEMPOTENCY_KEY"))
    }

    @Test
    fun `kill switch active returns 503 for transaction build`() = testApplication {
        System.setProperty("KILL_SWITCH_VALUE_MOVES", "true")

        application {
            configureApp()
        }

        val response = client.post("/transactions/build") {
            header(HttpHeaders.Authorization, "Bearer ${testJwt()}")
            contentType(ContentType.Application.Json)
            setBody("""{"to":"0x742d35Cc6634C0532925a3b844Bc454e4438f44e"}""")
        }

        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
        assertTrue(response.bodyAsText().contains("KILL_SWITCH_ACTIVE"))
        System.clearProperty("KILL_SWITCH_VALUE_MOVES")
    }

    @Test
    fun `invalid tx hash returns required error code`() = testApplication {
        application {
            configureApp()
        }

        val response = client.get("/transactions/status/not-a-hash") {
            header(HttpHeaders.Authorization, "Bearer ${testJwt()}")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("INVALID_TX_HASH"))
    }

    @Test
    fun `verify signature rejects nonce replay`() = testApplication {
        application {
            configureApp()
        }

        val walletAddress = TEST_CREDENTIALS.address
        val nonceBody = client.post("/auth/request-nonce") {
            contentType(ContentType.Application.Json)
            setBody("""{"walletAddress":"$walletAddress"}""")
        }.bodyAsText()
        val nonce = Regex(""""nonce":"([^"]+)"""").find(nonceBody)?.groupValues?.get(1)
            ?: error("Nonce missing from response: $nonceBody")

        val message = """
            letapay.app wants you to sign in with your Ethereum account:
            $walletAddress

            Sign in to Leta Pay

            URI: https://letapay.app
            Version: 1
            Chain ID: 1
            Nonce: $nonce
            Issued At: 2026-04-23T00:00:00Z
            Expiration Time: 2026-04-23T00:05:00Z
        """.trimIndent()

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

    private fun testJwt(sessionId: String = "session-test"): String =
        TestAuthSessionFactory.issueToken(
            wallet = "0x742d35Cc6634C0532925a3b844Bc454e4438f44e",
            sessionId = sessionId,
        )

    private fun sign(message: String): String {
        val signature = Sign.signPrefixedMessage(
            message.toByteArray(StandardCharsets.UTF_8),
            TEST_CREDENTIALS.ecKeyPair,
        )
        return buildString {
            append("0x")
            append(signature.r.joinToString("") { "%02x".format(it.toInt() and 0xff) })
            append(signature.s.joinToString("") { "%02x".format(it.toInt() and 0xff) })
            // Fix: web3j 4.12.2 exposes v as a byte array, so serialize its first recovery byte explicitly.
            append("%02x".format(signature.v.first().toInt() and 0xff))
        }
    }

    private fun String.asJsonString(): String =
        "\"" + replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""

    companion object {
        private val TEST_CREDENTIALS =
            Credentials.create("0x4c0883a69102937d6231471b5dbb6204fe512961708279da7af81c33a9ad2e16")
    }
}
