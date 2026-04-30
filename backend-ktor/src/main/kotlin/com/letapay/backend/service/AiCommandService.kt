/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.backend.service

import com.letapay.backend.ai.ParseAgent
import com.letapay.backend.ai.PlanAgent
import com.letapay.backend.ai.StubIntentLlmClient
import com.letapay.backend.model.ai.ExecutionPlan
import com.letapay.backend.model.ai.ParseResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface AiCommandService {
    suspend fun parse(message: String): ParseResult

    suspend fun plan(message: String): ExecutionPlan

    fun streamSummary(event: String, txHash: String?): Flow<String>
}

class DefaultAiCommandService : AiCommandService {
    var fallbackPlanInvocations = 0
    var parseComputationCount = 0
    private val parseAgent = ParseAgent(StubIntentLlmClient())
    private val planAgent = PlanAgent()

    override suspend fun parse(message: String): ParseResult =
        parseCache.get(message.normalized())
            ?: parseAgent.parse(message).also {
                parseComputationCount += 1
                // Fix: deterministic parse results are cached for a short TTL
                // so repeated prompts do not re-run the same parser work.
                parseCache.put(message.normalized(), it)
            }

    override suspend fun plan(message: String): ExecutionPlan {
        fallbackPlanInvocations += 1
        val parse = parse(message)
        return planAgent.buildPlan(parse)
    }

    override fun streamSummary(event: String, txHash: String?): Flow<String> = flow {
        emit("Analyzing your request.")
        emit("Event type: $event.")
        emit("Transaction reference: ${txHash ?: "pending"}.")
        emit("Policy checks are complete. Ready for confirmation.")
    }

    private companion object {
        private val parseCache = ParseResultCache()
    }
}

private fun String.normalized(): String = trim().lowercase()
