package com.unam.dora

import com.unam.dora.Message
import com.unam.dora.MessageDao
import com.unam.dora.Itinerary
import com.unam.dora.Sender
import com.unam.dora.GeminiApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ChatRepository(
    private val dao: MessageDao,
    private val api: GeminiApiService
) {
    // Flow of persisted chat messages
    val allMessages: Flow<List<Message>> = dao.getAllMessages()

    suspend fun insertMessage(message: Message) = dao.insertMessage(message)

    val json = kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
        // Optional: lenient = true für zusätzliche Flexibilität
    }

    /**
     * Calls Gemini API with a structured prompt, returns the Itinerary data class.
     */
    suspend fun fetchItinerary(prompt: String): Itinerary =
        withContext(Dispatchers.IO) {
            try {
                val includeCurrentItinerary = prompt.contains("modifica", ignoreCase = true) ||
                        prompt.contains("actualiza", ignoreCase = true)

                val enhancedPrompt = """
                $prompt
                
                Por favor, responde SOLAMENTE con un objeto JSON que siga EXACTAMENTE esta estructura. Ejemplo:
                {
                  "city": "Madrid",
                  "days": [
                    {
                      "day": 1,
                      "events": [
                        {
                          "time": "09:00",
                          "location": "Plaza Mayor",
                          "activity": "Desayuno y paseo"
                        },
                        {
                          "time": "12:00",
                          "location": "Museo del Prado",
                          "activity": "Visita cultural"
                        }
                      ]
                    },
                    {
                      "day": 2,
                      "events": [
                        {
                          "time": "10:00",
                          "location": "Parque del Retiro",
                          "activity": "Paseo matutino"
                        }
                      ]
                    }
                  ]
                }
                
                REQUISITOS OBLIGATORIOS:
                1. Responde SOLO con JSON válido, sin formateo Markdown
                2. Asegúrate que cada evento tenga exactamente los campos: time, location y activity
            """.trimIndent()

                val request = GeminiRequest(
                    contents = listOf(Content(parts = listOf(Part(text = enhancedPrompt))))
                )

                val response = api.generateItinerary(request)

                val rawText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: throw Exception("No se recibió una respuesta válida")

                val jsonText = rawText
                    .replace("```json", "")
                    .replace("```", "")
                    .trim()

                kotlinx.serialization.json.Json {
                    ignoreUnknownKeys = true
                }.decodeFromString<Itinerary>(jsonText)
            } catch (e: Exception) {
                Itinerary(
                    city = "", // Leerer String für city
                    days = emptyList(), // Leere Liste für days
                    error = "Error de Red"
                )
            }
        }

    private var customSystemPrompt = """
    Eres Dora, un asistente de viaje que ayuda a los usuarios con sus planes 
    de viaje. El usuario tiene un itinerario planificado y puede hacerte 
    preguntas o pedirte modificaciones sobre el mismo.
    
    Responde de manera útil, amable y concisa.
""".trimIndent()

    fun setCustomSystemPrompt(prompt: String) {
        customSystemPrompt = prompt
    }

    suspend fun fetchChatResponse(message: String, previousMessages: List<Message>): String =
        withContext(Dispatchers.IO) {
            try {
                // Konversationsverlauf formatieren
                val conversationHistory = previousMessages.takeLast(6).joinToString("\n") { msg ->
                    if (msg.sender == Sender.USER) "Usuario: ${msg.content}"
                    else "Asistente: ${msg.content}"
                }

                val fullPrompt = """
                $customSystemPrompt
                
                $conversationHistory
                
                Usuario: $message
            """.trimIndent()

                val request = GeminiRequest(
                    contents = listOf(Content(parts = listOf(Part(text = fullPrompt))))
                )

                val response = api.generateItinerary(request)
                return@withContext response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "Lo siento, no pude procesar tu solicitud en este momento."
            } catch (e: Exception) {
                return@withContext "Error al procesar tu solicitud: ${e.message}"
            }
        }
}
