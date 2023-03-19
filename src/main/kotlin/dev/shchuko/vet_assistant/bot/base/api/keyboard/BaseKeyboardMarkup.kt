package dev.shchuko.vet_assistant.bot.base.api.keyboard

import java.util.*

class BaseKeyboardMarkup(builderInit: Builder.() -> Unit) {
    val rows: List<List<(Locale) -> String>>

    init {
        rows = Builder().apply(builderInit).rows
    }

    @KeyboardDslBuilderMarker
    class Builder {
        internal val rows = mutableListOf<List<(Locale) -> String>>()

        fun row(rowInit: Row.() -> Unit) = rows.add(Row().apply(rowInit).buttons)

        @KeyboardDslBuilderMarker
        class Row {
            internal val buttons = mutableListOf<(Locale) -> String>()
            fun button(textProvider: (Locale) -> String) {
                buttons.add(textProvider)
            }
        }
    }
}