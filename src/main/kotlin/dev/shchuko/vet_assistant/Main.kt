package dev.shchuko.vet_assistant

import dev.shchuko.vet_assistant.bot.VetBotContext
import dev.shchuko.vet_assistant.bot.VetBotStateMachine
import dev.shchuko.vet_assistant.bot.base.api.BotFactory
import dev.shchuko.vet_assistant.bot.base.api.BotFactory.BotConfig
import dev.shchuko.vet_assistant.medicine.api.service.MedicineService
import dev.shchuko.vet_assistant.medicine.impl.repository.MedicineServiceRepository
import dev.shchuko.vet_assistant.medicine.impl.repository.MedicineServiceRepositoryImpl
import dev.shchuko.vet_assistant.medicine.impl.service.MedicineServiceImpl
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module

private val myModule = module {
    single<MedicineServiceRepository> { MedicineServiceRepositoryImpl() }
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
        modules(myModule)

        val bots = BotFactory.createBots<VetBotContext>()

        runBlocking {
            bots.forEach { launch { it.startPolling() } }
            delay(5000)
        }
    }
}
