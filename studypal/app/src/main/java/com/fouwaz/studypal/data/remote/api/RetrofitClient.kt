package com.fouwaz.studypal.data.remote.api

import com.fouwaz.studypal.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BASE_URL = "https://openrouter.ai/api/v1/"
    private const val TIMEOUT_SECONDS = 30L

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val authInterceptor = Interceptor { chain ->
        val apiKey = BuildConfig.OPENROUTER_API_KEY

        if (apiKey.isEmpty() || apiKey == "your_openrouter_api_key_here") {
            throw IllegalStateException(
                "OpenRouter API key not configured. " +
                    "Please add OPENROUTER_API_KEY to local.properties"
            )
        }

        val newRequest = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("HTTP-Referer", "https://github.com/voicestream-android")
            .addHeader("X-Title", "VoiceStream Android App")
            .build()

        chain.proceed(newRequest)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val openRouterApi: OpenRouterApiService = retrofit.create(OpenRouterApiService::class.java)
}
