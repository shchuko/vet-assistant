package dev.shchuko.vet_assistant.bot.base.statemachine

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

open class StateMachine<C>(init: Builder<C>.() -> Unit) {
    private val initialState: State<C>
    private val transitions: Map<String, StateTransitions<C>>
    private val states: Map<String, State<C>>
    private val globalErrorHandler: State<C>?

    init {
        val builder = Builder<C>()
        builder.apply(init)
        builder.validate()

        initialState = builder.initialState
        states = builder.states.toMap()
        transitions = builder.transitions.toMap()
        globalErrorHandler = builder.globalErrorHandler
    }

    /**
     * Serializable state machine state.
     */
    @Serializable
    class MachineContext<C> internal constructor(val userContext: C) {
        internal var nextStateId: String? = null

        /**
         * We don't serialize exceptions: they must be handled immediately
         */
        @Transient
        internal var error: Throwable? = null

        internal var paused: Boolean = false

        fun isRunning() = nextStateId != null
        fun isPaused() = paused
    }


    /**
     * Run state machine. Performs traverse over state machine states while state machine has states to transit into
     * (use [MachineContext.isRunning] to check) and state machine is not paused on some state (use
     * [MachineContext.isPaused] to check)
     *
     * To build [machineContext] use [buildInitialMachineContext] method.
     */
    fun run(machineContext: MachineContext<C>) {
        do {
            check(machineContext.isRunning()) { "machine state run called " }
            runIteration(machineContext)
        } while (machineContext.isRunning() && !machineContext.paused)
    }

    fun buildInitialMachineContext(externalContext: C) =
        MachineContext(externalContext).apply { nextStateId = initialState.id }

    private fun runIteration(machineCtx: MachineContext<C>) {
        val currentStateId = machineCtx.nextStateId ?: error("next() called when when hasNext() == false")
        val currentState = states[currentStateId] ?: error("not found state with $currentStateId in this state machine")
        val transition = transitions[currentStateId]
            ?: error("Unexpected internal error, no transition for state $currentState found")

        val userCtx = machineCtx.userContext!!
        try {
            // Enter a state and reset error
            val error = machineCtx.error
            machineCtx.error = null
            currentState.enter(userCtx, error)

            // Pause state machine if required
            machineCtx.paused = currentState.isPauseAfter(userCtx)

            // If the current state requests re-enter self, no more settings to update
            if (currentState.isReEnterSelf(userCtx)) {
                return
            }

            // Otherwise, get and setup next state
            val nextState = transition.onSuccess.getNextState(userCtx)
            machineCtx.nextStateId = nextState?.id

            // Update pause: disable if execution complete, disable pause if execution has finished
            machineCtx.paused = nextState?.isPauseBefore(userCtx) ?: false
        } catch (e: Throwable) {
            // Exceptions thrown in globalErrorHandler mustn't be passed to itself
            if (currentState == globalErrorHandler) {
                throw e
            }

            machineCtx.nextStateId = transition.onError?.getNextState(userCtx)?.id ?: globalErrorHandler?.id ?: throw e
            machineCtx.error = e
        }
    }

    class Builder<C> : StateMachineDslBuilder {
        /**
         * Use to set State Machine state to start execution from. Mandatory.
         */
        lateinit var initialState: State<C>

        /**
         * Global errors handler for the state machine. Optional.
         */
        var globalErrorHandler: State<C>? = null

        internal val transitions: MutableMap<String, StateTransitions<C>> = mutableMapOf()
        internal val states: MutableMap<String, State<C>> = mutableMapOf()

        fun state(state: State<C>, connector: StateTransitions.Builder<C>.() -> Unit) {
            check(!states.contains(state.id)) { "${state.id} is not unique" }
            states[state.id] = state
            transitions[state.id] = StateTransitions(connector)
        }

        internal fun validate() {
            check(this::initialState.isInitialized) { "${this::initialState.name} is not set" }

            val usedStates = collectAllUsedStates()

            val firstNotConnected = usedStates.firstOrNull { it.id !in states }
            check(firstNotConnected == null) { "$firstNotConnected state is used in state machine but not connected to it using ${this::state.name}()" }

            val reachableFromInitialState = collectAllReachableStates()
            val firstUnreachable =
                usedStates.firstOrNull { it !in reachableFromInitialState && it != globalErrorHandler }
            check(firstUnreachable == null) { "$firstUnreachable state is unreachable from state machine initial state" }

        }

        private fun collectAllUsedStates(): Set<State<C>> {
            val usedStates = mutableSetOf(initialState)
            transitions.forEach { (id, transition) ->
                usedStates.add(states[id] ?: error("Unexpected builder problem: $id state id is not found"))
                usedStates.addAll(transition.onSuccess.allStatesUsedInTransition())
                transition.onError?.let { usedStates.addAll(it.allStatesUsedInTransition()) }
            }
            globalErrorHandler?.let { usedStates.add(it) }
            return usedStates
        }

        private fun collectAllReachableStates(): Set<State<C>> {
            val visited = mutableSetOf<State<C>>()

            /* Traverse all states using DFS */
            val queue = ArrayDeque<State<C>>()
            queue.addLast(initialState)

            while (queue.isNotEmpty()) {
                val state = queue.removeFirst()
                val added = visited.add(state)
                assert(added)

                val stateTransitions = transitions[state.id] ?: continue
                val onSuccessStates = stateTransitions.onSuccess.allStatesUsedInTransition()
                val onErrorStates = stateTransitions.onError?.allStatesUsedInTransition() ?: emptySequence()
                queue.addAll((onSuccessStates + onErrorStates).distinct().filter { it !in visited })
            }
            return visited
        }
    }
}
