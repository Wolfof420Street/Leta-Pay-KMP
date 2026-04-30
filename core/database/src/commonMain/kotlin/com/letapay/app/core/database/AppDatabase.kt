/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.core.database

import com.letapay.app.core.database.dao.ChatMessageDao
import com.letapay.app.core.database.dao.ContactDao
import com.letapay.app.core.database.dao.DeviceTokenDao
import com.letapay.app.core.database.dao.PendingMessageDao
import com.letapay.app.core.database.dao.TransactionDao

interface AppDatabase {
    val transactionDao: TransactionDao
    val deviceTokenDao: DeviceTokenDao
    val chatMessageDao: ChatMessageDao
    val pendingMessageDao: PendingMessageDao
    val contactDao: ContactDao
}
