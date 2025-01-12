package com.example.firebase

import com.google.firebase.cloud.StorageClient
import java.io.File
import java.util.*

object FirebaseStorageService {

    fun uploadFile(file: File, contentType: String): String? {
        return try {
            val bucket = StorageClient.getInstance().bucket()
            val uniqueFileName = UUID.randomUUID().toString() + "-" + file.name
            val blob = bucket.create(uniqueFileName, file.readBytes(), contentType)
            blob.mediaLink // Return the public URL of the uploaded file
        } catch (e: Exception) {
            e.printStackTrace()
            null // Return null if the upload fails
        }
    }

    fun deleteFile(fileName: String) {
        try {
            val bucket = StorageClient.getInstance().bucket()
            val blob = bucket.get(fileName)
            blob?.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
