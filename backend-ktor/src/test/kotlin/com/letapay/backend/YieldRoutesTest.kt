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

class YieldRoutesTest {
    @Test
    fun `yield opportunities returns lido and aave`() = testApplication {
        application { module() }

        val response = client.get("/yield/opportunities") {
            header(HttpHeaders.Authorization, "Bearer ${testJwt()}")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("lido-eth-1"))
        assertTrue(body.contains("aave-usdc-137"))
    }

    @Test
    fun `yield opportunities can be filtered by chain`() = testApplication {
        application { module() }

        val response = client.get("/yield/opportunities?chain=1") {
            header(HttpHeaders.Authorization, "Bearer ${testJwt()}")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("lido-eth-1"))
        assertTrue(!body.contains("aave-usdc-137"))
    }

    @Test
    fun `yield stake returns unsigned lido tx`() = testApplication {
        application { module() }

        val response = client.post("/yield/stake") {
            header(HttpHeaders.Authorization, "Bearer ${testJwt()}")
            header("Idempotency-Key", "stake-key-1")
            contentType(ContentType.Application.Json)
            setBody("""{"opportunityId":"lido-eth-1","amount":"0.5","idempotencyKey":"stake-key-1"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"positionId\""))
        assertTrue(body.contains("\"unsignedTx\""))
    }

    @Test
    fun `yield stake below minimum returns 400`() = testApplication {
        application { module() }

        val response = client.post("/yield/stake") {
            header(HttpHeaders.Authorization, "Bearer ${testJwt()}")
            header("Idempotency-Key", "stake-key-2")
            contentType(ContentType.Application.Json)
            setBody("""{"opportunityId":"lido-eth-1","amount":"0.001","idempotencyKey":"stake-key-2"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `yield stake with kill switch returns 503`() = testApplication {
        System.setProperty("KILL_SWITCH_VALUE_MOVES", "true")
        application { module() }

        val response = client.post("/yield/stake") {
            header(HttpHeaders.Authorization, "Bearer ${testJwt()}")
            header("Idempotency-Key", "stake-key-3")
            contentType(ContentType.Application.Json)
            setBody("""{"opportunityId":"lido-eth-1","amount":"0.5","idempotencyKey":"stake-key-3"}""")
        }

        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
        assertTrue(response.bodyAsText().contains("KILL_SWITCH_ACTIVE"))
        System.clearProperty("KILL_SWITCH_VALUE_MOVES")
    }

    @Test
    fun `yield unstake rejects positions owned by another wallet`() = testApplication {
        application { module() }

        val stakeResponse = client.post("/yield/stake") {
            header(HttpHeaders.Authorization, "Bearer ${testJwt(wallet = WALLET_A)}")
            header("Idempotency-Key", "stake-key-4")
            contentType(ContentType.Application.Json)
            setBody("""{"opportunityId":"lido-eth-1","amount":"0.5","idempotencyKey":"stake-key-4"}""")
        }
        val positionId = extractField(stakeResponse.bodyAsText(), "positionId")

        val response = client.post("/yield/unstake") {
            header(HttpHeaders.Authorization, "Bearer ${testJwt(wallet = WALLET_B)}")
            header("Idempotency-Key", "unstake-key-1")
            contentType(ContentType.Application.Json)
            setBody("""{"positionId":"$positionId","amount":"0.1","idempotencyKey":"unstake-key-1"}""")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertTrue(response.bodyAsText().contains("POSITION_WRONG_OWNER"))
    }

    @Test
    fun `yield unstake rejects non active positions`() = testApplication {
        application { module() }

        val stakeResponse = client.post("/yield/stake") {
            header(HttpHeaders.Authorization, "Bearer ${testJwt(wallet = WALLET_A)}")
            header("Idempotency-Key", "stake-key-5")
            contentType(ContentType.Application.Json)
            setBody("""{"opportunityId":"lido-eth-1","amount":"0.5","idempotencyKey":"stake-key-5"}""")
        }
        val positionId = extractField(stakeResponse.bodyAsText(), "positionId")

        val firstUnstake = client.post("/yield/unstake") {
            header(HttpHeaders.Authorization, "Bearer ${testJwt(wallet = WALLET_A)}")
            header("Idempotency-Key", "unstake-key-2")
            contentType(ContentType.Application.Json)
            setBody("""{"positionId":"$positionId","amount":"0.1","idempotencyKey":"unstake-key-2"}""")
        }
        assertEquals(HttpStatusCode.OK, firstUnstake.status)

        val secondUnstake = client.post("/yield/unstake") {
            header(HttpHeaders.Authorization, "Bearer ${testJwt(wallet = WALLET_A)}")
            header("Idempotency-Key", "unstake-key-3")
            contentType(ContentType.Application.Json)
            setBody("""{"positionId":"$positionId","amount":"0.1","idempotencyKey":"unstake-key-3"}""")
        }

        assertEquals(HttpStatusCode.Conflict, secondUnstake.status)
        assertTrue(secondUnstake.bodyAsText().contains("POSITION_NOT_ACTIVE"))
    }

    @Test
    fun `yield positions only returns authenticated wallet positions`() = testApplication {
        application { module() }

        client.post("/yield/stake") {
            header(HttpHeaders.Authorization, "Bearer ${testJwt(wallet = WALLET_A)}")
            header("Idempotency-Key", "stake-key-6")
            contentType(ContentType.Application.Json)
            setBody("""{"opportunityId":"lido-eth-1","amount":"0.5","idempotencyKey":"stake-key-6"}""")
        }
        client.post("/yield/stake") {
            header(HttpHeaders.Authorization, "Bearer ${testJwt(wallet = WALLET_B)}")
            header("Idempotency-Key", "stake-key-7")
            contentType(ContentType.Application.Json)
            setBody("""{"opportunityId":"aave-usdc-137","amount":"5","idempotencyKey":"stake-key-7"}""")
        }

        val response = client.get("/yield/positions") {
            header(HttpHeaders.Authorization, "Bearer ${testJwt(wallet = WALLET_A)}")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains(WALLET_A))
        assertTrue(!body.contains(WALLET_B))
    }

    @Test
    fun `yield stake rejects disabled base opportunities`() = testApplication {
        System.setProperty("BASE_STAKING_ENABLED", "false")
        try {
            application { module() }

            val response = client.post("/yield/stake") {
                header(HttpHeaders.Authorization, "Bearer ${testJwt()}")
                header("Idempotency-Key", "stake-key-base")
                contentType(ContentType.Application.Json)
                setBody("""{"opportunityId":"base-usdc-8453","amount":"5","idempotencyKey":"stake-key-base"}""")
            }

            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertTrue(response.bodyAsText().contains("OPPORTUNITY_DISABLED"))
        } finally {
            System.clearProperty("BASE_STAKING_ENABLED")
        }
    }

    private fun testJwt(wallet: String = WALLET_A): String =
        TestAuthSessionFactory.issueToken(
            wallet = wallet,
            sessionId = "session-${wallet.takeLast(4)}",
        )

    private fun extractField(body: String, field: String): String =
        Regex(""""$field":"([^"]+)"""").find(body)?.groupValues?.get(1)
            ?: error("Field $field not found in $body")

    companion object {
        private const val WALLET_A = "0x742d35Cc6634C0532925a3b844Bc454e4438f44e"
        private const val WALLET_B = "0x1111111111111111111111111111111111111111"
    }
}
