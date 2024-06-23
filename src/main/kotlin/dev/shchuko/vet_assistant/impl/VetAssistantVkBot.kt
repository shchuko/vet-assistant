package dev.shchuko.vet_assistant.impl

import com.petersamokhin.vksdk.core.client.VkApiClient
import com.petersamokhin.vksdk.core.model.VkSettings
import com.petersamokhin.vksdk.core.model.event.MessageNew
import com.petersamokhin.vksdk.http.VkKtorHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.minutes


class VetAssistantVkBot(groupId: Int, apiKey: String) : VetAssistantBot() {
    companion object {
        private val logger = LoggerFactory.getLogger(VetAssistantVkBot::class.java)
    }

    private val vkHttpClient = VkKtorHttpClient(
        CoroutineScope(Dispatchers.Default).coroutineContext, // have no idea why this is needed
        HttpClient(CIO) {
            install(HttpRequestRetry) {
                retryOnExceptionOrServerErrors(maxRetries = 10)
                exponentialDelay()
            }

            install(HttpTimeout) {
                connectTimeoutMillis = 2.minutes.inWholeMilliseconds
                requestTimeoutMillis = 2.minutes.inWholeMilliseconds
                socketTimeoutMillis = 2.minutes.inWholeMilliseconds
            }
        }
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val client = VkApiClient(groupId, apiKey, VkApiClient.Type.Community, VkSettings(vkHttpClient)).also { client ->
        client.onMessage { handleMessage(it) }
    }

    private val cs = CoroutineScope(Dispatchers.Default + SupervisorJob())


    private fun handleMessage(event: MessageNew): Job = cs.launch {
        val result = handleSearchMedicineRequest(event.message.text) ?: return@launch
        client.sendMessage {
            peerId = event.message.peerId
            message = result
                // vk bot does not print '+'
                .replace("+", "%2B")
        }.execute()
    }

    override suspend fun startPolling() = coroutineScope {
        var restart = false
        while (isActive) {
            try {
                client.startLongPolling(restart)
                restart = true
            } catch (e: Exception) {
                logger.error("VK Polling failed with exception", e)
                ensureActive()
                logger.error("Restarting VK Polling", e)
            }
        }
    }
}