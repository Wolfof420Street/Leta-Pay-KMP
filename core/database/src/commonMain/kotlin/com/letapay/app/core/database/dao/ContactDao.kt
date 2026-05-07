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

import com.letapay.app.core.database.entity.ContactEntity
import kotlinx.coroutines.flow.Flow

interface ContactDao {
    fun observeContacts(ownerWallet: String): Flow<List<ContactEntity>>

    suspend fun upsertContacts(ownerWallet: String, contacts: List<ContactEntity>)

    suspend fun deleteContact(ownerWallet: String, contactWallet: String)
}
