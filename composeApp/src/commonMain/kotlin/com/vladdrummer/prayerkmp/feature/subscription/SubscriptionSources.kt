package com.vladdrummer.prayerkmp.feature.subscription

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val SUBSCRIPTION_STATUS_URL = "https://vladdrummer.ru/prayer/api/v1/subscription/status"

class BackendSubscriptionSource(
    private val url: String = SUBSCRIPTION_STATUS_URL,
) : SubscriptionSourceChecker {

    override val sourceType: SubscriptionSourceType = SubscriptionSourceType.Backend

    private val json = Json { ignoreUnknownKeys = true }
    private val httpClient by lazy {
        HttpClient {
            install(ContentNegotiation) {
                json(json)
            }
        }
    }

    override suspend fun check(email: String): SubscriptionSourceResult {
        val response = runCatching {
            httpClient.get(url) {
                parameter("email", email)
            }.body<BackendSubscriptionResponse>()
        }.getOrElse {
            return SubscriptionSourceResult.Unavailable(
                source = sourceType,
                reason = it.message
            )
        }

        return if (response.hasActiveSubscription) {
            SubscriptionSourceResult.Active(
                source = sourceType,
                expirationAt = response.expirationAt
            )
        } else {
            SubscriptionSourceResult.Inactive(
                source = sourceType,
                expirationAt = response.expirationAt
            )
        }
    }
}

class GooglePlaySubscriptionSourceStub : SubscriptionSourceChecker {
    override val sourceType: SubscriptionSourceType = SubscriptionSourceType.GooglePlay

    override suspend fun check(email: String): SubscriptionSourceResult {
        return SubscriptionSourceResult.Unavailable(
            source = sourceType,
            reason = "Google Play source is not connected yet"
        )
    }
}

class AppStoreSubscriptionSourceStub : SubscriptionSourceChecker {
    override val sourceType: SubscriptionSourceType = SubscriptionSourceType.AppStore

    override suspend fun check(email: String): SubscriptionSourceResult {
        return SubscriptionSourceResult.Unavailable(
            source = sourceType,
            reason = "App Store source is not connected yet"
        )
    }
}

@Serializable
private data class BackendSubscriptionResponse(
    @SerialName("email") val email: String,
    @SerialName("hasActiveSubscription") val hasActiveSubscription: Boolean,
    @SerialName("expirationAt") val expirationAt: String? = null,
)
