package com.app.chatbot.model

import kotlinx.datetime.Clock

data class Message(
    val id: Long,
    val text: String,
    val isIncoming: Boolean,
    val createdAtMs: Long = Clock.System.now().toEpochMilliseconds()
)