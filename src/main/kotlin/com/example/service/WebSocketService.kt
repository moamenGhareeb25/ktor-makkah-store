package com.example.service

import com.example.repository.ProfileRepository
import io.ktor.websocket.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

/**
 * WebSocket service to manage real-time user connections.
 */
class WebSocketService(private val profileRepository: ProfileRepository) {
    private val connections = ConcurrentHashMap<String, MutableSet<DefaultWebSocketSession>>()
    private val mutex = Mutex()

    /**
     * Adds a WebSocket connection and marks user online.
     */
    suspend fun addConnection(userId: String, session: DefaultWebSocketSession) {
        mutex.withLock {
            connections.computeIfAbsent(userId) { mutableSetOf() }.add(session)
            profileRepository.updateOnlineStatus(userId, true) // Set as online
            broadcastStatus(userId, true)
        }
    }

    /**
     * Removes a WebSocket connection and marks user offline if needed.
     */
    suspend fun removeConnection(userId: String, session: DefaultWebSocketSession) {
        mutex.withLock {
            connections[userId]?.remove(session)
            if (connections[userId]?.isEmpty() == true) {
                connections.remove(userId)
                profileRepository.updateOnlineStatus(userId, false) // Mark as offline
                profileRepository.updateLastSeen(userId)
                broadcastStatus(userId, false)
            }
        }
    }

    /**
     * Broadcasts user status to all active WebSocket sessions.
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
}
