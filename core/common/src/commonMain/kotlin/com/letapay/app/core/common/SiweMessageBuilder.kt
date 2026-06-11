/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.core.common

import kotlinx.datetime.Instant

fun buildSiweMessage(
    domain: String,
    address: String,
    uri: String,
    nonce: String,
    chainId: Int,
    issuedAt: Instant,
    statement: String? = null,
): String = buildString {
    appendLine("$domain wants you to sign in with your Ethereum account:")
    appendLine(address)
    appendLine()
    if (!statement.isNullOrBlank()) {
        appendLine(statement.trim())
        appendLine()
    }
    appendLine("URI: $uri")
    appendLine("Version: 1")
    appendLine("Chain ID: $chainId")
    appendLine("Nonce: $nonce")
    appendLine("Issued At: $issuedAt")
}
