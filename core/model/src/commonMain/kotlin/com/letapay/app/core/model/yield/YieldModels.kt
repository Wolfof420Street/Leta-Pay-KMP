/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.core.model.yield

import com.letapay.app.core.model.blockchain.UnsignedTx
import kotlinx.serialization.Serializable

@Serializable
data class YieldOpportunity(
    val opportunityId: String,
    val chain: Long,
    val provider: String,
    val asset: String,
    val outputAsset: String,
    val currentApyBps: Int,
    val minAmount: String,
    val unbondingDays: Int,
    val enabled: Boolean,
)

@Serializable
data class StakeRequest(
    val opportunityId: String,
    val amount: String,
    val idempotencyKey: String,
)

@Serializable
data class UnstakeRequest(
    val positionId: String,
    val amount: String,
    val idempotencyKey: String,
)

@Serializable
data class StakingPosition(
    val positionId: String,
    val opportunityId: String,
    val walletAddress: String,
    val stakedAmount: String,
    val currentValue: String,
    val currentValueUsd: String,
    val accruedRewards: String,
    val status: StakingStatus,
    val chain: Long,
    val createdAt: Long,
)

@Serializable
enum class StakingStatus {
    Active,
    PendingUnstake,
    Unstaked,
}

@Serializable
data class StakeResponse(
    val unsignedTx: UnsignedTx,
    val positionId: String,
)

@Serializable
data class UnstakeResponse(
    val unsignedTx: UnsignedTx,
    val unbondingDays: Int,
)
