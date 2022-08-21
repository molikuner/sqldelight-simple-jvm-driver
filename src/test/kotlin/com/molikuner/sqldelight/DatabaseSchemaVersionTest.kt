package com.molikuner.sqldelight

import app.cash.sqldelight.db.QueryResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals

internal class DatabaseSchemaVersionTest {

    @Test
    fun `databaseSchemaVersionSqlite - should get PRAGAM user_version from DB`() {
        // Given
        val expectedDatabaseVersion = 123L
        val driver = mockReadableSyncDriver(expectedDatabaseVersion.toInt())

        // When
        val actualDatabaseVersion = driver.databaseSchemaVersionSqlite().value

        // Then
        assertEquals(expectedDatabaseVersion, actualDatabaseVersion)
        verify { driver.executeQuery<Long>(any(), "PRAGMA user_version", any(), 0) }
    }

    @Test
    fun `setDatabaseSchemaVersionSqlite - should update PRAGMA user_version in DB`() {
        // Given
        val schemaVersion = 123L
        val driver = mockWritableSyncDriver(schemaVersion.toInt())
        val queryResultGetter = mockk<(QueryResult<Long>) -> Long> {
            every { this@mockk(any()) } answers { (arg(0) as QueryResult<Long>).value }
        }

        // When
        driver.setDatabaseSchemaVersionSqlite(schemaVersion, queryResultGetter)

        // Then
        verify { driver.execute(any(), "PRAGMA user_version = $schemaVersion", any()) }
        verify { queryResultGetter(any()) }
    }


}
