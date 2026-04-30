/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.core.network.portfolio

import com.letapay.app.core.model.wallet.PortfolioBalance
import com.letapay.app.core.network.bodyOrThrow
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import kotlinx.serialization.Serializable

class PortfolioApi(
    private val client: HttpClient,
) {
    suspend fun fetchBalance(sessionToken: String): PortfolioBalance {
        val response = client.get("/activity") {
            bearerAuth(sessionToken)
        }.bodyOrThrow<List<ActivityRecordResponse>>()

        return PortfolioBalance(
            balances = emptyList(),
            totalUsdValue = null,
            updatedAtEpochMillis = response.maxOfOrNull(ActivityRecordResponse::createdAt),
        )
    }
}

@Serializable
private data class ActivityRecordResponse(
    val txHash: String,
    val walletAddress: String,
    val toAddress: String,
    val status: String,
    val createdAt: Long,
)
