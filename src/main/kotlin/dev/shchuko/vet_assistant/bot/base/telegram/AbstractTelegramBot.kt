package dev.shchuko.vet_assistant.bot.base.telegram

import com.github.omarmiatello.telegram.*
import dev.shchuko.vet_assistant.bot.base.api.Bot
import dev.shchuko.vet_assistant.bot.base.api.BotContext
import dev.shchuko.vet_assistant.bot.base.api.keyboard.BotKeyboard
import dev.shchuko.vet_assistant.bot.base.api.model.*
import dev.shchuko.vet_assistant.bot.base.statemachine.StateMachine
import dev.shchuko.vet_assistant.bot.base.statemachine.StateMachineContext
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.*


private class TgBotMessage(override val text: String) : BotMessage

private class TgBotChat(override val chatId: String) : BotChat

private class TgBotUser(
    override val userId: String,
    override val username: String,
    override val locale: Locale?
) : BotUser

private class TgBotUpdate(
    override val message: TgBotMessage,
    override val chat: TgBotChat,
    override val user: TgBotUser,
    val callbackQueryId: String?
) : BotUpdate

private data class TgSendMessageResponse(override val messageId: String) : SendMessageResponse

open class AbstractTelegramBot<in C : BotContext>(
    private val mainStateMachine: StateMachine<C>,
    private val botContextBuilder: StateMachineContext.Builder<C>,
    apiKey: String
) : Bot {
    private val telegramClient = TelegramClient(apiKey)

    override suspend fun sendMessage(update: BotUpdate, text: String, keyboard: BotKeyboard?): SendMessageResponse {
        update as TgBotUpdate
        if (update.callbackQueryId != null) {
            telegramClient.answerCallbackQuery(update.callbackQueryId)
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


    override suspend fun poll() {
        // TODO rewrite dumb implementation
        coroutineScope {
            launch {
                var offset: Long? = -100
                val contexts = mutableMapOf<Pair<String, Long>, String>()
                while (this.isActive) {
                    val (ok, updates) = telegramClient.getUpdates(offset, timeout = 10)
                    if (ok) {
                        updates.forEach { update ->
                            offset = update.update_id + 1
                            val message = update.message
                            val callbackQuery = update.callback_query
                            if (message == null && callbackQuery == null) return@forEach

                            val user = message?.from ?: callbackQuery!!.from
                            val chatId = message?.chat?.id?.toString() ?: callbackQuery!!.message?.chat?.id!!.toString()
                            val messageText = message?.text ?: callbackQuery!!.data!!

                            val botUpdate = TgBotUpdate(
                                TgBotMessage(messageText),
                                TgBotChat(chatId),
                                TgBotUser(user.id.toString(), user.username!!, null),
                                callbackQuery?.id
                            )
                            val key = chatId to user.id

                            val contextSerialized = contexts.getOrPut(key) {
                                botContextBuilder.serialize(botContextBuilder.createNew())
                            }
                            val context = botContextBuilder.deserialize(contextSerialized)
                            context.update = botUpdate
                            context.bot = this@AbstractTelegramBot
                            mainStateMachine.run(context)
                            if (!context.isRunning(mainStateMachine)) {
                                contexts[key] = botContextBuilder.serialize(botContextBuilder.createNew())
                            } else {
                                contexts[key] = botContextBuilder.serialize(context)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun BotKeyboard?.toTelegramFullKeyboard(): KeyboardOption {
    if (this == null) return ReplyKeyboardRemove(remove_keyboard = true)
    return if (inline) InlineKeyboardMarkup(rows.map { row ->
        row.map { button ->
            InlineKeyboardButton(button, callback_data = button)
        }
    })
    else ReplyKeyboardMarkup(resize_keyboard = true, one_time_keyboard = true, keyboard = rows.map { row ->
        row.map { button ->
            KeyboardButton(button)
        }
    })
}
