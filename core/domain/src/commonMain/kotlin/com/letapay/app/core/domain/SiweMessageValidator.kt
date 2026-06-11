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

class SiweValidationException(
    val error: SiweValidationError,
) : RuntimeException(error.toString())

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
        Result.failure(SiweValidationException(SiweValidationError.ParsingError))
    }

    fun validate(
        message: SiweMessage,
        expectedNonce: String,
        config: SiweConfig,
    ): Result<Unit> {
        val nowEpochMillis = currentTimeMillis()
        val messageEpochMillis = message.issuedAt.toEpochMilliseconds()
        val messageOrigin = message.uri.toOriginOrNull()
        val expectedOrigin = config.appOrigin.toOriginOrNull()
        val res = when {
            message.nonce != expectedNonce ->
                Result.failure(SiweValidationException(SiweValidationError.InvalidNonce))
            message.domain != config.appDomain ->
                Result.failure(SiweValidationException(SiweValidationError.DomainMismatch))
            messageOrigin == null || expectedOrigin == null || messageOrigin != expectedOrigin ->
                Result.failure(SiweValidationException(SiweValidationError.UriMismatch))
            messageEpochMillis > nowEpochMillis ||
                messageEpochMillis < nowEpochMillis - 5.minutes.inWholeMilliseconds ->
                Result.failure(SiweValidationException(SiweValidationError.MessageExpired))
            else -> Result.success(Unit)
        }
        return res
    }
}

expect fun currentTimeMillis(): Long

private fun String.toOriginOrNull(): String? {
    val trimmed = trim()
    val schemeIndex = trimmed.indexOf("://")
    val scheme = trimmed.substring(0, schemeIndex).takeIf { schemeIndex > 0 }?.lowercase()
        ?: return null
    val authorityPart = trimmed.substring(schemeIndex + 3)
        .substringBefore('/')
        .substringBefore('?')
        .substringBefore('#')
    return authorityPart.takeIf(String::isNotBlank)?.let { "$scheme://$it" }
}
