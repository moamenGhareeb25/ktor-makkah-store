package com.example.plugins

import com.example.repository.ChatRepository
import com.example.repository.ProfileRepository
import com.example.repository.WebSocketManager
import com.example.service.UserStatusService
import com.example.service.WebSocketService
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

fun Application.configureWebSockets(chatRepository: ChatRepository, profileRepository: ProfileRepository,webSocketService: WebSocketService) {
    val userStatusService = UserStatusService(profileRepository)

    install(WebSockets)

    routing {
        webSocket("/chat") {
            val userId = call.parameters["userId"]
                ?: return@webSocket close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "User ID is required"))

            WebSocketManager.addConnection(userId, this)
            userStatusService.updateUserStatus(userId, true)

            try {
                incoming.consumeEach { frame ->
                    if (frame is Frame.Text) {
                        val receivedText = frame.readText()
                        val parsedMessage = Json.parseToJsonElement(receivedText).jsonObject

                        val eventType = parsedMessage["eventType"]?.jsonPrimitive?.content
                        val chatId = parsedMessage["chatId"]?.jsonPrimitive?.content

                        when (eventType) {
                            "message" -> {
                                val messageContent = parsedMessage["content"]?.jsonPrimitive?.content ?: return@consumeEach
                                if (chatId != null) {
                                    WebSocketManager.broadcastMessage(
                                        chatRepository.getChatParticipants(chatId),
                                        messageContent,
                                        userId
                                    )
                                }
                            }
                            "disconnect" -> {
                                WebSocketManager.removeConnection(userId, this) { id, status ->
                                    userStatusService.updateUserStatus(id, status)
                                }
                                close(CloseReason(CloseReason.Codes.NORMAL, "User disconnected"))
                            }
                        }
                    }
                }
            } finally {
                WebSocketManager.removeConnection(userId, this) { id, status ->
                    userStatusService.updateUserStatus(id, status)
                }
            }
        }
        webSocket("/ws") {
            val userId = call.parameters["userId"]
            if (userId == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "User ID is required"))
                return@webSocket
            }

            // Add user to WebSocket connections
            webSocketService.addConnection(userId, this)

            try {
                incoming.consumeEach { frame ->
                    if (frame is Frame.Text) {
                        val message = frame.readText()
                        println("Received message: $message")
                    }
                }
            } finally {
                // Remove user from WebSocket connections
                webSocketService.removeConnection(userId, this)
            }
        }
    }
}
