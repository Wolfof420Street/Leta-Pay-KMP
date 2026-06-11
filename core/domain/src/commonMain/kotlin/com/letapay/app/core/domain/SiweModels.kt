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

import kotlinx.datetime.Instant

data class SiweMessage(
    val domain: String,
    val address: String,
    val uri: String,
    val version: String,
    val chainId: Int,
    val nonce: String,
    val issuedAt: Instant,
)

data class SiweConfig(
    val appDomain: String,
    val appOrigin: String,
)

sealed class SiweValidationError {
    data object InvalidNonce : SiweValidationError()
    data object DomainMismatch : SiweValidationError()
    data object UriMismatch : SiweValidationError()
    data object MessageExpired : SiweValidationError()
    data object ParsingError : SiweValidationError()
}
