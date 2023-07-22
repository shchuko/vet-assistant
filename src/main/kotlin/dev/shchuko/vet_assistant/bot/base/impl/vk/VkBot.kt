package dev.shchuko.vet_assistant.bot.base.impl.vk

import api.longpoll.bots.LongPollBot
import api.longpoll.bots.methods.VkBotsMethods
import api.longpoll.bots.model.events.messages.MessageNew
import dev.shchuko.vet_assistant.bot.base.api.BotContext
import dev.shchuko.vet_assistant.bot.base.api.keyboard.BotKeyboard
import dev.shchuko.vet_assistant.bot.base.api.model.BotUpdate
import dev.shchuko.vet_assistant.bot.base.api.model.SendMessageResponse
import dev.shchuko.vet_assistant.bot.base.impl.BotBase
import dev.shchuko.vet_assistant.bot.base.statemachine.StateMachine
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await

internal class VkBot<in C : BotContext>(
    mainStateMachine: StateMachine<C>,
    botContextBuilder: StateMachine.Context.Builder<C>,
    apiKey: String
) : BotBase<C, Int>(mainStateMachine, botContextBuilder) {
    private val vk = VkBotsMethods(apiKey)

    private val underlyingBot = object : LongPollBot() {
        override fun getAccessToken() = apiKey

        override fun onMessageNew(messageNew: MessageNew) = try {
            val user = VkBotUser(
                messageNew.message.peerId.toString(),
                messageNew.message.peerId.toString(),
                null // TODO messageNew.clientInfo.langId
            )

            val botUpdate = VkBotUpdate(
                VkBotMessage(messageNew.message.text),
                VkBotChat(messageNew.message.peerId.toString()),
                user,
            )
            val key = messageNew.message.peerId
            runBlocking {// TODO non-blocking?
                sendUpdate(botUpdate, key)
            }
        } catch (_: Throwable) {
        }
    }

    override suspend fun sendMessage(update: BotUpdate, text: String, keyboard: BotKeyboard?): SendMessageResponse {
        val send = vk.messages.send()
        send.setPeerId(update.chat.chatId.toInt())
        send.setMessage(text)
        send.setKeyboard(keyboard.toVkFullKeyboard())
        val messageId = send.executeAsync().await().response
        return VkSendMessageResponse("${update.chat.chatId}:${messageId}")
    }

    override suspend fun editMessage(messageId: String, text: String?, keyboard: BotKeyboard?): SendMessageResponse {
        require(keyboard == null || keyboard.inline)
        val split = messageId.split(":")
        val realChatId = split[0].toInt()
        val realMessageId = split[1].toInt()

        val edit = vk.messages.edit().apply {
            setPeerId(realChatId)
            setMessageId(realMessageId)
            text?.let { setMessage(it) }
            setKeyboard(keyboard.toVkFullKeyboard())
        }

        edit.executeAsync().await()
        return VkSendMessageResponse(messageId)
    }

    override suspend fun pollForUpdates(): Unit = coroutineScope {
        launch(Dispatchers.IO) {
            underlyingBot.startPolling()
        }
        launch {
            while (true) {
                ensureActive()
                delay(1000)
            }
        }.invokeOnCompletion {
            underlyingBot.stopPolling()
        }
    }
}
