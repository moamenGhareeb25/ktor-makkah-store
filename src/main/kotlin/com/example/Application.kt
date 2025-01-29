package com.example

import com.example.dataFactory.DatabaseFactory
import com.example.firebase.Firebase
import com.example.plugins.*
import com.example.repository.*
import com.example.routes.configureRouting
import com.example.service.AuthorizationService
import com.example.service.PendingUpdateService
import com.example.service.ProfileService
import com.example.service.WebSocketService
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    embeddedServer(Netty, port = System.getenv("PORT")?.toInt() ?: 8080) {
        module()
    }.start(wait = true)
}

fun Application.module() {
    println("🚀 Starting Ktor application...")

    // ✅ Initialize Firebase
    Firebase.init()

    // ✅ Initialize Database
    DatabaseFactory.init()

// ✅ Install WebSockets Plugin
    install(io.ktor.server.websocket.WebSockets)

    // ✅ Configure Plugins (Security, Serialization, Monitoring, HTTP)
    configureSerialization()
    configureMonitoring()
    configureSecurity()
    configureHTTP()

    // ✅ Initialize Repositories
    val profileRepository = ProfileRepository()
    val chatRepository = ChatRepository(profileRepository)
    val taskRepository = TaskRepository()
    val kpiRepository = KPIRepository()
    val workLogRepository = WorkLogRepository()
    val delegationRepository = DelegationRepository()

    // ✅ Initialize Services
    val authorizationService = AuthorizationService(delegationRepository)
    val notificationService = NotificationService(profileRepository)
    val pendingUpdateService = PendingUpdateService(profileRepository)
    val profileService = ProfileService(profileRepository, pendingUpdateService, notificationService, authorizationService)

    // ✅ Configure Routes
    configureRouting(
        chatRepository,
        profileRepository,
        taskRepository,
        kpiRepository,
        workLogRepository,
        delegationRepository,
        profileService,
        notificationService,
        authorizationService
    )

    // ✅ Initialize Services
    val webSocketService = WebSocketService(profileRepository)

    // ✅ Configure WebSocket Routes
    configureWebSockets(chatRepository, profileRepository,webSocketService)

    println("✅ Ktor application started successfully!")
}
