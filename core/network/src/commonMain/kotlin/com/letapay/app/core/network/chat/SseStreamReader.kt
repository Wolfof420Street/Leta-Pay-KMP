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

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.readUTF8Line

class SseStreamReader {
    suspend fun readDataStream(
        response: HttpResponse,
        onData: (String) -> Unit,
    ) {
        val channel = response.bodyAsChannel()
        while (!channel.isClosedForRead) {
            val line = channel.readUTF8Line() ?: break
            if (line.startsWith(DATA_PREFIX)) {
                val payload = line.removePrefix(DATA_PREFIX).trim()
                if (payload.isNotBlank() && payload != DONE_MARKER) {
                    onData(payload)
                }
            }
        }
    }

    private companion object {
        const val DATA_PREFIX = "data:"
        const val DONE_MARKER = "[DONE]"
    }
}
