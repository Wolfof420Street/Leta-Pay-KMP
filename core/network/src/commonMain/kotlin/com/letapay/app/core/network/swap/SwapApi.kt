/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.core.network.swap

import com.letapay.app.core.model.swap.SwapExecuteRequest
import com.letapay.app.core.model.swap.SwapExecuteResponse
import com.letapay.app.core.model.swap.SwapQuoteRequest
import com.letapay.app.core.model.swap.SwapQuoteResponse
import com.letapay.app.core.network.bodyOrThrow
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody

class SwapApi(
    private val client: HttpClient,
) {
    suspend fun quote(sessionToken: String, request: SwapQuoteRequest): SwapQuoteResponse {
        return client.post("/swap/quote") {
            bearerAuth(sessionToken)
            setBody(request)
        }.bodyOrThrow()
    }

    suspend fun execute(
        sessionToken: String,
        quoteId: String,
        idempotencyKey: String,
    ): SwapExecuteResponse {
        val response = client.post("/swap/execute") {
            bearerAuth(sessionToken)
            header("Idempotency-Key", idempotencyKey)
            setBody(SwapExecuteRequest(quoteId = quoteId, idempotencyKey = idempotencyKey))
        }.bodyOrThrow<SwapExecuteResponse>()
        return response
    }
}
