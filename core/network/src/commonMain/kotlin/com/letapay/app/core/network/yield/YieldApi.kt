/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.core.network.yield

import com.letapay.app.core.model.yield.StakeRequest
import com.letapay.app.core.model.yield.StakeResponse
import com.letapay.app.core.model.yield.StakingPosition
import com.letapay.app.core.model.yield.UnstakeRequest
import com.letapay.app.core.model.yield.UnstakeResponse
import com.letapay.app.core.model.yield.YieldOpportunity
import com.letapay.app.core.network.bodyOrThrow
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody

class YieldApi(
    private val client: HttpClient,
) {
    suspend fun getOpportunities(sessionToken: String): List<YieldOpportunity> {
        return client.get("/yield/opportunities") {
            bearerAuth(sessionToken)
        }.bodyOrThrow()
    }

    suspend fun getPositions(sessionToken: String): List<StakingPosition> {
        return client.get("/yield/positions") {
            bearerAuth(sessionToken)
        }.bodyOrThrow()
    }

    suspend fun stake(
        sessionToken: String,
        opportunityId: String,
        amount: String,
        idempotencyKey: String,
    ): StakeResponse {
        val response = client.post("/yield/stake") {
            bearerAuth(sessionToken)
            header("Idempotency-Key", idempotencyKey)
            setBody(StakeRequest(opportunityId = opportunityId, amount = amount, idempotencyKey = idempotencyKey))
        }.bodyOrThrow<StakeResponse>()
        return response
    }

    suspend fun unstake(
        sessionToken: String,
        positionId: String,
        amount: String,
        idempotencyKey: String,
    ): UnstakeResponse {
        val response = client.post("/yield/unstake") {
            bearerAuth(sessionToken)
            header("Idempotency-Key", idempotencyKey)
            setBody(UnstakeRequest(positionId = positionId, amount = amount, idempotencyKey = idempotencyKey))
        }.bodyOrThrow<UnstakeResponse>()
        return response
    }
}
