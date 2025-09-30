package com.app.chatbot.repo

import com.app.chatbot.model.Message
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Test


@OptIn(ExperimentalCoroutinesApi::class)
class ChatRepositoryTest {

    @Test
    fun initial_state_should_have_empty_messages_list() = runTest {
        // Given
        val repository = ChatRepository()

        // When & Then
        val initialMessages = repository.messages.value
        assertTrue(initialMessages.isEmpty())
    }

    @Test
    fun sendOutgoing_should_create_outgoing_message_with_incremented_id() = runTest {
        // Given
        val repository = ChatRepository()
        val messageText = "Hello, world!"

        // When
        val message = repository.sendOutgoing(messageText)

        // Then
        assertEquals(1L, message.id)
        assertEquals(messageText, message.text)
        assertFalse(message.isIncoming)

        // Verify it's added to messages flow
        val messages = repository.messages.value
        assertEquals(1, messages.size)
        assertEquals(message, messages.first())
    }

    @Test
    fun pushIncoming_should_create_incoming_message_with_incremented_id() = runTest {
        // Given
        val repository = ChatRepository()
        val messageText = "Hi there!"

        // When
        val message = repository.pushIncoming(messageText)

        // Then
        assertEquals(1L, message.id)
        assertEquals(messageText, message.text)
        assertTrue(message.isIncoming)

        // Verify it's added to messages flow
        val messages = repository.messages.value
        assertEquals(1, messages.size)
        assertEquals(message, messages.first())
    }

    @Test
    fun multiple_messages_should_have_sequential_ids() = runTest {
        // Given
        val repository = ChatRepository()

        // When
        val outgoing1 = repository.sendOutgoing("Message 1")
        val incoming1 = repository.pushIncoming("Reply 1")
        val outgoing2 = repository.sendOutgoing("Message 2")

        // Then
        assertEquals(1L, outgoing1.id)
        assertEquals(2L, incoming1.id)
        assertEquals(3L, outgoing2.id)

        val messages = repository.messages.value
        assertEquals(3, messages.size)
    }

    @Test
    fun messages_flow_should_emit_updates_when_messages_are_added() = runTest {
        // Given
        val repository = ChatRepository()
        val collectedMessages = mutableListOf<List<Message>>()


        // Collect messages from flow
        val job = launch {
            repository.messages.collect { messages ->
                collectedMessages.add(messages)
            }
        }

        // Wait for initial emission
        advanceUntilIdle()

        // When
        repository.sendOutgoing("First message")
        advanceUntilIdle()

        repository.pushIncoming("Second message")
        advanceUntilIdle()

        // Give some time for flow to emit
        advanceUntilIdle()

        // Then
        assertEquals(3, collectedMessages.size) // Initial empty + 2 updates
        assertEquals(0, collectedMessages[0].size) // Initial empty
        assertEquals(1, collectedMessages[1].size) // After first message
        assertEquals(2, collectedMessages[2].size) // After second message

        job.cancel()
    }

    @Test
    fun simulateReplyFor_should_add_incoming_message_after_delay() = runTest {
        // Given
        val repository = ChatRepository()
        val replyText = "Simulated reply"
        val delayMs = 100L

        // When
        val startTime = currentTime
        val message = repository.simulateReplyFor(replyText, delayMs)
        val endTime = currentTime

        // Then
        assertTrue(message.isIncoming)
        assertEquals(replyText, message.text)
        assertEquals(1L, message.id)

        // Verify delay occurred (allowing some tolerance for test execution time)
        assertTrue(endTime - startTime >= delayMs)

        // Verify message is in flow
        val messages = repository.messages.value
        assertEquals(1, messages.size)
        assertEquals(message, messages.first())
    }

    @Test
    fun simulateReplyFor_should_use_default_delay_when_not_specified() = runTest {
        // Given
        val repository = ChatRepository()
        val replyText = "Default delay reply"

        // When
        val startTime = currentTime
        val message = repository.simulateReplyFor(replyText) // Using default 3000ms
        val endTime = currentTime

        // Then
        assertTrue(message.isIncoming)
        assertEquals(replyText, message.text)

        // Verify delay occurred (default is 3000ms)
        assertTrue(endTime - startTime >= 3000L)
    }

    @Test
    fun clear_should_reset_messages_and_id_counter() = runTest {
        // Given
        val repository = ChatRepository()

        // Add some messages first
        repository.sendOutgoing("Message 1")
        repository.pushIncoming("Message 2")
        repository.sendOutgoing("Message 3")

        // Verify messages exist
        assertEquals(3, repository.messages.value.size)

        // When
        repository.clear()

        // Then
        assertTrue(repository.messages.value.isEmpty())

        // Verify id counter is reset
        val newMessage = repository.sendOutgoing("After clear")
        assertEquals(1L, newMessage.id) // Should start from 1 again
    }

    @Test
    fun messages_should_maintain_order_when_added() = runTest {
        // Given
        val repository = ChatRepository()

        // When
        val msg1 = repository.sendOutgoing("First")
        val msg2 = repository.pushIncoming("Second")
        val msg3 = repository.sendOutgoing("Third")

        // Then
        val messages = repository.messages.value
        assertEquals(3, messages.size)
        assertEquals(msg1, messages[0])
        assertEquals(msg2, messages[1])
        assertEquals(msg3, messages[2])
    }

    @Test
    fun concurrent_message_additions_should_maintain_thread_safety() = runTest {
        // Given
        val repository = ChatRepository()
        val numberOfConcurrentMessages = 100

        // When - Launch multiple coroutines concurrently
        val jobs = (1..numberOfConcurrentMessages).map { index ->
            launch {
                if (index % 2 == 0) {
                    repository.sendOutgoing("Outgoing $index")
                } else {
                    repository.pushIncoming("Incoming $index")
                }
            }
        }

        // Wait for all jobs to complete
        jobs.forEach { it.join() }

        // Then
        val messages = repository.messages.value
        assertEquals(numberOfConcurrentMessages, messages.size)

        // Verify all IDs are unique
        val ids = messages.map { it.id }.toSet()
        assertEquals(numberOfConcurrentMessages, ids.size)
    }

    @Test
    fun empty_text_messages_should_be_handled_correctly() = runTest {
        // Given
        val repository = ChatRepository()

        // When
        val outgoing = repository.sendOutgoing("")
        val incoming = repository.pushIncoming("")

        // Then
        assertEquals("", outgoing.text)
        assertEquals("", incoming.text)
        assertFalse(outgoing.isIncoming)
        assertTrue(incoming.isIncoming)

        assertEquals(2, repository.messages.value.size)
    }

    @Test
    fun very_long_text_messages_should_be_handled_correctly() = runTest {
        // Given
        val repository = ChatRepository()
        val longText = "A".repeat(10000) // Very long string

        // When
        val message = repository.sendOutgoing(longText)

        // Then
        assertEquals(longText, message.text)
        assertEquals(longText.length, message.text.length)
        assertEquals(1, repository.messages.value.size)
    }

    @Test
    fun special_characters_in_messages_should_be_preserved() = runTest {
        // Given
        val repository = ChatRepository()
        val specialText = "Hello! 👋 This has émojis and spëcial chars: @#$%^&*()"

        // When
        val message = repository.pushIncoming(specialText)

        // Then
        assertEquals(specialText, message.text)
        assertEquals(specialText, repository.messages.value.first().text)
    }
}
