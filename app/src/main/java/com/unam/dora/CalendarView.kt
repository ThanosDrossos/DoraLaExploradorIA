package com.unam.dora

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    onDaySelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
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

        // Tabulatoren für Tagesauswahl
        if (itinerary != null && itinerary.days.isNotEmpty()) {
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
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(dayPlan.events) { event ->
                        EventCard(event = event)
                    }
                    item {
                        Spacer(modifier = Modifier.height(150.dp))
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
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
fun EventCard(event: Event) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = event.time,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = event.location,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = event.activity,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}