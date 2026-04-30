/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.core.data.repository

import com.letapay.app.core.model.result.Resource
import com.letapay.app.core.model.transaction.BuildRequest
import com.letapay.app.core.model.transaction.BuildResponse
import com.letapay.app.core.model.transaction.SendResponse
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getTransactions(): Flow<Resource<List<Any>>>
    suspend fun buildTransaction(request: BuildRequest): Resource<BuildResponse>
    suspend fun sendTransaction(
        signedTx: String,
        toAddress: String,
        asset: String,
        amount: String,
        chainId: Long,
    ): Resource<SendResponse>
}
