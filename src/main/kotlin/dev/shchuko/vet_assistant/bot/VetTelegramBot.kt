package dev.shchuko.vet_assistant.bot

import dev.shchuko.vet_assistant.bot.base.telegram.AbstractTelegramBot

class VetTelegramBot : AbstractTelegramBot<UserContext>(BotMainStateMachine)