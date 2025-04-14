package com.zelretch.aniiiiiict.data.util

import android.content.Context
import android.os.Environment
import com.zelretch.aniiiiiict.util.Logger
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
    context: Context,
    private val logger: Logger
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
            logger.logDebug(TAG, "HTTPをHTTPSに変換: $url -> $httpsUrl", "ensureHttps")
            httpsUrl
        } else {
            url
        }
    }

    /**
     * 指定されたURLから画像をダウンロードし、ローカルに保存します
     * @param workId 作品ID
     * @param imageUrl 画像のURL
     * @return 保存された画像のパス、エラー時はnull
     */
    suspend fun downloadImage(workId: Long, imageUrl: String): String? {
        return withContext(Dispatchers.IO) {
            val imageFile = File(imageDir, "work_${workId}.jpg")

            if (imageFile.exists()) {
                logger.logDebug(
                    TAG,
                    "画像はすでにキャッシュされています: $workId",
                    "downloadImage"
                )
                return@withContext imageFile.absolutePath
            }

            val secureUrl = ensureHttps(imageUrl)

            logger.logDebug(TAG, "画像のダウンロードを開始: $secureUrl", "downloadImage")
            val startTime = System.currentTimeMillis()

            val request = Request.Builder()
                .url(secureUrl)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    val endTime = System.currentTimeMillis()
                    logger.logDebug(
                        TAG,
                        "ダウンロード時間: ${endTime - startTime}ms",
                        "downloadImage"
                    )

                    if (!response.isSuccessful) {
                        val message = "画像のダウンロードに失敗: HTTP ${response.code}"
                        logger.logWarning(TAG, message, "downloadImage")

                        if (response.code == HttpURLConnection.HTTP_NOT_FOUND) {
                            imageFile.createNewFile()
                            logger.logWarning(
                                TAG,
                                "404のため空ファイルを作成: $workId",
                                "downloadImage"
                            )
                            return@withContext null
                        }

                        return@withContext null
                    }

                    response.body?.bytes()?.let { bytes ->
                        try {
                            val tempFile = File(imageDir, "temp_${workId}.jpg")
                            FileOutputStream(tempFile).use { outputStream ->
                                outputStream.write(bytes)
                            }

                            if (tempFile.exists() && tempFile.length() > 0) {
                                if (tempFile.renameTo(imageFile)) {
                                    logger.logDebug(
                                        TAG,
                                        "画像の保存に成功: $workId (${bytes.size / 1024}KB)",
                                        "downloadImage"
                                    )
                                    return@withContext imageFile.absolutePath
                                } else {
                                    logger.logError(
                                        TAG,
                                        "ファイル名の変更に失敗 - workId: $workId",
                                        "downloadImage"
                                    )
                                    return@withContext null
                                }
                            } else {
                                logger.logError(
                                    TAG,
                                    "空のファイルが生成されました - workId: $workId",
                                    "downloadImage"
                                )
                                return@withContext null
                            }
                        } catch (e: Exception) {
                            logger.logError(
                                TAG,
                                "ファイル操作中にエラー: ${e.message}",
                                "downloadImage"
                            )
                            return@withContext null
                        }
                    } ?: run {
                        logger.logError(
                            TAG,
                            "レスポンスボディがありません - workId: $workId",
                            "downloadImage"
                        )
                        return@withContext null
                    }
                }
            } catch (e: Exception) {
                logger.logError(
                    TAG,
                    "画像の保存に失敗 - workId: $workId, url: $secureUrl, エラー: ${e.message}",
                    "downloadImage"
                )
                return@withContext null
            }
        }
    }
} 