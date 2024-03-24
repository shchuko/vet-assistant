package dev.shchuko.vet_assistant.impl

import dev.shchuko.vet_assistant.api.UserService
import dev.shchuko.vet_assistant.impl.db.TelegramUserEntity
import dev.shchuko.vet_assistant.impl.db.TelegramUserTable
import org.jetbrains.exposed.sql.transactions.transaction

class UserServiceImpl : UserService {
    override fun isTelegramBotAdministrator(username: String): Boolean = transaction {
        TelegramUserEntity.find {
            TelegramUserTable.telegramUsername eq username
        }.singleOrNull()?.isAdmin ?: false
    }
}