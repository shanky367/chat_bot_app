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


/**
 * Unit tests for the [ChatRepository] class.
 *
 * This test class verifies the core functionalities of the [ChatRepository],
 * including message creation (outgoing and incoming), ID generation,
 * message flow updates, reply simulation, clearing messages, and handling
 * various edge cases like empty, long, or special character messages.
 *
 * It utilizes [kotlinx.coroutines.test.runTest] for testing coroutine-based
 * functionalities and ensures proper synchronization and state management.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatRepositoryTest {

    /**
     * Verifies that when a ChatRepository is initialized,
     * its `messages` StateFlow initially contains an empty list.
     * This ensures that there are no pre-existing messages upon repository creation.
     */
    @Test
    fun initial_state_should_have_empty_messages_list() = runTest {
        // Given
        val repository = ChatRepository()

        // When & Then
        val initialMessages = repository.messages.value
        assertTrue(initialMessages.isEmpty())
    }

    /**
     * Tests that sending an outgoing message:
     * 1. Creates a message with the correct text.
     * 2. Sets the `isIncoming` flag to false.
     * 3. Assigns an ID of 1L for the first message.
     * 4. Adds the message to the `messages` StateFlow.
     */
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

    /**
     * Tests that `pushIncoming` creates a new incoming message.
     * It verifies that the created message:
     * - Has an ID of 1 (as it's the first message).
     * - Contains the correct text.
     * - Is marked as incoming.
     * It also checks that the message is correctly added to the `messages` StateFlow.
     */
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

    /**
     * Tests that multiple messages, both outgoing and incoming, are assigned sequential IDs.
     * It sends an outgoing message, then an incoming message, then another outgoing message,
     * and verifies that their IDs are 1, 2, and 3 respectively.
     * It also checks that the total number of messages in the repository is 3.
     */
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

    /**
     * Tests that the `messages` Flow emits updates correctly when new messages are added.
     * It verifies:
     * 1. The initial emission is an empty list.
     * 2. After sending an outgoing message, the flow emits a list containing that message.
     * 3. After pushing an incoming message, the flow emits an updated list containing both messages.
     * The order of messages in the flow emissions is also checked.
     */
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

    /**
     * Tests that `simulateReplyFor` correctly adds an incoming message after the specified delay.
     *
     * This test verifies:
     * - The returned message is marked as incoming (`isIncoming` is true).
     * - The message text matches the input `replyText`.
     * - The message ID is correctly assigned (starts from 1).
     * - The function execution time is at least the specified `delayMs`, indicating the delay occurred.
     * - The new message is present in the `messages` flow.
     */
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

    /**
     * Tests that `simulateReplyFor` uses the default delay (3000ms) when no specific delay is provided.
     * It sends a reply and verifies that:
     * 1. The message is marked as incoming.
     * 2. The message text is correct.
     * 3. The time elapsed between starting the simulation and receiving the message is at least the default delay.
     */
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

    /**
     * Tests that the `clear()` method correctly resets the repository.
     * It first adds a few messages to the repository and verifies their presence.
     * Then, it calls `clear()` and checks:
     * 1. The `messages` StateFlow becomes empty.
     * 2. The internal ID counter is reset, meaning a new message sent after clearing
     *    will have an ID of 1.
     */
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

    /**
     * Tests that messages, when added to the repository (both outgoing and incoming),
     * maintain their order of addition in the `messages` StateFlow.
     * It adds an outgoing, then an incoming, then another outgoing message,
     * and verifies that the `messages` list reflects this exact order.
     */
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

    /**
     * Tests the thread safety of the repository when adding messages concurrently.
     * It launches a large number of coroutines, each adding either an outgoing or
     * an incoming message.
     *
     * It verifies that:
     * 1. The total number of messages in the repository matches the number of
     *    coroutines launched.
     * 2. All message IDs are unique, ensuring that the ID generation and message
     *    addition process is atomic and correctly synchronized across multiple threads.
     */
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

    /**
     * Tests that sending and receiving empty text messages are handled correctly.
     * It verifies that the message text is empty, the `isIncoming` flag is set correctly,
     * and the messages are added to the repository's message list.
     */
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

    /**
     * Tests that the repository correctly handles sending messages with very long text content.
     * It verifies that the message text is preserved, the length is correct,
     * and the message is added to the repository.
     */
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

    /**
     * Tests that special characters, including emojis and symbols, are correctly preserved
     * when creating incoming messages.
     * It sends an incoming message containing various special characters and verifies that
     * the text of the created message and the text of the message stored in the repository
     * are identical to the original input string.
     */
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
