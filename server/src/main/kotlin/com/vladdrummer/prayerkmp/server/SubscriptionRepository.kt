package com.vladdrummer.prayerkmp.server

import kotlinx.serialization.Serializable

interface SubscriptionRepository {
    fun getStatus(email: String): SubscriptionStatusResponse
}

class JdbcSubscriptionRepository(
    private val databaseClient: DatabaseClient,
) : SubscriptionRepository {

    override fun getStatus(email: String): SubscriptionStatusResponse {
        databaseClient.connection().use { connection ->
            connection.prepareStatement(
                """
                SELECT expiration_at
                FROM subscription
                WHERE email = ?
                ORDER BY expiration_at DESC
                LIMIT 1
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, email)
                statement.executeQuery().use { resultSet ->
                    if (!resultSet.next()) {
                        return SubscriptionStatusResponse(
                            email = email,
                            hasActiveSubscription = false,
                            expirationAt = null
                        )
                    }
                    val expiration = resultSet.getTimestamp("expiration_at")
                    val hasActive = expiration?.time?.let { it > System.currentTimeMillis() } ?: false
                    return SubscriptionStatusResponse(
                        email = email,
                        hasActiveSubscription = hasActive,
                        expirationAt = expiration?.toString()
                    )
                }
            }
        }
    }
}

@Serializable
data class SubscriptionStatusResponse(
    val email: String,
    val hasActiveSubscription: Boolean,
    val expirationAt: String?,
)
