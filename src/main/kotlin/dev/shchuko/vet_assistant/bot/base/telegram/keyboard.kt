package dev.shchuko.vet_assistant.bot.base.telegram

import com.github.omarmiatello.telegram.*
import dev.shchuko.vet_assistant.bot.base.api.keyboard.BotKeyboard

internal fun BotKeyboard?.toTelegramFullKeyboard(): KeyboardOption {
    if (this == null) return ReplyKeyboardRemove(remove_keyboard = true)
    return if (inline) InlineKeyboardMarkup(rows.map { row ->
        row.map { button ->
            InlineKeyboardButton(button, callback_data = button)
        }
    })
    else ReplyKeyboardMarkup(resize_keyboard = true, one_time_keyboard = true, keyboard = rows.map { row ->
        row.map { button ->
            KeyboardButton(button)
        }
    })
}
