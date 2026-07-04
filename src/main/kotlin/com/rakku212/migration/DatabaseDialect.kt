package com.rakku212.migration

import java.sql.Connection

/**
 * サポートするデータベース方言
 *
 * 履歴テーブル（[MigrationConfig.historyTableName]）の CREATE TABLE 文を方言ごとに生成する
 * [MigrateManager] は [MigrationConfig.dialect] が未指定の場合
 * 接続の JDBC URL または `databaseProductName` から自動判定する
 *
 * @see MigrationConfig.dialect
 */
enum class DatabaseDialect {
    /** SQLite（`jdbc:sqlite:`） */
    SQLITE,

    /** MySQL（`jdbc:mysql:`） */
    MYSQL,

    /** MariaDB（`jdbc:mariadb:`） */
    MARIADB,
    ;

    /**
     * マイグレーション履歴テーブルを作成する DDL を返す
     *
     * `CREATE TABLE IF NOT EXISTS` を含むため、既存テーブルは変更されない
     *
     * @param tableName 履歴テーブル名（[MigrationConfig.historyTableName]）
     */
    fun createHistoryTableSql(tableName: String): String = when (this) {
        SQLITE -> """
            CREATE TABLE IF NOT EXISTS $tableName (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                migration_name TEXT NOT NULL UNIQUE,
                executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """.trimIndent()

        MYSQL, MARIADB -> """
            CREATE TABLE IF NOT EXISTS $tableName (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                migration_name VARCHAR(255) NOT NULL UNIQUE,
                executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """.trimIndent()
    }

    companion object {
        /**
         * JDBC URL から方言を判定する
         *
         * @param jdbcUrl 接続 URL（大文字小文字を区別しない）
         * @return 対応する [DatabaseDialect]
         * @throws MigrationException `jdbc:sqlite:` / `jdbc:mysql:` / `jdbc:mariadb:` 以外の URL
         */
        fun fromJdbcUrl(jdbcUrl: String): DatabaseDialect {
            val normalized = jdbcUrl.lowercase()
            return when {
                normalized.startsWith("jdbc:sqlite:") -> SQLITE
                normalized.startsWith("jdbc:mysql:") -> MYSQL
                normalized.startsWith("jdbc:mariadb:") -> MARIADB
                else -> throw MigrationException("サポートされていない JDBC URL です: $jdbcUrl")
            }
        }

        /**
         * 既存の JDBC [Connection] から方言を判定する
         *
         * まず [fromJdbcUrl] を試し、失敗した場合は
         * [java.sql.DatabaseMetaData.getDatabaseProductName] でフォールバックする
         *
         * @param connection 判定に使う接続（読み取りのみ）
         * @throws MigrationException 判定できないデータベースの場合
         */
        fun fromConnection(connection: Connection): DatabaseDialect {
            val jdbcUrl = connection.metaData.url
            return try {
                fromJdbcUrl(jdbcUrl)
            } catch (_: MigrationException) {
                when (connection.metaData.databaseProductName.lowercase()) {
                    "sqlite" -> SQLITE
                    "mysql" -> MYSQL
                    "mariadb" -> MARIADB
                    else -> throw MigrationException(
                        "サポートされていないデータベースです: ${connection.metaData.databaseProductName}",
                    )
                }
            }
        }
    }
}
