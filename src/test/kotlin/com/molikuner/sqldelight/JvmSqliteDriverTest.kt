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

import com.molikuner.sqldelight.JvmSqliteDriver.Companion.normalize
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import kotlin.test.Test
import kotlin.test.assertEquals

class JvmSqliteDriverTest {

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
}
