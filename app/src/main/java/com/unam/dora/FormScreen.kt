package com.unam.dora

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlin.collections.remove
import androidx.compose.ui.text.style.TextAlign
import kotlin.collections.remove
import kotlin.math.roundToInt
import kotlin.text.chunked
import kotlin.text.forEach


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FormScreen(
    onGenerate: (city: String, days: Int, moods: List<String>) -> Unit
) {

    var city by remember { mutableStateOf("") }
    var days by remember { mutableStateOf(1f) }

    // Easter Egg Überprüfung
    LaunchedEffect(city) {
        if (city.trim() == "Mochila Mochila") {
            EasterEggState.showSplash = true
            EasterEggState.isHackerThemeEnabled = true
            city = ""  // Setzt das Eingabefeld zurück
        }
    }

    val moodOptions = listOf(
        "Relajación 😊" to "relaxation",
        "Arte 🎨" to "art",
        "Comida 🍕" to "food",
        "Historia 🏛️" to "history",
        "Playa 🏖️" to "beach",
        "Vida nocturna 🎉" to "nightlife"
    )


    val selectedMoods = remember { mutableStateListOf<String>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Planifica tu viaje", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = city,
            onValueChange = { city = it },
            label = { Text("Ciudad de destino") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            )
        )

        Column {
            Text("Duración: ${days.roundToInt()} días")
            Slider(
                value = days,
                onValueChange = { newValue ->
                    // Diskretisiere den Wert direkt auf ganze Zahlen
                    days = newValue.roundToInt().toFloat()
                },
                valueRange = 1f..7f,
                // Entferne steps Parameter, da wir manuell diskretisieren
                modifier = Modifier.fillMaxWidth()
            )
        }

        Text("Seleccionar intereses:")
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            moodOptions.chunked(3).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    rowItems.forEach { (displayText, apiValue) ->
                        val isSelected = apiValue in selectedMoods
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) selectedMoods.remove(apiValue)
                                else selectedMoods.add(apiValue)
                            },
                            label = {
                                Text(
                                    text = displayText,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                        )
                    }
                    // Fülle leere Plätze in der letzten Reihe auf
                    repeat(3 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                onGenerate(city.trim(), days.toInt(), selectedMoods.toList())
            },
            enabled = city.isNotBlank() && selectedMoods.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Generar Plan")
        }
    }
}