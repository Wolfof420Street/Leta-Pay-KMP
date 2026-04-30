/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.core.network.ai

import com.letapay.app.core.model.ai.ParseRequest
import com.letapay.app.core.model.ai.PlanRequest
import com.letapay.app.core.network.bodyOrThrow
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody

class AiApi(
    private val client: HttpClient,
) {
    suspend fun parseCommand(sessionToken: String, message: String): String {
        return client.post("/ai/parse") {
            bearerAuth(sessionToken)
            setBody(ParseRequest(message))
        }.bodyOrThrow()
    }

    suspend fun planCommand(sessionToken: String, message: String): String {
        return client.post("/ai/plan") {
            bearerAuth(sessionToken)
            setBody(PlanRequest(message))
        }.bodyOrThrow()
    }
}
