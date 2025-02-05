package com.example.firebase

import com.google.firebase.cloud.StorageClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

object FirebaseStorageService {

    /**
     * Asynchronously uploads a file to Firebase Storage.
     * @return The public download URL of the file, or null if an error occurs.
     */
    suspend fun uploadFile(file: File, contentType: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                Firebase.init()
                val firebaseConfig = Firebase.init()
                val bucket = StorageClient.getInstance().bucket(firebaseConfig.storage_bucket)
                val uniqueFileName = "${UUID.randomUUID()}-${file.name}"
                val blob = bucket.create(uniqueFileName, file.readBytes(), contentType)
                println("✅ File uploaded successfully: ${blob.mediaLink}")
                blob.mediaLink
            } catch (e: Exception) {
                println("❌ Error uploading file: ${e.message}")
                null
            }
        }
    }
}
