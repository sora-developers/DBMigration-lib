package com.rakku212.migration

import java.sql.Connection
import java.sql.SQLException

/**
 * 1 件のデータベースマイグレーションを表す抽象クラス
 *
 * サブクラスは [MigrationContext] を受け取る public コンストラクタを持ち、
 * [execute] で DDL / DML を実行する
 *
 * ## 命名規則
 *
 * クラス名の先頭に `_` を付けると、ファイル名によるソート順を保ちつつ
 * [name] からは `_` が除かれる
 *
 * ```
 * class _0001_01_01_000001_Create_users(context: MigrationContext) : Migration(context) {
 *     override fun execute(connection: Connection) {
 *         connection.createStatement().use { stmt ->
 *             stmt.execute("CREATE TABLE users (id INTEGER PRIMARY KEY)")
 *         }
 *     }
 * }
 * // name == "0001_01_01_000001_Create_users"
 * ```
 *
 * @param context マイグレーション実行時に共有されるコンテキスト
 * @see MigrateManager
 * @see MigrationDiscoverer
 */
abstract class Migration(
    protected val context: MigrationContext,
) {
    /**
     * マイグレーションの一意な識別名
     *
     * デフォルトではクラスの simpleName を使用する。先頭が `_` の場合は除去される
     * 履歴テーブル（[MigrationConfig.historyTableName]）の `migration_name` 列に記録される
     */
    val name: String
        get() {
            val simple = this::class.simpleName
                ?: error("Migration class must have a simple name")
            return if (simple.startsWith("_")) simple.drop(1) else simple
        }

    /**
     * マイグレーションの SQL を実行する
     *
     * 呼び出し元（[MigrateManager]）がトランザクションを管理するため、
     * このメソッド内で commit / rollback しないこと
     *
     * @param connection トランザクション中の JDBC 接続（autoCommit = false）
     * @throws SQLException SQL 実行に失敗した場合
     */
    @Throws(SQLException::class)
    abstract fun execute(connection: Connection)
}
