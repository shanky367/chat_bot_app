package com.app.gupshup_app.events

import com.app.gupshup_app.model.Message

sealed interface ChatEvent {
    data class InputChanged(val text: String) : ChatEvent
    object SendClicked : ChatEvent
    data class NewMessages(val messages: List<Message>) : ChatEvent
}
