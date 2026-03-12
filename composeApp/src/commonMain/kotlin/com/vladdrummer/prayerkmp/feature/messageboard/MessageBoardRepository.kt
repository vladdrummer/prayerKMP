package com.vladdrummer.prayerkmp.feature.messageboard

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class MessageBoardRepository(
    private val baseUrl: String = "https://vladdrummer.ru/prayer/api/v1/messageboard",
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val client by lazy { HttpClient() }

    suspend fun showData(): MessageBoardResponse {
        return request(path = "show")
    }

    suspend fun performVote(id: Int, gmail: String): MessageBoardResponse {
        return request(path = "vote") {
            parameter("id", id)
            parameter("gmail", gmail)
        }
    }

    suspend fun postData(
        text: String,
        gmail: String,
        name: String,
        senderType: Int,
    ): MessageBoardResponse {
        return request(path = "post") {
            parameter("data", text)
            parameter("gmail", gmail)
            parameter("name", name)
            parameter("sender_type", senderType)
        }
    }

    suspend fun editData(
        id: Int,
        text: String,
        gmail: String,
        name: String,
    ): MessageBoardResponse {
        return request(path = "edit") {
            parameter("id", id)
            parameter("data", text)
            parameter("gmail", gmail)
            parameter("name", name)
        }
    }

    suspend fun removeData(
        id: Int,
        gmail: String,
    ): MessageBoardResponse {
        return request(path = "remove") {
            parameter("id", id)
            parameter("gmail", gmail)
        }
    }

    private suspend fun request(
        path: String,
        params: io.ktor.client.request.HttpRequestBuilder.() -> Unit = {}
    ): MessageBoardResponse {
        val body = client.get("$baseUrl/$path", params).bodyAsText()
        runCatching {
            return json.decodeFromString(MessageBoardResponse.serializer(), body)
        }
        val apiError = runCatching {
            json.decodeFromString(MessageBoardApiError.serializer(), body)
        }.getOrNull()
        if (apiError?.error?.isNotBlank() == true) {
            throw IllegalStateException(apiError.error)
        }
        throw IllegalStateException("Не удалось обработать ответ сервера")
    }
}

@Serializable
data class MessageBoardResponse(
    @SerialName("messages") val messages: List<MessageBoardMessageDto> = emptyList(),
)

@Serializable
data class MessageBoardMessageDto(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("message") val message: String,
    @SerialName("date") val date: String,
    @SerialName("votes") val votes: Int,
    @SerialName("sender_type") val senderType: Int,
    @SerialName("gmail") val gmail: String = "",
)

@Serializable
private data class MessageBoardApiError(
    @SerialName("error") val error: String,
)
