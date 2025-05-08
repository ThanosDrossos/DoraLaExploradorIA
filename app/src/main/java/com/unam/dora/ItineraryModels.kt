package com.unam.dora

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Itinerary(
    val city: String,
    val days: List<DayPlan>,
    var isLoading: Boolean = false,
    val error: String? = null
)

@Serializable
data class DayPlan(
    val day: Int,
    @SerialName("events") private val _events: List<Event>? = null,
    @SerialName("activities") private val _activities: List<Event>? = null
) {
    val events: List<Event>
        get() = _events ?: _activities ?: emptyList()
}

enum class EventRating {
    NEUTRAL,
    LIKED,
    DISLIKED
}
@Serializable
data class Event(
    val time: String,
    val location: String,
    val activity: String,
    val rating: EventRating = EventRating.NEUTRAL,
    val description: String = "",           // Ausführliche Beschreibung
    val visitorInfo: String = "",           // Öffnungszeiten, Kosten, etc.
    val imagePath: String? = null           // Pfad zum lokal gespeicherten Bild
)

// Optional: a scheduled event with actual timestamp for internal use
data class ScheduledEvent(
    val day: Int,
    val event: Event,
    val scheduledMillis: Long // epoch millis
)
