/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.backend.model.transaction

import com.letapay.backend.model.swap.UnsignedTx
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
    val unsignedTx: UnsignedTx? = null,
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
