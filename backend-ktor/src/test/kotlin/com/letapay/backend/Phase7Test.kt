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

import com.letapay.backend.service.AgentKitClient
import com.letapay.backend.service.DeviceTokenRecord
import com.letapay.backend.service.DeviceTokenService
import com.letapay.backend.service.HealthService
import com.letapay.backend.service.NotificationPayloadFactory
import com.letapay.backend.service.PushMessagingClient
import com.letapay.backend.service.PushNotificationService
import com.letapay.backend.service.SendResult
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.koin.ktor.ext.get
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.koin.dsl.module as koinModule

class Phase7Test {
    @Test
    fun `put device token with valid token returns 204`() = testApplication {
        application { configureApp() }

        val response = client.put("/device-token") {
            header(HttpHeaders.Authorization, "Bearer ${testJwt()}")
            contentType(ContentType.Application.Json)
            setBody("""{"fcmToken":"token-1","platform":"android"}""")
        }

        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `put device token twice keeps one active token per wallet and platform`() = testApplication {
        lateinit var service: DeviceTokenService
        application {
            configureApp()
            service = get()
        }

        client.put("/device-token") {
            header(HttpHeaders.Authorization, "Bearer ${testJwt()}")
            contentType(ContentType.Application.Json)
            setBody("""{"fcmToken":"token-1","platform":"android"}""")
        }
        client.put("/device-token") {
            header(HttpHeaders.Authorization, "Bearer ${testJwt()}")
            contentType(ContentType.Application.Json)
            setBody("""{"fcmToken":"token-2","platform":"android"}""")
        }

        val records = runBlocking { service.records(TEST_WALLET, "android") }
        assertEquals(2, records.size)
        assertEquals(1, records.count { it.active })
        assertTrue(records.any { it.fcmToken == "token-2" && it.active })
    }

    @Test
    fun `delete device token marks token inactive`() = testApplication {
        lateinit var service: DeviceTokenService
        application {
            configureApp()
            service = get()
        }

        client.put("/device-token") {
            header(HttpHeaders.Authorization, "Bearer ${testJwt()}")
            contentType(ContentType.Application.Json)
            setBody("""{"fcmToken":"token-delete","platform":"android"}""")
        }

        val response = client.delete("/device-token") {
            header(HttpHeaders.Authorization, "Bearer ${testJwt()}")
            contentType(ContentType.Application.Json)
            setBody("""{"fcmToken":"token-delete"}""")
        }

        assertEquals(HttpStatusCode.NoContent, response.status)
        val active = runBlocking { service.activeTokens(TEST_WALLET) }
        assertTrue(active.none { it.fcmToken == "token-delete" })
    }

    @Test
    fun `health returns 200 with status field present`() = testApplication {
        application { configureApp() }

        val response = client.get("/health")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("\"status\""))
    }

    @Test
    fun `health returns degraded db when probe reports degraded`() = testApplication {
        application {
            configureApp(
                koinModule {
                    single<HealthService> {
                        object : HealthService {
                            override suspend fun dbStatus(): String = "degraded"
                            override suspend fun firebaseStatus(): String = "ok"
                        }
                    }
                },
            )
        }

        val response = client.get("/health")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("\"db\":\"degraded\""))
    }

    @Test
    fun `request body greater than 64kb returns 413`() = testApplication {
        application { configureApp() }

        val oversizedWallet = "0x" + "a".repeat(70_000)
        val response = client.post("/auth/request-nonce") {
            contentType(ContentType.Application.Json)
            setBody("""{"walletAddress":"$oversizedWallet"}""")
        }

        assertEquals(HttpStatusCode.PayloadTooLarge, response.status)
    }

    @Test
    fun `timeout on coinbase quote returns 504 upstream timeout`() = testApplication {
        application {
            configureApp(
                koinModule {
                    single<AgentKitClient> {
                        object : AgentKitClient {
                            override suspend fun buildTransfer(
                                fromAddress: String,
                                toAddress: String,
                                asset: String,
                                amount: String,
                                chainId: Long,
                            ) = error("unused")

                            override suspend fun getSwapQuote(
                                fromAddress: String,
                                request: com.letapay.backend.model.swap.SwapQuoteRequest,
                            ): com.letapay.backend.model.swap.SwapQuote {
                                delay(9_000L)
                                error("timeout")
                            }

                            override suspend fun buildSwap(
                                fromAddress: String,
                                quote: com.letapay.backend.model.swap.SwapQuote,
                            ) = error("unused")

                            override suspend fun buildStake(
                                fromAddress: String,
                                request: com.letapay.backend.model.yield.StakeRequest,
                                chainId: Long,
                            ) = error("unused")
                        }
                    }
                },
            )
        }

        val response = client.post("/swap/quote") {
            header(HttpHeaders.Authorization, "Bearer ${testJwt()}")
            contentType(ContentType.Application.Json)
            setBody("""{"fromAsset":"ETH","toAsset":"USDC","amount":"0.1","chain":1,"slippageBps":50}""")
        }

        assertEquals(HttpStatusCode.GatewayTimeout, response.status)
        assertTrue(response.bodyAsText().contains("UPSTREAM_TIMEOUT"))
    }

    @Test
    fun `push notification payload builders set required type fields`() {
        assertEquals(
            "TX_CONFIRMED",
            NotificationPayloadFactory.txConfirmed(
                "1",
                "ETH",
                "0x1",
                1,
                "Ethereum",
            ).data["type"],
        )
        assertEquals(
            "TX_FAILED",
            NotificationPayloadFactory.txFailed("0x1", "Ethereum").data["type"],
        )
        assertEquals(
            "SWAP_CONFIRMED",
            NotificationPayloadFactory.swapConfirmed(
                "0x1",
                "1",
                "ETH",
                "2500",
                "USDC",
            ).data["type"],
        )
        assertEquals(
            "STAKE_CONFIRMED",
            NotificationPayloadFactory.stakeConfirmed(
                "position-1",
                "1",
                "ETH",
                "Lido",
            ).data["type"],
        )
    }

    @Test
    fun `unregistered fcm error marks device token inactive without failing others`() = runTest {
        val service = InMemoryDeviceTokenService().apply {
            upsert(TEST_WALLET, "bad-token", "android")
            upsert(TEST_WALLET, "good-token", "ios")
        }
        val client = object : PushMessagingClient {
            override suspend fun sendSingle(
                token: String,
                payload: com.letapay.backend.service.NotificationPayload,
            ): SendResult =
                error("unused")

            override suspend fun sendMany(
                tokens: List<String>,
                payload: com.letapay.backend.service.NotificationPayload,
            ): List<SendResult> = listOf(
                SendResult(token = "bad-token", success = false, errorCode = "UNREGISTERED"),
                SendResult(token = "good-token", success = true),
            )
        }

        PushNotificationService(service, client).notifyWallet(
            TEST_WALLET,
            NotificationPayloadFactory.txConfirmed("1", "ETH", "0x1", 1, "Ethereum"),
        )

        val active = service.activeTokens(TEST_WALLET)
        assertTrue(active.none { it.fcmToken == "bad-token" })
        assertTrue(active.any { it.fcmToken == "good-token" })
    }

    private fun testJwt(wallet: String = TEST_WALLET): String =
        TestAuthSessionFactory.issueToken(
            wallet = wallet,
            sessionId = "phase7-session",
        )

    private companion object {
        private const val TEST_WALLET = "0x742d35Cc6634C0532925a3b844Bc454e4438f44e"
    }
}

private class InMemoryDeviceTokenService : DeviceTokenService {
    private val records = mutableListOf<DeviceTokenRecord>()

    override suspend fun upsert(walletAddress: String, fcmToken: String, platform: String) {
        records.filter { it.walletAddress == walletAddress && it.platform == platform }
            .forEach { records[records.indexOf(it)] = it.copy(active = false) }
        records += DeviceTokenRecord(
            id = "${walletAddress.take(6)}-$platform-$fcmToken",
            walletAddress = walletAddress,
            fcmToken = fcmToken,
            platform = platform,
            active = true,
        )
    }

    override suspend fun deactivate(walletAddress: String, fcmToken: String) {
        records.replaceAll { record ->
            if (record.walletAddress == walletAddress && record.fcmToken == fcmToken) {
                record.copy(active = false)
            } else {
                record
            }
        }
    }

    override suspend fun deactivateToken(fcmToken: String) {
        records.replaceAll { if (it.fcmToken == fcmToken) it.copy(active = false) else it }
    }

    override suspend fun activeTokens(walletAddress: String): List<DeviceTokenRecord> =
        records.filter { it.walletAddress == walletAddress && it.active }

    override suspend fun records(walletAddress: String, platform: String?): List<DeviceTokenRecord> =
        records.filter { it.walletAddress == walletAddress && (platform == null || it.platform == platform) }
}
