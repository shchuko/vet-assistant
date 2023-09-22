package dev.shchuko.vet_assistant

import dev.shchuko.vet_assistant.bot.VetBotContext
import dev.shchuko.vet_assistant.bot.VetBotStateMachine
import dev.shchuko.vet_assistant.bot.base.api.BotFactory
import dev.shchuko.vet_assistant.bot.base.api.BotFactory.BotConfig
import dev.shchuko.vet_assistant.medicine.api.MedicineService
import dev.shchuko.vet_assistant.medicine.impl.service.MedicineServiceImpl
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(::main.javaClass)

private val myModule = module {
    single<Database> { Database.connect("jdbc:h2:mem:regular;DB_CLOSE_DELAY=-1;", "org.h2.Driver") }
    single<MedicineService> { MedicineServiceImpl(get()) }

    single<BotConfig<VetBotContext>>(named("VetBotConfig")) {
        BotConfig(
            vkApiKey = System.getenv("VK_BOT_API_KEY"),
            telegramToken = System.getenv("TELEGRAM_BOT_API_KEY"),
            stateMachine = VetBotStateMachine,
            contextBuilder = VetBotContext.Builder
        )
    }
}

fun main() {
    startKoin {
        logger.info("Initializing Koin")
        modules(myModule)



        logger.info("Creating bots")
        val bots = BotFactory.createBots<VetBotContext>()

        logger.info("Starting ${bots.size} bots")
        runBlocking {
            bots.forEach { launch { it.startPolling() } }
            logger.info("Bots started")
        }
    }
}
