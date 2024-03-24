package dev.shchuko.vet_assistant.impl

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.document
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.TelegramFile
import dev.shchuko.vet_assistant.api.UserService
import org.koin.core.component.inject

class VetAssistantTelegramBot(apiKey: String) : VetAssistantBot() {
    // 100 mb
    private val maxFileSizeBytes = 100 * 1024 * 1024

    private val userService by inject<UserService>()

    private val bot = bot {
        token = apiKey

        dispatch {
            text {
                val messageText = message.text ?: return@text
                val result = handleSearchMedicineRequest(messageText) ?: return@text
                bot.sendMessage(ChatId.fromId(message.chat.id), result)
            }

            command(Commands.GET_ALL) {
                if (!message.isAdminMessage()) return@command
                sendCurrentMedicineEntries(bot, message)
            }

            document {
                if (!message.isAdminMessage()) return@document

                sendCurrentMedicineEntries(bot, message)

                val document = message.document ?: return@document

                val fileSize = document.fileSize ?: return@document
                if (fileSize > maxFileSizeBytes) {
                    bot.sendMessage(ChatId.fromId(message.chat.id), getFileTooBigMessage())
                } else {
                    try {
                        val content =
                            bot.downloadFileBytes(document.fileId)?.toString(Charsets.UTF_8) ?: return@document
                        handleSetAllMedicineCsv(content)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun Message.isAdminMessage(): Boolean {
        val username = chat.username ?: return false
        return userService.isTelegramBotAdministrator(username)
    }

    private fun sendCurrentMedicineEntries(bot: Bot, message: Message) {
        bot.sendDocument(
            ChatId.fromId(message.chat.id),
            TelegramFile.ByByteArray(
                handleGetAllMedicineCsv().toByteArray(Charsets.UTF_8),
                filename = "medicine_list.csv"
            )
        )
    }

    override suspend fun startPolling() {
        bot.startPolling()
    }
}