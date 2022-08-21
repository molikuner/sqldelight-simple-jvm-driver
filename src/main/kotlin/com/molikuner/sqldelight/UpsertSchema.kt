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

import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.db.migrateWithCallbacks

/**
 * This method upserts the provided schema in the database, automatically deciding how
 * whether migrate or create the schema in the first place.
 * **You should not just start using this method in an existing project, as this will
 * likely try to recreate the schema on first run. Instead, this is intended for new
 * projects**
 *
 * **Warning:** This method should only be used with synchronous SqlDrivers like JDBC.
 *
 * @param schema the schema for the DB
 * @param upgradeCallbacks callbacks, that will be executed, when a migration to the
 *                         specified version happens
 * @param downgradeHandler a method to handle downgrades of the database. You might
 *                         decide to drop the database and create it again.
 */
public fun <SyncSqlDriver : SqlDriver> SyncSqlDriver.upsertSchemaSync(
    schema: SqlSchema,
    vararg upgradeCallbacks: AfterVersion,
    downgradeHandler: SyncSqlDriver.(databaseVersion: Int) -> Unit = {
        throw IllegalStateException(
            "Downgrading the database isn't supported out of the box! Database is at version $it whereas the schema is at version ${schema.version}"
        )
    }
): SyncSqlDriver = upsertSchema(
    schema = schema,
    upgradeCallbacks = upgradeCallbacks,
    downgradeHandler = downgradeHandler,
    queryResultGetter = { it.value } // this will fail, when used with an async driver like R2DBC
)

/**
 * This method upserts the provided schema in the database, automatically deciding how
 * whether migrate or create the schema in the first place.
 * **You should not just start using this method in an existing project, as this will
 * likely try to recreate the schema on first run. Instead, this is intended for new
 * projects**
 *
 * The suspend modifier is required for async drivers like R2DBC. When using JDBC, it
 * is safe to use [upsertSchemaSync] instead.
 *
 * @param schema the schema for the DB
 * @param upgradeCallbacks callbacks, that will be executed, when a migration to the
 *                         specified version happens
 * @param downgradeHandler a method to handle downgrades of the database. You might
 *                         decide to drop the database and create it again.
 */
public suspend fun <SuspendingSqlDriver : SqlDriver> SuspendingSqlDriver.upsertSchema(
    schema: SqlSchema,
    vararg upgradeCallbacks: AfterVersion,
    downgradeHandler: SuspendingSqlDriver.(databaseVersion: Int) -> Unit = {
        throw IllegalStateException(
            "Downgrading the database isn't supported out of the box! Database is at version $it whereas the schema is at version ${schema.version}"
        )
    }
): SuspendingSqlDriver = upsertSchema(
    schema = schema,
    upgradeCallbacks = upgradeCallbacks,
    downgradeHandler = downgradeHandler,
    queryResultGetter = { it.await() }
)

private inline fun <Driver : SqlDriver> Driver.upsertSchema(
    schema: SqlSchema,
    vararg upgradeCallbacks: AfterVersion,
    downgradeHandler: Driver.(databaseVersion: Int) -> Unit,
    queryResultGetter: (QueryResult<Long>) -> Long
) = apply {
    val databaseVersion = queryResultGetter(databaseSchemaVersionSqlite()).toInt()
    val schemaVersion = schema.version.toLong()
    when {
        databaseVersion == 0 -> {
            schema.create(this)
            setDatabaseSchemaVersionSqlite(schemaVersion, queryResultGetter)
        }
        databaseVersion < schema.version -> {
            schema.migrateWithCallbacks(this, databaseVersion, schema.version, *upgradeCallbacks)
            setDatabaseSchemaVersionSqlite(schemaVersion, queryResultGetter)
        }
        databaseVersion > schema.version -> {
            downgradeHandler(this, databaseVersion)
            setDatabaseSchemaVersionSqlite(schemaVersion, queryResultGetter)
        }
    }
}
