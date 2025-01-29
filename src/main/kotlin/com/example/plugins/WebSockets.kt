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

fun Application.configureWebSockets(
    chatRepository: ChatRepository,
    profileRepository: ProfileRepository,
    webSocketService: WebSocketService
) {
    val userStatusService = UserStatusService(profileRepository)

    routing {
        // 🔹 WebSocket Route for Chat
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
                                        chatRepository.getParticipants(chatId),
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

        // 🔹 WebSocket Route for General WebSocket Services
        webSocket("/ws") {
            val userId = call.parameters["userId"]
                ?: return@webSocket close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "User ID is required"))

            webSocketService.addConnection(userId, this)

            try {
                incoming.consumeEach { frame ->
                    if (frame is Frame.Text) {
                        val receivedText = frame.readText()
                        val parsedMessage = Json.parseToJsonElement(receivedText).jsonObject

                        when (parsedMessage["eventType"]?.jsonPrimitive?.content) {
                            "message" -> {
                                val chatId = parsedMessage["chatId"]?.jsonPrimitive?.content
                                val messageContent = parsedMessage["content"]?.jsonPrimitive?.content

                                if (chatId != null && messageContent != null) {
                                    webSocketService.broadcastMessage(
                                        chatRepository.getParticipants(chatId),
                                        messageContent,
                                        userId
                                    )
                                }
                            }
                            "disconnect" -> {
                                webSocketService.removeConnection(userId, this)
                                close(CloseReason(CloseReason.Codes.NORMAL, "User disconnected"))
                            }
                        }
                    }
                }
            } finally {
                webSocketService.removeConnection(userId, this)
            }
        }
    }
}
