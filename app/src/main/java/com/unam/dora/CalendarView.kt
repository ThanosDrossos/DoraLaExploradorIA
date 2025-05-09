package com.unam.dora

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun CalendarView(
    itinerary: Itinerary?,
    currentDay: Int = 1,
    onDaySelected: (Int) -> Unit,
    onEventRatingChanged: (Int, EventRating) -> Unit,
    onEventClicked: (Event) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Überschrift mit Stadt
        itinerary?.let {
            Text(
                text = "Tu viaje a ${it.city}",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        if (itinerary?.error != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = itinerary.error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        } else if (itinerary != null && itinerary.days.isNotEmpty()) {
            // Tagesauswahl
            ScrollableTabRow(
                selectedTabIndex = currentDay - 1,
                edgePadding = 8.dp
            ) {
                itinerary.days.forEach { dayPlan ->
                    Tab(
                        selected = dayPlan.day == currentDay,
                        onClick = { onDaySelected(dayPlan.day) },
                        text = { Text("Día ${dayPlan.day}") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Aktivitäten des ausgewählten Tages
            val selectedDay = itinerary.days.find { it.day == currentDay }
            selectedDay?.let { dayPlan ->
                // Scrollstate für die LazyColumn hinzufügen
                val listState = rememberLazyListState()
                
                // LazyColumn nimmt nur den verfügbaren Platz ein und wird scrollbar
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f) // Wichtig: nimmt verfügbaren Platz ein
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(
                        start = 0.dp,
                        top = 0.dp,
                        end = 0.dp,
                        bottom = 200.dp // Ausreichend großer Abstand am unteren Rand
                    )
                ) {
                    items(dayPlan.events.size) { index ->
                        EventCard(
                            event = dayPlan.events[index],
                            onRatingChanged = { rating ->
                                onEventRatingChanged(index, rating)
                            },
                            onEventClicked = {
                                onEventClicked(dayPlan.events[index])
                            },
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Nutze verfügbaren Platz
                    .background(MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center
            ) {
                if (itinerary?.isLoading == true) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Construyendo tu itinerario...",
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Text(
                        text = "No hay itinerario disponible.\nPide a Dora que cree uno para ti.",
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun EventCard(
    event: Event,
    onRatingChanged: (EventRating) -> Unit,
    onEventClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
            .clickable { onEventClicked() },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bestehender Event-Content
            Column(modifier = Modifier.weight(1f)) {
                Text(text = event.time, style = MaterialTheme.typography.bodyMedium)
                Text(text = event.location, style = MaterialTheme.typography.titleMedium)
                Text(text = event.activity, style = MaterialTheme.typography.bodyLarge)
            }

            // Neue Rating-Icons
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onRatingChanged(EventRating.LIKED) }) {
                    Icon(
                        imageVector = if (event.rating == EventRating.LIKED)
                            Icons.Filled.CheckCircle
                        else
                            Icons.Outlined.AddCircle,
                        contentDescription = "Me gusta",
                        tint = if (event.rating == EventRating.LIKED)
                            Color.Green
                        else
                            Color.Gray
                    )
                }
                IconButton(onClick = { onRatingChanged(EventRating.DISLIKED) }) {
                    Icon(
                        imageVector = Icons.Outlined.Clear,
                        contentDescription = "No me gusta",
                        tint = if (event.rating == EventRating.DISLIKED)
                            Color.Red
                        else
                            Color.Gray
                    )
                }
            }
        }
    }
}
