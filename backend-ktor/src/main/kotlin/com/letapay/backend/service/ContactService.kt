/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.backend.service

import com.letapay.backend.model.contact.ContactRecord

interface ContactService {
    suspend fun list(ownerWallet: String): List<ContactRecord>
}

class StubContactService : ContactService {
    override suspend fun list(ownerWallet: String): List<ContactRecord> = listOf(
        ContactRecord(
            id = "contact_demo_1",
            displayName = "Satoshi Demo",
            walletAddress = "0x000000000000000000000000000000000000dEaD",
        ),
    )
}
