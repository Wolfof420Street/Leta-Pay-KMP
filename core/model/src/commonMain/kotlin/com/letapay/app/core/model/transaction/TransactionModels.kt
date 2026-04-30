/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.core.model.transaction

import kotlinx.serialization.Serializable

@Serializable
data class BuildRequest(
    val to: String,
    val amount: String? = null,
    val asset: String? = null,
)

@Serializable
data class BuildResponse(
    val status: String,
    val preview: String,
)

@Serializable
data class SendRequest(
    val signedTx: String,
)

@Serializable
data class SendResponse(
    val txHash: String,
    val status: String,
)

@Serializable
data class TransactionRecord(
    val txHash: String,
    val walletAddress: String,
    val toAddress: String,
    val status: String,
    val createdAt: Long,
)

@Serializable
data class TransactionStatusResponse(
    val txHash: String,
    val status: String,
)
