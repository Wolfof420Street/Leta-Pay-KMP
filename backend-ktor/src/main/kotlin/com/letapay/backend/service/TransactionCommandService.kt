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

import com.letapay.backend.error.AddressRejectedError
import com.letapay.backend.model.transaction.BuildRequest
import com.letapay.backend.model.transaction.BuildResponse

interface TransactionCommandService {
    suspend fun build(
        walletAddress: String,
        request: BuildRequest,
        chainId: Long,
    ): BuildResponse
}

class DefaultTransactionCommandService(
    private val screeningService: ScreeningService,
    private val agentKitClient: AgentKitClient,
) : TransactionCommandService {
    override suspend fun build(
        walletAddress: String,
        request: BuildRequest,
        chainId: Long,
    ): BuildResponse {
        if (!screeningService.check(request.to)) {
            throw AddressRejectedError()
        }
        val unsignedTx = agentKitClient.buildTransfer(
            fromAddress = walletAddress,
            toAddress = request.to,
            asset = request.asset ?: "ETH",
            amount = request.amount ?: "0",
            chainId = chainId,
        ).getOrThrow()
        return BuildResponse(
            status = "prepared",
            preview = "Prepared unsigned transaction for ${request.to}.",
            unsignedTx = unsignedTx,
        )
    }
}
