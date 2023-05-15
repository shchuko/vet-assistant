package dev.shchuko.vet_assistant.bot.base.impl

import dev.shchuko.vet_assistant.bot.base.api.Bot
import dev.shchuko.vet_assistant.bot.base.api.BotCommands
import dev.shchuko.vet_assistant.bot.base.api.BotContext
import dev.shchuko.vet_assistant.bot.base.api.model.BotUpdate
import dev.shchuko.vet_assistant.bot.base.statemachine.StateMachine
import dev.shchuko.vet_assistant.bot.base.statemachine.StateMachineContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors.newFixedThreadPool
import java.util.concurrent.Executors.newSingleThreadExecutor

internal abstract class BotBase<in C : BotContext, CHAT_ID_T>(
    private val mainStateMachine: StateMachine<C>,
    private val botContextBuilder: StateMachineContext.Builder<C>,
) : Bot, BotCommands {
    private data class ChatContext(val mutex: Mutex, var botContextSerialized: String)

    private val pollDispatcher = newSingleThreadExecutor().asCoroutineDispatcher()
    private val ioDispatcher = newFixedThreadPool(Runtime.getRuntime().availableProcessors()).asCoroutineDispatcher()

    private val cs = CoroutineScope(pollDispatcher)
    private val updatesChannel: Channel<Pair<BotUpdate, ChatContext>> = Channel(capacity = Channel.UNLIMITED)
    private val chatContextMap: MutableMap<CHAT_ID_T, ChatContext> = ConcurrentHashMap()

    open suspend fun pollImpl() {}

    override suspend fun start() {
        cs.launch {
            launch { restartOnError { consumeUpdates() } }
            launch(pollDispatcher) { restartOnError { pollImpl() } }
        }
    }

    private suspend fun consumeUpdates() = coroutineScope {
        withContext(pollDispatcher) {
            for (pair in updatesChannel) {
                launch(ioDispatcher) {
                    val (update, chatCtx) = pair
                    chatCtx.mutex.withLock {
                        val deserialized = botContextBuilder.deserialize(chatCtx.botContextSerialized)
                        deserialized.update = update
                        deserialized.bot = this@BotBase

                        mainStateMachine.run(deserialized)

                        chatCtx.botContextSerialized = if (deserialized.isRunning(mainStateMachine))
                            botContextBuilder.serialize(deserialized)
                        else
                            botContextBuilder.serialize(botContextBuilder.createNew())

                    }
                }
            }
        }
    }

    protected suspend fun sendUpdate(update: BotUpdate, chatKey: CHAT_ID_T) {
        val context = chatContextMap.getOrPut(chatKey) {
            val mutex = Mutex()
            val botContext = botContextBuilder.serialize(botContextBuilder.createNew())
            ChatContext(mutex, botContext)
        }
        updatesChannel.send(Pair(update, context))
    }

    protected fun sendUpdate2(update: BotUpdate, chatKey: CHAT_ID_T) = cs.launch(pollDispatcher) {
        sendUpdate(update, chatKey)
    }

    private suspend fun CoroutineScope.restartOnError(action: suspend () -> Unit) {
        while (isActive) {
            try {
                action()
            } catch (e: Exception) {
                e.printStackTrace() // TODO log
            }
        }
    }
}