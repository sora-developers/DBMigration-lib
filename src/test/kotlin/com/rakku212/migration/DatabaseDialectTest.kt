package com.rakku212.migration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DatabaseDialectTest {
    @Test
    fun `JDBC URL から方言を判定する`() {
        assertEquals(DatabaseDialect.SQLITE, DatabaseDialect.fromJdbcUrl("jdbc:sqlite:file:test.db"))
        assertEquals(DatabaseDialect.MYSQL, DatabaseDialect.fromJdbcUrl("jdbc:mysql://localhost:3306/app"))
        assertEquals(DatabaseDialect.MARIADB, DatabaseDialect.fromJdbcUrl("jdbc:mariadb://localhost:3306/app"))
    }

    @Test
    fun `未対応の JDBC URL は例外になる`() {
        assertFailsWith<MigrationException> {
            DatabaseDialect.fromJdbcUrl("jdbc:postgresql://localhost:5432/app")
        }
    }

    @Test
    fun `SQLite 用の履歴テーブル DDL を生成する`() {
        val sql = DatabaseDialect.SQLITE.createHistoryTableSql("migration_history")

        assertTrue(sql.contains("INTEGER PRIMARY KEY AUTOINCREMENT"))
        assertTrue(sql.contains("migration_name TEXT NOT NULL UNIQUE"))
    }

    @Test
    fun `MySQL 用の履歴テーブル DDL を生成する`() {
        val sql = DatabaseDialect.MYSQL.createHistoryTableSql("migration_history")

        assertTrue(sql.contains("BIGINT AUTO_INCREMENT PRIMARY KEY"))
        assertTrue(sql.contains("migration_name VARCHAR(255) NOT NULL UNIQUE"))
        assertTrue(sql.contains("ENGINE=InnoDB"))
    }

    @Test
    fun `MariaDB 用の履歴テーブル DDL を生成する`() {
        val sql = DatabaseDialect.MARIADB.createHistoryTableSql("migration_history")

        assertTrue(sql.contains("BIGINT AUTO_INCREMENT PRIMARY KEY"))
        assertTrue(sql.contains("migration_name VARCHAR(255) NOT NULL UNIQUE"))
        assertTrue(sql.contains("ENGINE=InnoDB"))
    }
}
