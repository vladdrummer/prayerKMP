package com.vladdrummer.prayerkmp.server

import kotlinx.serialization.Serializable

interface CloudRepository {
    fun getCloudState(email: String): CloudStateResponse?
    fun saveCloudState(email: String, stateJson: String)
}

class JdbcCloudRepository(
    private val databaseClient: DatabaseClient,
) : CloudRepository {

    override fun getCloudState(email: String): CloudStateResponse? {
        databaseClient.connection().use { connection ->
            connection.prepareStatement(
                """
                SELECT email, state_json, updated_at
                FROM cloud
                WHERE email = ?
                LIMIT 1
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, email)
                statement.executeQuery().use { resultSet ->
                    if (!resultSet.next()) return null
                    return CloudStateResponse(
                        email = resultSet.getString("email"),
                        stateJson = resultSet.getString("state_json"),
                        updatedAt = resultSet.getString("updated_at").orEmpty(),
                    )
                }
            }
        }
    }

    override fun saveCloudState(email: String, stateJson: String) {
        databaseClient.connection().use { connection ->
            connection.prepareStatement(
                """
                INSERT INTO cloud (email, state_json, updated_at)
                VALUES (?, ?, NOW())
                ON DUPLICATE KEY UPDATE
                  state_json = VALUES(state_json),
                  updated_at = NOW()
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, email)
                statement.setString(2, stateJson)
                statement.executeUpdate()
            }
        }
    }
}

@Serializable
data class CloudStateResponse(
    val email: String,
    val stateJson: String,
    val updatedAt: String,
)
