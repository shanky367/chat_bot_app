package com.app.chatbot.repo


import com.app.chatbot.model.Message
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong


/**
 * Repository for managing chat messages.
 *
 * This class handles the storage and retrieval of chat messages,
 * as well as simulating incoming messages and clearing the chat history.
 */
class ChatRepository {
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()

    private val idCounter = AtomicLong(0L)

    /**
     * Sends an outgoing message from the user.
     * @param text The text content of the outgoing message.
     * @return The [Message] object that was created and sent.
     */
    fun sendOutgoing(text: String): Message {
        val msg = Message(id = idCounter.incrementAndGet(), text = text, isIncoming = false)
        _messages.value = _messages.value + msg
        return msg
    }

    /**
     * Pushes an incoming message to the chat.
     *
     * This function creates a new [Message] object with the provided text, marks it as incoming,
     * assigns it a unique ID, adds it to the list of messages, and then returns the created message.
     *
     * @param text The text content of the incoming message.
     * @return The newly created [Message] object representing the incoming message.
     */
    fun pushIncoming(text: String): Message {
        val msg = Message(id = idCounter.incrementAndGet(), text = text, isIncoming = true)
        _messages.value = _messages.value + msg
        return msg
    }

    /**
     * Simulates receiving a reply from the chatbot after a specified delay.
     *
     * @param text The content of the simulated incoming message.
     * @param replyDelayMs The delay in milliseconds before the reply is "received".
     *                     Defaults to 3000ms (3 seconds).
     * @return The [Message] object representing the simulated incoming reply.
     */
    suspend fun simulateReplyFor(text: String, replyDelayMs: Long = 3000L): Message {
        delay(replyDelayMs)
        return pushIncoming(text)
    }

    /**
     * Clears all messages from the repository and resets the ID counter.
     */
    fun clear() {
        _messages.value = emptyList()
        idCounter.set(0L)
    }
}

