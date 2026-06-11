/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.core.analytics

import template.core.base.analytics.AnalyticsHelper

/**
 * Extension functions for Leta Pay specific analytics operations
 */

fun AnalyticsHelper.trackWalletConnection(address: String) {
    logEvent(
        LetaPayEventTypes.WALLET_CONNECTED,
        LetaPayParamKeys.WALLET_ADDRESS to address,
    )
}

fun AnalyticsHelper.trackAiIntent(command: String, intent: String) {
    logEvent(
        LetaPayEventTypes.AI_INTENT_PARSED,
        mapOf(
            LetaPayParamKeys.COMMAND_TEXT to command,
            LetaPayParamKeys.INTENT_TYPE to intent,
        ),
    )
}

fun AnalyticsHelper.trackSwapExecution(
    from: String,
    toToken: String,
    amount: String,
    toAmount: String,
    txHash: String,
) {
    logEvent(
        LetaPayEventTypes.SWAP_EXECUTED,
        mapOf(
            LetaPayParamKeys.FROM_TOKEN to from,
            LetaPayParamKeys.TO_TOKEN to toToken,
            LetaPayParamKeys.FROM_AMOUNT to amount,
            LetaPayParamKeys.TO_AMOUNT to toAmount,
            LetaPayParamKeys.TRANSACTION_HASH to txHash,
        ),
    )
}

fun AnalyticsHelper.trackGasEstimate(feeUsd: String) {
    logEvent(
        LetaPayEventTypes.GAS_ESTIMATED,
        LetaPayParamKeys.ESTIMATED_FEE_USD to feeUsd,
    )
}

fun AnalyticsHelper.letapayTracker(): LetaPayAnalyticsTracker = LetaPayAnalyticsTracker(this)
