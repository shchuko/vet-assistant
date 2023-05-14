package dev.shchuko.vet_assistant.bot.base.statemachine


fun <C : StateMachineContext> StateMachine.Builder<C>.state(id: String, init: StateBuilder<C>.() -> Unit): State<C> {
    val builder = StateBuilder<C>().apply(init)
    val state = builder.createState(id)
    val transitions = builder.transitions
    state(state, transitions)
    return state
}

class StateBuilder<C : StateMachineContext> : StateMachineDslBuilder {
    lateinit var transitions: StateTransitions.Builder<C>.() -> Unit

    var onEnter: suspend State<C>.(context: C, error: Throwable?) -> Unit = { _, _ -> }

    var preEnterFilter: (suspend State<C>.(context: C) -> Boolean)? = null

    var isPauseAfter: (suspend State<C>.(context: C) -> Boolean)? = null

    var reEnterSelfPredicate: (suspend State<C>.(context: C) -> Boolean)? = null

    internal fun createState(id: String): State<C> {
        check(this::transitions.isInitialized) { "${this::class.simpleName}#${this::transitions.name} is not initialized" }

        return object : State<C> {
            override val id = id

            override suspend fun enter(context: C, error: Throwable?) = this@StateBuilder.onEnter(this, context, error)

            override suspend fun preEnterFilter(context: C) =
                this@StateBuilder.preEnterFilter?.invoke(this, context) ?: super.preEnterFilter(context)

            override suspend fun isPauseAfter(context: C) =
                this@StateBuilder.isPauseAfter?.invoke(this, context) ?: super.isPauseAfter(context)


            override suspend fun isReEnterSelf(context: C) =
                this@StateBuilder.reEnterSelfPredicate?.invoke(this, context) ?: super.isReEnterSelf(context)
        }
    }
}


fun <C : StateMachineContext> StateMachine.Builder<C>.plainChain(
    vararg states: State<C>,
    lastStateTransitionsInit: StateTransitions.Builder<C>.() -> Unit
) {
    require(states.size >= 2) { "At least two states required to build plain chain" }

    for (i in 0 until states.size - 1) {
        val stateToConnect = states[i]
        val nextChainState = states[i + 1]

        state(stateToConnect) {
            onSuccess = plain(nextChainState.id)
        }
    }

    state(states.last(), lastStateTransitionsInit)
}

fun <C : StateMachineContext> StateMachine.Builder<C>.subStateMachine(
    id: String,
    stateMachine: StateMachine<C>,
    transitions: StateTransitions.Builder<C>.() -> Unit
) = subStateMachine(id, stateMachine, { this }, transitions)

fun <WRAPPING_CONTEXT : StateMachineContext, SUB_CONTEXT : StateMachineContext> StateMachine.Builder<WRAPPING_CONTEXT>.subStateMachine(
    id: String,
    stateMachine: StateMachine<SUB_CONTEXT>,
    contextMapper: WRAPPING_CONTEXT.() -> SUB_CONTEXT,
    transitions: StateTransitions.Builder<WRAPPING_CONTEXT>.() -> Unit
) = state(StateMachineAsState(id, stateMachine, contextMapper), transitions)

open class StateMachineAsState<WRAPPING_CONTEXT : StateMachineContext, SUB_CONTEXT : StateMachineContext>(
    override val id: String,
    private val stateMachine: StateMachine<SUB_CONTEXT>,
    private val subContextMapper: WRAPPING_CONTEXT.() -> SUB_CONTEXT,
) : State<WRAPPING_CONTEXT> {
    override suspend fun enter(context: WRAPPING_CONTEXT, error: Throwable?) {
        val subContext = context.subContextMapper()
        stateMachine.run(subContext)
    }

    override suspend fun isPauseAfter(context: WRAPPING_CONTEXT) = context.subContextMapper().isPaused(stateMachine)

    override suspend fun isReEnterSelf(context: WRAPPING_CONTEXT) = context.subContextMapper().isRunning(stateMachine)
}


