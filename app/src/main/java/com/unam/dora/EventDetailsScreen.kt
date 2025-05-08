package com.unam.dora

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.unam.dora.Event
import kotlinx.coroutines.delay
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    event: Event,
    onBackPressed: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    // Zustand für das Nachladen von Details
    var isLoading by remember { mutableStateOf(false) }

    val currentItinerary by viewModel.itinerary.collectAsState()

    var currentEvent by remember { mutableStateOf(event) }

    // Auf fehlende Details prüfen und ggf. nachladen
    LaunchedEffect(currentItinerary) {
        val updatedEvent = currentItinerary?.days?.flatMap { it.events }
            ?.find { it.location == event.location && it.activity == event.activity }

        updatedEvent?.let {
            currentEvent = it
        }
    }

    // Details bei Bedarf nachladen
    LaunchedEffect(currentEvent) {
        if (currentEvent.description.isBlank() || currentEvent.visitorInfo.isBlank() ||
            currentEvent.imagePath.isNullOrBlank()) {
            isLoading = true
            viewModel.loadEventDetailsIfMissing(currentEvent)
            delay(300) // Kurze Verzögerung für Ladeindikator
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(event.location) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Bild laden (mit Fehlerbehandlung)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator()
                } else if (event.imagePath != null) {
                    event.imagePath?.let { path ->
                        val file = File(path)
                        if (file.exists()) {
                            AsyncImage(
                                model = file,
                                contentDescription = event.activity,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text("Bild konnte nicht geladen werden")
                            // Hier könnte man auch ein Neuladen auslösen
                        }
                    }
                } else {
                    Text("Kein Bild verfügbar")
                }
            }

            Text(
                text = event.activity,
                style = MaterialTheme.typography.headlineSmall
            )

            // Zeit anzeigen
            Text(
                text = "Zeit: ${event.time}",
                style = MaterialTheme.typography.bodyLarge
            )

            // Beschreibung mit Neuladen-Funktion
            if (event.description.isNotBlank()) {
                Text(
                    text = "Beschreibung",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(event.description)
            } else if (!isLoading) {
                Button(onClick = { viewModel.loadEventDetailsIfMissing(event) }) {
                    Text("Beschreibung laden")
                }
            }

            // Besucherinfo mit Neuladen-Funktion
            if (event.visitorInfo.isNotBlank()) {
                Text(
                    text = "Besucherinformation",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(event.visitorInfo)
            }
        }
    }
}