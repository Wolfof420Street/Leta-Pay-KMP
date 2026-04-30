/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.core.network.transaction

import com.letapay.app.core.model.transaction.BuildRequest
import com.letapay.app.core.model.transaction.BuildResponse
import com.letapay.app.core.model.transaction.SendRequest
import com.letapay.app.core.model.transaction.SendResponse
import com.letapay.app.core.model.transaction.TransactionRecord
import com.letapay.app.core.model.transaction.TransactionStatusResponse
import com.letapay.app.core.network.bodyOrThrow
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody

class TransactionApi(
    private val client: HttpClient,
) {
    suspend fun buildTransaction(sessionToken: String, request: BuildRequest): BuildResponse {
        return client.post("/transactions/build") {
            bearerAuth(sessionToken)
            setBody(request)
        }.bodyOrThrow()
    }

    suspend fun sendTransaction(
        sessionToken: String,
        signedTx: String,
        toAddress: String,
        asset: String,
        amount: String,
        chainId: Long,
        idempotencyKey: String,
    ): SendResponse {
        val response = client.post("/transactions/send") {
            bearerAuth(sessionToken)
            header("Idempotency-Key", idempotencyKey)
            header("X-To-Address", toAddress)
            header("X-Asset", asset)
            header("X-Amount", amount)
            header("X-Chain-Id", chainId.toString())
            setBody(SendRequest(signedTx = signedTx))
        }.bodyOrThrow<SendResponse>()
        return response
    }

    suspend fun fetchActivity(sessionToken: String): List<TransactionRecord> {
        return client.get("/activity") {
            bearerAuth(sessionToken)
        }.bodyOrThrow()
    }

    suspend fun fetchTransactionStatus(sessionToken: String, txHash: String): TransactionStatusResponse {
        return client.get("/transactions/status/$txHash") {
            bearerAuth(sessionToken)
        }.bodyOrThrow()
    }
}
