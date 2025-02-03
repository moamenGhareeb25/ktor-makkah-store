package com.example.auth

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FirebaseAuthManager {
    private val firebaseAuth: FirebaseAuth

    init {
        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp()
        }
        firebaseAuth = FirebaseAuth.getInstance()
    }

    /**
     * Asynchronously verifies an ID token and retrieves the FirebaseToken.
     * @param idToken The ID token to verify.
     * @return The verified FirebaseToken, or null if verification fails.
     */
    suspend fun verifyIdToken(idToken: String): FirebaseToken? {
        return withContext(Dispatchers.IO) {
            try {
                firebaseAuth.verifyIdToken(idToken)
            } catch (e: Exception) {
                println("❌ Error verifying ID token: ${e.message}")
                null
            }
        }
    }

    /**
     * Checks asynchronously if a user exists in Firebase.
     * @param userId The user ID to check.
     * @return True if the user exists, false otherwise.
     */
    suspend fun checkUserInFirebase(userId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                firebaseAuth.getUser(userId) != null
            } catch (e: Exception) {
                println("❌ Error checking user in Firebase: ${e.message}")
                false
            }
        }
    }
}
