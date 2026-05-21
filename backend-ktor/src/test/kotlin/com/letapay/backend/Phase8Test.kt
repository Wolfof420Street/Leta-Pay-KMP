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

import com.letapay.backend.service.CircuitBreaker
import com.letapay.backend.service.CircuitBreaker.State
import com.letapay.backend.service.DefaultAiCommandService
import com.letapay.backend.service.HealthService
import com.letapay.backend.service.InMemoryKeyValueCache
import com.letapay.backend.service.ParseResultCache
import com.letapay.backend.service.PriceCache
import com.letapay.backend.service.StubCoinbaseService
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.koin.dsl.module as koinModule

class Phase8Test {
    @Test
    fun `price cache returns cached value on second call`() = runTest {
        val service = StubCoinbaseService(
            HttpClient(CIO),
            PriceCache(InMemoryKeyValueCache()),
            CircuitBreaker("coinbase"),
        )

        service.getSpotPrice("ETH", "USD", 1)
        service.getSpotPrice("ETH", "USD", 1)

        assertEquals(1, service.spotPriceFetchCount)
    }

    @Test
    fun `price cache returns stale value when coinbase throws`() = runTest {
        val service = StubCoinbaseService(
            HttpClient(CIO),
            PriceCache(InMemoryKeyValueCache()),
            CircuitBreaker("coinbase"),
        )

        val fresh = service.getSpotPrice("ETH", "USD", 1)
        service.failSpotPrice = true
        val stale = service.getSpotPrice("ETH", "USD", 1)

        assertEquals(fresh.price, stale.price)
        assertEquals(1, service.spotPriceFetchCount)
    }

    @Test
    fun `parse result cache hits on identical deterministic message`() = runTest {
        val service = DefaultAiCommandService(ParseResultCache(InMemoryKeyValueCache()))

        service.parse("check my balance")
        service.parse("check my balance")

        assertEquals(1, service.parseComputationCount)
    }

    @Test
    fun `circuit breaker opens after threshold consecutive failures`() = runTest {
        val breaker = CircuitBreaker(name = "coinbase", failureThreshold = 2, resetTimeoutMs = 60_000L)

        repeat(2) {
            assertFailsWith<IllegalStateException> {
                breaker.execute { throw IllegalStateException("boom") }
            }
        }

        assertEquals(State.OPEN, breaker.currentState())
    }

    @Test
    fun `circuit breaker transitions to half open after timeout`() = runTest {
        var now = 0L
        val breaker = CircuitBreaker(
            name = "coinbase",
            failureThreshold = 1,
            resetTimeoutMs = 1_000L,
            nowProvider = { now },
        )

        assertFailsWith<IllegalStateException> {
            breaker.execute { throw IllegalStateException("boom") }
        }
        now = 2_000L
        breaker.execute { "ok" }

        assertEquals(State.CLOSED, breaker.currentState())
    }

    @Test
    fun `health returns degraded db when probe fails`() = testApplication {
        application {
            configureApp(
                koinModule {
                    single<HealthService> {
                        object : HealthService {
                            override suspend fun dbStatus(): String = "degraded"
                            override suspend fun firebaseStatus(): String = "degraded"
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
    fun `build fat jar task is declared in backend gradle build`() {
        val buildFile = File("build.gradle.kts").readText()
        assertTrue(buildFile.contains("buildFatJar"))
    }

    @Test
    fun `dockerfile contains hardened runtime directives`() {
        val dockerfile = File("Dockerfile").readText()
        assertTrue(dockerfile.contains("USER letapay") || dockerfile.contains("USER "))
    }
}
