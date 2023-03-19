package dev.shchuko.vet_assistant

//import io.ktor.server.application.*
import dev.shchuko.vet_assistant.bot.VetTelegramBot
import dev.shchuko.vet_assistant.bot.VetVkBot
import dev.shchuko.vet_assistant.bot.base.api.Bot
import dev.shchuko.vet_assistant.medicine.api.service.MedicineService
import dev.shchuko.vet_assistant.medicine.impl.repository.MedicineServiceRepository
import dev.shchuko.vet_assistant.medicine.impl.repository.MedicineServiceRepositoryImpl
import dev.shchuko.vet_assistant.medicine.impl.service.MedicineServiceImpl
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent.getKoin

private val myModule = module {
    single<MedicineServiceRepository> { MedicineServiceRepositoryImpl() }
    single<MedicineService> { MedicineServiceImpl(get()) }
    single<Bot>(named("VetTelegramBot")) { VetTelegramBot() }
    single<Bot>(named("VetVkBot")) { VetVkBot() }
}

fun main() {
    startKoin {
        modules(myModule)

        runBlocking {
            getKoin().getAll<Bot>().forEach { bot ->
                launch {
                    bot.poll()
                }
            }
        }
    }
}
