package dev.shchuko.vet_assistant.bot

import dev.shchuko.vet_assistant.bot.base.vk.AbstractVkBot

class VetVkBot : AbstractVkBot<UserContext>(BotMainStateMachine)
