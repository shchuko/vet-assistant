package dev.shchuko.vet_assistant.bot.base.api.model

interface BotUpdate {
    val message: BotMessage
    val chat: BotChat
    val user: BotUser
}