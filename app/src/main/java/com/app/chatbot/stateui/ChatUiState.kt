package com.app.chatbot.stateui

import com.app.chatbot.model.Message


/**
 * Represents the UI state for the chat screen.
 *
 * @property messages The list of messages in the chat.
 * @property inputText The current text entered by the user in the input field.
 * @property isSending A boolean indicating whether a message is currently being sent.
 */
data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isSending: Boolean = false
)
