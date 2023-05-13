package dev.shchuko.vet_assistant.bot.base.api

import dev.shchuko.vet_assistant.bot.base.api.keyboard.BotKeyboard
import dev.shchuko.vet_assistant.bot.base.api.model.BotUpdate

interface Bot {
    suspend fun sendMessage(update: BotUpdate, text: String, keyboard: BotKeyboard? = null): String

    suspend fun editMessage(messageId: String, text: String? = null, keyboard: BotKeyboard? = null): String

    suspend fun poll() // TODO refactor, generalize logic of serializing/deserializing context. Use kotlinx.coroutines Channel?
}