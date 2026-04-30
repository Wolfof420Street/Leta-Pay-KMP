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

import com.letapay.backend.model.ai.IntentType
import com.letapay.backend.model.ai.ParseResult

interface IntentLlmClient {
    suspend fun parseIntent(message: String): ParseResult?
}

class StubIntentLlmClient : IntentLlmClient {
    override suspend fun parseIntent(message: String): ParseResult? =
        ParseResult(
            intent = IntentType.Unknown,
            confidence = 0.30,
            missingRequired = listOf("intent"),
            safetyFlags = listOf("llm-fallback"),
            normalizedCommand = message.trim(),
            parserVersion = "koog-llm-fallback-v1",
        )
}

class ParseAgent(
    private val llmClient: IntentLlmClient,
) {
    suspend fun parse(message: String): ParseResult =
        deterministicParse(message)?.copy(parserVersion = "deterministic-v1")
            ?: requireNotNull(llmClient.parseIntent(message)) {
                "ParseAgent LLM client returned null parse result."
            }
}
