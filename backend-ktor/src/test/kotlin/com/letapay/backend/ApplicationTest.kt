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

    private fun testJwt(sessionId: String = "session-test"): String =
        TestAuthSessionFactory.issueToken(
            wallet = "0x742d35Cc6634C0532925a3b844Bc454e4438f44e",
            sessionId = sessionId,
        )
}
