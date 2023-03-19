package dev.shchuko.vet_assistant.bot.base.statemachine

class StateTransitions<C>(builderInit: Builder<C>.() -> Unit) {
    internal val onSuccess: Transition<C>
    internal val onError: Transition<C>?

    init {
        val builder = Builder<C>().apply(builderInit)
        builder.validate()
        onSuccess = builder.onSuccess
        onError = builder.onError
    }

    abstract class Transition<C> {
        internal abstract fun getNextState(context: C): State<C>?

        internal abstract fun allStatesUsedInTransition(): Sequence<State<C>>
    }

    class Builder<C> : StateMachineDslBuilder {
        /**
         * Transition will be performed if previous state traversed without errors. Mandatory.
         */
        var onSuccess: Transition<C> = noTransition()

        /**
         * Transition will be performed if the previous state traverse failed with exception. Exception will be sent
         * to the next state as run() argument. Optional. If set to null, the exception will be simply rethrown to
         * the upper level.
         */
        var onError: Transition<C>? = null

        /**
         * Plain transition. After previous state run completion, state machine execution continues with [nextState].
         */
        fun plain(nextState: State<C>): Transition<C> = PlainTransition(nextState)

        /**
         * Choice transition which uses context [C] to determine the next state the state machine must continue
         * execution with. See [ChoiceTransition.Builder].
         */
        fun choice(build: ChoiceTransition.Builder<C>.() -> Unit): Transition<C> =
            ChoiceTransition.Builder<C>().apply(build).build()

        /**
         * Use this transition type to mark your state as terminal, which means it has no states to move into after run.
         */
        fun noTransition(): Transition<C> = TransitionStub()


        internal fun validate() {

        }
    }

    class PlainTransition<C> internal constructor(private val nextState: State<C>) :
        Transition<C>() {
        override fun getNextState(context: C) = nextState
        override fun allStatesUsedInTransition() = sequenceOf(nextState)
    }

    class ChoiceTransition<C> private constructor(
        private val choiceMatchers: List<ChoiceEntry<C>>,
        private val elseBranch: ChoiceEntry<C>?
    ) : Transition<C>() {

        init {
            assert(choiceMatchers.none { it.predicate == null })
        }

        class Builder<C> : StateMachineDslBuilder {
            private val conditionMatchers = mutableListOf<ChoiceEntry<C>>()
            private var elseBranchMarcher: ChoiceEntry<C>? = null

            /**
             * Declare new [nextState] the state machine must continue execution with if [predicate] returned true.
             * All such states are traversed sequentially, the state machine will continue with the first match.
             * If no matches found, it will continue with [default] if set, or finish its execution.
             */
            fun case(predicate: (C) -> Boolean, nextState: State<C>) {
                conditionMatchers.add(ChoiceEntry(predicate, nextState))
            }

            /**
             * Fallback [nextState] to traverse into if no predicates set with [case] matched.
             */
            fun default(nextState: State<C>) {
                check(elseBranchMarcher == null) { "ifNoConditionMatched is already set" }
                elseBranchMarcher = ChoiceEntry(null, nextState)
            }

            internal fun build() = ChoiceTransition(conditionMatchers, elseBranchMarcher)
        }

        override fun allStatesUsedInTransition(): Sequence<State<C>> {
            val conditionalStates = choiceMatchers.asSequence().map { it.nextState }
            if (elseBranch != null) {
                return conditionalStates + sequenceOf(elseBranch.nextState)
            }
            return conditionalStates
        }


        private data class ChoiceEntry<C>(val predicate: ((C) -> Boolean)?, val nextState: State<C>)

        override fun getNextState(context: C): State<C>? =
            (choiceMatchers.find { it.predicate!!(context) } ?: elseBranch)?.nextState
    }

    class TransitionStub<C> : Transition<C>() {
        override fun getNextState(context: C) = null
        override fun allStatesUsedInTransition() = emptySequence<State<C>>()
    }
}