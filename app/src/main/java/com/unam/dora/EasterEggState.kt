package com.unam.dora

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object EasterEggState {
    var isHackerThemeEnabled by mutableStateOf(false)
    var showSplash by mutableStateOf(false)
}