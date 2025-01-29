package com.example.service

import com.example.repository.ProfileRepository
import io.ktor.websocket.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

/**
 * Handles WebSocket connections and manages real-time user statuses.
 */
class WebSocketService(private val profileRepository: ProfileRepository) {
    private val connections = ConcurrentHashMap<String, MutableSet<DefaultWebSocketSession>>()
    private val mutex = Mutex()

    /**
     * Adds a WebSocket connection and marks user as online.
     */
    suspend fun addConnection(userId: String, session: DefaultWebSocketSession) {
        mutex.withLock {
            connections.computeIfAbsent(userId) { mutableSetOf() }.add(session)
            profileRepository.updateOnlineStatus(userId, true)
            broadcastStatus(userId, true)
        }
    }

    /**
     * Removes a WebSocket connection and updates user status.
     */
    suspend fun removeConnection(userId: String, session: DefaultWebSocketSession) {
        mutex.withLock {
            connections[userId]?.remove(session)
            if (connections[userId]?.isEmpty() == true) {
                connections.remove(userId)
                profileRepository.updateOnlineStatus(userId, false)
                profileRepository.updateLastSeen(userId)
                broadcastStatus(userId, false)
            }
        }
    }

    /**
     * Broadcasts user online/offline status updates.
     */
    private suspend fun broadcastStatus(userId: String, isOnline: Boolean) {
        val statusMessage = Json.encodeToString(
            mapOf("userId" to userId, "isOnline" to isOnline, "lastSeen" to System.currentTimeMillis())
        )
        connections.forEach { (_, sessions) ->
            sessions.forEach { session ->
                session.send(Frame.Text(statusMessage))
            }
        }
    }

    /**
     * Broadcasts a message to all chat participants except the sender.
     */
    suspend fun broadcastMessage(participants: List<String>, message: String, senderId: String) {
        mutex.withLock {
            participants.filter { it != senderId }.forEach { participantId ->
                connections[participantId]?.forEach { session ->
                    session.send(Frame.Text(message))
                }
            }
        }
    }
}
