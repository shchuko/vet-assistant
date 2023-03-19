package dev.shchuko.vet_assistant.bot.base.api.model

import dev.shchuko.vet_assistant.bot.base.api.Bot
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class BotContext<T>(val botSubContext: T) {
    @Transient
    lateinit var bot: Bot

    @Transient
    lateinit var update: BotUpdate
}