package dev.shchuko.vet_assistant.bot.base.statemachine

class StateTransitions<C : StateMachine.Context>(builderInit: Builder<C>.() -> Unit) {
    internal val onSuccess: Transition<C>
    internal val onError: Transition<C>?

    init {
        val builder = Builder<C>().apply(builderInit)
        builder.validate()
        onSuccess = builder.onSuccess
        onError = builder.onError
    }

    abstract class Transition<C : StateMachine.Context> {
        internal abstract fun getNextStateId(context: C): String?

        internal abstract fun allStatesUsedInTransition(): Sequence<String>
    }

    class Builder<C : StateMachine.Context> : StateMachineDslBuilder {
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
         * Plain transition. After previous state run completion, state machine execution continues with [nextStateId].
         */
        fun plain(nextStateId: String): Transition<C> = PlainTransition(nextStateId)

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

    class PlainTransition<C : StateMachine.Context> internal constructor(private val nextStateId: String) :
        Transition<C>() {
        override fun getNextStateId(context: C) = nextStateId
        override fun allStatesUsedInTransition() = sequenceOf(nextStateId)
    }

    class ChoiceTransition<C : StateMachine.Context> private constructor(
        private val choiceMatchers: List<ChoiceEntry<C>>,
        private val elseBranch: ChoiceEntry<C>?
    ) : Transition<C>() {

        init {
            assert(choiceMatchers.none { it.predicate == null })
        }

        class Builder<C : StateMachine.Context> : StateMachineDslBuilder {
            private val conditionMatchers = mutableListOf<ChoiceEntry<C>>()
            private var elseBranchMarcher: ChoiceEntry<C>? = null

            /**
             * Declare new [nextStateId] the state machine must continue execution with if [predicate] returned true.
             * All such states are traversed sequentially, the state machine will continue with the first match.
             * If no matches found, it will continue with [default] if set, or finish its execution.
             */
            fun case(nextStateId: String, predicate: (C) -> Boolean) {
                conditionMatchers.add(ChoiceEntry(predicate, nextStateId))
            }

            /**
             * Fallback [nextStateId] to traverse into if no predicates set with [case] matched.
             */
            fun default(nextStateId: String) {
                check(elseBranchMarcher == null) { "ifNoConditionMatched is already set" }
                elseBranchMarcher = ChoiceEntry(null, nextStateId)
            }

            internal fun build() = ChoiceTransition(conditionMatchers, elseBranchMarcher)
        }

        override fun allStatesUsedInTransition(): Sequence<String> {
            val conditionalStates = choiceMatchers.asSequence().map { it.nextStateId }
            if (elseBranch != null) {
                return conditionalStates + sequenceOf(elseBranch.nextStateId)
            }
            return conditionalStates
        }


        private data class ChoiceEntry<C>(val predicate: ((C) -> Boolean)?, val nextStateId: String)

        override fun getNextStateId(context: C): String? =
            (choiceMatchers.find { it.predicate!!(context) } ?: elseBranch)?.nextStateId
    }

    class TransitionStub<C : StateMachine.Context> : Transition<C>() {
        override fun getNextStateId(context: C) = null
        override fun allStatesUsedInTransition() = emptySequence<String>()
    }
}