package dev.shchuko.vet_assistant.bot.base.api

import dev.shchuko.vet_assistant.bot.base.api.keyboard.BaseKeyboardMarkup
import dev.shchuko.vet_assistant.bot.base.api.model.BotUpdate

interface Bot {
    fun reply(update: BotUpdate, text: String? = null, keyboard: BaseKeyboardMarkup? = null)

    suspend fun poll()
}