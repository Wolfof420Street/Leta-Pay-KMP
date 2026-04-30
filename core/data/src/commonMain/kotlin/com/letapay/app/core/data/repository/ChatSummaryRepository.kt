/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.core.data.repository

import kotlinx.coroutines.flow.StateFlow

data class ChatSummaryState(
    val isStreaming: Boolean = false,
    val summaryText: String = "",
    val errorMessage: String? = null,
)

interface ChatSummaryRepository {
    val summaryState: StateFlow<ChatSummaryState>

    suspend fun streamSummary(
        event: String,
        transactionResult: String,
    )

    suspend fun clear()
}
