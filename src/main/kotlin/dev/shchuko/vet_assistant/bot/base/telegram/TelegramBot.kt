package dev.shchuko.vet_assistant.bot.base.telegram

import com.github.omarmiatello.telegram.InlineKeyboardMarkup
import com.github.omarmiatello.telegram.TelegramClient
import dev.shchuko.vet_assistant.bot.base.api.BotBase
import dev.shchuko.vet_assistant.bot.base.api.BotContext
import dev.shchuko.vet_assistant.bot.base.api.keyboard.BotKeyboard
import dev.shchuko.vet_assistant.bot.base.api.model.BotUpdate
import dev.shchuko.vet_assistant.bot.base.api.model.SendMessageResponse
import dev.shchuko.vet_assistant.bot.base.statemachine.StateMachine
import dev.shchuko.vet_assistant.bot.base.statemachine.StateMachineContext
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


open class TelegramBot<in C : BotContext>(
    mainStateMachine: StateMachine<C>,
    botContextBuilder: StateMachineContext.Builder<C>,
    apiKey: String,
) : BotBase<C, String>(mainStateMachine, botContextBuilder) {
    private val telegramClient = TelegramClient(apiKey)

    override suspend fun sendMessage(update: BotUpdate, text: String, keyboard: BotKeyboard?): SendMessageResponse {
        update as TgBotUpdate

        // always answer a callback query, ignore errors if any
        if (update.callbackQueryId != null) {
            try {
                telegramClient.answerCallbackQuery(update.callbackQueryId)
            } catch (_: Exception) {
            }
        }
        val response = telegramClient.sendMessage(
            chat_id = update.chat.chatId,
            text = text,
            reply_markup = keyboard.toTelegramFullKeyboard()
        )
        return TgSendMessageResponse("${response.result.chat.id}:${response.result.message_id}")
    }

    override suspend fun editMessage(messageId: String, text: String?, keyboard: BotKeyboard?): SendMessageResponse {
        require(keyboard == null || keyboard.inline)
        val telegramFullKeyboard = keyboard.toTelegramFullKeyboard() as? InlineKeyboardMarkup

        val split = messageId.split(":")
        val realChatId = split[0]
        val realMessageId = split[1].toLong()
        val response = if (text != null) telegramClient.editMessageText(
            realChatId,
            realMessageId,
            text = text,
            reply_markup = telegramFullKeyboard
        ) else telegramClient.editMessageReplyMarkup(
            realChatId,
            realMessageId,
            reply_markup = telegramFullKeyboard
        )

        return TgSendMessageResponse("${response.result.chat.id}:${response.result.message_id}")
    }


    override suspend fun pollImpl() {
        coroutineScope {
            launch(CoroutineName("telegram-poll")) {
                var offset: Long? = -100
                while (this.isActive) {
                    try {
                        val (ok, updates) = telegramClient.getUpdates(offset, timeout = 10)
                        if (ok) {
                            updates.forEach { update ->
                                offset = update.update_id + 1
                                val message = update.message
                                val callbackQuery = update.callback_query
                                if (message == null && callbackQuery == null) return@forEach

                                val user = message?.from ?: callbackQuery!!.from
                                val chatId =
                                    message?.chat?.id?.toString() ?: callbackQuery!!.message?.chat?.id!!.toString()
                                val messageText = message?.text ?: callbackQuery!!.data!!

                                val botUpdate = TgBotUpdate(
                                    TgBotMessage(messageText),
                                    TgBotChat(chatId),
                                    TgBotUser(user.id.toString(), user.username!!, null),
                                    callbackQuery?.id
                                )
                                val key = chatId to user.id
                                sendUpdate(botUpdate, key.toString())
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}

