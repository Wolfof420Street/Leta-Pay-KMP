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

interface NlpCommandParser {
    suspend fun parseCommand(input: String): ParseResult
}

data class ParseResult(
    val intent: IntentType,
    val confidenceScore: Float,
    val provider: String,
    val model: String,
    val promptVersion: String,
    val requiresClarification: Boolean,
    val clarificationPrompt: String? = null,
)

enum class ConfidenceBand {
    Reject,
    Clarify,
    Preview,
}

object ConfidenceThresholds {
    const val PREVIEW_THRESHOLD: Float = 0.85f
    const val CLARIFICATION_THRESHOLD: Float = 0.60f
}

val ParseResult.confidenceBand: ConfidenceBand
    get() = when {
        confidenceScore >= ConfidenceThresholds.PREVIEW_THRESHOLD -> ConfidenceBand.Preview
        confidenceScore >= ConfidenceThresholds.CLARIFICATION_THRESHOLD -> ConfidenceBand.Clarify
        else -> ConfidenceBand.Reject
    }

class DefaultNlpCommandParser : NlpCommandParser {
    override suspend fun parseCommand(input: String): ParseResult {
        val normalized = input.trim().lowercase()
        return when {
            normalized.startsWith("send ") -> parseSend(normalized)
            normalized.startsWith("swap ") -> parseSwap(normalized)
            normalized.startsWith("stake ") -> parseStake(normalized)
            normalized.contains("history") -> parseHistory()
            normalized.contains("balance") -> parseBalance()
            else -> rejectedResult()
        }
    }

    private fun parseSend(input: String): ParseResult {
        val words = input.split(" ")
        val amount = words.getOrNull(1)?.toDoubleOrNull()
        val asset = words.getOrNull(2)?.uppercase()
        val recipient = words.find { it.startsWith("0x") }
        val confidence = if (amount != null && asset != null && recipient != null) 0.90f else 0.72f

        return ParseResult(
            intent = IntentType.SendPayment(
                recipient = recipient.orEmpty(),
                asset = asset.orEmpty(),
                amount = amount ?: 0.0,
            ),
            confidenceScore = confidence,
            provider = PARSER_PROVIDER,
            model = PARSER_MODEL,
            promptVersion = PROMPT_VERSION,
            requiresClarification = confidence < ConfidenceThresholds.PREVIEW_THRESHOLD,
            clarificationPrompt = "Please include the amount, asset, and recipient wallet address.",
        )
    }

    private fun parseSwap(input: String): ParseResult {
        val match = Regex("""swap\s+([0-9]*\.?[0-9]+)\s+([a-zA-Z0-9]+)\s+(to|for)\s+([a-zA-Z0-9]+)""")
            .find(input)
        val amount = match?.groupValues?.getOrNull(1)?.toDoubleOrNull()
        val fromAsset = match?.groupValues?.getOrNull(2)?.uppercase()
        val toAsset = match?.groupValues?.getOrNull(4)?.uppercase()
        val confidence = if (amount != null && fromAsset != null && toAsset != null) 0.89f else 0.70f

        return ParseResult(
            intent = IntentType.SwapAsset(
                fromAsset = fromAsset.orEmpty(),
                toAsset = toAsset.orEmpty(),
                amount = amount ?: 0.0,
            ),
            confidenceScore = confidence,
            provider = PARSER_PROVIDER,
            model = PARSER_MODEL,
            promptVersion = PROMPT_VERSION,
            requiresClarification = confidence < ConfidenceThresholds.PREVIEW_THRESHOLD,
            clarificationPrompt = "Please include the amount and both assets for the swap.",
        )
    }

    private fun parseStake(input: String): ParseResult {
        val match = Regex("""stake\s+([0-9]*\.?[0-9]+)\s+([a-zA-Z0-9]+)""").find(input)
        val amount = match?.groupValues?.getOrNull(1)?.toDoubleOrNull()
        val asset = match?.groupValues?.getOrNull(2)?.uppercase()
        val confidence = if (amount != null && asset != null) 0.88f else 0.68f

        return ParseResult(
            intent = IntentType.StakeAsset(
                asset = asset,
                amount = amount ?: 0.0,
            ),
            confidenceScore = confidence,
            provider = PARSER_PROVIDER,
            model = PARSER_MODEL,
            promptVersion = PROMPT_VERSION,
            requiresClarification = confidence < ConfidenceThresholds.PREVIEW_THRESHOLD,
            clarificationPrompt = "Please include the staking amount and asset.",
        )
    }

    private fun parseBalance(): ParseResult = ParseResult(
        intent = IntentType.CheckBalance(),
        confidenceScore = 0.90f,
        provider = PARSER_PROVIDER,
        model = PARSER_MODEL,
        promptVersion = PROMPT_VERSION,
        requiresClarification = false,
    )

    private fun parseHistory(): ParseResult = ParseResult(
        intent = IntentType.ShowHistory(),
        confidenceScore = 0.91f,
        provider = PARSER_PROVIDER,
        model = PARSER_MODEL,
        promptVersion = PROMPT_VERSION,
        requiresClarification = false,
    )

    private fun rejectedResult(): ParseResult = ParseResult(
        intent = IntentType.CheckBalance(),
        confidenceScore = 0.40f,
        provider = PARSER_PROVIDER,
        model = PARSER_MODEL,
        promptVersion = PROMPT_VERSION,
        requiresClarification = true,
        clarificationPrompt =
        "I can help with balance checks, payments, swaps, and staking. " +
            "Try rephrasing the command.",
    )

    private companion object {
        const val PARSER_PROVIDER = "deterministic-fallback"
        const val PARSER_MODEL = "rule-engine-v1"
        const val PROMPT_VERSION = "phase1-bootstrap"
    }
}
