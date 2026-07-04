package com.rakku212.migration

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MigrateManagerTest {
    private fun createDataSource(): HikariDataSource {
        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:sqlite:file:migrate_test?mode=memory&cache=shared"
            maximumPoolSize = 1
        }
        return HikariDataSource(config)
    }

    private fun createContext(dataSource: HikariDataSource): MigrationContext {
        return MigrationContext(
            dataSource = dataSource,
            logger = MigrationLogger.println("test"),
        )
    }

    @Test
    fun `未実行のマイグレーションを順番に実行する`() {
        createDataSource().use { dataSource ->
            val context = createContext(dataSource)
            val config = MigrationConfig(
                migrationsPackage = "com.rakku212.migration.test.migrations",
                classLoader = MigrateManagerTest::class.java.classLoader,
            )

            val manager = MigrateManager(context, config)
            manager.runMigrations()

            dataSource.connection.use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.executeQuery("SELECT migration_name FROM migration_history ORDER BY migration_name").use { rs ->
                        val names = buildList {
                            while (rs.next()) {
                                add(rs.getString("migration_name"))
                            }
                        }
                        assertEquals(
                            listOf(
                                "0001_01_01_000001_Create_sample_table",
                                "0001_01_01_000002_Create_another_table",
                            ),
                            names,
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `2回目の実行では未実行のマイグレーションがない`() {
        createDataSource().use { dataSource ->
            val context = createContext(dataSource)
            val config = MigrationConfig(
                migrationsPackage = "com.rakku212.migration.test.migrations",
                classLoader = MigrateManagerTest::class.java.classLoader,
            )

            val manager = MigrateManager(context, config)
            manager.runMigrations()
            manager.runMigrations()

            dataSource.connection.use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.executeQuery("SELECT COUNT(*) AS count FROM migration_history").use { rs ->
                        rs.next()
                        assertEquals(2, rs.getInt("count"))
                    }
                }
            }
        }
    }

    @Test
    fun `マイグレーション失敗時は履歴に記録されない`() {
        createDataSource().use { dataSource ->
            val context = createContext(dataSource)
            val config = MigrationConfig(
                migrationsPackage = "com.rakku212.migration.test.failing",
                classLoader = MigrateManagerTest::class.java.classLoader,
            )

            assertFailsWith<MigrationException> {
                MigrateManager(context, config).runMigrations()
            }

            dataSource.connection.use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.executeQuery("SELECT COUNT(*) AS count FROM migration_history").use { rs ->
                        rs.next()
                        assertEquals(0, rs.getInt("count"))
                    }
                }
            }
        }
    }

    @Test
    fun `Migration name は先頭のアンダースコアを除去する`() {
        createDataSource().use { dataSource ->
            val context = createContext(dataSource)
            val migration = com.rakku212.migration.test.migrations._0001_01_01_000001_Create_sample_table(context)
            assertEquals("0001_01_01_000001_Create_sample_table", migration.name)
        }
    }

    @Test
    fun `MigrationDiscoverer は指定パッケージのサブクラスを検出する`() {
        createDataSource().use { dataSource ->
            val context = createContext(dataSource)
            val config = MigrationConfig(
                migrationsPackage = "com.rakku212.migration.test.migrations",
                classLoader = MigrateManagerTest::class.java.classLoader,
            )

            val migrations = MigrationDiscoverer.discover(context, config)
            val names = migrations.map { it.name }.sorted()
            assertTrue(names.contains("0001_01_01_000001_Create_sample_table"))
            assertTrue(names.contains("0001_01_01_000002_Create_another_table"))
        }
    }
}
