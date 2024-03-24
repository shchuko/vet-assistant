package dev.shchuko.vet_assistant.impl

import com.petersamokhin.vksdk.core.client.VkApiClient
import com.petersamokhin.vksdk.core.model.VkSettings
import com.petersamokhin.vksdk.core.model.event.MessageNew
import com.petersamokhin.vksdk.http.VkOkHttpClient
import kotlinx.coroutines.*


class VetAssistantVkBot(groupId: Int, apiKey: String) : VetAssistantBot() {

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
        }.execute()
    }

    override suspend fun startPolling() {
        client.startLongPolling()
    }
}