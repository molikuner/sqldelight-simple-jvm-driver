/**  Copyright 2022 molikuner
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
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class UpsertSchemaSyncTest {

    @Test
    fun `upsertSchemaSync - uninitialized database - should create schema`() {
        // Given
        val schema = mockk<SqlSchema> {
            every { version } returns 123
            every { create(any()) } returns QueryResult.Unit
        }
        val driver = mockSyncDriver(databaseVersion = 0, schema.version)

        // When
        driver.upsertSchemaSync(schema)

        // Then
        verify { schema.create(driver) }
    }

    @Test
    fun `upsertSchemaSync - older database - should migrate`() {
        // Given
        val schema = mockk<SqlSchema> {
            every { version } returns 123
            every { migrate(any(), any(), any()) } returns QueryResult.Unit
        }
        val driver = mockSyncDriver(databaseVersion = 10, schema.version)

        // When
        driver.upsertSchemaSync(schema)

        // Then
        verify { schema.migrate(driver, 10, 123) }
    }

    @Test
    fun `upsertSchemaSync - newer database - should call downgradeHandler`() {
        // Given
        val schema = mockk<SqlSchema> {
            every { version } returns 123
        }
        val driver = mockSyncDriver(databaseVersion = 1234, schema.version)

        val downgradeHandler = mockk<SqlDriver.(Int) -> Unit> {
            justRun { this@mockk(any(), any()) }
        }

        // When
        driver.upsertSchemaSync(schema, downgradeHandler = downgradeHandler)

        // Then
        verify { downgradeHandler(driver, 1234) }
    }

    @Test
    fun `upsertSchemaSync default downgradeHandler - when invoked - should throw exception`() {
        // Given
        val schema = mockk<SqlSchema> {
            every { version } returns 123
        }
        val driver = mockSyncDriver(databaseVersion = 1234, schema.version)

        // When
        val exception = runCatching { driver.upsertSchemaSync(schema) }.exceptionOrNull()

        // Then
        assertNotNull(exception, "Expected the driver creation to fail, but no exception was thrown")
        assertEquals(
            expected = "Downgrading the database isn't supported out of the box! Database is at version 1234 whereas the schema is at version ${schema.version}",
            actual = exception.message
        )
        assertEquals(IllegalStateException::class, exception::class)
    }

    @Test
    fun `upsertSchemaSync - upgrade callbacks are called when upgrading`() {
        // Given
        val schema = mockk<SqlSchema> {
            every { version } returns 1234
            every { migrate(any(), any(), any()) } returns QueryResult.Unit
        }
        val driver = mockSyncDriver(databaseVersion = 1, schema.version)
        val migrationCallback12 = mockk<(SqlDriver) -> Unit> {
            justRun { this@mockk(any()) }
        }
        val migrationCallback123 = mockk<(SqlDriver) -> Unit> {
            justRun { this@mockk(any()) }
        }

        // When
        driver.upsertSchemaSync(
            schema,
            upgradeCallbacks = arrayOf(
                AfterVersion(12, migrationCallback12),
                AfterVersion(123, migrationCallback123)
            )
        )

        // Then
        verifyOrder {
            schema.migrate(driver, 1, 13)
            migrationCallback12(driver)
            schema.migrate(driver, 13, 124)
            migrationCallback123(driver)
            schema.migrate(driver, 124, 1234)
        }
    }
}
