/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.core.model.chat

import kotlinx.serialization.Serializable

@Serializable
data class Contact(
    val walletAddress: String,
    val alias: String? = null,
    val isFavorite: Boolean = false,
    val lastInteractionAtEpochMillis: Long = 0L,
    val source: String = "server",
)
