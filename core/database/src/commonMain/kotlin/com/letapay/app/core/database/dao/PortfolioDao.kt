/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.core.database.dao

import kotlinx.coroutines.flow.Flow

interface PortfolioDao {
    fun getLatestBalance(walletAddress: String): Flow<String?>
    suspend fun upsertBalance(walletAddress: String, serializedData: String, updatedAt: Long)
    suspend fun getCachedSpotPrice(cacheKey: String): String?
    suspend fun upsertSpotPrice(cacheKey: String, price: String, updatedAt: Long)
}
