/**
 * データベースマイグレーションを Kotlin クラスで定義・実行するためのライブラリ
 *
 * ## 基本的な使い方
 *
 * 1. [MigrationContext] に [javax.sql.DataSource] と [MigrationLogger] を渡す
 * 2. [MigrationConfig] でマイグレーションクラスのパッケージを指定する
 * 3. [MigrateManager.runMigrations] を呼び出す
 *
 * ```
 * val context = MigrationContext(dataSource, MigrationLogger.println())
 * val config = MigrationConfig(migrationsPackage = "com.example.migrations")
 * MigrateManager(context, config).runMigrations()
 * ```
 *
 * ## マイグレーションクラスの作成
 *
 * [Migration] を継承し、[MigrationContext] を受け取るコンストラクタと
 * [Migration.execute] を実装する。クラス名は `_0001_01_01_000001_Create_users` のように
 * 先頭に `_` を付けてソート順を制御できる（実行時の [Migration.name] からは `_` が除かれる）
 *
 * @see MigrateManager
 * @see Migration
 * @see MigrationConfig
 */
package com.rakku212.migration
