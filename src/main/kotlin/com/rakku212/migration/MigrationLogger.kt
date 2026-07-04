package com.rakku212.migration

import kotlin.io.println

/**
 * マイグレーション実行中のログ出力インターフェース
 *
 * アプリケーションのロギング基盤に合わせて実装するか、
 * [println] で標準出力へ出力する実装を使う
 */
interface MigrationLogger {
    /** 通常の進捗メッセージ（例: マイグレーション開始・完了） */
    fun info(message: String)

    /** 警告（例: コンストラクタ不足でスキップされたクラス） */
    fun warning(message: String)

    /** エラー（例: SQL 失敗、検出失敗） */
    fun severe(message: String)

    companion object {
        /**
         * 標準出力に `[prefix] LEVEL: message` 形式で出力する [MigrationLogger]
         *
         * ```
         * MigrationLogger.println("myapp")
         * // => [myapp] INFO: マイグレーション完了: ...
         * ```
         *
         * @param prefix ログ行の先頭に付ける識別子
         * デフォルトは `"migration"`
         */
        fun println(prefix: String = "migration"): MigrationLogger = object : MigrationLogger {
            override fun info(message: String) {
                println("[$prefix] INFO: $message")
            }

            override fun warning(message: String) {
                println("[$prefix] WARN: $message")
            }

            override fun severe(message: String) {
                println("[$prefix] ERROR: $message")
            }
        }
    }
}
