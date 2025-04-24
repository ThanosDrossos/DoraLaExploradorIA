package com.unam.dora

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.unam.dora.Message
import com.unam.dora.ChatViewModel
import com.unam.dora.MessageBubble
import com.unam.dora.InputBar
import androidx.hilt.navigation.compose.hiltViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ConversationScreen(vm: ChatViewModel = hiltViewModel()) {
    val messages by vm.messages.collectAsState()
    var inputText by remember { mutableStateOf("") }

    Scaffold(
        bottomBar = {
            InputBar(
                text = inputText,
                onTextChange = { inputText = it },
                onSend = {
                    vm.sendUserMessage(inputText)
                    inputText = ""
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(8.dp)
        ) {
            items(messages) { msg ->
                MessageBubble(message = msg)
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}