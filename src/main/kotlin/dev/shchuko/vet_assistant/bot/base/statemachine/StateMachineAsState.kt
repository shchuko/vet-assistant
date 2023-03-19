package dev.shchuko.vet_assistant.bot.base.statemachine

open class StateMachineAsState<WRAPPING_CONTEXT, SUB_CONTEXT>(
    override val id: String,
    private val stateMachine: StateMachine<SUB_CONTEXT>,
    private val subContextMapper: WRAPPING_CONTEXT.() -> StateMachine.MachineContext<SUB_CONTEXT>,
) : State<WRAPPING_CONTEXT> {
    override fun enter(context: WRAPPING_CONTEXT, error: Throwable?) {
        val subContext = context.subContextMapper()
        stateMachine.run(subContext)
    }

    override fun isPauseAfter(context: WRAPPING_CONTEXT) = context.subContextMapper().isPaused()

    override fun isReEnterSelf(context: WRAPPING_CONTEXT) = context.subContextMapper().isRunning()
}