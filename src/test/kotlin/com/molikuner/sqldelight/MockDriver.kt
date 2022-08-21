package com.molikuner.sqldelight

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import io.mockk.every
import io.mockk.mockk

fun mockReadableSyncDriver(databaseVersion: Int, mock: SqlDriver = mockk()): SqlDriver = mock.apply {
    every {
        executeQuery<Long>(
            identifier = 0,
            sql = "PRAGMA user_version",
            mapper = captureLambda(),
            parameters = 0
        )
    } answers {
        val mapper = lambda<(SqlCursor) -> Long>().captured
        QueryResult.Value(mapper(mockk {
            every { getLong(0) } returns databaseVersion.toLong()
        }))
    }
}

fun mockWritableSyncDriver(schemaVersion: Int, mock: SqlDriver = mockk()): SqlDriver = mock.apply {
    every {
        execute(
            identifier = null,
            sql = "PRAGMA user_version = $schemaVersion",
            parameters = 0
        )
    } returns QueryResult.Value(1)
}

fun mockSyncDriver(databaseVersion: Int, schemaVersion: Int): SqlDriver = mockk {
    mockReadableSyncDriver(databaseVersion, mock = this)
    mockWritableSyncDriver(schemaVersion, mock = this)
}

fun mockAsyncDriver(databaseVersion: Int, schemaVersion: Int): SqlDriver = mockk {
    every {
        executeQuery<Long>(
            identifier = 0,
            sql = "PRAGMA user_version",
            mapper = captureLambda(),
            parameters = 0
        )
    } answers {
        val mapper = lambda<(SqlCursor) -> Long>().captured
        QueryResult.AsyncValue {
            mapper(mockk {
                every { getLong(0) } returns databaseVersion.toLong()
            })
        }
    }

    every {
        execute(
            identifier = null,
            sql = "PRAGMA user_version = $schemaVersion",
            parameters = 0
        )
    } returns QueryResult.AsyncValue { 1 }
}
