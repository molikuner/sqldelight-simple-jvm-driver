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

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.util.Properties

/**
 * A simple wrapper for the SqlDriver defined for the JVM, which automatically
 * upgrades and initializes the DB.
 * **You should not just replace the JdbcSqliteDriver with this class in a
 * running system, but it's useful on new systems.**
 *
 * @param schema your schema for the DB
 * @param path your path to the DB file/[JvmSqliteDriver.IN_MEMORY] for in memory DBs
 * @param properties your properties for the underlying JdbcSqliteDriver
 * @param downgradeHandler a method to handle downgrades of the database. You might
 *                         decide to drop the database and create it again.
 */
public class JvmSqliteDriver @JvmOverloads public constructor(
    schema: SqlDriver.Schema,
    path: String,
    properties: Properties = Properties(),
    downgradeHandler: JvmSqliteDriver.(databaseVersion: Int) -> Unit = {
        throw IllegalStateException(
            "Downgrading the database isn't supported out of the box! Database is at version $it whereas the schema is at version ${schema.version}"
        )
    }
) : SqlDriver by JdbcSqliteDriver(normalize(path), properties) {

    init {
        val databaseVersion = databaseSchemaVersion()
        when {
            databaseVersion == 0 -> {
                schema.create(this)
                setDatabaseSchemaVersion(schema.version)
            }
            databaseVersion < schema.version -> {
                schema.migrate(this, databaseVersion, schema.version)
                setDatabaseSchemaVersion(schema.version)
            }
            databaseVersion > schema.version -> {
                downgradeHandler(this, databaseVersion)
                setDatabaseSchemaVersion(schema.version)
            }
        }
    }

    /**
     * Return the current database schema version. Useful when migrating,
     * but should always be the newest version. Recreate this driver to migrate.
     *
     * @return the current schema version
     */
    public fun databaseSchemaVersion(): Int = executeQuery(
        identifier = 0,
        sql = "PRAGMA user_version",
        mapper = { it.getLong(0)?.toInt() ?: throw IllegalStateException("Could not get schema version from db") },
        parameters = 0
    )

    private fun setDatabaseSchemaVersion(newVersion: Int) {
        // we don't save this statement, i.e. identifier = null, since it will be used only once anyway
        execute(identifier = null, "PRAGMA user_version = $newVersion", 0)
    }

    public companion object {
        /**
         * A simple string to create a in memory DB. Pass as path parameter in [JvmSqliteDriver] constructor.
         */
        public const val IN_MEMORY: String = ""

        private val normalizationRegex = "^(?:jdbc:)?(?:sqlite:)?(.*)$".toRegex()
        internal fun normalize(path: String): String =
            "jdbc:sqlite:${normalizationRegex.matchEntire(path)?.groupValues?.get(1) ?: throw IllegalArgumentException("Could not normalize database path")}"
    }
}
