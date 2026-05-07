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

import com.letapay.backend.model.ai.Entities
import com.letapay.backend.model.ai.IntentType
import com.letapay.backend.model.ai.ParseResult

private val SEND_ETH = Regex(
    """send\s+([\d.]+)\s+(\w+)\s+to\s+(0x[a-fA-F0-9]{40}|[\w.-]+\.eth)""",
    RegexOption.IGNORE_CASE,
)
private val SWAP = Regex(
    """swap\s+([\d.]+)\s+(\w+)\s+(?:to|for)\s+(\w+)(?:\s+on\s+(\w+))?""",
    RegexOption.IGNORE_CASE,
)
private val BALANCE_WORDS = setOf("balance", "how much", "what's in", "check wallet")
private val HISTORY_WORDS = setOf("history", "transactions", "activity", "recent", "past")

fun deterministicParse(message: String): ParseResult? =
    parseSend(message)
        ?: parseSwap(message)
        ?: parseBalance(message)
        ?: parseHistory(message)

private fun parseSend(message: String): ParseResult? =
    SEND_ETH.find(message)?.let { match ->
        ParseResult(
            intent = IntentType.SendPayment,
            confidence = 0.95,
            entities = Entities(
                amount = match.groupValues[1],
                asset = match.groupValues[2].uppercase(),
                recipient = match.groupValues[3],
            ),
            missingRequired = emptyList(),
            safetyFlags = emptyList(),
            normalizedCommand = message.trim(),
            parserVersion = "deterministic-v1",
        )
    }

private fun parseSwap(message: String): ParseResult? =
    SWAP.find(message)?.let { match ->
        ParseResult(
            intent = IntentType.SwapAsset,
            confidence = 0.92,
            entities = Entities(
                amount = match.groupValues[1],
                fromAsset = match.groupValues[2].uppercase(),
                toAsset = match.groupValues[3].uppercase(),
                chain = match.groupValues.getOrNull(4)?.let(::resolveChainId),
            ),
            missingRequired = emptyList(),
            safetyFlags = emptyList(),
            normalizedCommand = message.trim(),
            parserVersion = "deterministic-v1",
        )
    }

private fun parseBalance(message: String): ParseResult? {
    val lower = message.lowercase()
    return if (BALANCE_WORDS.any(lower::contains)) {
        ParseResult(
            intent = IntentType.CheckBalance,
            confidence = 0.90,
            missingRequired = emptyList(),
            safetyFlags = emptyList(),
            normalizedCommand = message.trim(),
            parserVersion = "deterministic-v1",
        )
    } else {
        null
    }
}

private fun parseHistory(message: String): ParseResult? {
    val lower = message.lowercase()
    return if (HISTORY_WORDS.any(lower::contains)) {
        ParseResult(
            intent = IntentType.ShowHistory,
            confidence = 0.88,
            missingRequired = emptyList(),
            safetyFlags = emptyList(),
            normalizedCommand = message.trim(),
            parserVersion = "deterministic-v1",
        )
    } else {
        null
    }
}

private fun resolveChainId(name: String): Long? = when (name.lowercase()) {
    "eth", "ethereum", "mainnet" -> 1L
    "poly", "polygon", "matic" -> 137L
    "base" -> 8453L
    else -> null
}
