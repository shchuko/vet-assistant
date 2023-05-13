package dev.shchuko.vet_assistant.bot

import dev.shchuko.vet_assistant.bot.base.vk.AbstractVkBot


class VetVkBot(apiKey: String) : AbstractVkBot<VetBotContext>(
    VetBotStateMachine2,
    VetBotContext.Builder,
    apiKey
)
