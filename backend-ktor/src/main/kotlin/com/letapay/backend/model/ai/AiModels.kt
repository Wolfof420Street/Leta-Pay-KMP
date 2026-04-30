/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.backend.model.ai

import kotlinx.serialization.Serializable

@Serializable
data class ParseRequest(
    val message: String,
)

@Serializable
data class PlanRequest(
    val message: String,
)

@Serializable
data class ParseResult(
    val intent: IntentType,
    val confidence: Double,
    val entities: Entities = Entities(),
    val missingRequired: List<String>,
    val safetyFlags: List<String>,
    val normalizedCommand: String,
    val parserVersion: String,
)

@Serializable
enum class IntentType {
    SendPayment,
    SwapAsset,
    StakeAsset,
    CheckBalance,
    ShowHistory,
    Unknown,
}

@Serializable
data class Entities(
    val recipient: String? = null,
    val amount: String? = null,
    val asset: String? = null,
    val fromAsset: String? = null,
    val toAsset: String? = null,
    val chain: Long? = null,
    val slippageBps: Int? = null,
    val opportunityId: String? = null,
    val timeRange: String? = null,
    val status: String? = null,
)

@Serializable
data class ExecutionPlan(
    val planId: String,
    val planType: String,
    val requiresClarification: Boolean,
    val clarificationQuestions: List<String> = emptyList(),
    val preview: String? = null,
    val steps: List<String>,
    val policy: String,
    val idempotencyKey: String? = null,
    val expiresAt: Long,
)
