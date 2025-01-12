// Refactored and improved backend code for your chat application.

package com.example

import com.example.dataFactory.DatabaseFactory
import com.example.firebase.Firebase
import com.example.plugins.*
import com.example.repository.ChatRepository
import com.example.repository.ProfileRepository
import com.example.routes.configureRouting
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    embeddedServer(Netty, port = System.getenv("PORT")?.toInt() ?: 8080) {
        module()
    }.start(wait = true)
}

fun Application.module() {
    println("Starting Ktor application...")

    // Initialize Firebase
    Firebase.init()

    // Initialize Database
    DatabaseFactory.init()

    // Configure Plugins
    configureSerialization()
    configureMonitoring()
    configureSecurity()
    configureHTTP()

    // Configure Routes
    val profileRepository = ProfileRepository()
    val chatRepository = ChatRepository(profileRepository)
    configureRouting(chatRepository, profileRepository)

    configureSocketIO(chatRepository)


    println("Ktor application started successfully!")
}
