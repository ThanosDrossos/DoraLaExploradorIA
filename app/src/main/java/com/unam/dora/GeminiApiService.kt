package com.unam.dora

import com.unam.dora.Itinerary
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

// Replace base URL with actual Gemini endpoint
interface GeminiApiService {
    @POST("/v1beta/models/gemini-2.0-flash:generateContent")
    @Headers("Content-Type: application/json")
    suspend fun generateItinerary(
        @Body request: GeminiRequest
    ): GeminiResponse<Itinerary>
}

@Serializable
data class GeminiRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig = GenerationConfig()
)

@Serializable
data class Content(
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String
)

@Serializable
data class GenerationConfig(
    val temperature: Double = 0.7,
    @SerialName("response_mime_type")
    val responseMimeType: String = "application/json"
)

@Serializable
data class GeminiResponse<T>(
    val candidates: List<Candidate> = emptyList()
)

@Serializable
data class Candidate(
    val content: Content
)