package dev.shchuko.vet_assistant.bot

import dev.shchuko.vet_assistant.bot.base.telegram.AbstractTelegramBot

class VetTelegramBot(apiKey: String) : AbstractTelegramBot<VetBotContext>(
    VetBotStateMachine2,
    VetBotContext.Builder,
    apiKey
)