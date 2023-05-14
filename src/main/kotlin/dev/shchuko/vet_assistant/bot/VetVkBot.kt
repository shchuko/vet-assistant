package dev.shchuko.vet_assistant.bot

import dev.shchuko.vet_assistant.bot.base.vk.VkBot

class VetVkBot(apiKey: String) : VkBot<VetBotContext>(
    VetBotStateMachine2,
    VetBotContext.Builder,
    apiKey
)
