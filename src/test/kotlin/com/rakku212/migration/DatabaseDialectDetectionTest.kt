package com.rakku212.migration

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlin.test.Test
import kotlin.test.assertEquals

class DatabaseDialectDetectionTest {
    @Test
    fun `SQLite 接続から方言を自動判定する`() {
        HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = "jdbc:sqlite:file:dialect_detection_test?mode=memory&cache=shared"
                maximumPoolSize = 1
            },
        ).use { dataSource ->
            dataSource.connection.use { connection ->
                assertEquals(DatabaseDialect.SQLITE, DatabaseDialect.fromConnection(connection))
            }
        }
    }
}
