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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import template.core.base.analytics.rememberAnalyticsHelper

/**
 * Leta Pay specific Compose analytics utilities
 */

@Composable
fun TrackLetaPayScreen(
    screenName: String,
    walletAddress: String? = null,
    additionalParams: Map<String, String> = emptyMap(),
) {
    val analytics = rememberAnalyticsHelper()

    LaunchedEffect(screenName) {
        val params = mutableMapOf<String, String>()
        walletAddress?.let { params[LetaPayParamKeys.WALLET_ADDRESS] = it }
        params.putAll(additionalParams)

        analytics.logScreenView(screenName)
        if (params.isNotEmpty()) {
            analytics.logEvent("screen_context", params)
        }
    }
}

@Composable
fun TrackAiInteraction(command: String, isStreaming: Boolean) {
    val analytics = rememberAnalyticsHelper()

    LaunchedEffect(command) {
        if (!isStreaming) {
            analytics.logEvent(
                LetaPayEventTypes.AI_COMMAND_SUBMITTED,
                LetaPayParamKeys.COMMAND_TEXT to command,
            )
        }
    }
}
