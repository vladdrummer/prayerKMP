package com.vladdrummer.prayerkmp.feature.cloud

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CloudRepository(
    private val baseUrl: String = "https://vladdrummer.ru/prayer/api/v1",
) {
    private val client by lazy { HttpClient() }
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun load(email: String): CloudServerState {
        val body = client.get("$baseUrl/cloud") {
            parameter("email", email)
        }.bodyAsText()
        runCatching {
            return json.decodeFromString(CloudServerState.serializer(), body)
        }
        val error = runCatching {
            json.decodeFromString(ServerError.serializer(), body)
        }.getOrNull()
        if (error?.error?.isNotBlank() == true) {
            throw IllegalStateException(error.error)
        }
        throw IllegalStateException("Не удалось обработать ответ сервера")
    }

    suspend fun save(email: String, stateJson: String) {
        val body = client.put("$baseUrl/cloud") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(CloudSaveRequest(email = email, stateJson = stateJson)))
        }.bodyAsText()
        runCatching {
            json.decodeFromString(UpsertResponse.serializer(), body)
        }.getOrElse {
            val error = runCatching {
                json.decodeFromString(ServerError.serializer(), body)
            }.getOrNull()
            if (error?.error?.isNotBlank() == true) {
                throw IllegalStateException(error.error)
            }
            throw IllegalStateException("Не удалось обработать ответ сервера")
        }
    }
}

@Serializable
data class CloudServerState(
    @SerialName("email") val email: String,
    @SerialName("stateJson") val stateJson: String,
    @SerialName("updatedAt") val updatedAt: String,
)

@Serializable
private data class CloudSaveRequest(
    @SerialName("email") val email: String,
    @SerialName("stateJson") val stateJson: String,
)

@Serializable
private data class UpsertResponse(
    @SerialName("success") val success: Boolean,
)

@Serializable
private data class ServerError(
    @SerialName("error") val error: String,
)
