package dev.shchuko.vet_assistant.bot

import dev.shchuko.vet_assistant.bot.base.telegram.TelegramBot

class VetTelegramBot(apiKey: String) : TelegramBot<VetBotContext>(
    VetBotStateMachine2,
    VetBotContext.Builder,
    apiKey
)