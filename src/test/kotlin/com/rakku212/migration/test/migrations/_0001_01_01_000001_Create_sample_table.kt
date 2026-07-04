@file:Suppress("ClassName")

package com.rakku212.migration.test.migrations

import com.rakku212.migration.Migration
import com.rakku212.migration.MigrationContext
import java.sql.Connection

class _0001_01_01_000001_Create_sample_table(
    context: MigrationContext,
) : Migration(context) {
    override fun execute(connection: Connection) {
        connection.createStatement().use { stmt ->
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS sample (
                    id INTEGER PRIMARY KEY
                )
                """.trimIndent(),
            )
        }
    }
}
