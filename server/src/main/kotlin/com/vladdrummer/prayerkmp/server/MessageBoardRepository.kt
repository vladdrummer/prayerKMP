package com.vladdrummer.prayerkmp.server

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.sql.SQLException

class MessageBoardRepository(
    private val databaseClient: DatabaseClient,
) {

    init {
        ensureVotesTable()
    }

    fun showData(): MessageBoardResponse {
        databaseClient.connection().use { connection ->
            connection.prepareStatement(
                """
                SELECT id, message, date, name, gmail, votes, sender_type
                FROM webboard
                ORDER BY date DESC
                LIMIT 30
                """.trimIndent()
            ).use { statement ->
                statement.executeQuery().use { resultSet ->
                    val messages = mutableListOf<MessageBoardMessage>()
                    while (resultSet.next()) {
                        messages += MessageBoardMessage(
                            id = resultSet.getInt("id"),
                            message = resultSet.getString("message").orEmpty(),
                            date = resultSet.getString("date").orEmpty(),
                            name = resultSet.getString("name").orEmpty(),
                            gmail = resultSet.getString("gmail").orEmpty(),
                            votes = resultSet.getInt("votes"),
                            senderType = resultSet.getInt("sender_type"),
                        )
                    }
                    return MessageBoardResponse(messages = messages)
                }
            }
        }
    }

    fun postData(
        messageText: String,
        gmail: String,
        name: String,
        senderType: Int,
    ): MessageBoardResponse {
        databaseClient.connection().use { connection ->
            connection.prepareStatement(
                """
                INSERT INTO webboard (name, message, sender_type, gmail)
                VALUES (?, ?, ?, ?)
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, name)
                statement.setString(2, messageText)
                statement.setInt(3, senderType)
                statement.setString(4, gmail)
                statement.executeUpdate()
            }
        }
        return showData()
    }

    fun editData(
        id: Int,
        messageText: String,
        gmail: String,
        name: String,
    ): MessageBoardResponse {
        databaseClient.connection().use { connection ->
            connection.prepareStatement(
                """
                UPDATE webboard
                SET name = ?, message = ?
                WHERE id = ? AND gmail = ?
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, name)
                statement.setString(2, messageText)
                statement.setInt(3, id)
                statement.setString(4, gmail)
                val affected = statement.executeUpdate()
                if (affected == 0) {
                    throw IllegalStateException("Нельзя редактировать чужую просьбу или запись не найдена")
                }
            }
        }
        return showData()
    }

    fun removeData(
        id: Int,
        gmail: String,
    ): MessageBoardResponse {
        databaseClient.connection().use { connection ->
            connection.prepareStatement(
                """
                DELETE FROM webboard
                WHERE id = ? AND gmail = ?
                """.trimIndent()
            ).use { statement ->
                statement.setInt(1, id)
                statement.setString(2, gmail)
                val affected = statement.executeUpdate()
                if (affected == 0) {
                    throw IllegalStateException("Нельзя удалить чужую просьбу или запись не найдена")
                }
            }
        }
        return showData()
    }

    fun performVote(
        id: Int,
        gmail: String,
    ): MessageBoardResponse {
        databaseClient.connection().use { connection ->
            connection.autoCommit = false
            try {
                connection.prepareStatement(
                    """
                    INSERT INTO webboard_votes (message_id, gmail)
                    VALUES (?, ?)
                    """.trimIndent()
                ).use { statement ->
                    statement.setInt(1, id)
                    statement.setString(2, gmail)
                    statement.executeUpdate()
                }

                connection.prepareStatement(
                    """
                    UPDATE webboard
                    SET votes = votes + 1
                    WHERE id = ?
                    """.trimIndent()
                ).use { statement ->
                    statement.setInt(1, id)
                    val affected = statement.executeUpdate()
                    if (affected == 0) {
                        throw IllegalStateException("Просьба не найдена")
                    }
                }
                connection.commit()
            } catch (e: SQLException) {
                connection.rollback()
                if (e.errorCode == 1062) {
                    throw IllegalStateException("Вы уже голосовали за эту просьбу")
                }
                throw e
            } catch (e: Throwable) {
                connection.rollback()
                throw e
            } finally {
                connection.autoCommit = true
            }
        }
        return showData()
    }

    private fun ensureVotesTable() {
        databaseClient.connection().use { connection ->
            connection.prepareStatement(
                """
                CREATE TABLE IF NOT EXISTS webboard_votes (
                    message_id INT NOT NULL,
                    gmail VARCHAR(191) NOT NULL,
                    voted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (message_id, gmail)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8
                """.trimIndent()
            ).use { statement ->
                statement.executeUpdate()
            }
        }
    }
}

@Serializable
data class MessageBoardResponse(
    val messages: List<MessageBoardMessage>,
)

@Serializable
data class MessageBoardMessage(
    val id: Int,
    val message: String,
    val date: String,
    val name: String,
    val gmail: String,
    val votes: Int,
    @SerialName("sender_type")
    val senderType: Int,
)
