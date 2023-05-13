package dev.shchuko.vet_assistant.bot.base.statemachine

interface State<in C : StateMachineContext> {
    val id: String
        get() = this::class.simpleName!!

    fun preEnterFilter(context: C) = true

    suspend fun enter(context: C, error: Throwable?)

    fun isPauseAfter(context: C): Boolean = false

    fun isReEnterSelf(context: C): Boolean = false
}