package dev.shchuko.vet_assistant.bot.base.vk

import dev.shchuko.vet_assistant.bot.base.api.model.*
import java.util.*

internal data class VkBotMessage(override val text: String) : BotMessage

internal data class VkBotChat(override val chatId: String) : BotChat

internal data class VkBotUser(
    override val userId: String,
    override val username: String,
    override val locale: Locale?
) : BotUser

internal data class VkBotUpdate(
    override val message: VkBotMessage,
    override val chat: VkBotChat,
    override val user: VkBotUser
) : BotUpdate

internal data class VkSendMessageResponse(override val messageId: String) : SendMessageResponse
