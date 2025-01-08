package com.example.auth

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseToken

object FirebaseAuth {
    private val firebaseAuth: FirebaseAuth

    init {
        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp()
        }
        firebaseAuth = FirebaseAuth.getInstance()
    }

    fun verifyIdToken(idToken: String): FirebaseToken? {
        return try {
            firebaseAuth.verifyIdToken(idToken)
        } catch (e: Exception) {
            null
        }
    }
}