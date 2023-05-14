package dev.shchuko.vet_assistant.bot.base.api

import dev.shchuko.vet_assistant.bot.base.api.keyboard.BotKeyboard
import dev.shchuko.vet_assistant.bot.base.api.model.BotUpdate
import dev.shchuko.vet_assistant.bot.base.api.model.SendMessageResponse

interface Bot {
    suspend fun sendMessage(update: BotUpdate, text: String, keyboard: BotKeyboard? = null): SendMessageResponse

    suspend fun editMessage(messageId: String, text: String? = null, keyboard: BotKeyboard? = null): SendMessageResponse

    suspend fun poll() // TODO refactor, generalize logic of serializing/deserializing context. Use kotlinx.coroutines Channel?
}