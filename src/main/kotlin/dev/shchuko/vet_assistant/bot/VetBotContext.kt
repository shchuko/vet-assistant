package dev.shchuko.vet_assistant.bot

import dev.shchuko.vet_assistant.bot.base.api.BotContext
import dev.shchuko.vet_assistant.bot.base.statemachine.StateMachine
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
class VetBotContext : BotContext() {
    object Builder : StateMachine.Context.Builder<VetBotContext>() {
        override fun createNew(): VetBotContext = VetBotContext()

        override fun deserialize(string: String): VetBotContext =
            serializerFormat.decodeFromString(serializer(), string)

        override fun serialize(context: VetBotContext): String = serializerFormat.encodeToString(serializer(), context)
    }

    @Transient
    override val mySerializersModule = SerializersModule {
        include(super.mySerializersModule)
        polymorphic(BotContext::class) {
            subclass(VetBotContext::class)
        }
    }

    enum class MainMenuCommand {
        MEDICINE_SEARCH,
        ADMIN_MENU
    }

    var mainMenuCommand: MainMenuCommand? = null

    var askMedicineSearchMessageId: String? = null
    var medicineSearchCancel: Boolean = false
    var medicineSearchName: String? = null
}