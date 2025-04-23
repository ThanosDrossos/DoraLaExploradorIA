package com.unam.dora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.unam.dora.ConversationScreen
import com.unam.dora.ui.theme.DoraLaExploradorIATheme
import com.unam.dora.FormScreen
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel
import com.unam.dora.ChatViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DoraLaExploradorIATheme {
                TravelAppNavGraph()
            }
        }
    }
}

@Composable
fun TravelAppNavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "form") {
        composable("form") {
            FormScreen(
                onGenerate = { city, days, moods ->
                    val vm: ChatViewModel = hiltViewModel()
//                  vm.setTripPreferences(city, days, moods)
//                  vm.generateItinerary()
                    navController.navigate("chat") {
                        // clear back stack if needed
                        popUpTo("form")
                    }
                }
            )
        }
        composable("chat") {
            ConversationScreen()
        }
    }
}