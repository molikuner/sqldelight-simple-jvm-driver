/**  Copyright 2019 molikuner
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

import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.db.use
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
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
 */
class JvmSqliteDriver @JvmOverloads constructor(
    schema: SqlDriver.Schema,
    path: String,
    properties: Properties = Properties()
) : SqlDriver by JdbcSqliteDriver(path.normalizedDBPath, properties) {

    init {
        val initSchemaVersion = databaseSchemaVersion()
        when {
            initSchemaVersion == 0 -> {
                schema.create(this)
                setDatabaseSchemaVersion(schema.version)
            }
            initSchemaVersion < schema.version -> {
                schema.migrate(this, initSchemaVersion, schema.version)
                setDatabaseSchemaVersion(schema.version)
            }
            initSchemaVersion > schema.version -> throw IllegalStateException("You can't downgrade?")
        }
    }

    /**
     * Return the current database schema version. Useful when migrating,
     * but should always be the newest version. Recreate this driver to migrate.
     *
     * @return the current schema version
     */
    public fun databaseSchemaVersion(): Int = executeQuery(null, "PRAGMA user_version", 0).use {
        it.getLong(0)?.toInt() ?: throw IllegalStateException("Could not get schema version from db")
    }

    private fun setDatabaseSchemaVersion(newVersion: Int) {
        execute(null, "PRAGMA user_version = $newVersion", 0)
    }

    companion object {
        /**
         * A simple string to create a in memory DB. Pass as path parameter in [JvmSqliteDriver] constructor.
         */
        public const val IN_MEMORY: String = ""

        private val String.normalizedDBPath: String
            get() = "jdbc:sqlite:${"^(?:jdbc:)?(?:sqlite:)?(.+)$".toRegex().matchEntire(this)?.groupValues?.get(1)
                ?: throw IllegalArgumentException("Could not normalize database path")}"
    }
}
