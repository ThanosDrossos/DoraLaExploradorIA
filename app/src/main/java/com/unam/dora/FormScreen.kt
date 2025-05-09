package com.unam.dora

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlin.text.toFloat
import kotlin.text.toInt


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
        "Relaxation 😊" to "relaxation",
        "Art 🎨" to "art",
        "Foodie 🍕" to "food",
        "History 🏛️" to "history",
        "Beach 🏖️" to "beach",
        "Nightlife 🎉" to "nightlife"
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
            label = { Text("Destination City") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            )
        )

        Column {
            Text("Duración: ${days.roundToInt()} days")
            Slider(
                value = days,
                onValueChange = { newValue ->
                    // Diskretisiere den Wert direkt auf ganze Zahlen
                    days = newValue.roundToInt().toFloat()
                },
                valueRange = 1f..14f,
                // Entferne steps Parameter, da wir manuell diskretisieren
                modifier = Modifier.fillMaxWidth()
            )
        }

        Text("Seleccionar intereses:")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(moodOptions.size) { idx ->
                val (displayText, apiValue) = moodOptions[idx]
                val isSelected = apiValue in selectedMoods
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        if (isSelected) selectedMoods.remove(apiValue)
                        else selectedMoods.add(apiValue)
                    },
                    label = { Text(displayText) }
                )
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
            Text("Generate Plan")
        }
    }
}