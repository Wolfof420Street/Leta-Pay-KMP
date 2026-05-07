/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.core.database

import template.core.base.database.createSqlDriver

fun createSqlDelightAppDatabase(platformContext: Any? = null): AppDatabase {
    val driver = createSqlDriver(
        schema = LetaPayDatabase.Schema,
        databaseName = "letapay.db",
        platformContext = platformContext,
    )
    return SqlDelightAppDatabase(driver)
}
