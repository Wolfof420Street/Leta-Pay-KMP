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

import com.letapay.app.core.database.entity.StakingPositionEntity
import kotlinx.coroutines.flow.Flow

interface StakingPositionDao {
    fun observePositions(walletAddress: String): Flow<List<StakingPositionEntity>>
    suspend fun replacePositions(walletAddress: String, positions: List<StakingPositionEntity>)
}
