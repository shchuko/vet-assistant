package dev.shchuko.vet_assistant

import dev.shchuko.vet_assistant.api.IBot
import dev.shchuko.vet_assistant.api.MedicineListSerializer
import dev.shchuko.vet_assistant.api.UserService
import dev.shchuko.vet_assistant.api.VetMedicineService
import dev.shchuko.vet_assistant.impl.*
import dev.shchuko.vet_assistant.impl.db.ActiveIngredientTable
import dev.shchuko.vet_assistant.impl.db.MedicineAnalogueTable
import dev.shchuko.vet_assistant.impl.db.MedicineTable
import dev.shchuko.vet_assistant.impl.db.TelegramUserTable
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent.getKoin
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(::main.javaClass)

private val vkGroupId = System.getenv("VK_GROUP_ID")?.toInt() ?: error("Missing VK_GROUP_ID")
private val vkApiKey = System.getenv("VK_API_KEY") ?: error("Missing VK_API_KEY")
private val telegramApiKey = System.getenv("TELEGRAM_API_KEY") ?: error("Missing TELEGRAM_API_KEY")

val mainModule = module {
    single<VetMedicineService> { VetMedicineServiceImpl() }
    single<MedicineListSerializer> { MedicineListCsvSerializer() }
    single<UserService> { UserServiceImpl() }

    single<IBot>(named("telegram")) { VetAssistantTelegramBot(telegramApiKey) }
    single<IBot>(named("vk")) { VetAssistantVkBot(vkGroupId, vkApiKey) }
}

private fun initDB() {
    Database.connect("jdbc:h2:./h2db.db", "org.h2.Driver")
    transaction {
        SchemaUtils.create(MedicineTable, ActiveIngredientTable, MedicineAnalogueTable, TelegramUserTable)
    }
    logger.info("DB init complete")
}

fun main() {
    initDB()

    startKoin {
        modules(mainModule)
    }

    runBlocking {
        getKoin().getAll<IBot>().forEach { bot ->
            launch { bot.startPolling() }
        }
    }
}
