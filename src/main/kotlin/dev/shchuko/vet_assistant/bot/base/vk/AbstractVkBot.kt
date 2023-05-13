package dev.shchuko.vet_assistant.bot.base.vk

import api.longpoll.bots.LongPollBot
import api.longpoll.bots.model.events.messages.MessageNew
import api.longpoll.bots.model.objects.additional.Keyboard
import api.longpoll.bots.model.objects.additional.buttons.Button
import api.longpoll.bots.model.objects.additional.buttons.TextButton
import dev.shchuko.vet_assistant.bot.base.api.Bot
import dev.shchuko.vet_assistant.bot.base.api.BotContext
import dev.shchuko.vet_assistant.bot.base.api.keyboard.BotKeyboard
import dev.shchuko.vet_assistant.bot.base.api.model.BotChat
import dev.shchuko.vet_assistant.bot.base.api.model.BotMessage
import dev.shchuko.vet_assistant.bot.base.api.model.BotUpdate
import dev.shchuko.vet_assistant.bot.base.api.model.BotUser
import dev.shchuko.vet_assistant.bot.base.statemachine.StateMachine
import dev.shchuko.vet_assistant.bot.base.statemachine.StateMachineContext
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*

private class VkBotMessage(override val text: String) : BotMessage

private class VkBotChat(override val chatId: String) : BotChat

private class VkBotUser(
    override val userId: String,
    override val username: String,
    override val locale: Locale?
) : BotUser

private class VkBotUpdate(
    override val message: VkBotMessage,
    override val chat: VkBotChat,
    override val user: VkBotUser
) : BotUpdate

open class AbstractVkBot<in C : BotContext>(
    private val mainStateMachine: StateMachine<C>,
    private val botContextBuilder: StateMachineContext.Builder<C>,
    apiKey: String
) : Bot, LongPollBot() {
    private val accessToken = apiKey
    private val contexts = mutableMapOf<Int, String>()

    override suspend fun sendMessage(update: BotUpdate, text: String, keyboard: BotKeyboard?): String {
        val send = vk.messages.send()
        send.setPeerId(update.chat.chatId.toInt())
        send.setMessage(text)
        send.setKeyboard(keyboard.toVkFullKeyboard())
        val messageId = send.executeAsync().await().response
        return "${update.chat.chatId}:${messageId}"
    }

    override suspend fun editMessage(messageId: String, text: String?, keyboard: BotKeyboard?): String {
        val split = messageId.split(":")
        val realChatId = split[0].toInt()
        val realMessageId = split[1].toInt()

        val edit = vk.messages.edit()
        edit.setPeerId(realChatId)
        edit.setMessageId(realMessageId)
        if (text != null) {
            edit.setMessage(text)
        }
        require(keyboard == null || keyboard.inline)
        edit.setKeyboard(keyboard.toVkFullKeyboard())
        edit.executeAsync().await()
        return messageId
    }

    override suspend fun poll() {
        coroutineScope { launch { startPolling() } }
    }

    override fun getAccessToken() = accessToken

    override fun onMessageNew(messageNew: MessageNew) {
        // TODO rewrite dumb implementation
        val user = VkBotUser(
            messageNew.message.peerId.toString(),
            messageNew.message.peerId.toString(),
            null // messageNew.clientInfo.langId
        )

        val botUpdate = VkBotUpdate(
            VkBotMessage(messageNew.message.text),
            VkBotChat(messageNew.message.peerId.toString()),
            user,
        )

        val key = messageNew.message.peerId

        val contextSerialized = contexts.getOrPut(key) {
            botContextBuilder.serialize(botContextBuilder.createNew())
        }
        val context = botContextBuilder.deserialize(contextSerialized)
        context.update = botUpdate
        context.bot = this@AbstractVkBot
        runBlocking { // TODO non-blocking
            mainStateMachine.run(context)
        }

        if (!context.isRunning(mainStateMachine)) {
            contexts[key] = botContextBuilder.serialize(botContextBuilder.createNew())
        } else {
            contexts[key] = botContextBuilder.serialize(context)
        }
    }

}

private fun BotKeyboard?.toVkFullKeyboard(): Keyboard {
    if (this == null) return Keyboard(emptyList())
    return Keyboard(rows.map { row ->
        row.map { button ->
            @Suppress("USELESS_CAST")
            TextButton(Button.Color.POSITIVE, TextButton.Action(button)) as Button
        }
    }).setInline(inline).setOneTime(!inline)
}