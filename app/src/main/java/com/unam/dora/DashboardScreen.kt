package com.unam.dora

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.LocalDate
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DashboardScreen(
    viewModel: ChatViewModel = hiltViewModel(),
    onNavigateToEventDetails: (Event) -> Unit,
    onBackPressed: () -> Unit
) {
    val itinerary by viewModel.itinerary.collectAsState()
    val selectedEvent by viewModel.selectedEvent.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val hasRatingChanges by viewModel.hasRatingChanges.collectAsState()

    var selectedDay by remember { mutableStateOf(1) }
    var chatExpanded by remember { mutableStateOf(false) }

    // Beobachte das ausgewählte Event und navigiere, wenn eines ausgewählt ist
    LaunchedEffect(selectedEvent) {
        selectedEvent?.let {
            onNavigateToEventDetails(it)
            viewModel.clearSelectedEvent()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CalendarView(
            itinerary = itinerary,
            currentDay = selectedDay,
            onDaySelected = { selectedDay = it },
            onEventRatingChanged = { eventIndex, rating ->
                viewModel.updateEventRating(selectedDay, eventIndex, rating)
            },
            onEventClicked = { event ->
                // Hier muss ein Event-Objekt übergeben werden, kein Index
                viewModel.showEventDetails(selectedDay, itinerary?.days?.find { it.day == selectedDay }?.events?.indexOf(event) ?: 0)
            }
        )

        // Chat-Komponente
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            // Zeige "Änderungen anwenden"-Button an, wenn Bewertungen geändert wurden
            AnimatedVisibility(visible = hasRatingChanges) {
                Button(
                    onClick = { viewModel.applyRatingChanges() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Änderungen anwenden")
                }
            }

            ExpandableChatBar(
                messages = messages,
                isExpanded = chatExpanded,
                onExpandToggle = { chatExpanded = !chatExpanded },
                onSendMessage = { message ->
                    viewModel.sendUserMessage(message)
                },
                onMoodSelected = { mood ->
                    viewModel.updateItineraryWithMood(selectedDay, mood)
                }
            )
        }
    }
}