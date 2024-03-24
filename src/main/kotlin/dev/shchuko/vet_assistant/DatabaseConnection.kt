package dev.shchuko.vet_assistant

import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.Schema
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.slf4j.LoggerFactory
import java.sql.Connection


object DatabaseConnection {
    private val logger = LoggerFactory.getLogger(this::class.java)

    object DatabaseConnectionSettings {
        val url by lazy {
            val host = getEnv("POSTGRES_HOST", "localhost")
            val port = getEnv("POSTGRES_PORT", "5432")
            val databaseName = getEnv("POSTGRES_DATABASE_NAME", "vet_tg_bot")
            "jdbc:postgresql://$host:$port/$databaseName"
        }
        const val SCHEMA = "public"
        val user = getEnv("POSTGRES_USERNAME", "postgres")
        val password = getEnv("POSTGRES_PASSWORD", "postgres")
    }

    private val initOnce: Unit by lazy {
        val url = DatabaseConnectionSettings.url
        val user = DatabaseConnectionSettings.user
        val password = DatabaseConnectionSettings.password
        val schemaName = DatabaseConnectionSettings.SCHEMA

        logger.info("Applying Flyway migrations")
        val flyway = Flyway.configure()
            .dataSource(url, user, password)
            .baselineVersion("1.0")
            .baselineOnMigrate(true)
            .load()
        flyway.migrate()

        logger.info("Migrations are applied, connecting to database, url=$url, schema=$schemaName, user=$user")
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
        Database.connect(
            url = url,
            user = user,
            password = password,
            databaseConfig = DatabaseConfig {
                defaultSchema = Schema(schemaName)
            }
        )
        Unit
    }

    fun init(): Unit = initOnce
}

private fun getEnv(name: String, default: String? = null): String {
    val effectiveValue = System.getenv(name) ?: default
    check(!effectiveValue.isNullOrBlank()) { "$name env variable is mandatory but not set" }
    return effectiveValue
}