package dev.shchuko.vet_assistant.bot.base.vk

import dev.shchuko.vet_assistant.bot.base.api.Bot
import dev.shchuko.vet_assistant.bot.base.api.keyboard.BaseKeyboardMarkup
import dev.shchuko.vet_assistant.bot.base.api.model.BotContext
import dev.shchuko.vet_assistant.bot.base.api.model.BotUpdate
import dev.shchuko.vet_assistant.bot.base.statemachine.StateMachine
import kotlinx.coroutines.delay

open class AbstractVkBot<T>(mainStateMachine: StateMachine<BotContext<T>>) : Bot {
    override fun reply(update: BotUpdate, text: String?, keyboard: BaseKeyboardMarkup?) {
//        TODO("Not yet implemented")
    }

    override suspend fun poll() {
        while (true) {
            println("vk bot poll")
            delay(1000)
        }
//        TODO("Not yet implemented")
    }
}