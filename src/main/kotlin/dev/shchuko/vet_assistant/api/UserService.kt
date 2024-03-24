package dev.shchuko.vet_assistant.api

interface UserService {
    fun isTelegramBotAdministrator(username: String): Boolean
}