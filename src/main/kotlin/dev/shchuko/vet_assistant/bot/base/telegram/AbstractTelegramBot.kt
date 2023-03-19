package dev.shchuko.vet_assistant.bot.base.telegram

import dev.shchuko.vet_assistant.bot.base.api.Bot
import dev.shchuko.vet_assistant.bot.base.api.keyboard.BaseKeyboardMarkup
import dev.shchuko.vet_assistant.bot.base.api.model.BotContext
import dev.shchuko.vet_assistant.bot.base.api.model.BotUpdate
import dev.shchuko.vet_assistant.bot.base.statemachine.StateMachine
import kotlinx.coroutines.delay

open class AbstractTelegramBot<T>(mainStateMachine: StateMachine<BotContext<T>>) : Bot {
    override fun reply(update: BotUpdate, text: String?, keyboard: BaseKeyboardMarkup?) {
//        TODO("Not yet implemented")
    }

    override suspend fun poll() {
        /**
         *
         *             val apiKey = ""
         *
         *             val telegramApi = TelegramClient(apiKey)
         *
         *             var offset: Long? = -100
         *             while (this.isActive) {
         *                 val (ok, updates) = telegramApi.getUpdates(offset, timeout = 10)
         *                 if (ok) {
         *                     updates.forEach { update ->
         *                         offset = update.update_id + 1
         *                         val message = update.message
         *                         if (message != null) {
         *                             telegramApi.sendMessage(message.chat.id.toString(), message.text ?: "No text")
         *                         }
         *                     }
         *                 }
         *                 delay(1000)
         *             }
         *
         */


        while (true) {
            println("telegram bot poll")
            delay(800)
        }
//        TODO("Not yet implemented")
    }
}