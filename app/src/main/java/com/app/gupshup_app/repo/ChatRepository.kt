package com.app.gupshup_app.repo


import com.app.gupshup_app.model.Message
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong


class ChatRepository {
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()

    private val idCounter = AtomicLong(0L)//Atomic ID generation

    fun sendOutgoing(text: String): Message {
        val msg = Message(id = idCounter.incrementAndGet(), text = text, isIncoming = false)
        _messages.value = _messages.value + msg
        return msg
    }

    fun pushIncoming(text: String): Message {
        val msg = Message(id = idCounter.incrementAndGet(), text = text, isIncoming = true)
        _messages.value = _messages.value + msg
        return msg
    }

    suspend fun simulateReplyFor(text: String, replyDelayMs: Long = 3000L): Message {
        delay(replyDelayMs)
        return pushIncoming(text)
    }

    // Clear repository (for tests)
    fun clear() {
        _messages.value = emptyList()
        idCounter.set(0L)
    }
}

