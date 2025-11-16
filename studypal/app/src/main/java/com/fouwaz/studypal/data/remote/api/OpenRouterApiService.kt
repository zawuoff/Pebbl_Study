package com.fouwaz.studypal.data.remote.api

import com.fouwaz.studypal.data.remote.model.ChatRequest
import com.fouwaz.studypal.data.remote.model.ChatResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface OpenRouterApiService {

    @POST("chat/completions")
    suspend fun createChatCompletion(
        @Body request: ChatRequest
    ): ChatResponse
}
