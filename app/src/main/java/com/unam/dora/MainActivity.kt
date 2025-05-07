package com.unam.dora

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.unam.dora.ui.theme.DoraLaExploradorIATheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.unam.dora.ui.theme.ThemeDark
import androidx.compose.runtime.*

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            if (EasterEggState.showSplash) {
                HackerSplashScreen {
                    EasterEggState.showSplash = false
                }
            } else {
                if (EasterEggState.isHackerThemeEnabled) {
                    ThemeDark {
                        TravelAppNavGraph()
                    }
                } else {
                    DoraLaExploradorIATheme {
                        TravelAppNavGraph()
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TravelAppNavGraph() {
    val navController = rememberNavController()
    val sharedViewModel: ChatViewModel = hiltViewModel()

    Surface(                                      // Neue Surface hier
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        NavHost(navController = navController, startDestination = "form") {
            composable("form") {
                FormScreen(
                    onGenerate = { city, days, moods ->
                        sharedViewModel.setTripPreferences(city, days, moods)
                        sharedViewModel.generateItinerary()
                        navController.navigate("dashboard") {
                            popUpTo("form") { inclusive = true }
                        }
                    }
                )
            }
            composable("dashboard") {
                DashboardScreen(viewModel = sharedViewModel)
            }
            composable("chat") {
                ConversationScreen(vm = sharedViewModel)
            }
        }
    }
}