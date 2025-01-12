package com.example.plugins

import com.example.firebase.FirebaseStorageService
import com.example.model.Message
import com.example.model.TypingIndicator
import com.example.repository.ChatRepository
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.concurrent.ConcurrentHashMap

val activeConnections = ConcurrentHashMap<String, MutableSet<DefaultWebSocketSession>>()

fun Application.configureSocketIO(chatRepository: ChatRepository) {
    install(WebSockets)

    routing {
        webSocket("/chat") {
            val userId = call.parameters["userId"]
            if (userId == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "User ID is required"))
                return@webSocket
            }

            activeConnections.computeIfAbsent(userId) { mutableSetOf() }.add(this)

            try {
                incoming.consumeEach { frame ->
                    if (frame is Frame.Text) {
                        val receivedText = frame.readText()
                        val parsedMessage = Json.parseToJsonElement(receivedText).jsonObject

                        val eventType = parsedMessage["eventType"]?.jsonPrimitive?.content
                        val chatId = parsedMessage["chatId"]?.jsonPrimitive?.content
                        val contentType = parsedMessage["contentType"]?.jsonPrimitive?.content
                        val content = parsedMessage["content"]?.jsonPrimitive?.content

                        when (eventType) {
                            "message" -> {
                                if (chatId != null && contentType != null && content != null) {
                                    val message = Message(
                                        senderId = userId,
                                        contentType = contentType,
                                        content = handleContent(contentType, content)
                                    )
                                    broadcastMessage(chatId, message, userId)
                                    chatRepository.storeMessage(chatId, userId, message)
                                    chatRepository.notifyParticipants(chatId, message, userId)
                                }
                            }
                            "typing" -> {
                                if (chatId != null) {
                                    val isTyping = parsedMessage["isTyping"]?.jsonPrimitive?.boolean ?: false
                                    broadcastTypingIndicator(chatId, userId, isTyping)
                                }
                            }
                        }
                    }
                }
            } finally {
                activeConnections[userId]?.remove(this)
                if (activeConnections[userId]?.isEmpty() == true) {
                    activeConnections.remove(userId)
                }
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