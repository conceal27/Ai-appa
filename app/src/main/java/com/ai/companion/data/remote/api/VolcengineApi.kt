package com.ai.companion.data.remote.api

import android.util.Base64
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.coroutines.resume

object VolcengineApi {
    private const val REGION = "cn-north-1"
    private const val SERVICE = "sami"
    private const val HOST = "openspeech.bytedance.com"
    private const val SCHEME = "https"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    /**
     * 火山引擎签名算法
     */
    private fun sign(
        secretKey: String,
        method: String,
        path: String,
        query: String,
        headers: Map<String, String>,
        payload: String,
        signedHeaders: List<String>
    ): String {
        // 1. 生成规范请求
        val canonicalRequest = buildString {
            append("$method\n")
            append("$path\n")
            append("$query\n")
            signedHeaders.forEach { key ->
                append("${key.lowercase()}:${headers[key]}\n")
            }
            append("\n")
            append(signedHeaders.joinToString(";"))
            append("\n")
            append(sha256Hex(payload))
        }

        // 2. 生成签名字符串
        val date = getCurrentDate()
        val stringToSign = buildString {
            append("HMAC-SHA256\n")
            append("${date}T${getCurrentTime()}Z\n")
            append("$date/$REGION/$SERVICE/request\n")
            append(sha256Hex(canonicalRequest))
        }

        // 3. 计算签名
        val kDate = hmacSha256(secretKey.toByteArray(), date)
        val kRegion = hmacSha256(kDate, REGION)
        val kService = hmacSha256(kRegion, SERVICE)
        val kSigning = hmacSha256(kService, "request")
        val signature = hmacSha256Hex(kSigning, stringToSign)

        return signature
    }

    private fun sha256Hex(text: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(text.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun hmacSha256(key: ByteArray, data: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data.toByteArray(Charsets.UTF_8))
    }

    private fun hmacSha256Hex(key: ByteArray, data: String): String {
        return hmacSha256(key, data).joinToString("") { "%02x".format(it) }
    }

    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    private fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("HHmmss", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    /**
     * ASR 语音识别 - 将音频文件转为文字
     */
    suspend fun speechToText(
        accessKey: String,
        secretKey: String,
        appId: String,
        audioFile: File,
        language: String = "zh-CN"
    ): Result<String> = suspendCancellableCoroutine { continuation ->
        try {
            val path = "/api/v1/asr/recognize"
            val method = "POST"
            
            val payload = JSONObject().apply {
                put("app", JSONObject().apply {
                    put("appid", appId)
                    put("token", "")
                    put("cluster", "")
                })
                put("user", JSONObject().apply {
                    put("uid", "user_001")
                })
                put("request", JSONObject().apply {
                    put("reqid", "req_${System.currentTimeMillis()}")
                    put("nbest", 1)
                    put("result_type", "full")
                })
                put("audio", JSONObject().apply {
                    put("format", "wav")
                    put("rate", 16000)
                    put("bits", 16)
                    put("channel", 1)
                    put("codec", "pcm")
                })
            }.toString()

            val headers = mutableMapOf(
                "Host" to HOST,
                "Content-Type" to "application/json",
                "X-Date" to "${getCurrentDate()}T${getCurrentTime()}Z"
            )

            val signedHeaders = listOf("content-type", "host", "x-date")
            val signature = sign(secretKey, method, path, "", headers, payload, signedHeaders)

            val authorization = buildString {
                append("HMAC-SHA256 ")
                append("Credential=$accessKey/${getCurrentDate()}/$REGION/$SERVICE/request, ")
                append("SignedHeaders=${signedHeaders.joinToString(";")}, ")
                append("Signature=$signature")
            }

            // 构建 multipart 请求
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("payload", payload)
                .addFormDataPart(
                    "audio",
                    audioFile.name,
                    audioFile.asRequestBody("audio/wav".toMediaType())
                )
                .build()

            val request = Request.Builder()
                .url("$SCHEME://$HOST$path")
                .addHeader("Authorization", authorization)
                .addHeader("X-Date", headers["X-Date"]!!)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    val result = json.optJSONObject("result")
                    val text = result?.optJSONArray("sentences")
                        ?.optJSONObject(0)
                        ?.optString("text", "") ?: ""
                    
                    continuation.resume(Result.success(text))
                } else {
                    val error = response.body?.string() ?: "ASR failed"
                    continuation.resume(Result.failure(Exception(error)))
                }
            }
        } catch (e: Exception) {
            continuation.resume(Result.failure(e))
        }
    }

    /**
     * TTS 语音合成 - 将文字转为语音
     */
    suspend fun textToSpeech(
        accessKey: String,
        secretKey: String,
        appId: String,
        text: String,
        voiceType: String = "zh_female_qingxin",
        speed: Float = 1.0f,
        pitch: Float = 1.0f
    ): Result<File> = suspendCancellableCoroutine { continuation ->
        try {
            val path = "/api/v1/tts"
            val method = "POST"

            val payload = JSONObject().apply {
                put("app", JSONObject().apply {
                    put("appid", appId)
                    put("token", "")
                    put("cluster", "")
                })
                put("user", JSONObject().apply {
                    put("uid", "user_001")
                })
                put("request", JSONObject().apply {
                    put("reqid", "req_${System.currentTimeMillis()}")
                    put("text", text)
                    put("text_type", "plain")
                    put("operation", "submit")
                })
                put("audio", JSONObject().apply {
                    put("voice_type", voiceType)
                    put("encoding", "mp3")
                    put("speed_ratio", speed.toDouble())
                    put("volume_ratio", 1.0)
                    put("pitch_ratio", pitch.toDouble())
                })
            }.toString()

            val headers = mutableMapOf(
                "Host" to HOST,
                "Content-Type" to "application/json",
                "X-Date" to "${getCurrentDate()}T${getCurrentTime()}Z"
            )

            val signedHeaders = listOf("content-type", "host", "x-date")
            val signature = sign(secretKey, method, path, "", headers, payload, signedHeaders)

            val authorization = buildString {
                append("HMAC-SHA256 ")
                append("Credential=$accessKey/${getCurrentDate()}/$REGION/$SERVICE/request, ")
                append("SignedHeaders=${signedHeaders.joinToString(";")}, ")
                append("Signature=$signature")
            }

            val request = Request.Builder()
                .url("$SCHEME://$HOST$path")
                .addHeader("Authorization", authorization)
                .addHeader("X-Date", headers["X-Date"]!!)
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    // 创建临时文件保存音频
                    val tempFile = File.createTempFile("tts_${System.currentTimeMillis()}", ".mp3")
                    response.body?.byteStream()?.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    continuation.resume(Result.success(tempFile))
                } else {
                    val error = response.body?.string() ?: "TTS failed"
                    continuation.resume(Result.failure(Exception(error)))
                }
            }
        } catch (e: Exception) {
            continuation.resume(Result.failure(e))
        }
    }

    /**
     * 图片识别 OCR
     */
    suspend fun recognizeImage(
        accessKey: String,
        secretKey: String,
        appId: String,
        imageFile: File
    ): Result<String> = suspendCancellableCoroutine { continuation ->
        try {
            val path = "/api/v1/ocr"
            val method = "POST"

            val payload = JSONObject().apply {
                put("app", JSONObject().apply {
                    put("appid", appId)
                    put("token", "")
                })
                put("request", JSONObject().apply {
                    put("reqid", "req_${System.currentTimeMillis()}")
                    put("language", "zh-CN")
                })
            }.toString()

            val headers = mutableMapOf(
                "Host" to HOST,
                "X-Date" to "${getCurrentDate()}T${getCurrentTime()}Z"
            )

            val signedHeaders = listOf("host", "x-date")
            val signature = sign(secretKey, method, path, "", headers, payload, signedHeaders)

            val authorization = buildString {
                append("HMAC-SHA256 ")
                append("Credential=$accessKey/${getCurrentDate()}/$REGION/$SERVICE/request, ")
                append("SignedHeaders=${signedHeaders.joinToString(";")}, ")
                append("Signature=$signature")
            }

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("payload", payload)
                .addFormDataPart(
                    "image",
                    imageFile.name,
                    imageFile.asRequestBody("image/jpeg".toMediaType())
                )
                .build()

            val request = Request.Builder()
                .url("$SCHEME://$HOST$path")
                .addHeader("Authorization", authorization)
                .addHeader("X-Date", headers["X-Date"]!!)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    val result = json.optJSONObject("result")
                    val texts = mutableListOf<String>()
                    
                    result?.optJSONArray("lines")?.let { lines ->
                        for (i in 0 until lines.length()) {
                            lines.optJSONObject(i)?.optString("text")?.let {
                                texts.add(it)
                            }
                        }
                    }
                    
                    continuation.resume(Result.success(texts.joinToString("\n")))
                } else {
                    val error = response.body?.string() ?: "OCR failed"
                    continuation.resume(Result.failure(Exception(error)))
                }
            }
        } catch (e: Exception) {
            continuation.resume(Result.failure(e))
        }
    }

    /**
     * 简单的连接测试
     */
    suspend fun testConnection(
        accessKey: String,
        secretKey: String,
        appId: String
    ): Boolean {
        return accessKey.isNotBlank() && secretKey.isNotBlank() && appId.isNotBlank()
    }
}
