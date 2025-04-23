package com.unam.dora.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A form to collect city, days and mood selections.
 * onGenerate is a **normal** callback, not a @Composable lambda.
 */
@Composable
fun FormScreen(
    onGenerate: (city: String, days: Int, moods: List<String>) -> Unit
) {
    var city by remember { mutableStateOf("") }
    var days by remember { mutableStateOf(1f) }
    val moodOptions = listOf(
        "Relaxation 😊", "Art 🎨", "Foodie 🍕",
        "History 🏛️", "Beach 🏖️", "Nightlife 🎉"
    )
    val selectedMoods = remember { mutableStateListOf<String>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Plan Your Trip", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = city,
            onValueChange = { city = it },
            label = { Text("Destination City") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Column {
            Text("Duration: ${days.toInt()} days")
            Slider(
                value = days,
                onValueChange = { days = it.coerceIn(1f, 14f) },
                valueRange = 1f..14f,
                steps = 13,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Text("Select your interests:")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(moodOptions.size) { idx ->
                val mood = moodOptions[idx]
                val isSelected = mood in selectedMoods
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        if (isSelected) selectedMoods.remove(mood)
                        else selectedMoods.add(mood)
                    },
                    label = { Text(mood) }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                // directly invoke the callback from within a @Composable scope
                onGenerate(city.trim(), days.toInt(), selectedMoods.toList())
            },
            enabled = city.isNotBlank() && selectedMoods.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Generate Plan")
        }
    }
}
