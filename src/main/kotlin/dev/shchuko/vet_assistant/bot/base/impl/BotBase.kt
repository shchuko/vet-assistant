package dev.shchuko.vet_assistant.bot.base.impl

import dev.shchuko.vet_assistant.bot.base.api.Bot
import dev.shchuko.vet_assistant.bot.base.api.BotCommands
import dev.shchuko.vet_assistant.bot.base.api.BotContext
import dev.shchuko.vet_assistant.bot.base.api.model.BotUpdate
import dev.shchuko.vet_assistant.bot.base.statemachine.StateMachine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

internal abstract class BotBase<C : BotContext, CHAT_ID_T>(
    private val mainStateMachine: StateMachine<C>,
    private val botContextBuilder: StateMachine.Context.Builder<C>,
) : Bot, BotCommands {
    private data class ChatContext(val mutex: Mutex, var botContextSerialized: String)

    private val updatesFlow = MutableSharedFlow<Pair<BotUpdate, ChatContext>>()
    private val chatContextMap: MutableMap<CHAT_ID_T, ChatContext> = ConcurrentHashMap()

    protected abstract suspend fun pollForUpdates()

    final override suspend fun startPolling(): Unit = coroutineScope {
        launch {
            restartOnError {
                updatesFlow.collect { (update, chatCtx) ->
                    launch(Dispatchers.IO) {
                        chatCtx.mutex.withLock {
                            val deserialized = botContextBuilder.deserialize(chatCtx.botContextSerialized)
                            deserialized.update = update
                            deserialized.bot = this@BotBase

                            mainStateMachine.run(deserialized)

                            chatCtx.botContextSerialized = if (deserialized.isRunning(mainStateMachine)) {
                                botContextBuilder.serialize(deserialized)
                            } else {
                                botContextBuilder.serialize(botContextBuilder.createNew())
                            }
                        }
                    }
                }
            }
        }

        pollForUpdates()
    }

    protected suspend fun sendUpdate(update: BotUpdate, chatKey: CHAT_ID_T) {
        val context = chatContextMap.getOrPut(chatKey) {
            val mutex = Mutex()
            val botContext = botContextBuilder.serialize(botContextBuilder.createNew())
            ChatContext(mutex, botContext)
        }
        updatesFlow.emit(Pair(update, context))
    }

    private suspend fun restartOnError(action: suspend () -> Unit) = coroutineScope {
        while (true) {
            ensureActive()
            try {
                action()
            } catch (e: Throwable) {
                e.printStackTrace() // TODO log
            }
        }
    }
}