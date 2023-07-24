package dev.shchuko.vet_assistant.bot.base.statemachine

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

open class StateMachine<C : StateMachine.Context>(val id: String, init: Builder<C>.() -> Unit) {
    internal val initialState: State<C>
    private val transitions: Map<String, StateTransitions<C>>
    private val states: Map<String, State<C>>
    private val globalErrorHandler: State<C>?

    init {
        val builder = Builder<C>()
        builder.apply(init)
        builder.validate()

        states = builder.states.toMap()
        transitions = builder.transitions.toMap()
        initialState = states.getValue(builder.initialStateId)

        val globalErrorHandlerId = builder.globalErrorHandlerId
        globalErrorHandler = if (globalErrorHandlerId != null) states.getValue(globalErrorHandlerId) else null
    }

    fun runBlocking(machineContext: C) = runBlocking { run(machineContext) }

    suspend fun run(machineContext: C) {
        if (!machineContext.has(this) || machineContext.getNextState(this) == null) {
            machineContext.register(this)
        }

        do {
            check(machineContext.isRunning(this)) { "Unable to run on already finished state machine" }
            runIteration(machineContext)
        } while (machineContext.isRunning(this) && !machineContext.isPaused(this))
    }

    private suspend fun runIteration(machineCtx: C) {
        val currentStateId = machineCtx.getNextState(this) ?: error("next() called when when hasNext() == false")
        val currentState = states[currentStateId] ?: error("not found state with $currentStateId in this state machine")
        val transition = transitions[currentStateId]
            ?: error("Unexpected internal error, no transition for state $currentState found")

        try {
            // If not propagating error - check preEnterFilter and re-enter state if filter did not match.
            if (machineCtx.error == null && !currentState.preEnterFilter(machineCtx)) {
                machineCtx.updatePaused(this, true)
                return
            }

            // Enter a state and reset error
            val error = machineCtx.error
            machineCtx.error = null
            currentState.enter(machineCtx, error)

            // Pause state machine if required
            machineCtx.updatePaused(this, currentState.isPauseAfter(machineCtx))

            // If the current state requests re-enter self, no more settings to update
            if (currentState.isReEnterSelf(machineCtx)) {
                return
            }

            // Otherwise, get and setup next state
            val nextStateId = transition.onSuccess.getNextStateId(machineCtx)
            if (nextStateId == null) {
                machineCtx.unregister(this)
            } else {
                machineCtx.updateNextState(this, nextStateId)
            }
        } catch (e: Throwable) {
            // Exceptions thrown in globalErrorHandler mustn't be passed to itself
            if (currentState == globalErrorHandler) {
                throw e
            }

            val errorHandler = transition.onError?.getNextStateId(machineCtx) ?: globalErrorHandler?.id
            if (errorHandler == null) {
                machineCtx.unregister(this)
                throw e
            }

            machineCtx.updateNextState(this, errorHandler)
            machineCtx.error = e
            machineCtx.updatePaused(this, false)
        }
    }

    class Builder<C : Context> : StateMachineDslBuilder {
        /**
         * Use to set State Machine state to start execution from. Mandatory.
         */
        lateinit var initialStateId: String

        /**
         * Global errors handler for the state machine. Optional.
         */
        var globalErrorHandlerId: String? = null

        internal val transitions: MutableMap<String, StateTransitions<C>> = mutableMapOf()
        internal val states: MutableMap<String, State<C>> = mutableMapOf()

        fun state(state: State<C>, connector: StateTransitions.Builder<C>.() -> Unit) {
            check(!states.contains(state.id)) { "${state.id} is not unique" }
            states[state.id] = state
            transitions[state.id] = StateTransitions(connector)
        }

        internal fun validate() {
            check(this::initialStateId.isInitialized) { "${this::initialStateId.name} is not set" }

            val usedStates = collectAllUsedStates()

            val firstNotConnected = usedStates.firstOrNull { it.id !in states }
            check(firstNotConnected == null) { "$firstNotConnected state is used in state machine but not connected to it using ${this::state.name}()" }

            val reachableFromInitialState = collectAllReachableStates()
            val firstUnreachable =
                usedStates.firstOrNull { it !in reachableFromInitialState && it != globalErrorHandlerId?.let(::getStateOrThrow) }
            check(firstUnreachable == null) { "$firstUnreachable state is unreachable from state machine initial state" }

        }

        private fun collectAllUsedStates(): Set<State<C>> {
            val usedStates = mutableSetOf(getStateOrThrow(initialStateId))
            transitions.forEach { (id, transition) ->
                usedStates.add(getStateOrThrow(id))
                usedStates.addAll(transition.onSuccess.allStatesUsedInTransition().map(::getStateOrThrow))
                transition.onError?.let {
                    usedStates.addAll(it.allStatesUsedInTransition().map(::getStateOrThrow))
                }
            }
            globalErrorHandlerId?.let { usedStates.add(getStateOrThrow(it)) }
            return usedStates
        }

        private fun getStateOrThrow(id: String): State<C> =
            states[id] ?: error("Unexpected builder problem: $id state id is not found")

        private fun collectAllReachableStates(): Set<State<C>> {
            val visited = mutableSetOf<State<C>>()

            /* Traverse all states using DFS */
            val queue = ArrayDeque<State<C>>()
            queue.addLast(getStateOrThrow(initialStateId))

            while (queue.isNotEmpty()) {
                val state = queue.removeFirst()
                val added = visited.add(state)
                assert(added)

                val stateTransitions = transitions[state.id] ?: continue
                val onSuccessStates = stateTransitions.onSuccess.allStatesUsedInTransition().map(::getStateOrThrow)
                val onErrorStates =
                    stateTransitions.onError?.allStatesUsedInTransition()?.map(::getStateOrThrow) ?: emptySequence()
                queue.addAll((onSuccessStates + onErrorStates).distinct().filter { it !in visited })
            }
            return visited
        }
    }

    /**
     * Serializable state machine state.
     */
    @Serializable
    open class Context {
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
            polymorphic(Context::class)
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


        abstract class Builder<C : Context> {
            protected val serializerFormat by lazy { Json { serializersModule = createNew().mySerializersModule } }

            abstract fun createNew(): C
            abstract fun serialize(context: C): String
            abstract fun deserialize(string: String): C
        }
    }
}
