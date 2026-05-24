package com.ai.companion.data.remote.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeepSeekApi @Inject constructor(
    private val client: OkHttpClient
) {
    companion object {
        private const val TAG = "DeepSeekApi"
        private const val BASE_URL = "https://api.deepseek.com"
        private const val CHAT_ENDPOINT = "/chat/completions"
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }

    data class ChatMessage(
        val role: String,
        val content: String
    )

    data class ChatCompletionResponse(
        val id: String,
        val choices: List<Choice>,
        val usage: Usage? = null
    ) {
        data class Choice(
            val index: Int,
            val message: ChatMessage,
            val finishReason: String
        )

        data class Usage(
            val promptTokens: Int,
            val completionTokens: Int,
            val totalTokens: Int
        )
    }

    /**
     * 非流式聊天请求
     */
    suspend fun chatCompletion(
        apiKey: String,
        messages: List<ChatMessage>,
        temperature: Float = 0.7f,
        maxTokens: Int = 1024
    ): Result<ChatCompletionResponse> = withContext(Dispatchers.IO) {
        try {
            val requestBody = JSONObject().apply {
                put("model", "deepseek-chat")
                put("messages", JSONArray().apply {
                    messages.forEach { msg ->
                        put(JSONObject().apply {
                            put("role", msg.role)
                            put("content", msg.content)
                        })
                    }
                })
                put("temperature", temperature)
                put("max_tokens", maxTokens)
                put("stream", false)
            }

            val request = Request.Builder()
                .url("$BASE_URL$CHAT_ENDPOINT")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                Log.d(TAG, "Response code: ${response.code}, body: ${body.take(200)}")

                if (response.isSuccessful) {
                    val json = JSONObject(body)
                    val id = json.optString("id", "")
                    val choicesJson = json.optJSONArray("choices") ?: JSONArray()
                    val choices = mutableListOf<ChatCompletionResponse.Choice>()

                    for (i in 0 until choicesJson.length()) {
                        val choiceJson = choicesJson.getJSONObject(i)
                        val messageJson = choiceJson.optJSONObject("message") ?: JSONObject()
                        choices.add(
                            ChatCompletionResponse.Choice(
                                index = choiceJson.optInt("index", 0),
                                message = ChatMessage(
                                    role = messageJson.optString("role", "assistant"),
                                    content = messageJson.optString("content", "")
                                ),
                                finishReason = choiceJson.optString("finish_reason", "")
                            )
                        )
                    }

                    val usageJson = json.optJSONObject("usage")
                    val usage = usageJson?.let {
                        ChatCompletionResponse.Usage(
                            promptTokens = it.optInt("prompt_tokens", 0),
                            completionTokens = it.optInt("completion_tokens", 0),
                            totalTokens = it.optInt("total_tokens", 0)
                        )
                    }

                    Result.success(
                        ChatCompletionResponse(
                            id = id,
                            choices = choices,
                            usage = usage
                        )
                    )
                } else {
                    Result.failure(Exception("API Error (${response.code}): ${body.take(200)}"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Chat completion failed", e)
            Result.failure(e)
        }
    }

    /**
     * 流式聊天请求（打字机效果）
     */
    fun streamChatCompletion(
        apiKey: String,
        messages: List<ChatMessage>,
        temperature: Float = 0.7f,
        maxTokens: Int = 800
    ): Flow<Result<String>> = callbackFlow {
        Log.d(TAG, "Starting stream chat completion")

        try {
            val requestBody = JSONObject().apply {
                put("model", "deepseek-chat")
                put("messages", JSONArray().apply {
                    messages.forEach { msg ->
                        put(JSONObject().apply {
                            put("role", msg.role)
                            put("content", msg.content)
                        })
                    }
                })
                put("temperature", temperature)
                put("max_tokens", maxTokens)
                put("stream", true)
            }

            val request = Request.Builder()
                .url("$BASE_URL$CHAT_ENDPOINT")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "text/event-stream")
                .post(requestBody.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val fullContent = StringBuilder()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: java.io.IOException) {
                    Log.e(TAG, "Stream request failed", e)
                    trySend(Result.failure(e))
                    close()
                }

                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string() ?: "Unknown error"
                        Log.e(TAG, "Stream error: ${response.code} - $errorBody")
                        trySend(Result.failure(Exception("API Error: ${response.code}")))
                        close()
                        return
                    }

                    val source = response.body?.source()
                    if (source == null) {
                        trySend(Result.failure(Exception("Empty response")))
                        close()
                        return
                    }

                    try {
                        val buffer = okio.Buffer()
                        while (!source.exhausted()) {
                            source.read(buffer, 8192)
                            val chunk = buffer.readUtf8()
                            
                            val lines = chunk.split("\n")
                            for (line in lines) {
                                if (line.startsWith("data: ")) {
                                    val data = line.removePrefix("data: ").trim()
                                    if (data == "[DONE]") {
                                        // Stream complete
                                        close()
                                        return
                                    }

                                    try {
                                        val json = JSONObject(data)
                                        val choices = json.optJSONArray("choices")
                                        if (choices != null && choices.length() > 0) {
                                            val choice = choices.getJSONObject(0)
                                            val finishReason = choice.optString("finish_reason")
                                            
                                            if (finishReason == "stop" || finishReason == "length") {
                                                close()
                                                return
                                            }

                                            val delta = choice.optJSONObject("delta")
                                            val content = delta?.optString("content", "") ?: ""
                                            
                                            if (content.isNotEmpty()) {
                                                fullContent.append(content)
                                                trySend(Result.success(fullContent.toString()))
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.w(TAG, "Failed to parse SSE chunk: $line", e)
                                    }
                                }
                            }
                        }
                        close()
                    } catch (e: Exception) {
                        Log.e(TAG, "Stream reading failed", e)
                        trySend(Result.failure(e))
                        close()
                    }
                }
            })

            awaitClose {
                Log.d(TAG, "Stream closed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Stream setup failed", e)
            trySend(Result.failure(e))
            close()
        }
    }

    /**
     * 测试API连接
     */
    suspend fun testConnection(apiKey: String): Boolean {
        return try {
            val result = chatCompletion(
                apiKey = apiKey,
                messages = listOf(ChatMessage("user", "Hi")),
                maxTokens = 10
            )
            result.isSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Connection test failed", e)
            false
        }
    }
}
