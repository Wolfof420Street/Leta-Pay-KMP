/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.core.domain

import kotlin.time.Duration.Companion.minutes
import kotlin.time.Clock

class SiweMessageValidator {

    fun parse(message: String): Result<SiweMessage> = try {
        val lines = message.lines()
        val domain = lines[0].substringBefore(" wants")
        val address = lines[1]
        val uri = lines.find { it.startsWith("URI: ") }?.substringAfter("URI: ")
            ?: throw Exception("Missing URI")
        val version = lines.find { it.startsWith("Version: ") }?.substringAfter("Version: ")
            ?: throw Exception("Missing Version")
        val chainId = lines.find { it.startsWith("Chain ID: ") }?.substringAfter("Chain ID: ")?.toIntOrNull()
            ?: throw Exception("Missing Chain ID")
        val nonce = lines.find { it.startsWith("Nonce: ") }?.substringAfter("Nonce: ")
            ?: throw Exception("Missing Nonce")
        val issuedAtStr = lines.find { it.startsWith("Issued At: ") }?.substringAfter("Issued At: ")
            ?: throw Exception("Missing Issued At")
        val issuedAt = kotlinx.datetime.Instant.parse(issuedAtStr)

        Result.success(
            SiweMessage(
                domain = domain,
                address = address,
                uri = uri,
                version = version,
                chainId = chainId,
                nonce = nonce,
                issuedAt = issuedAt,
            ),
        )
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun validate(
        message: SiweMessage,
        expectedNonce: String,
        config: SiweConfig,
    ): Result<Unit> {
        val nowEpochMillis = Clock.System.now().toEpochMilliseconds()
        val messageEpochMillis = message.issuedAt.toEpochMilliseconds()
        val res = when {
            message.nonce != expectedNonce -> Result.failure(Exception("invalid_nonce"))
            message.domain != config.appDomain -> Result.failure(Exception("domain_mismatch"))
            !message.uri.startsWith(config.appOrigin) -> Result.failure(Exception("uri_mismatch"))
            messageEpochMillis > nowEpochMillis ||
                messageEpochMillis < nowEpochMillis - 5.minutes.inWholeMilliseconds ->
                Result.failure(Exception("message_expired"))
            else -> Result.success(Unit)
        }
        return res
    }
}
