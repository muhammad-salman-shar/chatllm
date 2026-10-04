package com.neurasamu.build.samu_chat.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

data class ChatRequest(
    val baseUrl: String,
    val apiKey: String,
    val model: String,
    val messages: List<Pair<String, String>>,   // role to content
    val temperature: Double = 0.7,
    val topP: Double = 0.9,
    val maxTokens: Int = 1024,
    val stream: Boolean = true
)

sealed class ChatEvent {
    data class Token(val text: String) : ChatEvent()
    data class Done(val fullText: String) : ChatEvent()
    data class Error(val message: String) : ChatEvent()
}

object ChatClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.MINUTES)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json; charset=utf-8".toMediaType()

    fun stream(req: ChatRequest): Flow<ChatEvent> = flow {
        val url = normalizeUrl(req.baseUrl)
        val body = buildJson(req)

        val builder = Request.Builder()
            .url(url)
            .post(body.toRequestBody(jsonType))
            .header("Content-Type", "application/json")
            .header("Accept", "text/event-stream")

        if (req.apiKey.isNotBlank()) {
            builder.header("Authorization", "Bearer ${req.apiKey}")
        }

        val request = builder.build()

        try {
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val errBody = resp.body?.string() ?: ""
                    emit(ChatEvent.Error("HTTP ${resp.code}: $errBody"))
                    return@flow
                }

                if (req.stream) {
                    streamResponse(resp.body!!.byteStream(), req)
                } else {
                    val raw = resp.body!!.string()
                    val text = parseNonStream(raw)
                    emit(ChatEvent.Token(text))
                    emit(ChatEvent.Done(text))
                }
            }
        } catch (e: Exception) {
            emit(ChatEvent.Error("${e.javaClass.simpleName}: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun kotlinx.coroutines.flow.FlowCollector<ChatEvent>.streamResponse(
        input: java.io.InputStream,
        req: ChatRequest
    ) {
        val reader = BufferedReader(InputStreamReader(input))
        val full = StringBuilder()
        reader.use {
            var line: String?
            while (it.readLine().also { l -> line = l } != null) {
                val l = line ?: continue
                if (l.isBlank()) continue
                if (!l.startsWith("data:")) continue
                val payload = l.removePrefix("data:").trim()
                if (payload == "[DONE]") break

                try {
                    val obj = JSONObject(payload)
                    val choices = obj.optJSONArray("choices") ?: continue
                    if (choices.length() == 0) continue
                    val delta = choices.getJSONObject(0).optJSONObject("delta")
                    val token = delta?.optString("content", "") ?: ""
                    if (token.isNotEmpty()) {
                        full.append(token)
                        emit(ChatEvent.Token(token))
                    }
                } catch (_: Exception) {}
            }
        }
        emit(ChatEvent.Done(full.toString()))
    }

    private fun buildJson(req: ChatRequest): String {
        val arr = JSONArray()
        req.messages.forEach { (role, content) ->
            arr.put(JSONObject().apply {
                put("role", role)
                put("content", content)
            })
        }
        return JSONObject().apply {
            put("model", req.model)
            put("messages", arr)
            put("temperature", req.temperature)
            put("top_p", req.topP)
            put("max_tokens", req.maxTokens)
            put("stream", req.stream)
        }.toString()
    }

    private fun parseNonStream(raw: String): String {
        return try {
            JSONObject(raw)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .optString("content", "")
        } catch (_: Exception) { raw }
    }

    private fun normalizeUrl(base: String): String {
        var u = base.trim().trimEnd('/')
        if (!u.startsWith("http://") && !u.startsWith("https://")) {
            u = "http://$u"
        }
        if (!u.endsWith("/chat/completions")) {
            if (!u.endsWith("/v1")) u = "$u/v1"
            u = "$u/chat/completions"
        }
        return u
    }
}
