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

import com.letapay.backend.error.CircuitOpenError
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class CircuitBreaker(
    private val name: String,
    private val failureThreshold: Int = 5,
    private val resetTimeoutMs: Long = 30_000L,
    private val nowProvider: () -> Long = System::currentTimeMillis,
) {
    enum class State { CLOSED, OPEN, HALF_OPEN }

    private val state = AtomicReference(State.CLOSED)
    private val failureCount = AtomicInteger(0)
    private val openedAt = AtomicReference(0L)

    suspend fun <T> execute(block: suspend () -> T): T {
        when (state.get()) {
            State.OPEN -> {
                if (nowProvider() > openedAt.get() + resetTimeoutMs) {
                    state.set(State.HALF_OPEN)
                } else {
                    throw CircuitOpenError(name)
                }
            }
            State.CLOSED, State.HALF_OPEN -> Unit
        }

        return try {
            val result = block()
            failureCount.set(0)
            state.set(State.CLOSED)
            result
        } catch (exception: Exception) {
            val failures = failureCount.incrementAndGet()
            if (state.get() == State.HALF_OPEN || failures >= failureThreshold) {
                state.set(State.OPEN)
                openedAt.set(nowProvider())
            }
            throw exception
        }
    }

    fun currentState(): State = state.get()
}
