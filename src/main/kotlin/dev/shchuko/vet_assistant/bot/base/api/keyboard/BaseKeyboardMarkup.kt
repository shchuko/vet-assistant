package dev.shchuko.vet_assistant.bot.base.api.keyboard

class BaseKeyboardMarkup(builderInit: Builder.() -> Unit) {
    val rows: List<List<String>>

    init {
        rows = Builder().apply(builderInit).verify().rows
    }

    @KeyboardDslBuilderMarker
    class Builder {
        internal val rows = mutableListOf<List<String>>()

        fun row(rowInit: Row.() -> Unit) = rows.add(Row().apply(rowInit).buttons)

        internal fun verify(): Builder {
            val duplicatedNames = rows.flatten().groupBy { it }.filter { it.value.size > 1 }
            check(duplicatedNames.isEmpty()) {
                "Keyboard button names are duplicated: ${duplicatedNames.keys}"
            }
            return this
        }

        @KeyboardDslBuilderMarker
        class Row {
            internal val buttons = mutableListOf<String>()
            fun button(text: String) {
                buttons.add(text)
            }
        }
    }
}