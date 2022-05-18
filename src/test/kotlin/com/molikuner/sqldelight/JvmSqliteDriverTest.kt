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

import com.molikuner.sqldelight.JvmSqliteDriver.Companion.IN_MEMORY
import com.molikuner.sqldelight.JvmSqliteDriver.Companion.normalize
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import java.sql.DriverManager
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class JvmSqliteDriverTest {
    @AfterTest
    fun tearDown() = unmockkAll()

    @Test
    fun `normalize - empty string - should return IN_MEMMORY`() {
        // Given
        val input = ""

        // When
        val normalized = normalize(input)

        // Then
        assertEquals(JdbcSqliteDriver.IN_MEMORY, normalized)
    }

    @Test
    fun `normalize - jdbc missing without filename - should add jdbc`() {
        // Given
        val input = "sqlite:"

        // When
        val normalized = normalize(input)

        // Then
        assertEquals("jdbc:sqlite:", normalized)
    }

    @Test
    fun `normalize - sqlite missing without filename - should add sqlite`() {
        // Given
        val input = "jdbc:"

        // When
        val normalized = normalize(input)

        // Then
        assertEquals("jdbc:sqlite:", normalized)
    }

    @Test
    fun `normalize - complete path for IN_MEMORY - should return without modification`() {
        // Given
        val input = "jdbc:sqlite:"

        // When
        val normalized = normalize(input)

        // Then
        assertEquals(input, normalized)
    }

    @Test
    fun `normalize - only filename - should add jdbc and sqlite`() {
        // Given
        val input = "abc.file"

        // When
        val normalized = normalize(input)

        // Then
        assertEquals("jdbc:sqlite:abc.file", normalized)
    }

    @Test
    fun `normalize - jdbc missing with filename - should add jdbc`() {
        // Given
        val input = "sqlite:abc.file"

        // When
        val normalized = normalize(input)

        // Then
        assertEquals("jdbc:sqlite:abc.file", normalized)
    }

    @Test
    fun `normalize - sqlite missing with filename - should add sqlite`() {
        // Given
        val input = "jdbc:abc.file"

        // When
        val normalized = normalize(input)

        // Then
        assertEquals("jdbc:sqlite:abc.file", normalized)
    }

    @Test
    fun `normalize - complete path for file - should return without modification`() {
        // Given
        val input = "jdbc:sqlite:abc.file"

        // When
        val normalized = normalize(input)

        // Then
        assertEquals(input, normalized)
    }

    @Test
    fun `init - uninitialized database - should create schema`() {
        // Give
        val schema = mockk<SqlDriver.Schema> {
            every { version } returns 123
            justRun { create(any()) }
        }
        mockDriverWithDatabaseVersion(databaseVersion = 0, schema.version)

        // When
        val driver = JvmSqliteDriver(schema, IN_MEMORY)

        // Then
        verify { schema.create(driver) }
    }

    @Test
    fun `init - older database - should migrate`() {
        // Give
        val schema = mockk<SqlDriver.Schema> {
            every { version } returns 123
            justRun { migrate(any(), any(), any()) }
        }
        mockDriverWithDatabaseVersion(databaseVersion = 10, schema.version)

        // When
        val driver = JvmSqliteDriver(schema, IN_MEMORY)

        // Then
        verify { schema.migrate(driver, 10, 123) }
    }

    @Test
    fun `init - newer database - should call downgradeHandler`() {
        // Give
        val schema = mockk<SqlDriver.Schema> {
            every { version } returns 123
        }
        mockDriverWithDatabaseVersion(databaseVersion = 1234, schema.version)

        val downgradeHandler = mockk<JvmSqliteDriver.(Int) -> Unit> {
            justRun { this@mockk(any(), any()) }
        }

        // When
        val driver = JvmSqliteDriver(schema, IN_MEMORY, downgradeHandler = downgradeHandler)

        // Then
        verify { downgradeHandler(driver, 1234) }
    }

    @Test
    fun `default downgradeHandler - when invoked - should throw exception`() {
        // Give
        val schema = mockk<SqlDriver.Schema> {
            every { version } returns 123
        }
        mockDriverWithDatabaseVersion(databaseVersion = 1234, schema.version)

        // When
        val exception = runCatching { JvmSqliteDriver(schema, IN_MEMORY) }.exceptionOrNull()

        // Then
        assertNotNull(exception, "Expected the driver creation to fail, but no exception was thrown")
        assertEquals(
            expected = "Downgrading the database isn't supported out of the box! Database is at version 1234 whereas the schema is at version ${schema.version}",
            actual = exception.message
        )
        assertEquals(IllegalStateException::class, exception::class)
    }

    private fun mockDriverWithDatabaseVersion(databaseVersion: Int, schemaVersion: Int) {
        mockkStatic(DriverManager::class)
        every { DriverManager.getConnection(any(), any()) } returns mockk()

        mockkConstructor(JdbcSqliteDriver::class)
        every {
            anyConstructed<JdbcSqliteDriver>().executeQuery(
                identifier = 0,
                sql = "PRAGMA user_version",
                parameters = 0
            )
        } returns mockk {
            every { getLong(0) } returns databaseVersion.toLong()
            justRun { close() }
        }
        justRun {
            anyConstructed<JdbcSqliteDriver>().execute(
                identifier = null,
                sql = "PRAGMA user_version = $schemaVersion",
                parameters = 0
            )
        }
    }
}
