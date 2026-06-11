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

import com.letapay.backend.service.ScreeningService
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.koin.dsl.module as koinModule

class TransactionRoutesTest {
    private val walletA = "0x742d35Cc6634C0532925a3b844Bc454e4438f44e"
    private val walletB = "0x1111111111111111111111111111111111111111"

    @Test
    fun `transactions build requires idempotency key`() = testApplication {
        application { configureApp(backendTestOverrides()) }

        val response = client.post("/transactions/build") {
            header(HttpHeaders.Authorization, "Bearer ${testJwt(walletA, "tx-session-1")}")
            contentType(ContentType.Application.Json)
            setBody("""{"to":"0x742d35Cc6634C0532925a3b844Bc454e4438f44e"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("MISSING_IDEMPOTENCY_KEY"))
    }

    @Test
    fun `transactions build rejects screened address`() = testApplication {
        application {
            configureApp(
                koinModule {
                    includes(backendTestOverrides())
                    single<ScreeningService> {
                        object : ScreeningService {
                            override suspend fun check(address: String): Boolean = false
                        }
                    }
                },
            )
        }

        val response = client.post("/transactions/build") {
            header(HttpHeaders.Authorization, "Bearer ${testJwt(walletA, "tx-session-2")}")
            header("Idempotency-Key", "build-key-reject")
            contentType(ContentType.Application.Json)
            setBody("""{"to":"0x00000000000000000000000000000000000000bad"}""")
        }

        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
        assertTrue(response.bodyAsText().contains("ADDRESS_REJECTED"))
    }

    @Test
    fun `transactions send stores and returns status plus history`() = testApplication {
        application { configureApp(backendTestOverrides()) }

        val sendResponse = client.post("/transactions/send") {
            header(HttpHeaders.Authorization, "Bearer ${testJwt(walletA, "tx-session-3")}")
            header("Idempotency-Key", "send-key-1")
            header("X-To-Address", "0x1111111111111111111111111111111111111111")
            header("X-Asset", "USDC")
            header("X-Amount", "25")
            header("X-Chain-Id", "137")
            contentType(ContentType.Application.Json)
            setBody("""{"signedTx":"0xdeadbeef"}""")
        }

        assertEquals(HttpStatusCode.OK, sendResponse.status)
        val txHash = Regex(""""txHash":"([^"]+)"""").find(sendResponse.bodyAsText())?.groupValues?.get(1)
            ?: error("txHash not found")

        val statusResponse = client.get("/transactions/status/$txHash") {
            header(HttpHeaders.Authorization, "Bearer ${testJwt(walletA, "tx-session-4")}")
        }
        assertEquals(HttpStatusCode.OK, statusResponse.status)
        assertTrue(statusResponse.bodyAsText().contains("submitted"))

        val historyResponse = client.get("/transactions/history") {
            header(HttpHeaders.Authorization, "Bearer ${testJwt(walletA, "tx-session-5")}")
        }
        assertEquals(HttpStatusCode.OK, historyResponse.status)
        assertTrue(historyResponse.bodyAsText().contains(txHash))
    }

    @Test
    fun `idempotency key is wallet-scoped and cannot be poisoned cross-wallet`() = testApplication {
        application { configureApp(backendTestOverrides()) }
        val key = "cross-wallet-key-1"

        val attacker = client.post("/transactions/build") {
            header(HttpHeaders.Authorization, "Bearer ${testJwt(walletB, "tx-session-attacker")}")
            header("Idempotency-Key", key)
            contentType(ContentType.Application.Json)
            setBody("""{"to":"0x9999999999999999999999999999999999999999"}""")
        }
        assertEquals(HttpStatusCode.OK, attacker.status)

        val victimFirst = client.post("/transactions/build") {
            header(HttpHeaders.Authorization, "Bearer ${testJwt(walletA, "tx-session-victim-1")}")
            header("Idempotency-Key", key)
            contentType(ContentType.Application.Json)
            setBody("""{"to":"0x742d35Cc6634C0532925a3b844Bc454e4438f44e"}""")
        }
        assertEquals(HttpStatusCode.OK, victimFirst.status)

        val victimReplay = client.post("/transactions/build") {
            header(HttpHeaders.Authorization, "Bearer ${testJwt(walletA, "tx-session-victim-2")}")
            header("Idempotency-Key", key)
            contentType(ContentType.Application.Json)
            setBody("""{"to":"0x742d35Cc6634C0532925a3b844Bc454e4438f44e"}""")
        }
        assertEquals(HttpStatusCode.OK, victimReplay.status)
        assertEquals(victimFirst.bodyAsText(), victimReplay.bodyAsText())
    }

    private fun testJwt(wallet: String, sessionId: String): String =
        TestAuthSessionFactory.issueToken(wallet = wallet, sessionId = sessionId)
}
