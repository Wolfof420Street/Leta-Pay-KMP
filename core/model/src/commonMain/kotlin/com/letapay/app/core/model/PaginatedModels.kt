/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.core.model

import kotlinx.serialization.Serializable

@Serializable
data class PaginatedRequest(
    val limit: Int = 20,
    val offset: Int = 0,
) {
    val clampedLimit: Int get() = limit.coerceIn(1, 100)
}

@Serializable
data class PaginatedResponse<T>(
    val items: List<T>,
    val total: Int,
    val offset: Int,
    val limit: Int,
)
