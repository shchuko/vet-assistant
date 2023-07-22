package dev.shchuko.vet_assistant.bot.base.statemachine

interface State<in C : StateMachine.Context> {
    val id: String
        get() = this::class.simpleName!!

    suspend fun preEnterFilter(context: C) = true

    suspend fun enter(context: C, error: Throwable?)

    suspend fun isPauseAfter(context: C): Boolean = false

    suspend fun isReEnterSelf(context: C): Boolean = false
}