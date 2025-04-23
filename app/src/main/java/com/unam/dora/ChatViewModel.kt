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

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    // --- Repository & API setup ---
    private val repo: ChatRepository

    init {
        // 1. Build Retrofit instance for Gemini API
        val retrofit = Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/") // adjust to actual base URL
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(GeminiApiService::class.java)

        // 2. Get DAO and create repository
        val dao = AppDatabase.getDatabase(app).messageDao()
        repo = ChatRepository(dao, apiService)
    }

    // --- Chat messages ---
    val messages: StateFlow<List<Message>> =
        repo.allMessages
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
            repo.insertMessage(Message(sender = Sender.USER, content = prompt))

            // 2️⃣ Fetch structured itinerary from API
            val rawItinerary: Itinerary = repo.fetchItinerary(prompt)
            _itinerary.value = rawItinerary

            // 3️⃣ Persist a human-readable summary as assistant message
            val summary = formatItinerarySummary(rawItinerary)
            repo.insertMessage(Message(sender = Sender.ASSISTANT, content = summary))

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

    /** Simple echo placeholder; replace with actual LLM call as needed. */
    fun sendUserMessage(text: String) {
        viewModelScope.launch {
            // Insert user message
            val userMsg = Message(sender = Sender.USER, content = text)
            repo.insertMessage(userMsg)

            // Insert assistant reply (stub)
            val replyText = "Assistant echo: $text"
            val assistantMsg = Message(sender = Sender.ASSISTANT, content = replyText)
            repo.insertMessage(assistantMsg)
        }
    }
}
