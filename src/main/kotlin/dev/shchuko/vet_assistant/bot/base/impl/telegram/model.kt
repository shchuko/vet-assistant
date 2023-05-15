package dev.shchuko.vet_assistant.bot.base.impl.telegram

import dev.shchuko.vet_assistant.bot.base.api.model.*
import java.util.*

internal data class TgBotMessage(override val text: String) : BotMessage

internal data class TgBotChat(override val chatId: String) : BotChat

internal data class TgBotUser(
    override val userId: String,
    override val username: String,
    override val locale: Locale?
) : BotUser

internal data class TgBotUpdate(
    override val message: TgBotMessage,
    override val chat: TgBotChat,
    override val user: TgBotUser,
    val callbackQueryId: String?
) : BotUpdate

internal data class TgSendMessageResponse(override val messageId: String) : SendMessageResponse
