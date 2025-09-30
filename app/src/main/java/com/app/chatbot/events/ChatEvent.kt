package com.app.chatbot.events

import com.app.chatbot.model.Message

sealed interface ChatEvent {
    data class InputChanged(val text: String) : ChatEvent
    object SendClicked : ChatEvent
    data class NewMessages(val messages: List<Message>) : ChatEvent
}
