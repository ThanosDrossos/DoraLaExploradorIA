// app/src/main/java/com/unam/dora/ChatViewModel.kt
package com.unam.dora

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import com.unam.dora.AppDatabase
import com.unam.dora.Message
import com.unam.dora.Sender
import com.unam.dora.ChatRepository
import com.unam.dora.Event
import com.unam.dora.Itinerary
import com.unam.dora.ScheduledEvent
import com.unam.dora.GeminiApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    app: Application,
    private val repository: ChatRepository
): AndroidViewModel(app) {

    private val _selectedEvent = MutableStateFlow<Event?>(null)
    val selectedEvent: StateFlow<Event?> = _selectedEvent

    // Funktion zum Anzeigen der Event-Details
    fun showEventDetails(dayIndex: Int, eventIndex: Int) {
        val currentItinerary = _itinerary.value ?: return
        val day = currentItinerary.days.find { it.day == dayIndex } ?: return

        if (eventIndex < day.events.size) {
            _selectedEvent.value = day.events[eventIndex]
            resetRatingChanges()
        }
    }

    // Funktion zum Zurücksetzen des ausgewählten Events
    fun clearSelectedEvent() {
        _selectedEvent.value = null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun generateItinerary() {
        viewModelScope.launch {
            _itinerary.value = Itinerary(tripCity, emptyList(), true)

            try {
                val prompt = buildItineraryPrompt()
                repository.insertMessage(Message(sender = Sender.USER, content = prompt))

                val rawItinerary = repository.fetchItinerary(prompt, tripCity, tripDays, tripMoods)
                _itinerary.value = rawItinerary

                val summary = formatItinerarySummary(rawItinerary)
                repository.insertMessage(Message(sender = Sender.ASSISTANT, content = summary))

                // Generiere Details für alle Events
                Log.d("ChatViewModel", "Generiere Event-Details für Reiseplan... Current Itinerary: ${_itinerary.value}")
                generateEventDetails(_itinerary)
                Log.d("ChatViewModel", "Itinerary mit Details done: ${_itinerary.value!!}")
                //_itinerary.value = updatedItinerary

                // Erstelle Schedule
                val events = buildSchedule(_itinerary.value!!)
                _schedule.value = events

            } catch (e: Exception) {
                _itinerary.value = Itinerary(tripCity, emptyList(), false, e.message)
            }
        }
    }


    private suspend fun generateEventDetails(_itinerary: MutableStateFlow<Itinerary?>) {
        withContext(Dispatchers.IO) {
            val currentItinerary = _itinerary.value ?: return@withContext

            currentItinerary.days.flatMap { day ->
                day.events.map { event ->
                    async {
                        try {
                            Log.d("ChatViewModel", "Generiere Details für Event: $event")

                            val details = repository.fetchEventDetails(
                                event.location,
                                event.activity,
                                currentItinerary.city
                            )

                            Log.d("ChatViewModel", "Lade Bild für: ${event.location}")
                            val filename = "event_${event.location.replace(" ", "_")}.jpg"

                            val imagePath = ImageCrawlHelper.getImage(
                                event.location,
                                currentItinerary.city,
                                filename,
                                getApplication()
                            )

                            val updatedEvent = event.copy(
                                description = details.description,
                                visitorInfo = details.visitorInfo,
                                imagePath = imagePath,
                                completelyLoaded = true
                            )

                            // Itinerary Update
                            val (currentValue, updatedDays) = synchronized(_itinerary) {
                                val currentValue = _itinerary.value
                                val updatedDays = currentValue?.days?.map { d ->
                                    if (d.day == day.day) {
                                        d.copy(_events = d.events.map { e ->
                                            if (e.location == event.location && e.activity == event.activity) {
                                                updatedEvent
                                            } else e
                                        })
                                    } else d
                                }
                                Pair(currentValue, updatedDays)
                            }

                            // Updates außerhalb des synchronized Blocks
                            currentValue?.let { itinerary ->
                                _itinerary.value = itinerary.copy(days = updatedDays ?: emptyList())
                                Log.d("ChatViewModel", "Event aktualisiert: ${event.location}")
                            }

                            // Selected Event Update außerhalb des synchronized Blocks
                            if (_selectedEvent.value?.time == updatedEvent.time) {
                                _selectedEvent.emit(updatedEvent)
                                Log.d("ChatViewModel", "Selected Event aktualisiert!: ${_selectedEvent.value}")
                            } else {
                                Log.d("ChatViewModel", "Selected Event nicht aktualisiert!: ${_selectedEvent.value} ${_selectedEvent.value?.time} - ${updatedEvent.time}")
                            }

                        } catch (e: Exception) {
                            Log.e("ChatViewModel", "Fehler bei Event-Details: ${e.message}")
                        }
                    }
                }
            }.awaitAll()
        }
    }

    private suspend fun generateAndSaveImage(
        location: String,
        activity: String,
        city: String,
        filename: String
    ): String? {
        return try {
            val prompt = "Generate a photorealistic image of $activity at $location in $city. Make it look like a professional travel photograph."
            val imageData = repository.generateImage(prompt)

            // Bild speichern
            val file = File(getApplication<Application>().filesDir, filename)
            FileOutputStream(file).use { it.write(imageData) }

            // Log hinzufügen, um zu überprüfen, ob das Bild gespeichert wurde
            Log.d("ChatViewModel", "Bild wurde gespeichert: ${file.absolutePath} (${file.length()} bytes)")
            file.absolutePath
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Fehler beim Generieren des Bildes: ${e.message}", e)
            null
        }
    }

    private val _hasRatingChanges = MutableStateFlow(false)
    val hasRatingChanges: StateFlow<Boolean> = _hasRatingChanges

    fun resetRatingChanges() {
        _hasRatingChanges.value = false

        var updatedDays = _itinerary.value?.days?.map { dayPlan ->
            val updatedEvents = dayPlan.events.mapIndexed { index, event ->
                event.copy(rating = EventRating.NEUTRAL)
            }
            dayPlan.copy(_events = updatedEvents)
        }
        _itinerary.update { it?.copy(days = updatedDays!!) }
        Log.d("ChatViewModel", "Rating changes reset")
    }

    fun shouldShowRatingButtons(): Boolean {
        return _hasRatingChanges.value
    }

    fun getHasRatingChangesFlow(): StateFlow<Boolean> {
        return _hasRatingChanges
    }

    // Methode zum Aktualisieren der Bewertung
    fun updateEventRating(day: Int, eventIndex: Int, rating: EventRating) {
        val currentItinerary = _itinerary.value ?: return

        /*
        if (_hasRatingChanges.value == false) {
            // Alle Events in allen Tagen auf LIKED setzen
            var updatedDays = currentItinerary.days.map { dayPlan ->
                val updatedEvents = dayPlan.events.mapIndexed { index, event ->
                    if (index == eventIndex) event.copy(rating = EventRating.LIKED) else event
                }
                dayPlan.copy(_events = updatedEvents)
            }

            updatedDays = currentItinerary.days.map { dayPlan ->
                dayPlan.copy(_events = dayPlan.events.map { event ->
                    event.copy(rating = EventRating.LIKED)
                })
            }

            _itinerary.update { it?.copy(days = updatedDays) }
            _hasRatingChanges.value = true

            //now set all buttons to LIKED
            Log.d("ChatViewModel", "Click set all to LIKED")
        }


        // Erstelle eine neue Itinerary mit der aktualisierten Bewertung
        var updatedDays = currentItinerary.days.map { dayPlan ->
            if (dayPlan.day == day) {
                val updatedEvents = dayPlan.events.mapIndexed { index, event ->
                    if (index == eventIndex) event.copy(rating = rating) else event
                }
                dayPlan.copy(_events = updatedEvents)
            } else {
                dayPlan
            }
        }
        */

        //CHANGE only the element
        var updatedDays = currentItinerary.days.map { dayPlan ->
            val updatedEvents = dayPlan.events.mapIndexed { index, event ->
                if (index == eventIndex)
                    event.copy(rating = rating)
                else
                    if (_hasRatingChanges.value == false) {
                        Log.d("ChatViewModel", "Click set all to LIKED")
                        event.copy(rating = EventRating.LIKED)
                    } else {
                        event
                    }
            }
            dayPlan.copy(_events = updatedEvents)
        }
        _itinerary.update { it?.copy(days = updatedDays) }

        if (_hasRatingChanges.value == false) {
            _hasRatingChanges.value = true
        }

        Log.d("ChatViewModel", "Click outside")
    }

    // Methode zum Anwenden der Änderungen
    @RequiresApi(Build.VERSION_CODES.O)
    fun applyRatingChanges() {
        viewModelScope.launch {
            val currentItinerary = _itinerary.value ?: return@launch

            // Aktualisiere den Reiseplan basierend auf den Bewertungen
            val updatedItinerary = repository.updateItineraryWithRatings(currentItinerary)
            _itinerary.value = updatedItinerary

            // Schedule aktualisieren
            val events = buildSchedule(updatedItinerary)
            _schedule.value = events

            // Status zurücksetzen
            _hasRatingChanges.value = false

            // Bestätigungsnachricht
            val assistantMsg = Message(
                sender = Sender.ASSISTANT,
                content = "He actualizado tu itinerario según tus preferencias"
            )
            repository.insertMessage(assistantMsg)

            //now load event stuff from AI
            try {
                Log.d("ChatViewModel", "Generiere Event-Details für aktualisierten Reiseplan...")
                // Generiere Details für alle Events
                generateEventDetails(_itinerary)
                Log.d("ChatViewModel", "Itinerary mit Details done: ${_itinerary.value!!}")
                //_itinerary.value = updatedItinerary

                // Erstelle Schedule
                val events = buildSchedule(_itinerary.value!!)
                _schedule.value = events

            } catch (e: Exception) {
                _itinerary.value = Itinerary(tripCity, emptyList(), false, e.message)
            }
        }
    }

    val messages: StateFlow<List<Message>> =
        repository.allMessages
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // --- Itinerary & Schedule state ---
    // In ChatViewModel.kt
    private val _itinerary = MutableStateFlow<Itinerary?>(
        Itinerary(
            city = "",
            days = emptyList(),
            isLoading = false
        )
    )

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

    private fun buildItineraryPrompt(): String =
        "Planifica un viaje de $tripDays días a $tripCity. Intereses: ${tripMoods.joinToString()}. Responde ÚNICAMENTE con un JSON que cumpla con el esquema de itinerario."

    private fun formatItinerarySummary(itin: Itinerary): String {
        val sb = StringBuilder("Aquí tienes tu itinerario de ${itin.days.size} días para ${itin.city}:")
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
            val currentItinerary = _itinerary.value ?: return@launch

            // Detaillierten aktuellen Plan einschließen
            val currentPlanDetails = currentItinerary.days.joinToString("\n\n") { dayPlan ->
                "DIA ${dayPlan.day}:\n" + dayPlan.events.joinToString("\n") { event ->
                    "[${event.time}] ${event.activity} en ${event.location}"
                }
            }

            // Anfrage erstellen
            val prompt = """
        Aqui está el itinerario actual para la ciudad ${currentItinerary.city} con ${currentItinerary.days.size} días:
            
        $currentPlanDetails
            
        Por favor, solamente modifica el itinerario para el día $day en ${currentItinerary.city} 
        con exactamente ${currentItinerary.days.size} días en total. Solo cambia el dia $day y manten los otros iguales. 
        para incluir más actividades de tipo "$mood". Responda en espanol.
        Mantén el mismo formato JSON válido y la misma estructura de datos y responde UNICAMENTE con el JSON.
        """.trimIndent()

            // Benutzer-Nachricht einfügen
            val userMsg = Message(sender = Sender.USER, content = prompt)
            repository.insertMessage(userMsg)

            try {
                // Aktualisiertes Itinerar abrufen
                val updatedItinerary = repository.fetchItinerary(
                    prompt,
                    currentItinerary.city,
                    currentItinerary.days.size,
                    listOf(mood)
                )

                _itinerary.value = updatedItinerary

                try {
                    Log.d("ChatViewModel", "Generiere Event-Details für aktualisierten Reiseplan...")
                    generateEventDetails(_itinerary)
                    Log.d("ChatViewModel", "Itinerary mit Details done: ${_itinerary.value!!}")
                } catch (e: Exception) {
                    Log.e("ChatViewModel", "Fehler beim Generieren der Event-Details: ${e.message}")
                }


                // Zusammenfassung der Änderungen
                val summary = "He actualizado tu itinerario para el día $day con más actividades de $mood."
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

            val currentItinerary = _itinerary.value
            val currentDays = tripDays
            val currentCity = tripCity

            // Vorherige Nachrichten für Kontext
            val previousMessages = messages.value.takeLast(6)

            // Überprüfen, ob dies eine Planänderung oder eine Frage ist
            val isItineraryUpdate = determineIfItineraryUpdate(text)

            if (isItineraryUpdate) {
                try {
                    Log.d("ChatViewModel", "Itinerary-Änderung erkannt, verarbeite...")

                    // Wenn es sich um eine Planänderung handelt, aktualisieren wir den Plan
                    val updatedItinerary = repository.fetchItinerary(text, currentCity, currentDays, tripMoods)

                    // Debug-Ausgabe für das aktualisierte Itinerary
                    Log.d("ChatViewModel", "Aktualisiertes Itinerary erhalten: $updatedItinerary")

                    // Speichern des aktualisierten Itineraries
                    _itinerary.update { updatedItinerary }

                    try {
                        Log.d("ChatViewModel", "Generiere Event-Details für aktualisierten Reiseplan...")
                        generateEventDetails(_itinerary)
                        Log.d("ChatViewModel", "Itinerary mit Details done: ${_itinerary.value!!}")
                    } catch (e: Exception) {
                        Log.e("ChatViewModel", "Fehler beim Generieren der Event-Details: ${e.message}")
                    }

                    // Sicherstellen, dass der neue Wert gesetzt wurde
                    Log.d("ChatViewModel", "Neuer Itinerary-Wert: ${_itinerary.value}")

                    val responseText = repository.fetchChatResponse(text, previousMessages, currentItinerary)
                    val assistantMsg = Message(sender = Sender.ASSISTANT, content = responseText.toString())
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
                val responseText = repository.fetchChatResponse(text, previousMessages, currentItinerary)
                val assistantMsg = Message(sender = Sender.ASSISTANT, content = responseText.toString())
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
            "quitar", "eliminar", "reemplazar", "reorganizar", "ajustar", "añade", "agrega",
            "sustituir", "sustituye", "reorganiza", "ajusta", "modificar", "modifica",
            "actualizar", "mover", "añadir", "agregar", "quitar", "eliminar", "reemplazar",
        )

        return updateKeywords.any { keyword ->
            text.contains(keyword, ignoreCase = true)
        }
    }
    // Nachladen von fehlenden Event-Details und Bildern
    fun loadEventDetailsIfMissing(event: Event) {
        viewModelScope.launch {
            try {
                val currentItinerary = _itinerary.value ?: return@launch

                // Suchen des Tages und des Event-Index
                var targetDay: DayPlan? = null
                var eventIndex = -1

                for (day in currentItinerary.days) {
                    val index = day.events.indexOfFirst { it.location == event.location && it.activity == event.activity }
                    if (index != -1) {
                        targetDay = day
                        eventIndex = index
                        break
                    }
                }

                if (targetDay == null || eventIndex == -1) return@launch

                // Details generieren, falls fehlend
                val details = if (event.description.isBlank() || event.visitorInfo.isBlank()) {
                    repository.fetchEventDetails(event.location, event.activity, currentItinerary.city)
                } else {
                    null
                }

                // Bild generieren, falls fehlend oder Datei nicht existiert
                val imagePath = if (event.imagePath.isNullOrBlank() || !File(event.imagePath).exists()) {
                    // Stabilen Dateinamen erstellen (ohne Zeitstempel)
                    val filename = "event_${event.location.replace(" ", "_")}_${event.activity.replace(" ", "_")}.jpg"

                    generateAndSaveImage(
                        event.location,
                        event.activity,
                        currentItinerary.city,
                        filename
                    )
                } else {
                    event.imagePath
                }

                // Event aktualisieren
                val updatedEvent = event.copy(
                    description = details?.description ?: event.description,
                    visitorInfo = details?.visitorInfo ?: event.visitorInfo,
                    imagePath = imagePath
                )

                // Im Itinerary aktualisieren
                val updatedEvents = targetDay.events.toMutableList()
                updatedEvents[eventIndex] = updatedEvent

                val updatedDay = targetDay.copy(_events = updatedEvents)
                val updatedDays = currentItinerary.days.toMutableList()
                val dayIndex = updatedDays.indexOfFirst { it.day == targetDay.day }

                if (dayIndex >= 0) {
                    updatedDays[dayIndex] = updatedDay
                    _itinerary.value = currentItinerary.copy(days = updatedDays)
                }

            } catch (e: Exception) {
                Log.e("ChatViewModel", "Fehler beim Nachladen der Event-Details: ${e.message}")
            }
        }
    }
}