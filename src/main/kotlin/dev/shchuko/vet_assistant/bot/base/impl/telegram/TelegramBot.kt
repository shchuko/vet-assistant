package dev.shchuko.vet_assistant.bot.base.impl.telegram

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import dev.shchuko.vet_assistant.bot.base.api.BotContext
import dev.shchuko.vet_assistant.bot.base.api.keyboard.BotKeyboard
import dev.shchuko.vet_assistant.bot.base.api.model.BotUpdate
import dev.shchuko.vet_assistant.bot.base.api.model.SendMessageResponse
import dev.shchuko.vet_assistant.bot.base.impl.BotBase
import dev.shchuko.vet_assistant.bot.base.statemachine.StateMachine
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.util.*


internal class TelegramBot<in C : BotContext>(
    mainStateMachine: StateMachine<C>,
    botContextBuilder: StateMachine.Context.Builder<C>,
    apiKey: String,
) : BotBase<C, String>(mainStateMachine, botContextBuilder) {
    private val underlyingBot = bot {
        token = apiKey

        dispatch {
            text {
                val botUpdate = TgBotUpdate(
                    TgBotMessage(text),
                    TgBotChat(message.chat.id.toString()),
                    TgBotUser(
                        message.from!!.id.toString(),
                        message.from?.username!!,
                        languageCodeToLocale(message.from?.languageCode)
                    ),
                    null
                )
                val key = botUpdate.chat.chatId to botUpdate.user.userId
                sendUpdate(botUpdate, key.toString())
            }

            callbackQuery {
                val botUpdate = TgBotUpdate(
                    TgBotMessage(callbackQuery.data),
                    TgBotChat(callbackQuery.message!!.chat.id.toString()),
                    TgBotUser(
                        callbackQuery.from.id.toString(),
                        callbackQuery.from.username!!,
                        languageCodeToLocale(callbackQuery.from.languageCode)
                    ),
                    null
                )
                val key = botUpdate.chat.chatId to botUpdate.user.userId
                sendUpdate(botUpdate, key.toString())
            }
        }
    }

    override suspend fun sendMessage(update: BotUpdate, text: String, keyboard: BotKeyboard?): SendMessageResponse {
        update as TgBotUpdate

        if (update.callbackQueryId != null) {
            underlyingBot.answerCallbackQuery(update.callbackQueryId)
        }

        val response = underlyingBot.sendMessage(
            chatId = ChatId.fromId(update.chat.chatId.toLong()),
            text = text,
            replyMarkup = keyboard.toTelegramFullKeyboard()
        )
        response.get().messageId
        return TgSendMessageResponse("${update.chat.chatId}:${response.get().messageId}")
    }


    override suspend fun editMessage(messageId: String, text: String?, keyboard: BotKeyboard?): SendMessageResponse {
        require(keyboard == null || keyboard.inline)
        val response = TgSendMessageResponse(messageId)
        if (text == null && keyboard == null) return response

        val telegramFullKeyboard = keyboard.toTelegramFullKeyboard() as? InlineKeyboardMarkup
        val split = messageId.split(":")
        val realChatId = ChatId.fromId(split[0].toLong())
        val realMessageId = split[1].toLong()

        val answer = if (text != null) {
            underlyingBot.editMessageText(realChatId, realMessageId, text = text, replyMarkup = telegramFullKeyboard)
        } else {
            underlyingBot.editMessageReplyMarkup(realChatId, realMessageId, replyMarkup = telegramFullKeyboard)
        }
        return response
    }

    override suspend fun pollForUpdates(): Unit = coroutineScope {
        underlyingBot.startPolling()

        launch {
            while (true) {
                ensureActive()
                delay(1000)
            }
        }.invokeOnCompletion {
            underlyingBot.stopPolling()
        }
    }

    private fun languageCodeToLocale(code: String?): Locale? = code?.let { Locale.forLanguageTag(it) }
}

