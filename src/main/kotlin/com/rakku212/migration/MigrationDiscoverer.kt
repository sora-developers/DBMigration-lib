package com.rakku212.migration

import io.github.classgraph.ClassGraph
import io.github.classgraph.ClassInfo

/**
 * 指定パッケージ内の [Migration] サブクラスを ClassGraph で検出する
 *
 * 通常は [MigrateManager] から内部的に呼び出される
 * 抽象クラス・インターフェースは除外され、
 * [MigrationContext] を受け取るコンストラクタを持たないクラスは警告ログのうえスキップされる
 */
object MigrationDiscoverer {
    /**
     * [config.migrationsPackage] 配下の具象 [Migration] インスタンスを生成して返す
     *
     * @param context 各マイグレーションのコンストラクタに渡すコンテキスト
     * @param config スキャン対象パッケージと ClassLoader
     * @return 検出・初期化に成功したマイグレーションのリスト（順序は未ソート）
     * @throws MigrationException クラスパスのスキャン自体に失敗した場合
     */
    fun discover(
        context: MigrationContext,
        config: MigrationConfig,
    ): List<Migration> {
        val migrations = mutableListOf<Migration>()

        try {
            ClassGraph()
                .enableClassInfo()
                .overrideClassLoaders(config.classLoader)
                .acceptPackages(config.migrationsPackage)
                .scan()
                .use { scanResult ->
                    val migrationClassInfos = scanResult.getSubclasses(Migration::class.java.name)

                    for (classInfo in migrationClassInfos) {
                        if (classInfo.isAbstract || classInfo.isInterface) {
                            continue
                        }

                        instantiateMigration(context, classInfo)?.let { migration ->
                            migrations.add(migration)
                        }
                    }
                }
        } catch (e: Exception) {
            context.logger.severe("マイグレーションクラスの検出に失敗しました: ${e.message}")
            throw MigrationException("マイグレーションクラスの検出に失敗しました", e)
        }

        return migrations
    }

    private fun instantiateMigration(
        context: MigrationContext,
        classInfo: ClassInfo,
    ): Migration? {
        val fqcn = classInfo.name

        return try {
            val clazz = classInfo.loadClass(false)
            if (!Migration::class.java.isAssignableFrom(clazz)) {
                return null
            }

            val migrationClass = clazz.asSubclass(Migration::class.java)

            val constructor = migrationClass.getConstructor(MigrationContext::class.java)
            constructor.newInstance(context)
        } catch (e: NoSuchMethodException) {
            context.logger.warning("マイグレーションクラス $fqcn に MigrationContext を受け取るコンストラクタがありません")
            null
        } catch (e: Exception) {
            context.logger.severe("マイグレーションクラス $fqcn の初期化に失敗しました: ${e.message}")
            null
        }
    }
}
