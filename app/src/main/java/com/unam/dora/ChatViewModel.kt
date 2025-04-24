// app/src/main/java/com/unam/dora/ChatViewModel.kt
package com.unam.dora

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.unam.dora.AppDatabase
import com.unam.dora.Message
import com.unam.dora.Sender
import com.unam.dora.ChatRepository
import com.unam.dora.Event
import com.unam.dora.Itinerary
import com.unam.dora.ScheduledEvent
import com.unam.dora.GeminiApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    app: Application,
    private val repository: ChatRepository
): AndroidViewModel(app) {

    val messages: StateFlow<List<Message>> =
        repository.allMessages
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // --- Itinerary & Schedule state ---
    private val _itinerary = MutableStateFlow<Itinerary?>(null)
    val itinerary: StateFlow<Itinerary?> = _itinerary

    private val _schedule = MutableStateFlow<List<ScheduledEvent>>(emptyList())
    val schedule: StateFlow<List<ScheduledEvent>> = _schedule

    // --- Trip preferences ---
    private var tripCity: String = ""
    private var tripDays: Int = 1
    private var tripMoods: List<String> = emptyList()

    fun setTripPreferences(city: String, days: Int, moods: List<String>) {
        tripCity = city
        tripDays = days
        tripMoods = moods
    }

    /**
     * Generates an itinerary via Gemini, persists the chat messages,
     * updates the internal itinerary model and schedules.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun generateItinerary() {
        viewModelScope.launch {
            // 1️⃣ Echo user prompt in chat
            val prompt = buildItineraryPrompt()
            repository.insertMessage(Message(sender = Sender.USER, content = prompt))

            // 2️⃣ Fetch structured itinerary from API
            val rawItinerary: Itinerary = repository.fetchItinerary(prompt)
            _itinerary.value = rawItinerary

            // 3️⃣ Persist a human-readable summary as assistant message
            val summary = formatItinerarySummary(rawItinerary)
            repository.insertMessage(Message(sender = Sender.ASSISTANT, content = summary))

            // 4️⃣ Build and store the scheduled events for time-based triggers
            val events = buildSchedule(rawItinerary)
            _schedule.value = events

            // 5️⃣ (Optional) Persist raw JSON for history
            // repo.insertMessage(Message(sender = Sender.ASSISTANT, content = rawItineraryJson))
        }
    }

    private fun buildItineraryPrompt(): String =
        "Plan a $tripDays-day trip to $tripCity. Interests: ${tripMoods.joinToString()}. Respond ONLY with JSON matching the Itinerary schema."

    private fun formatItinerarySummary(itin: Itinerary): String {
        val sb = StringBuilder("Here's your ${itin.days.size}-day itinerary for ${itin.city}:\n")
        itin.days.forEach { dayPlan ->
            sb.append("Day ${dayPlan.day}:\n")
            dayPlan.events.forEach { evt ->
                sb.append("  ${evt.time} – ${evt.location}: ${evt.activity}\n")
            }
        }
        return sb.toString().trimEnd()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildSchedule(itin: Itinerary): List<ScheduledEvent> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        return itin.days
            .flatMap { dayPlan ->
                dayPlan.events.map { evt ->
                    // Parse "HH:mm" into a ZonedDateTime
                    val lt = LocalTime.parse(evt.time)
                    val dt = ZonedDateTime.of(
                        today.plusDays((dayPlan.day - 1).toLong()),
                        lt,
                        zone
                    )
                    ScheduledEvent(
                        day = dayPlan.day,
                        event = evt,
                        scheduledMillis = dt.toInstant().toEpochMilli()
                    )
                }
            }
            .sortedBy { it.scheduledMillis }
    }

// Neue Funktionen in ChatViewModel.kt ergänzen:

    /**
     * Aktualisiert den Reiseplan basierend auf einer Stimmung für einen bestimmten Tag
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun updateItineraryWithMood(day: Int, mood: String) {
        viewModelScope.launch {
            // Aktuelle Reisedaten abrufen
            val currentItinerary = _itinerary.value ?: return@launch

            // Anfrage erstellen
            val prompt = """
            Modifica mi itinerario para el día $day en ${currentItinerary.city} 
            para incluir más actividades de tipo "$mood".
            Mantén el mismo formato JSON y la misma estructura de datos.
        """.trimIndent()

            // Benutzer-Nachricht einfügen
            val userMsg = Message(sender = Sender.USER, content = prompt)
            repository.insertMessage(userMsg)

            try {
                // Aktualisiertes Itinerar abrufen
                val updatedItinerary = repository.fetchItinerary(prompt)
                _itinerary.value = updatedItinerary

                // Zusammenfassung der Änderungen
                val summary =
                    "He actualizado tu itinerario para el día $day con más actividades de $mood."
                val assistantMsg = Message(sender = Sender.ASSISTANT, content = summary)
                repository.insertMessage(assistantMsg)

                // Schedule aktualisieren
                val events = buildSchedule(updatedItinerary)
                _schedule.value = events
            } catch (e: Exception) {
                // Fehlermeldung
                val errorMsg = Message(
                    sender = Sender.ASSISTANT,
                    content = "No pude actualizar el itinerario: ${e.message}"
                )
                repository.insertMessage(errorMsg)
            }
        }
    }

    /**
     * Modifizierte sendUserMessage-Methode, um zwischen Fragen und Plan-Änderungen zu unterscheiden
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun sendUserMessage(text: String) {
        viewModelScope.launch {
            // Benutzer-Nachricht einfügen
            val userMsg = Message(sender = Sender.USER, content = text)
            repository.insertMessage(userMsg)

            // Vorherige Nachrichten für Kontext
            val previousMessages = messages.value.takeLast(6)

            // Überprüfen, ob dies eine Planänderung oder eine Frage ist
            val isItineraryUpdate = determineIfItineraryUpdate(text)

            if (isItineraryUpdate) {
                try {
                    // Wenn es sich um eine Planänderung handelt, aktualisieren wir den Plan
                    val updatedItinerary = repository.fetchItinerary(text)
                    _itinerary.value = updatedItinerary

                    // Änderungsbenachrichtigung
                    val assistantMsg = Message(
                        sender = Sender.ASSISTANT,
                        content = "¡He actualizado tu itinerario según tu solicitud!"
                    )
                    repository.insertMessage(assistantMsg)

                    // Schedule aktualisieren
                    val events = buildSchedule(updatedItinerary)
                    _schedule.value = events
                } catch (e: Exception) {
                    // Fehlerfall
                    val errorMsg = Message(
                        sender = Sender.ASSISTANT,
                        content = "No pude actualizar el itinerario: ${e.message}"
                    )
                    repository.insertMessage(errorMsg)
                }
            } else {
                // Normale Frage - normale Antwort zurückgeben
                val responseText = repository.fetchChatResponse(text, previousMessages)
                val assistantMsg = Message(sender = Sender.ASSISTANT, content = responseText)
                repository.insertMessage(assistantMsg)
            }
        }
    }

    /**
     * Bestimmt, ob die Anfrage des Benutzers eine Aktualisierung des Reiseplans ist
     */
    private fun determineIfItineraryUpdate(text: String): Boolean {
        val updateKeywords = listOf(
            "cambiar", "cambio", "modifica", "actualiza", "mover", "añadir", "agregar",
            "quitar", "eliminar", "reemplazar", "reorganizar", "ajustar"
        )

        return updateKeywords.any { keyword ->
            text.contains(keyword, ignoreCase = true)
        }
    }
}