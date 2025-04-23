// app/src/main/java/com/example/travelassistant/data/Message.kt
package com.unam.dora

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: Sender,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class Sender {
    USER,
    ASSISTANT
}
