/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.core.network.chat

import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.parameter
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpStatement

class ChatSummaryApi(
    private val client: HttpClient,
) {
    suspend fun prepareSummaryStream(
        sessionToken: String,
        event: String,
        transactionResult: String,
    ): HttpStatement = client.prepareGet(buildChatStreamUrl("", event, sessionToken)) {
        bearerAuth(sessionToken)
        parameter("txHash", transactionResult)
    }
}
