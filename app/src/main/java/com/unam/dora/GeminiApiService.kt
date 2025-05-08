package com.unam.dora

import com.unam.dora.Itinerary
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface GeminiApiService {
    @POST("/v1beta/models/gemini-2.0-flash:generateContent")
    @Headers("Content-Type: application/json")
    suspend fun generateItinerary(
        @Body request: GeminiRequest
    ): GeminiResponse<Itinerary>

    @POST("/v1beta/models/gemini-2.0-flash-preview-image-generation:generateContent")
    @Headers("Content-Type: application/json")
    suspend fun generateImage(
        @Body request: GeminiRequest
    ): GeminiImageResponse
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
    val responseMimeType: String = "application/json",
    @SerialName("responseModalities")
    val responseModalities: List<String>? = null
)

@Serializable
data class GeminiResponse<T>(
    val candidates: List<Candidate> = emptyList()
)

@Serializable
data class Candidate(
    val content: Content
)

// Für die Bildgenerierung
@Serializable
data class GeminiImageResponse(
    val candidates: List<ImageCandidate> = emptyList()
)

@Serializable
data class ImageCandidate(
    val content: ImageContent
)

@Serializable
data class ImageContent(
    val parts: List<ImagePart>
)

@Serializable
data class ImagePart(
    val image: String? = null,
    val text: String? = null,
    @SerialName("inlineData")
    val inlineData: InlineData? = null
)

@Serializable
data class InlineData(
    @SerialName("mimeType")
    val mimeType: String,
    val data: String
)

