package com.rakku212.migration

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.testcontainers.containers.MariaDBContainer
import org.testcontainers.containers.MySQLContainer
import kotlin.test.Test
import kotlin.test.assertEquals

class MigrateManagerMultiDbTest {
    @Test
    fun `MySQLでマイグレーションを実行する`() {

        MySQLContainer("mysql:8.4")
            .withDatabaseName("migration_test")
            .withUsername("test")
            .withPassword("test")
            .use { container ->
                container.start()
                runMigrations(
                    jdbcUrl = container.jdbcUrl,
                    username = container.username,
                    password = container.password,
                    driverClassName = container.driverClassName,
                    dialect = DatabaseDialect.MYSQL,
                )
            }
    }

    @Test
    fun `MariaDBでマイグレーションを実行する`() {

        MariaDBContainer("mariadb:11.4")
            .withDatabaseName("migration_test")
            .withUsername("test")
            .withPassword("test")
            .use { container ->
                container.start()
                runMigrations(
                    jdbcUrl = container.jdbcUrl,
                    username = container.username,
                    password = container.password,
                    driverClassName = container.driverClassName,
                    dialect = DatabaseDialect.MARIADB,
                )
            }
    }

    private fun runMigrations(
        jdbcUrl: String,
        username: String,
        password: String,
        driverClassName: String,
        dialect: DatabaseDialect,
    ) {
        val dataSource = HikariDataSource(
            HikariConfig().apply {
                this.jdbcUrl = jdbcUrl
                this.username = username
                this.password = password
                this.driverClassName = driverClassName
                maximumPoolSize = 1
            },
        )

        dataSource.use {
            val context = MigrationContext(
                dataSource = it,
                logger = MigrationLogger.println("test"),
            )
            val config = MigrationConfig(
                migrationsPackage = "com.rakku212.migration.test.migrations",
                classLoader = MigrateManagerMultiDbTest::class.java.classLoader,
                dialect = dialect,
            )

            MigrateManager(context, config).runMigrations()

            it.connection.use { conn ->
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
}
