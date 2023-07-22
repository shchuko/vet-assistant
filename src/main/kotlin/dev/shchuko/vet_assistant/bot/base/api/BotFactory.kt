package dev.shchuko.vet_assistant.bot.base.api

import dev.shchuko.vet_assistant.bot.base.impl.telegram.TelegramBot
import dev.shchuko.vet_assistant.bot.base.impl.vk.VkBot
import dev.shchuko.vet_assistant.bot.base.statemachine.StateMachine
import org.koin.java.KoinJavaComponent.getKoin

object BotFactory {
    open class BotConfig<C : BotContext>(
        internal val vkApiKey: String?,
        internal val telegramToken: String?,
        internal val stateMachine: StateMachine<C>,
        internal val contextBuilder: StateMachine.Context.Builder<C>
    )

    fun <C : BotContext> createBots(): List<Bot> {
        val configs = getKoin().getAll<BotConfig<C>>()

        val telegramBots = configs.asSequence().mapNotNull {
            if (it.telegramToken == null) null
            else TelegramBot(it.stateMachine, it.contextBuilder, it.telegramToken)
        }

        val vkBots = configs.asSequence().mapNotNull {
            if (it.vkApiKey == null) null
            else VkBot(it.stateMachine, it.contextBuilder, it.vkApiKey)
        }

        return (telegramBots + vkBots).toList()
    }
}