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

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DoraLaExploradorIATheme {
                TravelAppNavGraph()
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TravelAppNavGraph() {
    val navController = rememberNavController()
    // Gemeinsames ViewModel für alle Screens
    val sharedViewModel: ChatViewModel = hiltViewModel()

    NavHost(navController = navController, startDestination = "form") {
        composable("form") {
            FormScreen(
                onGenerate = { city, days, moods ->
                    sharedViewModel.setTripPreferences(city, days, moods)
                    sharedViewModel.generateItinerary()
                    navController.navigate("dashboard") {
                        popUpTo("form") { inclusive = true }  // remove form from backstack
                    }
                }
            )
        }
        composable("dashboard") {
            // Gemeinsames ViewModel weitergeben
            DashboardScreen(viewModel = sharedViewModel)
        }
        composable("chat") {
            // Gemeinsames ViewModel weitergeben
            ConversationScreen(vm = sharedViewModel)
        }
    }
}