package com.app.chatbot.model

import kotlinx.datetime.Clock

/**
 * Represents a chat message.
 *
 * @property id The unique identifier for the message.
 * @property text The content of the message.
 * @property isIncoming True if the message is from the chatbot, false if it's from the user.
 * @property createdAtMs The timestamp when the message was created, in milliseconds since the epoch.
 *                       Defaults to the current system time.
 */
data class Message(
    val id: Long,
    val text: String,
    val isIncoming: Boolean,
    val createdAtMs: Long = Clock.System.now().toEpochMilliseconds()
)