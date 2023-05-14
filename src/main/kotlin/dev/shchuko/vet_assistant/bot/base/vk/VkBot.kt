package dev.shchuko.vet_assistant.bot.base.vk

import api.longpoll.bots.LongPollBot
import api.longpoll.bots.methods.VkBotsMethods
import api.longpoll.bots.model.events.messages.MessageNew
import dev.shchuko.vet_assistant.bot.base.api.BotBase
import dev.shchuko.vet_assistant.bot.base.api.BotContext
import dev.shchuko.vet_assistant.bot.base.api.keyboard.BotKeyboard
import dev.shchuko.vet_assistant.bot.base.api.model.BotUpdate
import dev.shchuko.vet_assistant.bot.base.api.model.SendMessageResponse
import dev.shchuko.vet_assistant.bot.base.statemachine.StateMachine
import dev.shchuko.vet_assistant.bot.base.statemachine.StateMachineContext
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors


open class VkBot<in C : BotContext>(
    mainStateMachine: StateMachine<C>,
    botContextBuilder: StateMachineContext.Builder<C>,
    apiKey: String
) : BotBase<C, Int>(mainStateMachine, botContextBuilder) {
    private val pollingThread = Executors.newSingleThreadExecutor()
    private val vk = VkBotsMethods(apiKey)

    private val underlyingBot = object : LongPollBot() {
        override fun getAccessToken() = apiKey

        override fun onMessageNew(messageNew: MessageNew) {
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
            sendUpdate2(botUpdate, key)
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
        return VkSendMessageResponse(messageId)
    }

    override suspend fun pollImpl(): Unit = coroutineScope {
        // TODO support cancellation?
        CompletableFuture.runAsync({ underlyingBot.startPolling() }, pollingThread).await()
    }
}
