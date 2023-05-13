package dev.shchuko.vet_assistant.bot.base.api.keyboard

class BotKeyboard(builderInit: Builder.() -> Unit) {
    val rows: List<List<String>>
    val inline: Boolean

    init {
        val builder = Builder().apply(builderInit)
        rows = builder.rows
        inline = builder.inline
    }

    @KeyboardDslBuilderMarker
    class Builder {
        internal val rows = mutableListOf<List<String>>()
        var inline: Boolean = false

        fun row(rowInit: Row.() -> Unit) = rows.add(Row().apply(rowInit).buttons)

        @KeyboardDslBuilderMarker
        class Row {
            internal val buttons = mutableListOf<String>()
            fun button(text: String) {
                buttons.add(text)
            }
        }
    }
}

fun buildKeyboard(builderInit: BotKeyboard.Builder.() -> Unit) = BotKeyboard(builderInit)

@DslMarker
private annotation class KeyboardDslBuilderMarker