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
                val bucket = StorageClient.getInstance().bucket()
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

    /**
     * Asynchronously deletes a file from Firebase Storage.
     */
    suspend fun deleteFile(fileName: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val bucket = StorageClient.getInstance().bucket()
                val blob = bucket.get(fileName)
                blob?.delete() ?: return@withContext false
                println("✅ File deleted successfully: $fileName")
                true
            } catch (e: Exception) {
                println("❌ Error deleting file: ${e.message}")
                false
            }
        }
    }
}
