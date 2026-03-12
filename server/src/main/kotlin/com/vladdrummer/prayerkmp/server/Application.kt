package com.vladdrummer.prayerkmp.server

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.request.receive
import kotlinx.serialization.Serializable

fun main() {
    val port = (System.getenv("PORT") ?: "8081").toIntOrNull() ?: 8081
    embeddedServer(Netty, port = port, module = Application::module).start(wait = true)
}

fun Application.module() {
    val database = DatabaseClient.fromEnv()
    val cloudRepository = JdbcCloudRepository(database)
    val subscriptionRepository = JdbcSubscriptionRepository(database)
    val messageBoardDb = DatabaseClient.fromEnv(
        urlEnvKey = "ORTHO_DB_URL",
        userEnvKey = "ORTHO_DB_USER",
        passwordEnvKey = "ORTHO_DB_PASSWORD",
        defaultUrl = "jdbc:mysql://127.0.0.1:3306/orthodoxpray?useUnicode=true&characterEncoding=utf8",
        defaultUser = "orthopray",
        defaultPassword = "ortho5011"
    )
    val messageBoardRepository = MessageBoardRepository(messageBoardDb)

    install(ContentNegotiation) {
        json()
    }

    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(error = cause.message ?: "Некорректный запрос")
            )
        }
        exception<Throwable> { call, cause ->
            this@module.environment.log.error("Unhandled server error", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(error = "Внутренняя ошибка сервера")
            )
        }
    }

    routing {
        get("/") {
            call.respondText(
                text = "PrayerKMP server is running",
                contentType = ContentType.Text.Plain,
                status = HttpStatusCode.OK
            )
        }
        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }
        route("/api/v1") {
            get("/subscription/status") {
                val email = call.parameters["email"].orEmpty().trim()
                require(email.isNotBlank()) { "Параметр email обязателен" }
                call.respond(subscriptionRepository.getStatus(email))
            }
            get("/cloud") {
                val email = call.parameters["email"].orEmpty().trim()
                require(email.isNotBlank()) { "Параметр email обязателен" }
                val cloudState = cloudRepository.getCloudState(email)
                if (cloudState == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Данные не найдены"))
                } else {
                    call.respond(cloudState)
                }
            }
            put("/cloud") {
                val payload = call.receive<CloudSaveRequest>()
                require(payload.email.isNotBlank()) { "email не может быть пустым" }
                require(payload.stateJson.isNotBlank()) { "stateJson не может быть пустым" }
                cloudRepository.saveCloudState(payload.email.trim(), payload.stateJson)
                call.respond(UpsertResponse(success = true))
            }
            route("/messageboard") {
                get("/show") {
                    call.respond(messageBoardRepository.showData())
                }
                get("/vote") {
                    val id = call.parameters["id"].orEmpty().trim()
                    val gmail = call.parameters["gmail"].orEmpty().trim()
                    require(id.isNotBlank()) { "Параметр id обязателен" }
                    require(gmail.isNotBlank()) { "Параметр gmail обязателен" }
                    call.respond(
                        messageBoardRepository.performVote(
                            id = id.toIntOrNull() ?: throw IllegalArgumentException("Некорректный id"),
                            gmail = gmail
                        )
                    )
                }
                get("/post") {
                    val data = call.parameters["data"].orEmpty()
                    val gmail = call.parameters["gmail"].orEmpty().trim()
                    val name = call.parameters["name"].orEmpty().trim()
                    val senderType = call.parameters["sender_type"].orEmpty().trim()
                    require(data.isNotBlank()) { "Параметр data обязателен" }
                    require(gmail.isNotBlank()) { "Параметр gmail обязателен" }
                    require(name.isNotBlank()) { "Параметр name обязателен" }
                    require(senderType.isNotBlank()) { "Параметр sender_type обязателен" }
                    call.respond(
                        messageBoardRepository.postData(
                            messageText = data,
                            gmail = gmail,
                            name = name,
                            senderType = senderType.toIntOrNull() ?: throw IllegalArgumentException("Некорректный sender_type")
                        )
                    )
                }
                get("/edit") {
                    val id = call.parameters["id"].orEmpty().trim()
                    val data = call.parameters["data"].orEmpty()
                    val gmail = call.parameters["gmail"].orEmpty().trim()
                    val name = call.parameters["name"].orEmpty().trim()
                    require(id.isNotBlank()) { "Параметр id обязателен" }
                    require(data.isNotBlank()) { "Параметр data обязателен" }
                    require(gmail.isNotBlank()) { "Параметр gmail обязателен" }
                    require(name.isNotBlank()) { "Параметр name обязателен" }
                    call.respond(
                        messageBoardRepository.editData(
                            id = id.toIntOrNull() ?: throw IllegalArgumentException("Некорректный id"),
                            messageText = data,
                            gmail = gmail,
                            name = name
                        )
                    )
                }
                get("/remove") {
                    val id = call.parameters["id"].orEmpty().trim()
                    val gmail = call.parameters["gmail"].orEmpty().trim()
                    require(id.isNotBlank()) { "Параметр id обязателен" }
                    require(gmail.isNotBlank()) { "Параметр gmail обязателен" }
                    call.respond(
                        messageBoardRepository.removeData(
                            id = id.toIntOrNull() ?: throw IllegalArgumentException("Некорректный id"),
                            gmail = gmail
                        )
                    )
                }
            }
        }
    }
}

@Serializable
data class CloudSaveRequest(
    val email: String,
    val stateJson: String,
)

@Serializable
data class UpsertResponse(
    val success: Boolean,
)

@Serializable
data class ErrorResponse(
    val error: String,
)
