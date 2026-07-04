@file:Suppress("ClassName")

package com.rakku212.migration.test.failing

import com.rakku212.migration.Migration
import com.rakku212.migration.MigrationContext
import java.sql.Connection

class _0001_01_01_000001_Failing_migration(
    context: MigrationContext,
) : Migration(context) {
    override fun execute(connection: Connection) {
        connection.createStatement().use { stmt ->
            stmt.execute("INVALID SQL STATEMENT")
        }
    }
}
