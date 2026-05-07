/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.core.model.blockchain

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

private val walletAddressRegex = Regex("^0x[a-fA-F0-9]{40}$")

@Serializable
@JvmInline
value class WalletAddress(val value: String) {
    init {
        require(walletAddressRegex.matches(value)) { "Invalid wallet address" }
    }
}
