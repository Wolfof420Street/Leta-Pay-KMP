/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.backend.ai

import com.letapay.backend.model.ai.ExecutionPlan
import com.letapay.backend.model.ai.IntentType
import com.letapay.backend.model.ai.ParseResult
import java.util.UUID

enum class PlanPolicy {
    PASS,
    CLARIFY,
    REJECT,
}

class PlanAgent {
    fun buildPlan(
        validatedParse: ParseResult,
        plannerIntent: IntentType = validatedParse.intent,
    ): ExecutionPlan =
        when {
            validatedParse.intent != plannerIntent -> clarificationPlan(
                validatedParse,
                question =
                "I detected ${validatedParse.intent.name}, but planning inferred " +
                    "${plannerIntent.name}. Which action should continue?",
            )
            validatedParse.intent == IntentType.Unknown -> clarificationPlan(
                validatedParse,
                question = "I could not safely determine intent. Please clarify the exact wallet action.",
            )
            else -> successPlan(validatedParse)
        }

    private fun successPlan(validatedParse: ParseResult): ExecutionPlan {
        val policy = when (validatedParse.intent) {
            IntentType.SendPayment,
            IntentType.SwapAsset,
            IntentType.StakeAsset,
            IntentType.CheckBalance,
            IntentType.ShowHistory,
            -> PlanPolicy.PASS
            IntentType.Unknown -> PlanPolicy.CLARIFY
        }
        return ExecutionPlan(
            planId = UUID.randomUUID().toString(),
            planType = validatedParse.intent.name,
            requiresClarification = false,
            preview = "Prepared ${validatedParse.intent.name} plan for confirmation.",
            steps = listOf(
                "Validate parsed entities and policy constraints.",
                "Show a user-safe confirmation preview.",
                "Execute only after confirmation and policy pass.",
            ),
            policy = policy.name,
            idempotencyKey = UUID.randomUUID().toString(),
            expiresAt = System.currentTimeMillis() + 60_000,
        )
    }

    private fun clarificationPlan(parse: ParseResult, question: String): ExecutionPlan =
        ExecutionPlan(
            planId = UUID.randomUUID().toString(),
            planType = parse.intent.name,
            requiresClarification = true,
            clarificationQuestions = listOf(question),
            preview = "Clarification required before executing a value-moving action.",
            steps = listOf("Collect clarification.", "Re-parse intent.", "Re-run policy checks."),
            policy = PlanPolicy.CLARIFY.name,
            expiresAt = System.currentTimeMillis() + 60_000,
        )
}
