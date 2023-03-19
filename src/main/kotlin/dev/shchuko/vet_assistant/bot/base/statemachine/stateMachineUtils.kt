package dev.shchuko.vet_assistant.bot.base.statemachine


fun <C> StateMachine.Builder<C>.plainChain(
    vararg states: State<C>,
    lastStateTransitionsInit: StateTransitions.Builder<C>.() -> Unit
) {
    require(states.size >= 2) { "At least two states required to build plain chain" }

    for (i in 0 until states.size - 1) {
        val stateToConnect = states[i]
        val nextChainState = states[i + 1]

        state(stateToConnect) {
            onSuccess = plain(nextChainState)
        }
    }

    state(states.last(), lastStateTransitionsInit)
}

