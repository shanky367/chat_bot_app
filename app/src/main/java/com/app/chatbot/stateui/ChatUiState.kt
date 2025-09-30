package com.app.chatbot.stateui

import com.app.chatbot.model.Message


data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isSending: Boolean = false
)
