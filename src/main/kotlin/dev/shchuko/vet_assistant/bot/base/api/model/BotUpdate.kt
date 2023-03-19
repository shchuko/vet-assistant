package dev.shchuko.vet_assistant.bot.base.api.model

data class BotUpdate(val message: BotMessage, val chat: BotChat, val user: BotUser)