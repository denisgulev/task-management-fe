package com.example

import com.example.plugins.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.logging.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main(args: Array<String>) {
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
}

fun Application.module() {
    val client = HttpClient(CIO) {
        install(Logging) {
            level = LogLevel.ALL
        }
    }
    configureContentNegotiation()
    configureRouting(client)
    configureTemplate()
    configureSessions()
}