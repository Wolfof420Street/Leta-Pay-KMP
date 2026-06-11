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
 * Helper class for Leta Pay specific analytics tracking
 */
class LetaPayAnalyticsTracker(private val analyticsHelper: AnalyticsHelper) {

    fun trackWalletAuth(event: String, address: String? = null, error: String? = null) {
        val params = mutableMapOf<String, String>()
        address?.let { params[LetaPayParamKeys.WALLET_ADDRESS] = it }
        error?.let { params[LetaPayParamKeys.ERROR_MESSAGE] = it }
        analyticsHelper.logEvent(event, params)
    }

    fun trackAiCommand(command: String, intent: String? = null) {
        val params = mutableMapOf(LetaPayParamKeys.COMMAND_TEXT to command)
        intent?.let { params[LetaPayParamKeys.INTENT_TYPE] = it }
        analyticsHelper.logEvent(LetaPayEventTypes.AI_COMMAND_SUBMITTED, params)
    }

    fun trackDeFiOperation(
        operation: String,
        fromToken: String,
        toToken: String? = null,
        amount: String? = null,
        txHash: String? = null,
    ) {
        val params = mutableMapOf(
            LetaPayParamKeys.ACTION_TYPE to operation,
            LetaPayParamKeys.FROM_TOKEN to fromToken,
        )
        toToken?.let { params[LetaPayParamKeys.TO_TOKEN] = it }
        amount?.let { params[LetaPayParamKeys.FROM_AMOUNT] = it }
        txHash?.let { params[LetaPayParamKeys.TRANSACTION_HASH] = it }
        analyticsHelper.logEvent(operation, params)
    }

    fun trackTransaction(txHash: String, chainId: String, status: String) {
        val eventType = when (status.lowercase()) {
            "confirmed" -> LetaPayEventTypes.TRANSACTION_CONFIRMED
            "failed" -> LetaPayEventTypes.TRANSACTION_FAILED
            else -> LetaPayEventTypes.TRANSACTION_SENT
        }
        analyticsHelper.logEvent(
            eventType,
            mapOf(
                LetaPayParamKeys.TRANSACTION_HASH to txHash,
                LetaPayParamKeys.CHAIN_ID to chainId,
                LetaPayParamKeys.STATUS to status,
            ),
        )
    }
}
