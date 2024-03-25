package dev.shchuko.vet_assistant

import dev.shchuko.vet_assistant.api.IBot
import dev.shchuko.vet_assistant.api.MedicineListSerializer
import dev.shchuko.vet_assistant.api.UserService
import dev.shchuko.vet_assistant.api.VetMedicineService
import dev.shchuko.vet_assistant.impl.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
    single<VetMedicineService>(createdAtStart = true) { VetMedicineServiceImpl() }
    single<MedicineListSerializer> { MedicineListCsvSerializer() }
    single<UserService> { UserServiceImpl() }

    single<IBot>(named("telegram")) { VetAssistantTelegramBot(telegramApiKey) }
    single<IBot>(named("vk")) { VetAssistantVkBot(vkGroupId, vkApiKey) }
}


fun main() {
    DatabaseConnection.init()

    startKoin {
        modules(mainModule)
    }
    getKoin().get<VetMedicineService>().init()

    runBlocking {
        getKoin().getAll<IBot>().forEach { bot ->
            launch { bot.startPolling() }
        }
    }
}
