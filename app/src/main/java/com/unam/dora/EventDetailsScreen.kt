package com.unam.dora

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    var isLoading by remember { mutableStateOf(false) }
    val selectedEvent by viewModel.selectedEvent.collectAsState()
    var currentEvent by remember { mutableStateOf(event) }

    // Details nachladen
    LaunchedEffect(Unit) {
        Log.d("EventDetailScreen", "Initial event: ${event.imagePath}")
        Log.d("EventDetailScreen", "Initial selectedEvent: ${selectedEvent?.imagePath}")
        Log.d("EventDetailScreen", "Checking if details need to be loaded...")
        if (currentEvent.description.isBlank() || currentEvent.visitorInfo.isBlank() ||
            currentEvent.imagePath.isNullOrBlank()
        ) {
            isLoading = true
            //maybe add after loading here
            Log.d("EventDetailScreen", "Event details got updated!")
            isLoading = false
        }
    }

    LaunchedEffect(selectedEvent) {
        if (selectedEvent?.location == event.location &&
            selectedEvent?.activity == event.activity) {
            currentEvent = selectedEvent!!
            Log.d("EventDetailScreen", "Event wurde aktualisiert: ${selectedEvent?.imagePath}")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentEvent.location) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        val scrollState = rememberScrollState()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Bild laden (mit Fehlerbehandlung)
            if (currentEvent.completelyLoaded == false) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Log.d("EventDetailScreen", "Loading image for event: ${currentEvent}")
                    if (currentEvent?.completelyLoaded == false) {
                        CircularProgressIndicator()
                    } else if (currentEvent.imagePath != null) {
                        Log.d(
                            "EventDetailScreen",
                            "Image path for rendering: ${currentEvent.imagePath}"
                        )
                        currentEvent.imagePath?.let { path ->
                            val file = File(path)
                            if (file.exists()) {
                                AsyncImage(
                                    model = file,
                                    contentDescription = currentEvent.activity,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text("Bild konnte nicht geladen werden")
                                // Hier könnte man auch ein Neuladen auslösen
                            }
                        }
                    } else {
                        Log.d("EventDetailScreen", "No image path available")
                        Text("No hay foto")
                    }
                }
            } else if (currentEvent.imagePath != null) {
                Log.d(
                    "EventDetailScreen",
                    "Image path for rendering: ${currentEvent.imagePath}"
                )
                currentEvent.imagePath?.let { path ->
                    val file = File(path)
                    if (file.exists()) {
                        AsyncImage(
                            model = file,
                            contentDescription = currentEvent.activity,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Text(
                text = currentEvent.activity,
                style = MaterialTheme.typography.headlineSmall
            )

            // Zeit anzeigen
            Text(
                text = "Hora: ${currentEvent.time}",
                style = MaterialTheme.typography.bodyLarge
            )

            // Beschreibung mit Neuladen-Funktion
            if (currentEvent.description.isNotBlank()) {
                Text(
                    text = "Descripción",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(currentEvent.description)
            } else if (!isLoading) {
                Button(onClick = { viewModel.loadEventDetailsIfMissing(currentEvent) }) {
                    Text("Cargar detalles")
                }
            }

            // Besucherinfo mit Neuladen-Funktion
            if (currentEvent.visitorInfo.isNotBlank()) {
                Text(
                    text = "Información para visitantes",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(currentEvent.visitorInfo)
            }
        }
    }
}
