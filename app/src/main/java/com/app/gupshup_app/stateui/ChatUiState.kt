package com.app.gupshup_app.stateui

import com.app.gupshup_app.model.Message


data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isSending: Boolean = false
)
