package com.unam.dora

import kotlinx.serialization.Serializable

@Serializable
data class Itinerary(
    val city: String,
    val days: List<DayPlan>
)

@Serializable
data class DayPlan(
    val day: Int,
    val events: List<Event>
)

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
