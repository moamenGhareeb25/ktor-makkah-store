package com.example.routes

import com.example.auth.FirebaseAuthManager
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*


/**
 * Validates the Authorization header and extracts the user ID from the Firebase token.
 * @return The user ID if the token is valid, or `null` if validation fails.
 */
suspend fun ApplicationCall.validateAndExtractUserId(): String? {
    val idToken = request.headers["Authorization"]?.removePrefix("Bearer ")
    if (idToken == null) {
        respond(HttpStatusCode.Unauthorized, "Missing token")
        return null
    }
    val decodedToken = FirebaseAuthManager.verifyIdToken(idToken)
    if (decodedToken == null) {
        respond(HttpStatusCode.Unauthorized, "Invalid token")
        return null
    }
    return decodedToken.uid
}