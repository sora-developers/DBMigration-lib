package com.rakku212.migration

import javax.sql.DataSource

/**
 * マイグレーション実行時に共有される実行コンテキスト
 *
 * [MigrateManager] と各 [Migration] サブクラスに渡される
 *
 * ```
 * MigrationContext(
 *     dataSource = hikariDataSource,
 *     logger = MigrationLogger.println("myapp"),
 * )
 * ```
 *
 * @property dataSource マイグレーション対象のデータベース接続プール
 * @property logger 実行ログの出力先
 */
class MigrationContext(
    val dataSource: DataSource,
    val logger: MigrationLogger,
)
