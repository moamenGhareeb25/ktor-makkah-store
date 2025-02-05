package com.example

import com.example.auth.FirebaseNotificationService
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
import io.ktor.server.websocket.*
import java.util.*

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 10000 // Ensure it's the correct port
    embeddedServer(Netty, port = port) {
        module()
        printDecodedFirebaseConfig()
    }.start(wait = true)

}

fun printDecodedFirebaseConfig() {
    val firebaseBase64 = System.getenv("FIREBASE_CONFIG")

    if (firebaseBase64.isNullOrEmpty()) {
        println("❌ FIREBASE_CONFIG is not set in the environment.")
        return
    }

    try {
        val decodedJson = String(Base64.getDecoder().decode(firebaseBase64))
        println("✅ Decoded Firebase Config: \n$decodedJson")
    } catch (e: Exception) {
        println("❌ Error decoding FIREBASE_CONFIG: ${e.message}")
    }
}


fun Application.module() {
    println("🚀 Starting Ktor application...")
    printAllEnvironmentVariables()

    // ✅ Initialize Firebase (Check for errors)
    val firebaseConfig = try {
        Firebase.init()
    } catch (e: Exception) {
        println("❌ Firebase Initialization Failed: ${e.message}")
        return
    }

    println("✅ Firebase Config Loaded for Project: ${firebaseConfig.projectId}")


    // ✅ Initialize Database
    DatabaseFactory.init()

    // ✅ Install WebSockets (ONLY HERE)
    install(WebSockets)

    // ✅ Configure Plugins
    configureSerialization()
    configureMonitoring()
    configureSecurity()
    configureHTTP()

    // ✅ Initialize Firebase Notification Service (as Singleton)
    val firebaseNotificationService = FirebaseNotificationService()

    // ✅ Initialize Repositories
    val profileRepository = ProfileRepository()
    val chatRepository = ChatRepository(profileRepository, firebaseNotificationService)
    val taskRepository = TaskRepository()
    val kpiRepository = KPIRepository()
    val workLogRepository = WorkLogRepository()
    val delegationRepository = DelegationRepository()

    // ✅ Initialize Services
    val authorizationService = AuthorizationService(delegationRepository)
    val notificationService = NotificationService(profileRepository, firebaseNotificationService)
    val pendingUpdateService = PendingUpdateService(profileRepository)
    val profileService = ProfileService(profileRepository, pendingUpdateService, notificationService, authorizationService)
    val webSocketService = WebSocketService(profileRepository)

    // ✅ Configure Routes (Pass Dependencies Properly)
    configureRouting(
        chatRepository,
        profileRepository,
        taskRepository,
        kpiRepository,
        workLogRepository,
        delegationRepository,
        profileService,
        notificationService,
        authorizationService,
        firebaseNotificationService
    )

    // ✅ Configure WebSocket Routes (WITHOUT INSTALLING AGAIN)
    configureWebSockets(chatRepository, profileRepository, webSocketService)

    println("✅ Ktor application started successfully!")
}
fun printAllEnvironmentVariables() {
    println("🔍 Listing all environment variables:")
    System.getenv().forEach { (key, value) ->
        println("$key = $value")
    }
}