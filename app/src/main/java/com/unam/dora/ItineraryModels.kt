package com.unam.dora

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Itinerary(
    val city: String,
    val days: List<DayPlan>,
    var isLoading: Boolean = false
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

@Serializable
data class Event(
    val time: String,      // e.g., "09:00"
    val location: String,
    val activity: String
)

// Optional: a scheduled event with actual timestamp for internal use
data class ScheduledEvent(
    val day: Int,
    val event: Event,
    val scheduledMillis: Long // epoch millis
)
