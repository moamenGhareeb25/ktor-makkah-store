package com.example.auth

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseToken

object FirebaseAuthManager { // Renamed to avoid confusion with FirebaseAuth SDK class
    private val firebaseAuth: FirebaseAuth

    init {
        // Initialize Firebase if not already initialized
        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp()
        }
        firebaseAuth = FirebaseAuth.getInstance()
    }

    /**
     * Verifies the ID token and retrieves the FirebaseToken.
     * @param idToken The ID token to verify.
     * @return The verified FirebaseToken, or null if verification fails.
     */
    fun verifyIdToken(idToken: String): FirebaseToken? {
        return try {
            firebaseAuth.verifyIdToken(idToken)
        } catch (e: Exception) {
            println("Error verifying ID token: ${e.message}")
            null
        }
    }

    /**
     * Checks if a user exists in Firebase by their user ID.
     * @param userId The user ID to check.
     * @return True if the user exists in Firebase, false otherwise.
     */
    fun checkUserInFirebase(userId: String): Boolean {
        return try {
            val userRecord = firebaseAuth.getUser(userId)
            userRecord != null // User exists
        } catch (e: Exception) {
            println("Error checking user in Firebase: ${e.message}")
            false // User does not exist or an error occurred
        }
    }
}
