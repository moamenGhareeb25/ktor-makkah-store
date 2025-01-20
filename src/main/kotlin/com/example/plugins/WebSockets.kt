package com.example.plugins

import com.example.firebase.FirebaseStorageService
import com.example.model.Message
import com.example.model.TypingIndicator
import com.example.repository.ChatRepository
import com.example.repository.ProfileRepository
import com.example.repository.WebSocketManager
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.concurrent.ConcurrentHashMap

val activeConnections = ConcurrentHashMap<String, MutableSet<DefaultWebSocketSession>>()


fun Application.configureSocketIO(chatRepository: ChatRepository, profileRepository: ProfileRepository) {
    val webSocketManager = WebSocketManager(profileRepository)

    install(WebSockets)

    routing {
        webSocket("/chat") {
            val userId = call.parameters["userId"]
            if (userId == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "User ID is required"))
                return@webSocket
            }

            // Add the user to WebSocketManager
            webSocketManager.addConnection(userId, this)

            try {
                incoming.consumeEach { frame ->
                    if (frame is Frame.Text) {
                        val receivedText = frame.readText()
                        val parsedMessage = Json.parseToJsonElement(receivedText).jsonObject

                        val eventType = parsedMessage["eventType"]?.jsonPrimitive?.content
                        val chatId = parsedMessage["chatId"]?.jsonPrimitive?.content

                        when (eventType) {
                            "message" -> {
                                if (chatId != null) {
                                    val message = parsedMessage["content"]?.jsonPrimitive?.content ?: return@consumeEach
                                    webSocketManager.broadcastMessageToChat(
                                        participants = chatRepository.getChatParticipants(chatId),
                                        message = message,
                                        senderId = userId
                                    )
                                }
                            }
                        }
                    }
                }
            } finally {
                // Remove the user from WebSocketManager on disconnect
                webSocketManager.removeConnection(userId, this)
            }
        }
    }
}

        fun handleContent(contentType: String, content: String): String {
    return if (contentType == "file") {
        val file = java.io.File(content)
        FirebaseStorageService.uploadFile(file, "application/octet-stream") ?: throw Exception("Failed to upload file")
    } else {
        content
    }
}

suspend fun broadcastMessage(chatId: String, message: Message, senderId: String) {
    activeConnections.forEach { (userId, sessions) ->
        if (userId != senderId) {
            sessions.forEach { session ->
                session.sendSerializedMessage(message)
            }
        }
    }
}

suspend fun DefaultWebSocketSession.sendSerializedMessage(message: Message) {
    send(Frame.Text(Json.encodeToString(Message.serializer(), message)))
}

suspend fun broadcastTypingIndicator(chatId: String, userId: String, isTyping: Boolean) {
    val typingIndicator = TypingIndicator(
        chatId = chatId,
        userId = userId,
        isTyping = isTyping
    )
    activeConnections.forEach { (_, sessions) ->
        sessions.forEach { session ->
            session.send(Frame.Text(Json.encodeToString(TypingIndicator.serializer(), typingIndicator)))
        }
    }
}