package dev.shchuko.vet_assistant.bot.base.statemachine

interface State<C> {
    val id: String
        get() = this::class.simpleName!!

    fun enter(context: C, error: Throwable?)

    fun isPauseBefore(context: C): Boolean = false

    fun isPauseAfter(context: C): Boolean = false

    fun isReEnterSelf(context: C): Boolean = false
}