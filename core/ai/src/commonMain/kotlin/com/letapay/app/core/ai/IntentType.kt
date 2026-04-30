/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.core.ai

import kotlinx.serialization.Serializable

@Serializable
sealed class IntentType {

    @Serializable
    data class SendPayment(
        val recipient: String,
        val asset: String,
        val amount: Double,
        val chain: String? = null,
        val note: String? = null,
    ) : IntentType()

    @Serializable
    data class SwapAsset(
        val fromAsset: String,
        val toAsset: String,
        val amount: Double,
        val chain: String? = null,
        val slippageBps: Int? = null,
    ) : IntentType()

    @Serializable
    data class StakeAsset(
        val opportunityId: String? = null,
        val asset: String? = null,
        val chain: String? = null,
        val amount: Double,
        val lockPeriod: String? = null,
    ) : IntentType()

    @Serializable
    data class CheckBalance(
        val asset: String? = null,
        val chain: String? = null,
    ) : IntentType()

    @Serializable
    data class ShowHistory(
        val asset: String? = null,
        val chain: String? = null,
        val timeRange: String? = null,
        val status: String? = null,
    ) : IntentType()
}
