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

import com.letapay.backend.model.swap.SpotPrice
import com.letapay.backend.model.swap.SwapQuote
import com.letapay.backend.model.swap.SwapQuoteRequest
import com.letapay.backend.model.swap.UnsignedSwapTx
import com.letapay.backend.model.swap.UnsignedTx
import com.letapay.backend.service.CoinbaseService
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SwapRoutesTest {
    @Test
    fun `swap quote with valid body returns quote response`() = testApplication {
        application { module() }

        val response = client.post("/swap/quote") {
            header(HttpHeaders.Authorization, "Bearer ${testJwt()}")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {"fromAsset":"ETH","toAsset":"USDC","amount":"0.1","chain":1,"slippageBps":50}
                """.trimIndent(),
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("\"quoteId\""))
        assertTrue(response.bodyAsText().contains("\"expiresAt\""))
    }

    @Test
    fun `swap quote with low slippage returns 400`() = testApplication {
        application { module() }

        val response = client.post("/swap/quote") {
            header(HttpHeaders.Authorization, "Bearer ${testJwt()}")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {"fromAsset":"ETH","toAsset":"USDC","amount":"0.1","chain":1,"slippageBps":5}
                """.trimIndent(),
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `swap quote with unknown chain returns 400`() = testApplication {
        application { module() }

        val response = client.post("/swap/quote") {
            header(HttpHeaders.Authorization, "Bearer ${testJwt()}")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {"fromAsset":"ETH","toAsset":"USDC","amount":"0.1","chain":10,"slippageBps":50}
                """.trimIndent(),
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `swap execute with expired quote returns 422`() = testApplication {
        application { module() }

        val response = client.post("/swap/execute") {
            header(HttpHeaders.Authorization, "Bearer ${testJwt()}")
            header("Idempotency-Key", "00000000-0000-0000-0000-000000000101")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {"quoteId":"missing-quote","idempotencyKey":"00000000-0000-0000-0000-000000000101"}
                """.trimIndent(),
            )
        }

        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
        assertTrue(response.bodyAsText().contains("QUOTE_EXPIRED"))
    }

    @Test
    fun `swap execute without idempotency key returns 400`() = testApplication {
        application { module() }

        val response = client.post("/swap/execute") {
            header(HttpHeaders.Authorization, "Bearer ${testJwt()}")
            contentType(ContentType.Application.Json)
            setBody("""{"quoteId":"missing-quote","idempotencyKey":"mismatch"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("MISSING_IDEMPOTENCY_KEY"))
    }

    @Test
    fun `swap execute with kill switch active returns 503`() = testApplication {
        System.setProperty("KILL_SWITCH_VALUE_MOVES", "true")
        try {
            application { module() }

            val response = client.post("/swap/execute") {
                header(HttpHeaders.Authorization, "Bearer ${testJwt()}")
                header("Idempotency-Key", "00000000-0000-0000-0000-000000000102")
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    {"quoteId":"missing-quote","idempotencyKey":"00000000-0000-0000-0000-000000000102"}
                    """.trimIndent(),
                )
            }

            assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
            assertTrue(response.bodyAsText().contains("KILL_SWITCH_ACTIVE"))
        } finally {
            System.clearProperty("KILL_SWITCH_VALUE_MOVES")
        }
    }

    @Test
    fun `swap execute rejects mismatched quote execution params`() = testApplication {
        application {
            module(
                module {
                    single<CoinbaseService> {
                        object : CoinbaseService {
                            override suspend fun getSpotPrice(
                                fromAsset: String,
                                toAsset: String,
                                chain: Long,
                            ): SpotPrice =
                                SpotPrice(fromAsset, toAsset, chain, "1.0")

                            override suspend fun getSwapQuote(request: SwapQuoteRequest): SwapQuote =
                                SwapQuote(
                                    quoteId = "q-1",
                                    fromAmount = request.amount,
                                    toAmount = "10",
                                    rate = "1.0",
                                    priceImpactBps = 10,
                                    estimatedFeeUsd = "1.0",
                                    expiresAt = System.currentTimeMillis() + 60_000L,
                                    calldata = "0x1",
                                    chainId = 137L,
                                )

                            override suspend fun getSwapUnsignedTx(quoteId: String): UnsignedSwapTx =
                                UnsignedSwapTx(
                                    unsignedTx = UnsignedTx(
                                        to = "0x1111111254EEB25477B68fb85Ed929f73A960582",
                                        data = "0xfeedface",
                                        value = "0x0",
                                        gasLimit = "0x493e0",
                                        maxFeePerGas = "0x0",
                                        maxPriorityFeePerGas = "0x0",
                                        chainId = 1L,
                                    ),
                                    expiresAt = System.currentTimeMillis() + 60_000L,
                                )

                            override suspend fun getTxStatus(txHash: String, chain: Long) = error("unused")
                        }
                    }
                },
            )
        }

        client.post("/swap/quote") {
            header(HttpHeaders.Authorization, "Bearer ${testJwt()}")
            contentType(ContentType.Application.Json)
            setBody("""{"fromAsset":"ETH","toAsset":"USDC","amount":"0.1","chain":137,"slippageBps":50}""")
        }
        val execute = client.post("/swap/execute") {
            header(HttpHeaders.Authorization, "Bearer ${testJwt()}")
            header("Idempotency-Key", "00000000-0000-0000-0000-000000000103")
            contentType(ContentType.Application.Json)
            setBody("""{"quoteId":"q-1","idempotencyKey":"00000000-0000-0000-0000-000000000103"}""")
        }

        assertEquals(HttpStatusCode.UnprocessableEntity, execute.status)
        assertTrue(execute.bodyAsText().contains("QUOTE_MISMATCH"))
    }

    private fun testJwt(): String =
        TestAuthSessionFactory.issueToken(
            wallet = "0x742d35Cc6634C0532925a3b844Bc454e4438f44e",
            sessionId = "swap-session-test",
        )
}
