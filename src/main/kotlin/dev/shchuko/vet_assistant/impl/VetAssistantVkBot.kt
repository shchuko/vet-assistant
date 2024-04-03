package dev.shchuko.vet_assistant.impl

import com.petersamokhin.vksdk.core.client.VkApiClient
import com.petersamokhin.vksdk.core.model.VkSettings
import com.petersamokhin.vksdk.core.model.event.MessageNew
import com.petersamokhin.vksdk.http.VkOkHttpClient
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory


class VetAssistantVkBot(groupId: Int, apiKey: String) : VetAssistantBot() {
    companion object {
        private val logger = LoggerFactory.getLogger(VetAssistantVkBot::class.java)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val vkHttpClient = VkOkHttpClient()

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
        while (isActive) {
            try {
                client.startLongPolling()
            } catch (e: Exception) {
                if (e is CancellationException) {
                    throw e
                }
                client.stopLongPolling()
                logger.error("VK Polling failed with exception", e)
            }
        }
    }
}