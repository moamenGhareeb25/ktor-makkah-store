package com.example

import com.example.database.DatabaseFactory
import com.example.firebase.Firebase
import com.example.plugins.configureHTTP
import com.example.plugins.configureMonitoring
import com.example.plugins.configureSecurity
import com.example.plugins.configureSerialization
import com.example.repository.ProfileRepository
import com.example.routes.configureRouting
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    // Initialize Firebase
    Firebase.init()

    // Initialize Database
    DatabaseFactory.init()

    // Configure Plugins
    configureSerialization()
    configureMonitoring()
    configureSecurity()
    configureHTTP()

    // Configure Routing
    val profileRepository = ProfileRepository()
    configureRouting(profileRepository)

}