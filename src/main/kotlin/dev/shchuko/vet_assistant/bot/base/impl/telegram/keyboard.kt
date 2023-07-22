package dev.shchuko.vet_assistant.bot.base.impl.telegram

import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.ReplyKeyboardRemove
import com.github.kotlintelegrambot.entities.ReplyMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import dev.shchuko.vet_assistant.bot.base.api.keyboard.BotKeyboard

internal fun BotKeyboard?.toTelegramFullKeyboard(): ReplyMarkup {
    if (this == null) return ReplyKeyboardRemove()
    return if (inline) InlineKeyboardMarkup.create(rows.map { row ->
        row.map { button ->
            InlineKeyboardButton.CallbackData(button, callbackData = button)
        }
    })
    else KeyboardReplyMarkup.createSimpleKeyboard(resizeKeyboard = true, oneTimeKeyboard = true, keyboard = rows)
}
