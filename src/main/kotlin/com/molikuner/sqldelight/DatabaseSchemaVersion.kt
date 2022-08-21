/**  Copyright 2020-2022 molikuner
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.molikuner.sqldelight

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver

/**
 * Return the current database schema version of a SQLite database.
 *
 * @return the current schema version
 */
public fun SqlDriver.databaseSchemaVersionSqlite(): QueryResult<Long> = executeQuery(
    identifier = 0,
    sql = "PRAGMA user_version",
    mapper = {
        checkNotNull(it.getLong(0)) {
            "Could not get schema version from DB. Make sure it's a SQLite DB!"
        }
    },
    parameters = 0
)


/**
 * Sets the new schema version in the database.
 *
 * @param newVersion the new version, that should be saved in the DB
 * @param queryResultGetter a function, that can wait for the result of the query and return it.
 *                          This is required, to make sure, that the query is done, when returning.
 */
internal inline fun SqlDriver.setDatabaseSchemaVersionSqlite(
    newVersion: Long,
    queryResultGetter: (QueryResult<Long>) -> Long
) {
    // We don't save this statement, i.e. identifier = null, since it will be used only once anyway.
    queryResultGetter(execute(identifier = null, "PRAGMA user_version = $newVersion", 0))
}
