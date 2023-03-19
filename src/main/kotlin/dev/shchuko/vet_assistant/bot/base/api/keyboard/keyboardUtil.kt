package dev.shchuko.vet_assistant.bot.base.api.keyboard

fun buildKeyboard(builderInit: BaseKeyboardMarkup.Builder.() -> Unit) = BaseKeyboardMarkup(builderInit)