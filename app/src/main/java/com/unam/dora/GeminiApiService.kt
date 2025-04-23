package com.unam.dora

import com.unam.dora.Itinerary
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

// Replace base URL with actual Gemini endpoint
interface GeminiApiService {
    @POST("/v1beta/models/gemini-2.0-flash:generateContent")
    @Headers("Content-Type: application/json")
    suspend fun generateItinerary(
        @Body request: PromptRequest
    ): GeminiResponse<Itinerary>
}

/**
 * Sample request wrapper. Adapt fields to Gemini API spec.
 */
@kotlinx.serialization.Serializable
data class PromptRequest(
    val prompt: String,
    val temperature: Double = 0.7
)

/**
 * Generic wrapper for Gemini responses. The `data` field holds the parsed Itinerary JSON.
 */
@kotlinx.serialization.Serializable
data class GeminiResponse<T>(
    val data: T
)