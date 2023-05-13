package dev.shchuko.vet_assistant.bot.base.api

import dev.shchuko.vet_assistant.bot.base.api.model.BotUpdate
import dev.shchuko.vet_assistant.bot.base.statemachine.StateMachineContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
open class BotContext : StateMachineContext() {
    @Transient
    override val mySerializersModule = SerializersModule {
        include(super.mySerializersModule)
        polymorphic(StateMachineContext::class) {
            subclass(BotContext::class)
        }
    }

    @Transient
    lateinit var bot: Bot

    @Transient
    lateinit var update: BotUpdate
}