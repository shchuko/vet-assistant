package dev.shchuko.vet_assistant.api

interface IBot {
    suspend fun startPolling()
}