package com.example.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import java.io.FileInputStream

object Firebase {
    fun init() {
        val serviceAccount = FileInputStream("C:\\Users\\moame\\Downloads\\makkah-store-operations-firebase-adminsdk-n1ol9-921f8322d9.json")
        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
            .build()
        FirebaseApp.initializeApp(options)
    }
}