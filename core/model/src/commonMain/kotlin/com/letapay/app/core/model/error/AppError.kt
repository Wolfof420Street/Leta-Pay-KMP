/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.core.model.error

sealed class AppError(
    open val displayMessage: String = "Unknown error",
) : Exception(displayMessage) {
    data class Network(
        override val displayMessage: String = "Network error",
    ) : AppError(displayMessage)

    data class Unauthorized(
        override val displayMessage: String,
    ) : AppError(displayMessage)

    data class InsufficientBalance(
        val required: String,
        val actual: String,
    ) : AppError("Insufficient balance: required=$required actual=$actual")

    data class QuoteExpired(
        val expiresAtEpochMillis: Long,
    ) : AppError("Quote expired at $expiresAtEpochMillis")

    data class Validation(
        val field: String,
        val reason: String,
    ) : AppError("$field: $reason")

    data class ChainRejected(
        val reason: String,
        val code: Int,
    ) : AppError(reason)

    data class Unexpected(
        val causeMessage: String = "Unknown error",
    ) : AppError(causeMessage)
}
