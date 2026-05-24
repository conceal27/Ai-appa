package com.ai.companion.data.remote.api

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

object DoubaoApi {
    private const val BASE_URL = "https://ark.cn-beijing.volces.com"
    private const val IMAGE_ENDPOINT = "/api/v3/images/generations"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    data class ImageGenerationResult(
        val id: String,
        val url: String,
        val revisedPrompt: String? = null
    )

    /**
     * 文生图 - 根据文本描述生成图片
     */
    suspend fun generateImage(
        apiKey: String,
        prompt: String,
        model: String = "doubao-image-1.0",
        size: String = "1024x1024",
        n: Int = 1,
        quality: String = "standard",
        style: String = "vivid"
    ): Result<List<ImageGenerationResult>> = suspendCancellableCoroutine { continuation ->
        try {
            val payload = JSONObject().apply {
                put("model", model)
                put("prompt", prompt)
                put("n", n)
                put("size", size)
                put("quality", quality)
                put("style", style)
                put("response_format", "url")
            }

            val request = Request.Builder()
                .url("$BASE_URL$IMAGE_ENDPOINT")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody())
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    val dataArray = json.optJSONArray("data")
                    
                    val results = mutableListOf<ImageGenerationResult>()
                    dataArray?.let {
                        for (i in 0 until it.length()) {
                            val item = it.getJSONObject(i)
                            results.add(
                                ImageGenerationResult(
                                    id = item.optString("id", ""),
                                    url = item.optString("url", ""),
                                    revisedPrompt = item.optString("revised_prompt")
                                )
                            )
                        }
                    }
                    
                    continuation.resume(Result.success(results))
                } else {
                    val errorBody = response.body?.string() ?: ""
                    val errorMsg = try {
                        val errorJson = JSONObject(errorBody)
                        errorJson.optJSONObject("error")?.optString("message") ?: "Image generation failed"
                    } catch (e: Exception) {
                        "HTTP ${response.code}: $errorBody"
                    }
                    continuation.resume(Result.failure(Exception(errorMsg)))
                }
            }
        } catch (e: Exception) {
            continuation.resume(Result.failure(e))
        }
    }

    /**
     * 下载图片到本地文件
     */
    suspend fun downloadImage(
        imageUrl: String,
        outputFile: File
    ): Result<File> = suspendCancellableCoroutine { continuation ->
        try {
            val request = Request.Builder()
                .url(imageUrl)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.byteStream()?.use { input ->
                        outputFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    continuation.resume(Result.success(outputFile))
                } else {
                    continuation.resume(Result.failure(Exception("Download failed: ${response.code}")))
                }
            }
        } catch (e: Exception) {
            continuation.resume(Result.failure(e))
        }
    }

    /**
     * 测试API连接
     */
    suspend fun testConnection(apiKey: String): Boolean {
        return try {
            val result = generateImage(
                apiKey = apiKey,
                prompt = "a simple blue sky",
                size = "256x256",
                n = 1
            )
            result.isSuccess
        } catch (e: Exception) {
            false
        }
    }
}
