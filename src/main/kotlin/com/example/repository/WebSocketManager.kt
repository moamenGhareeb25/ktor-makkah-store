package com.example.repository

import io.ktor.websocket.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

/**
 * Singleton WebSocket manager handling connections and real-time events.
 */
object WebSocketManager {
    private val connections = ConcurrentHashMap<String, MutableSet<DefaultWebSocketSession>>()
    private val mutex = Mutex()

    /**
     * Adds a user WebSocket connection.
     */
    suspend fun addConnection(userId: String, session: DefaultWebSocketSession) {
        mutex.withLock {
            connections.computeIfAbsent(userId) { mutableSetOf() }.add(session)
        }
    }

    /**
     * Removes a WebSocket connection.
     * Marks user as offline if no more active connections exist.
     */
    suspend fun removeConnection(userId: String, session: DefaultWebSocketSession, updateStatus: suspend (String, Boolean) -> Unit) {
        mutex.withLock {
            connections[userId]?.remove(session)
            if (connections[userId]?.isEmpty() == true) {
                connections.remove(userId)
                updateStatus(userId, false)  // Mark as offline
            }
        }
    }

    /**
     * Broadcasts a message to all participants except the sender.
     */
    suspend fun broadcastMessage(participants: List<String>, message: String, senderId: String? = null) {
        mutex.withLock {
            participants.forEach { participantId ->
                if (participantId != senderId) {
                    connections[participantId]?.forEach { session ->
                        session.send(Frame.Text(message))
                    }
                }
            }
        }
    }

    /**
     * Sends online/offline status updates to all connected clients.
     */
    suspend fun broadcastStatus(userId: String, isOnline: Boolean) {
        val statusUpdate = Json.encodeToString(mapOf("userId" to userId, "isOnline" to isOnline))
        connections.forEach { (_, sessions) ->
            sessions.forEach { session ->
                session.send(Frame.Text(statusUpdate))
            }
        }
    }
}
