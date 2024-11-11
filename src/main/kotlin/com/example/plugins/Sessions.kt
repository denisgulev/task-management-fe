package com.example.plugins

import com.example.models.UserSession
import io.ktor.server.application.*
import io.ktor.server.sessions.*

fun Application.configureSessions() {
    install(Sessions) {
        cookie<UserSession>("user_session")// Store JWT in a session cookie
    }
}