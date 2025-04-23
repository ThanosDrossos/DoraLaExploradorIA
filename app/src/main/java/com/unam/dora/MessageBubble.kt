package com.unam.dora

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.unam.dora.Message
import com.unam.dora.Sender
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text

@Composable
fun MessageBubble(message: Message) {
    val isUser = message.sender == Sender.USER
    val backgroundColor = if (isUser)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isUser)
        MaterialTheme.colorScheme.onPrimary
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            tonalElevation = 2.dp,
            shadowElevation = 1.dp,
            shape = RoundedCornerShape(16.dp),
            color = backgroundColor,
            contentColor = contentColor,
            modifier = Modifier
                .padding(4.dp)
                .widthIn(max = 260.dp)
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}