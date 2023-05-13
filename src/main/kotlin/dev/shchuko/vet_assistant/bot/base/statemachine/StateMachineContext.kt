package dev.shchuko.vet_assistant.bot.base.statemachine

import dev.shchuko.vet_assistant.bot.base.api.BotContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic


/**
 * Serializable state machine state.
 */
@Serializable
open class StateMachineContext {
    @Serializable
    private data class StateMachineCondition(var nextStateId: String?, var paused: Boolean = false)

    private val nextStateIds = mutableMapOf<String, StateMachineCondition>()

    /**
     * We don't serialize exceptions: they must be handled immediately
     */
    @Transient
    internal var error: Throwable? = null

    @Transient
    open val mySerializersModule = SerializersModule {
        polymorphic(StateMachineContext::class)
    }

    fun isRunning(stateMachine: StateMachine<*>) = nextStateIds[stateMachine.id]?.nextStateId != null
    internal fun isPaused(stateMachine: StateMachine<*>) = nextStateIds[stateMachine.id]?.paused == true

    fun has(stateMachine: StateMachine<*>) = nextStateIds[stateMachine.id] != null

    fun register(stateMachine: StateMachine<*>) {
        nextStateIds[stateMachine.id] = StateMachineCondition(stateMachine.initialState.id)
    }

    fun unregister(stateMachine: StateMachine<*>) {
        nextStateIds.remove(stateMachine.id)
    }

    fun getNextState(stateMachine: StateMachine<*>): String? {
        val condition = checkNotNull(nextStateIds[stateMachine.id])
        return condition.nextStateId
    }

    fun updateNextState(stateMachine: StateMachine<*>, stateId: String) {
        val condition = checkNotNull(nextStateIds[stateMachine.id])
        condition.nextStateId = stateId
    }

    fun updatePaused(stateMachine: StateMachine<*>, paused: Boolean) {
        val condition = checkNotNull(nextStateIds[stateMachine.id])
        condition.paused = paused
    }


    abstract class Builder<C : BotContext> {
        protected val serializerFormat by lazy { Json { serializersModule = createNew().mySerializersModule } }

        abstract fun createNew(): C
        abstract fun serialize(context: C): String
        abstract fun deserialize(string: String): C
    }
}