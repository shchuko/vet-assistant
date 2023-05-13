package dev.shchuko.vet_assistant.bot.base.api.model

import java.util.*

interface BotUser {
    val userId: String
    val username: String
    val locale: Locale?
}
