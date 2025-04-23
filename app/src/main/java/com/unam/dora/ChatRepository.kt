package com.unam.dora

import com.unam.dora.Message
import com.unam.dora.MessageDao
import com.unam.dora.Itinerary
import com.unam.dora.Sender
import com.unam.dora.GeminiApiService
import com.unam.dora.PromptRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ChatRepository(
    private val dao: MessageDao,
    private val api: GeminiApiService
) {
    // Flow of persisted chat messages
    val allMessages: Flow<List<Message>> = dao.getAllMessages()

    suspend fun insertMessage(message: Message) = dao.insertMessage(message)

    /**
     * Calls Gemini API with a structured prompt, returns the Itinerary data class.
     */
    suspend fun fetchItinerary(prompt: String): Itinerary =
        withContext(Dispatchers.IO) {
            // call API, parse response JSON into Itinerary directly
            val response = api.generateItinerary(PromptRequest(prompt))
            response.data
        }
}
