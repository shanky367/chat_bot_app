package com.app.chatbot.events

import com.app.chatbot.model.Message

/**
 * Represents the different events that can occur within the chat screen.
 * These events are used to communicate user interactions and data updates
 * between the UI and the ViewModel.
 */
sealed interface ChatEvent {
    data class InputChanged(val text: String) : ChatEvent
    object SendClicked : ChatEvent
    data class NewMessages(val messages: List<Message>) : ChatEvent
}
