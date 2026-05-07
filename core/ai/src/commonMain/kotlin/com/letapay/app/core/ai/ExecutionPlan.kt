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

import com.letapay.app.core.common.UUIDGenerator
import kotlin.time.Clock

enum class ExecutionKind {
    Quote,
    Build,
    Sign,
    Broadcast,
    PollStatus,
    FetchBalance,
    FetchHistory,
}

enum class PlanFamily {
    TransferPlan,
    SwapPlan,
    StakePlan,
    BalanceQueryPlan,
    HistoryQueryPlan,
}

data class ExecutionStep(
    val stepId: String,
    val kind: ExecutionKind,
    val endpoint: String,
    val requiresUserConfirmation: Boolean,
    val timeoutMs: Long,
)

data class ExecutionPlan(
    val planId: String,
    val planFamily: PlanFamily,
    val intent: IntentType,
    val steps: List<ExecutionStep>,
    val previewSummary: String,
    val idempotencyKey: String,
    val expiresAt: Long,
)

interface ExecutionPreviewPlanner {
    fun createPreviewPlan(intent: IntentType): ExecutionPlan
}

class DefaultExecutionPreviewPlanner : ExecutionPreviewPlanner {
    override fun createPreviewPlan(intent: IntentType): ExecutionPlan {
        val now = Clock.System.now().toEpochMilliseconds()
        val (family, summary, steps) = when (intent) {
            is IntentType.SendPayment -> Triple(
                PlanFamily.TransferPlan,
                "Send ${intent.amount} ${intent.asset} to ${intent.recipient}",
                listOf(
                    ExecutionStep(
                        "quote-transfer",
                        ExecutionKind.Quote,
                        "/transactions/build",
                        false,
                        15_000,
                    ),
                    ExecutionStep(
                        "build-transfer",
                        ExecutionKind.Build,
                        "/transactions/build",
                        false,
                        15_000,
                    ),
                    ExecutionStep(
                        "sign-transfer",
                        ExecutionKind.Sign,
                        "walletconnect://sign",
                        true,
                        120_000,
                    ),
                    ExecutionStep(
                        "broadcast-transfer",
                        ExecutionKind.Broadcast,
                        "/transactions/send",
                        true,
                        30_000,
                    ),
                ),
            )
            is IntentType.SwapAsset -> Triple(
                PlanFamily.SwapPlan,
                "Swap ${intent.amount} ${intent.fromAsset} to ${intent.toAsset}",
                listOf(
                    ExecutionStep(
                        "quote-swap",
                        ExecutionKind.Quote,
                        "/swap/quote",
                        false,
                        15_000,
                    ),
                    ExecutionStep(
                        "build-swap",
                        ExecutionKind.Build,
                        "/swap/execute",
                        false,
                        15_000,
                    ),
                    ExecutionStep(
                        "sign-swap",
                        ExecutionKind.Sign,
                        "walletconnect://sign",
                        true,
                        120_000,
                    ),
                    ExecutionStep(
                        "broadcast-swap",
                        ExecutionKind.Broadcast,
                        "/transactions/send",
                        true,
                        30_000,
                    ),
                ),
            )
            is IntentType.StakeAsset -> Triple(
                PlanFamily.StakePlan,
                "Stake ${intent.amount} ${intent.asset.orEmpty()}",
                listOf(
                    ExecutionStep(
                        "quote-stake",
                        ExecutionKind.Quote,
                        "/yield/opportunities",
                        false,
                        15_000,
                    ),
                    ExecutionStep(
                        "build-stake",
                        ExecutionKind.Build,
                        "/yield/stake",
                        false,
                        15_000,
                    ),
                    ExecutionStep(
                        "sign-stake",
                        ExecutionKind.Sign,
                        "walletconnect://sign",
                        true,
                        120_000,
                    ),
                    ExecutionStep(
                        "broadcast-stake",
                        ExecutionKind.Broadcast,
                        "/transactions/send",
                        true,
                        30_000,
                    ),
                ),
            )
            is IntentType.CheckBalance -> Triple(
                PlanFamily.BalanceQueryPlan,
                "Check portfolio balance",
                listOf(
                    ExecutionStep(
                        "fetch-balance",
                        ExecutionKind.FetchBalance,
                        "/transactions/history",
                        false,
                        10_000,
                    ),
                ),
            )
            is IntentType.ShowHistory -> Triple(
                PlanFamily.HistoryQueryPlan,
                "Show recent transaction history",
                listOf(
                    ExecutionStep(
                        "fetch-history",
                        ExecutionKind.FetchHistory,
                        "/transactions/history",
                        false,
                        10_000,
                    ),
                ),
            )
        }

        return ExecutionPlan(
            planId = UUIDGenerator.generateUUID(),
            planFamily = family,
            intent = intent,
            steps = steps,
            previewSummary = summary,
            idempotencyKey = UUIDGenerator.generateUUID(),
            expiresAt = now + PREVIEW_TTL_MS,
        )
    }

    private companion object {
        const val PREVIEW_TTL_MS = 5 * 60 * 1000L
    }
}
