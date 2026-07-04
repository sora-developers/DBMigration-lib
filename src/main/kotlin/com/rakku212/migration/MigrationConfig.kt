package com.rakku212.migration

/**
 * マイグレーションの検出と履歴管理の設定
 *
 * ```
 * MigrationConfig(
 *     migrationsPackage = "com.example.app.migrations",
 *     historyTableName = "schema_migrations",
 *     dialect = DatabaseDialect.MYSQL, // 省略時は接続から自動判定
 * )
 * ```
 *
 * @property migrationsPackage [Migration] サブクラスをスキャンする Java パッケージ名
 * @property classLoader マイグレーションクラスの読み込みに使う ClassLoader
 *   デフォルトはスレッドの contextClassLoader
 * @property historyTableName 実行済みマイグレーション名を記録するテーブル名
 *   デフォルトは `"migration_history"`
 * @property dialect 履歴テーブル作成用の SQL 方言
 * `null` の場合は [javax.sql.DataSource] への接続から自動判定する
 */
class MigrationConfig(
    val migrationsPackage: String,
    val classLoader: ClassLoader = Thread.currentThread().contextClassLoader,
    val historyTableName: String = "migration_history",
    val dialect: DatabaseDialect? = null,
)
