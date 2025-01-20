package com.example.repository

import io.ktor.websocket.*

object ProfileWebSocketManager {
    private val connections = mutableSetOf<DefaultWebSocketSession>()

    /**
     * Add a WebSocket connection.
     */
    fun addConnection(session: DefaultWebSocketSession) {
        connections.add(session)
    }

    /**
     * Remove a WebSocket connection.
     */
    fun removeConnection(session: DefaultWebSocketSession) {
        connections.remove(session)
    }

    /**
     * Broadcast a message to all active WebSocket connections.
     */
    suspend fun broadcast(message: String) {
        connections.forEach { session ->
            session.send(Frame.Text(message))
        }
    }
}
