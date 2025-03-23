package dev.shchuko.vet_assistant.impl

import com.vk.api.sdk.client.VkApiClient
import com.vk.api.sdk.client.actors.GroupActor
import com.vk.api.sdk.events.longpoll.GroupLongPollApi
import com.vk.api.sdk.exceptions.ClientException
import com.vk.api.sdk.httpclient.HttpTransportClient
import com.vk.api.sdk.objects.callback.MessageNew
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory


class VetAssistantVkBot(groupId: Long, apiKey: String) : VetAssistantBot() {
    companion object {
        private val logger = LoggerFactory.getLogger(VetAssistantVkBot::class.java)
    }

    private val transportClient = HttpTransportClient()
    private val vk = VkApiClient(transportClient)
    private val groupActor = GroupActor(groupId, apiKey)
    private val cs = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override suspend fun startPolling() {
        val longPoll = VkMessageHandlerBridge(vk, groupActor, 1) { (fromId, userInput) ->
            if (fromId == Math.negateExact(groupActor.groupId)) {
                return@VkMessageHandlerBridge
            }
            cs.launch {
                try {
                    val result = handleSearchMedicineRequest(userInput) ?: return@launch

                    vk.messages().sendUserIds(groupActor)
                        .peerId(fromId)
                        .randomId(0)
                        .message(result)
                        .executeAsString()
                } catch (e: ClientException) {
                    logger.error("Failed to send message to peer={}", fromId, e)
                }
            }
        }
        longPoll.run()
    }


    private data class VkMessage(val peerId: Long, val text: String)

    private inner class VkMessageHandlerBridge(
        client: VkApiClient,
        actor: GroupActor,
        waitTime: Int,
        private val handler: (VkMessage) -> Unit
    ) : GroupLongPollApi(client, actor, waitTime) {
        override fun messageNew(groupId: Int?, message: MessageNew?) {
            val message = message ?: return
            handler(VkMessage(message.`object`.message.peerId, message.`object`.message.text ?: return))
        }
    }
}