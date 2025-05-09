package com.unam.dora

import android.util.Base64
import android.util.Log
import com.unam.dora.Message
import com.unam.dora.MessageDao
import com.unam.dora.Itinerary
import com.unam.dora.Sender
import com.unam.dora.GeminiApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

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

    @Serializable
    data class EventDetails(
        val description: String,
        val visitorInfo: String
    )

    suspend fun fetchEventDetails(location: String, activity: String, city: String): EventDetails =
        withContext(Dispatchers.IO) {
            try {
                val prompt = """
                Provide detailed information about "$activity" at "$location" in "$city". Reply in Spanish.
                Format your answer as a JSON object with two fields:
                1. "description": A detailed paragraph about what this place is, its history, and what visitors can do or see there.
                2. "visitorInfo": Practical information for visitors including opening hours, costs, and any special tips.
                
                Return ONLY a simple JSON object with these two string fields, no nested objects.
            """.trimIndent()

                val request = GeminiRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt))))
                )

                val response = api.generateItinerary(request)
                val rawText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: throw Exception("No valid response received")

                // Verbesserte JSON-Extraktion
                val jsonText = rawText
                    .replace("```json", "")
                    .replace("```", "")
                    .trim()

                try {
                    Log.d("ChatRepository", "Versuche JSON zu parsen: $jsonText")
                    return@withContext kotlinx.serialization.json.Json {
                        ignoreUnknownKeys = true
                        isLenient = true  // Toleranter JSON-Parser
                    }.decodeFromString<EventDetails>(jsonText)
                } catch (e: Exception) {
                    Log.e("ChatRepository", "JSON Parse Error: ${e.message}")

                    // Fallback: Manuelle Extraktion der Felder
                    val descPattern = "\"description\"\\s*:\\s*\"([^\"]+)\"".toRegex()
                    val infoPattern = "\"visitorInfo\"\\s*:\\s*\"([^\"]+)\"".toRegex()

                    val description = descPattern.find(jsonText)?.groupValues?.get(1) ?: "No hay descripción disponible."
                    val visitorInfo = infoPattern.find(jsonText)?.groupValues?.get(1) ?: "No hay infomación de visitantes disponible."

                    EventDetails(
                        description = description,
                        visitorInfo = visitorInfo
                    )
                }
            } catch (e: Exception) {
                Log.e("ChatRepository", "Fehler beim Abrufen der Event-Details: ${e.message}")
                EventDetails(
                    description = "Keine Beschreibung verfügbar.",
                    visitorInfo = "Keine Besucherinformationen verfügbar."
                )
            }
        }

    // Funktion zum Generieren eines Bildes
    suspend fun generateImage(prompt: String): ByteArray =
        withContext(Dispatchers.IO) {
            val imageRequest = GeminiRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                generationConfig = GenerationConfig(
                    temperature = 0.8,
                    responseModalities = listOf("TEXT", "IMAGE")
                )
            )

            Log.e("ChatRepository", "Using old generateImage function!")

            val response = api.generateImage(imageRequest)
            // Annahme: Die Antwort enthält base64-kodierte Bilddaten

            val imagePart = response.candidates.firstOrNull()?.content?.parts?.find {
                it.inlineData != null
            }

            val base64Image = imagePart?.inlineData?.data
                ?: throw Exception("No valid image received")

            Base64.decode(base64Image, Base64.DEFAULT)
        }

    /**
     * Calls Gemini API with a structured prompt, returns the Itinerary data class.
     */
    suspend fun fetchItinerary(prompt: String, city: String, days: Int, moods: List<String>): Itinerary =
        withContext(Dispatchers.IO) {
            try {
                val includeCurrentItinerary = prompt.contains("modifica", ignoreCase = true) ||
                        prompt.contains("actualiza", ignoreCase = true)

                val enhancedPrompt = """
                $prompt
                
                Crea un plan de viaje para la ciudad de $city con exactamente $days días, y las preferencias de viaje: $moods.toSingleString(). Responde en español.
                
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
    preguntas o pedirte modificaciones sobre el mismo. Solamente habla en espanol y crea los itinerarios en espanol.
    
    Responde de manera útil, amable y concisa.
""".trimIndent()

    fun setCustomSystemPrompt(prompt: String) {
        customSystemPrompt = prompt
    }

    suspend fun fetchChatResponse(message: String, previousMessages: List<Message>, currentItinerary: Itinerary?): String =
        withContext(Dispatchers.IO) {
            try {
                // Konversationsverlauf formatieren
                val conversationHistory = previousMessages.takeLast(6).joinToString("\n") { msg ->
                    if (msg.sender == Sender.USER) "Usuario: ${msg.content}"
                    else "Asistente: ${msg.content}"
                }

                val itineraryContext = currentItinerary?.let {
                            """
                    PLAN DE VIAJE ACTUAL:
                    Ciudad: ${it.city}
                    Días totales: ${it.days.size}
                    ${it.days.joinToString("\n\n") { day ->
                                "DÍA ${day.day}:\n" + day.events.joinToString("\n") { event ->
                                    "- ${event.time}: ${event.activity} en ${event.location}"
                                }
                            }}
                    """
                } ?: "No hay plan de viaje actual."


                val fullPrompt = """
                $customSystemPrompt
                
                $itineraryContext
                
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

    suspend fun updateItineraryWithRatings(currentItinerary: Itinerary): Itinerary =
        withContext(Dispatchers.IO) {
            // Sammle Informationen über bewertete Aktivitäten
            val likedActivities = mutableListOf<String>()
            val dislikedActivities = mutableListOf<String>()

            // Durchlaufe alle Tage und Events
            currentItinerary.days.forEach { day ->
                day.events.forEach { event ->
                    val description = "Día ${day.day}, ${event.time}: ${event.activity} en ${event.location}"
                    when (event.rating) {
                        EventRating.LIKED -> likedActivities.add(description)
                        EventRating.DISLIKED -> dislikedActivities.add(description)
                        else -> {} // Neutral aktivitäten ignorieren
                    }
                }
            }

            // Erstelle den Prompt für die API
            val prompt = """
            Tengo un itinerario para ${currentItinerary.city} con ${currentItinerary.days.size} días.
            
            Aqui esta el itinerario actual:
            
            ${currentItinerary.days.joinToString("\n\n") { day ->
                "DÍA ${day.day}:\n" + day.events.joinToString("\n") { event ->
                    "- ${event.time}: ${event.activity} en ${event.location}"
                }
            }}
            
            Actividades que me gustan y quiero MANTENER:
            ${likedActivities.joinToString("\n")}
            
            Actividades que NO me gustan y quiero REEMPLAZAR:
            ${dislikedActivities.joinToString("\n")}
            
            Actividades que son neutras y no me importan:
            ${currentItinerary.days.flatMap { it.events }
                .filter { it.rating == EventRating.NEUTRAL }
                .joinToString("\n") { event ->
                    "- ${event.time}: ${event.activity} en ${event.location}"
                }}
            
            Por favor, actualiza mi itinerario manteniendo las actividades que me gustan y 
            reemplazando las que no me gustan con nuevas sugerencias. Manten también las actividades neutras si tiene sentido:
            solo quitalas si: son muy similares a las que no me gustan o si ya hay actividades muy similares antes y despues (no quiero comer otra vez despues de comer por ejemplo).
            Mantén la misma estructura de días y el mismo número de eventos por día.
            Responde ÚNICAMENTE con el JSON actualizado en formato válido.
        """.trimIndent()

            // API-Aufruf mit dem vorhandenen fetchItinerary
            return@withContext fetchItinerary(
                prompt,
                currentItinerary.city,
                currentItinerary.days.size,
                emptyList()
            )
        }
}
