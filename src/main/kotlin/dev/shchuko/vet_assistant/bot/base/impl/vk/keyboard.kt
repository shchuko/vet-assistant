package dev.shchuko.vet_assistant.bot.base.impl.vk

import api.longpoll.bots.model.objects.additional.Keyboard
import api.longpoll.bots.model.objects.additional.buttons.Button
import api.longpoll.bots.model.objects.additional.buttons.TextButton
import dev.shchuko.vet_assistant.bot.base.api.keyboard.BotKeyboard

internal fun BotKeyboard?.toVkFullKeyboard(): Keyboard {
    if (this == null) return Keyboard(emptyList())
    return Keyboard(rows.map { row ->
        row.map { button ->
            @Suppress("USELESS_CAST")
            TextButton(Button.Color.POSITIVE, TextButton.Action(button)) as Button
        }
    }).setInline(inline).setOneTime(!inline)
}