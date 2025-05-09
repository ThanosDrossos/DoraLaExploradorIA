package com.unam.dora

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.LocalDate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar


@OptIn(ExperimentalMaterial3Api::class)
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

    LaunchedEffect(selectedEvent) {
        selectedEvent?.let {
            onNavigateToEventDetails(it)
            viewModel.clearSelectedEvent()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dein Reiseplan") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            CalendarView(
                itinerary = itinerary,
                currentDay = selectedDay,
                onDaySelected = { selectedDay = it },
                onEventRatingChanged = { eventIndex, rating ->
                    viewModel.updateEventRating(selectedDay, eventIndex, rating)
                },
                onEventClicked = { event ->
                    viewModel.showEventDetails(
                        selectedDay,
                        itinerary?.days?.find { it.day == selectedDay }?.events?.indexOf(event) ?: 0
                    )
                }
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Bottom
            ) {
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
}