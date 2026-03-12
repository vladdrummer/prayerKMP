package com.vladdrummer.prayerkmp.server

import java.sql.Connection
import java.sql.DriverManager

class DatabaseClient private constructor(
    private val url: String,
    private val user: String,
    private val password: String,
) {
    fun connection(): Connection = DriverManager.getConnection(url, user, password)

    companion object {
        fun fromEnv(): DatabaseClient {
            val url = System.getenv("DB_URL")
                ?: "jdbc:mysql://127.0.0.1:3306/prayer?useUnicode=true&characterEncoding=utf8"
            val user = System.getenv("DB_USER") ?: "orthopray"
            val password = System.getenv("DB_PASSWORD") ?: "ortho5011"
            return DatabaseClient(url = url, user = user, password = password)
        }

        fun fromEnv(
            urlEnvKey: String,
            userEnvKey: String,
            passwordEnvKey: String,
            defaultUrl: String,
            defaultUser: String,
            defaultPassword: String,
        ): DatabaseClient {
            val url = System.getenv(urlEnvKey) ?: defaultUrl
            val user = System.getenv(userEnvKey) ?: defaultUser
            val password = System.getenv(passwordEnvKey) ?: defaultPassword
            return DatabaseClient(url = url, user = user, password = password)
        }
    }
}
