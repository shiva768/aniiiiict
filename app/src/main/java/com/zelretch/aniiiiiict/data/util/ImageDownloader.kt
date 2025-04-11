package com.zelretch.aniiiiiict.data.util

import android.content.Context
import android.os.Environment
import android.util.Log
import com.zelretch.aniiiiiict.util.AniiiiiictLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageDownloader @Inject constructor(
    context: Context
) {
    private val client = OkHttpClient()
    private val imageDir =
        File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "work_images")
    private val TAG = "ImageDownloader"

    init {
        if (!imageDir.exists()) {
            imageDir.mkdirs()
        }
    }

    /**
     * HTTP URLをHTTPSに変換する
     * @param url 元のURL
     * @return HTTPSに変換されたURL
     */
    private fun ensureHttps(url: String): String {
        return if (url.startsWith("http://")) {
            val httpsUrl = url.replaceFirst("http://", "https://")
            Log.d(TAG, "HTTPをHTTPSに変換: $url -> $httpsUrl")
            httpsUrl
        } else {
            url
        }
    }

    suspend fun downloadImage(workId: Long, imageUrl: String): String {
        return withContext(Dispatchers.IO) {
            val imageFile = File(imageDir, "work_${workId}.jpg")

            if (imageFile.exists()) {
                Log.d(TAG, "画像はすでにキャッシュされています: $workId")
                return@withContext imageFile.absolutePath
            }

            val secureUrl = ensureHttps(imageUrl)

            Log.d(TAG, "画像のダウンロードを開始: $secureUrl")
            val startTime = System.currentTimeMillis()

            val request = Request.Builder()
                .url(secureUrl)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    val endTime = System.currentTimeMillis()
                    Log.d(TAG, "ダウンロード時間: ${endTime - startTime}ms")

                    if (!response.isSuccessful) {
                        val message = "画像のダウンロードに失敗: HTTP ${response.code}"
                        Log.w(TAG, message)

                        if (response.code == HttpURLConnection.HTTP_NOT_FOUND) {
                            imageFile.createNewFile()
                            Log.w(TAG, "404のため空ファイルを作成: $workId")
                            return@withContext ""
                        }

                        throw Exception(message)
                    }

                    response.body?.bytes()?.let { bytes ->
                        try {
                            val tempFile = File(imageDir, "temp_${workId}.jpg")
                            FileOutputStream(tempFile).use { outputStream ->
                                outputStream.write(bytes)
                            }

                            if (tempFile.exists() && tempFile.length() > 0) {
                                if (tempFile.renameTo(imageFile)) {
                                    Log.d(TAG, "画像の保存に成功: $workId (${bytes.size / 1024}KB)")
                                    return@withContext imageFile.absolutePath
                                } else {
                                    throw Exception("ファイル名の変更に失敗")
                                }
                            } else {
                                throw Exception("空のファイルが生成されました")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "ファイル操作中にエラー: ${e.message}")
                            throw e
                        }
                    } ?: throw Exception("レスポンスボディがありません")
                }
            } catch (e: Exception) {
                Log.e(TAG, "画像ダウンロード中にエラー: ${e.message}")
                AniiiiiictLogger.logError(e, "画像の保存に失敗 - workId: $workId, url: $secureUrl")
                throw Exception("Failed to download image: ${e.message}")
            }
        }
    }
} 