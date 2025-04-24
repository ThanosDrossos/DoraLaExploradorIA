package com.unam.dora

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.LocalDate

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DashboardScreen(
    viewModel: ChatViewModel = hiltViewModel()
) {
    val itinerary by viewModel.itinerary.collectAsState()
    val messages by viewModel.messages.collectAsState()

    var selectedDay by remember { mutableStateOf(1) }
    var chatExpanded by remember { mutableStateOf(false) }

    // Reset selectedDay wenn benötigt
    LaunchedEffect(itinerary) {
        if (itinerary?.days?.none { it.day == selectedDay } == true) {
            selectedDay = 1
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Kalender-/Listenansicht mit debug-Informationen
        Column(modifier = Modifier.fillMaxSize()) {
            if (itinerary == null) {
                Text(
                    text = "No hay itinerario disponible.",
                    modifier = Modifier.padding(16.dp)
                )
            }

            CalendarView(
                itinerary = itinerary,
                currentDay = selectedDay,
                onDaySelected = { selectedDay = it }
            )
        }

        // Erweiterbare Chat-Leiste am unteren Rand
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
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