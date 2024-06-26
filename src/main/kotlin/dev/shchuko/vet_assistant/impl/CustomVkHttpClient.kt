package dev.shchuko.vet_assistant.impl


import com.petersamokhin.vksdk.core.error.VkException
import com.petersamokhin.vksdk.core.http.ContentType
import com.petersamokhin.vksdk.core.http.HttpClient
import com.petersamokhin.vksdk.core.http.HttpClientConfig
import com.petersamokhin.vksdk.core.http.Response
import com.petersamokhin.vksdk.core.model.objects.UploadableContent
import io.ktor.client.engine.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*

open class CustomVkHttpClient(
    private var client: io.ktor.client.HttpClient,
) : HttpClient {

    /**
     * Apply client configuration
     *
     * @param config Configuration, such as read and connect timeout, etc
     */
    final override fun applyConfig(config: HttpClientConfig) {
        client = io.ktor.client.HttpClient(
            createEngineWithConfig(config) ?: throw IllegalStateException(ERROR_MESSAGE_INITIALIZATION)
        )
    }

    /**
     * Make GET request
     *
     * @param url Full request url: host, query, etc.
     */
    override suspend fun get(url: String): Response =
        client.get(urlString = url).toResponse()

    /**
     * Make POST request
     *
     * @param url Full request url: host, query, etc.
     * @param body Request body
     * @param bodyContentType Request body content type
     */
    override suspend fun post(url: String, body: ByteArray, bodyContentType: ContentType) =
        client.post(urlString = url) {
            setBody(ByteArrayContent(body, bodyContentType.toKtorContentType()))
        }.toResponse()

    /**
     * Upload file(s) to this URL
     *
     * @param uploadUrl URL where to upload files
     * @param items List of uploadable items (byte arrays or files)
     */
    override suspend fun postMultipart(uploadUrl: String, items: List<UploadableContent>): Response =
        callMultipart(uploadUrl, items).toResponse()

    private suspend fun callMultipart(uploadUrl: String, items: List<UploadableContent>): HttpResponse =
        client.post {
            url(uploadUrl)
            val parts = items.map { it.toFormPart() } // suspend

            setBody(MultiPartFormDataContent(formData { parts.forEach(::append) }))
        }

    private suspend fun UploadableContent.toFormPart(): FormPart<ByteArray> {
        @Suppress("EXPERIMENTAL_API_USAGE_FUTURE_ERROR")
        val headers = Headers.build {
            set(HttpHeaders.ContentDisposition, contentDisposition())
        }

        return when (this) {
            is UploadableContent.Bytes -> {
                FormPart(
                    key = fieldName,
                    value = bytes,
                    headers = headers
                )
            }

            is UploadableContent.File -> {
                FormPart(
                    key = fieldName,
                    value = file.readContent() ?: throw VkException("Can't read file contents"),
                    headers = headers
                )
            }

            is UploadableContent.Url -> {
                FormPart(
                    key = fieldName,
                    value = get(url).body ?: throw VkException("Can't read URL contents"),
                    headers = headers
                )
            }
        }
    }

    private fun ContentType.toKtorContentType(): io.ktor.http.ContentType =
        when (this) {
            ContentType.FormUrlEncoded -> io.ktor.http.ContentType.Application.FormUrlEncoded
        }

    /**
     * Instantiate desired client and apply basic configurations
     *
     * @param config Basic configurations
     * @return Desired HTTP client engine, e.g. CIO, etc.
     */
    open fun createEngineWithConfig(config: HttpClientConfig): HttpClientEngine? = null

    private suspend fun HttpResponse.toResponse(): Response =
        Response(status.value, readBytes())

    private companion object {
        private const val ERROR_MESSAGE_INITIALIZATION = "Please, provide ktor HttpClient to constructor, " +
                "or override createEngineWithConfig"
    }
}