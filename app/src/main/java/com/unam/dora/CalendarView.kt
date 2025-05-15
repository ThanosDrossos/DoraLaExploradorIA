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
import com.google.ai.client.generativeai.BuildConfig
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun CalendarView(
    itinerary: Itinerary?,
    currentDay: Int = 1,
    onDaySelected: (Int) -> Unit,
    onEventRatingChanged: (Int, EventRating) -> Unit,
    onEventClicked: (Event) -> Unit,
    viewModel: ChatViewModel
) {
    // Key für die Recomposition erzwingen, wenn sich der Itinerary ändert
    val itineraryKey by remember(itinerary) { mutableStateOf(System.currentTimeMillis()) }

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


            // Debug-Info (kann in Produktion entfernt werden)
            if (BuildConfig.DEBUG && false) { //never print it ;)
                Text(
                    text = "Itinerary ID: $itineraryKey",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
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
            // Tagesauswahl mit key für Recomposition
            key(itineraryKey) {
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
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Aktivitäten des ausgewählten Tages mit key für Recomposition
            key(itineraryKey, currentDay) {
                val selectedDay = itinerary.days.find { it.day == currentDay }
                selectedDay?.let { dayPlan ->
                    val listState = rememberLazyListState()

                    // LazyColumn nimmt nur den verfügbaren Platz ein und wird scrollbar
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(
                            start = 0.dp,
                            top = 0.dp,
                            end = 0.dp,
                            bottom = 200.dp
                        )
                    ) {
                        items(dayPlan.events.size, key = { index -> "${dayPlan.events[index].location}_${dayPlan.events[index].activity}" }) { index ->
                            EventCard(
                                event = dayPlan.events[index],
                                onRatingChanged = { rating ->
                                    onEventRatingChanged(index, rating)
                                },
                                onEventClicked = {
                                    onEventClicked(dayPlan.events[index])
                                },
                                modifier = Modifier.padding(8.dp),
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
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
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (event.completelyLoaded) {
                    Modifier.clickable { onEventClicked() }
                } else {
                    Modifier
                }
            ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = event.time, style = MaterialTheme.typography.bodyMedium)
                Text(text = event.location, style = MaterialTheme.typography.titleMedium)
                Text(text = event.activity, style = MaterialTheme.typography.bodyLarge)

                // Optionaler Ladeindikator
                if (!event.completelyLoaded) {
                    Text(
                        text = "Cargando detalles…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            //Rating buttons nur anzeigen wenn das so soll
            //if (viewModel.shouldShowRatingButtons()) {
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
            //}
        }
    }
}
