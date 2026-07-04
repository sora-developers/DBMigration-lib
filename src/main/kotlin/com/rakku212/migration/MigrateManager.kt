package com.rakku212.migration

import java.sql.Connection
import java.sql.SQLException

/**
 * マイグレーションの検出・実行を行うエントリポイント
 *
 * [runMigrations] を呼び出すと、以下を順に行う
 *
 * 1. 履歴テーブル（[MigrationConfig.historyTableName]）を作成（存在しない場合）
 * 2. 未実行の [Migration] を [Migration.name] の昇順で検出
 * 3. 各マイグレーションを個別トランザクションで実行し、成功時に履歴へ記録
 *
 * 2 回目以降の呼び出しでは、履歴に存在するマイグレーションはスキップされる
 *
 * ```
 * val manager = MigrateManager(context, config)
 * manager.runMigrations()
 * ```
 *
 * @param context データソースとロガー
 * @param config マイグレーション検出と履歴テーブルの設定
 * @see MigrationContext
 * @see MigrationConfig
 */
class MigrateManager(
    private val context: MigrationContext,
    private val config: MigrationConfig,
) {
    private val dialect: DatabaseDialect by lazy { resolveDialect() }

    /**
     * 未実行のマイグレーションをすべて実行する
     *
     * 未実行のマイグレーションがない場合は何もせずに返る
     *
     * @throws MigrationException 履歴テーブルの作成、検出、または実行中にエラーが発生した場合
     */
    fun runMigrations() {
        try {
            createMigrationTable()
            val executedMigrations = getExecutedMigrations()
            executeMigrations(executedMigrations)
        } catch (e: SQLException) {
            context.logger.severe("マイグレーション実行中にエラーが発生しました: ${e.message}")
            throw MigrationException("マイグレーション実行中にエラーが発生しました", e)
        }
    }

    private fun createMigrationTable() {
        val createTableSql = dialect.createHistoryTableSql(config.historyTableName)

        context.dataSource.connection.use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute(createTableSql)
            }
        }
    }

    private fun getExecutedMigrations(): List<String> {
        val executedMigrations = mutableListOf<String>()
        val selectSql = "SELECT migration_name FROM ${config.historyTableName} ORDER BY executed_at"

        context.dataSource.connection.use { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeQuery(selectSql).use { rs ->
                    while (rs.next()) {
                        executedMigrations.add(rs.getString("migration_name"))
                    }
                }
            }
        }

        return executedMigrations
    }

    private fun executeMigrations(executedMigrations: List<String>) {
        val migrations = getMigrationDefinitions()
        val pendingMigrations = migrations.filter { it.name !in executedMigrations }

        if (pendingMigrations.isEmpty()) {
            return
        }

        context.logger.info("${pendingMigrations.size}個のマイグレーションを実行します")

        for (migration in pendingMigrations) {
            context.logger.info("マイグレーション実行中: ${migration.name}")

            context.dataSource.connection.use { conn ->
                conn.autoCommit = false

                try {
                    migration.execute(conn)
                    recordMigration(conn, migration.name)
                    conn.commit()
                    context.logger.info("マイグレーション完了: ${migration.name}")
                } catch (e: Exception) {
                    conn.rollback()
                    context.logger.severe("マイグレーション失敗: ${migration.name} - ${e.message}")
                    throw (e as? SQLException)
                        ?: SQLException("マイグレーション失敗: ${migration.name}", e)
                }
            }
        }
    }

    private fun getMigrationDefinitions(): List<Migration> {
        return MigrationDiscoverer.discover(context, config)
            .sortedBy { it.name }
    }

    private fun recordMigration(conn: Connection, migrationName: String) {
        val insertSql = "INSERT INTO ${config.historyTableName} (migration_name) VALUES (?)"

        conn.prepareStatement(insertSql).use { pstmt ->
            pstmt.setString(1, migrationName)
            pstmt.executeUpdate()
        }
    }

    private fun resolveDialect(): DatabaseDialect {
        config.dialect?.let { return it }

        context.dataSource.connection.use { connection ->
            return DatabaseDialect.fromConnection(connection)
        }
    }
}
