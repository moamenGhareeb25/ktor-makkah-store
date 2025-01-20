package com.example.repository

import io.ktor.websocket.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages WebSocket connections and handles real-time user status updates.
 */
class WebSocketManager(private val profileRepository: ProfileRepository) {

    private val connections = ConcurrentHashMap<String, MutableSet<DefaultWebSocketSession>>()
    private val mutex = Mutex()

    /**
     * Add a WebSocket connection for a specific user and mark them as online.
     */
    suspend fun addConnection(userId: String, session: DefaultWebSocketSession) {
        mutex.withLock {
            connections.computeIfAbsent(userId) { mutableSetOf() }.add(session)
            broadcastStatus(userId, true) // Mark user as online
        }
    }

    /**
     * Remove a WebSocket connection for a specific user.
     * If no active connections remain for the user, mark them as offline.
     */
    suspend fun removeConnection(userId: String, session: DefaultWebSocketSession) {
        mutex.withLock {
            connections[userId]?.remove(session)
            if (connections[userId]?.isEmpty() == true) {
                connections.remove(userId)
                broadcastStatus(userId, false) // Mark user as offline
                profileRepository.updateLastSeen(userId) // Update "last seen" in the database
            }
        }
    }

    /**
     * Broadcast a message to all participants of a chat, excluding the sender.
     */
    suspend fun broadcastMessageToChat(participants: List<String>, message: String, senderId: String? = null) {
        mutex.withLock {
            participants.forEach { participantId ->
                if (participantId != senderId) { // Exclude sender
                    connections[participantId]?.forEach { session ->
                        session.send(Frame.Text(message))
                    }
                }
            }
        }
    }

    /**
     * Broadcast a user's online/offline status to all connected clients.
     */
    private suspend fun broadcastStatus(userId: String, isOnline: Boolean) {
        val statusUpdate = Json.encodeToString(
            mapOf("userId" to userId, "isOnline" to isOnline, "lastSeen" to System.currentTimeMillis())
        )
        connections.forEach { (_, sessions) ->
            sessions.forEach { session ->
                session.send(Frame.Text(statusUpdate))
            }
        }
    }
}
