/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package template.core.base.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver

actual fun createSqlDriver(
    schema: SqlSchema<QueryResult.Value<Unit>>,
    databaseName: String,
    platformContext: Any?,
): SqlDriver {
    val databasePath = AppDatabaseFactory().databasePath(databaseName = databaseName)
    return JdbcSqliteDriver(url = "jdbc:sqlite:$databasePath")
}
