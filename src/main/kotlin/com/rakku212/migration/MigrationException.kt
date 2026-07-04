package com.rakku212.migration

/**
 * マイグレーションの検出・実行中に発生した実行時例外
 *
 * [MigrateManager.runMigrations] や [MigrationDiscoverer.discover]、
 * 未サポート DB への [DatabaseDialect.fromJdbcUrl] 呼び出しなどでスローされる
 *
 * @param message エラーの概要
 * @param cause 根本原因（SQL 例外など）
 * 省略可
 */
class MigrationException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
