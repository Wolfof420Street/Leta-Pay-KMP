/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.core.model

import com.letapay.app.core.model.blockchain.WalletAddress
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val walletAddress: WalletAddress,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val createdAt: Long,
    val lastActiveAt: Long,
)
