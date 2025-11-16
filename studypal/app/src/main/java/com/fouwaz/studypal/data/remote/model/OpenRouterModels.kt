package com.fouwaz.studypal.data.remote.model

import com.google.gson.annotations.SerializedName

/**
 * OpenRouter API Request/Response Models
 * OpenAI-compatible schema
 */

data class ChatRequest(
    @SerializedName("model")
    val model: String,

    @SerializedName("messages")
    val messages: List<ChatMessage>,

    @SerializedName("temperature")
    val temperature: Double = 0.7,

    @SerializedName("max_tokens")
    val maxTokens: Int? = null,

    @SerializedName("top_p")
    val topP: Double? = null
)

data class ChatMessage(
    @SerializedName("role")
    val role: String, // "system", "user", "assistant"

    @SerializedName("content")
    val content: String
)

data class ChatResponse(
    @SerializedName("id")
    val id: String,

    @SerializedName("model")
    val model: String,

    @SerializedName("choices")
    val choices: List<ChatChoice>,

    @SerializedName("usage")
    val usage: Usage? = null,

    @SerializedName("error")
    val error: ApiError? = null
)

data class ChatChoice(
    @SerializedName("index")
    val index: Int,

    @SerializedName("message")
    val message: ChatMessage,

    @SerializedName("finish_reason")
    val finishReason: String?
)

data class Usage(
    @SerializedName("prompt_tokens")
    val promptTokens: Int,

    @SerializedName("completion_tokens")
    val completionTokens: Int,

    @SerializedName("total_tokens")
    val totalTokens: Int
)

data class ApiError(
    @SerializedName("message")
    val message: String,

    @SerializedName("type")
    val type: String?,

    @SerializedName("code")
    val code: String?
)
