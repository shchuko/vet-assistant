package dev.shchuko.vet_assistant.bot.base.api

import dev.shchuko.vet_assistant.bot.base.api.model.BotUpdate
import dev.shchuko.vet_assistant.bot.base.statemachine.StateMachine
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
open class BotContext : StateMachine.Context() {
    @Transient
    override val mySerializersModule = SerializersModule {
        include(super.mySerializersModule)
        polymorphic(StateMachine.Context::class) {
            subclass(BotContext::class)
        }
    }

    @Transient
    lateinit var bot: BotCommands

    @Transient
    lateinit var update: BotUpdate
}