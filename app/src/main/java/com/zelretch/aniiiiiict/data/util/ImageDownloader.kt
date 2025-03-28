package com.zelretch.aniiiiiict.data.util

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageDownloader @Inject constructor(
    private val context: Context
) {
    private val client = OkHttpClient()
    private val imageDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "work_images")

    init {
        if (!imageDir.exists()) {
            imageDir.mkdirs()
        }
    }

    suspend fun downloadImage(workId: Long, imageUrl: String): String {
        return withContext(Dispatchers.IO) {
            val imageFile = File(imageDir, "work_${workId}.jpg")
            
            if (imageFile.exists()) {
                return@withContext imageFile.absolutePath
            }

            val request = Request.Builder()
                .url(imageUrl)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw Exception("Failed to download image: ${response.code}")
                    }
                    response.body?.bytes()?.let { bytes ->
                        FileOutputStream(imageFile).use { outputStream ->
                            outputStream.write(bytes)
                        }
                        imageFile.absolutePath
                    } ?: throw Exception("Failed to save image")
                }
            } catch (e: Exception) {
                throw Exception("Failed to download image: ${e.message}")
            }
        }
    }
} 