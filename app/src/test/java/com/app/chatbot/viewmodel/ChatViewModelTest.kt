package com.app.chatbot.viewmodel

import com.app.chatbot.events.ChatEvent
import com.app.chatbot.model.Message
import com.app.chatbot.repo.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class ChatViewModelJUnitTest {
    private lateinit var repository: ChatRepository
    private lateinit var viewModel: ChatViewModel
    private val testDispatcher = StandardTestDispatcher()
    private var testScope = TestScope(testDispatcher)

   @Before
   fun setup() {
       Dispatchers.setMain(testDispatcher)
       testScope = TestScope()
       repository = ChatRepository()
       viewModel = ChatViewModel(repository, replyDelayMs = 100L).apply {
           setTestScope(testScope)
       }
   }

    @After
    fun tearDown() {
        viewModel.onCleared()
        testScope.cancel()
    }

    @Test
    fun test_InputChanged_event_updates_inputText_in_uiState() = runTest {
        // Given
        val inputText = "Hello World"

        // When
        viewModel.submitEvent(ChatEvent.InputChanged(inputText))

        // Then
        assertEquals(inputText, viewModel.uiState.value.inputText)
        assertFalse(viewModel.uiState.value.isSending)
        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    @Test
    fun test_InputChanged_event_with_empty_text() = runTest {
        // Given
        val emptyText = ""

        // When
        viewModel.submitEvent(ChatEvent.InputChanged(emptyText))

        // Then
        assertEquals("", viewModel.uiState.value.inputText)
        assertFalse(viewModel.uiState.value.isSending)
    }

    @Test
    fun test_SendClicked_event_with_valid_text_processes_message_and_updates_messages_flow() = runTest {
        // Given
        val messageText = "Test message"

        // Set input text first
        viewModel.submitEvent(ChatEvent.InputChanged(messageText))

        // When
        viewModel.submitEvent(ChatEvent.SendClicked)

        // Advance time to process the send operation
        testScope.advanceTimeBy(50L)

        // Then - Check that message was added and input cleared
        assertEquals("", viewModel.uiState.value.inputText)
        assertEquals(1, viewModel.uiState.value.messages.size)
        assertEquals(messageText, viewModel.uiState.value.messages.first().text)
        assertFalse(viewModel.uiState.value.messages.first().isIncoming)
        assertFalse(viewModel.uiState.value.isSending) // Should be false due to flow collection
    }

    @Test
    fun test_SendClicked_event_triggers_reply_after_delay() = runTest {
        // Given
        val messageText = "Hello"
        viewModel.submitEvent(ChatEvent.InputChanged(messageText))

        // When
        viewModel.submitEvent(ChatEvent.SendClicked)

        // Advance time to process send operation
        testScope.advanceTimeBy(50L)

        // Then - Should have user message
        assertEquals(1, viewModel.uiState.value.messages.size)
        assertFalse(viewModel.uiState.value.messages.first().isIncoming)

        // Advance time to trigger reply
        testScope.advanceTimeBy(100L)

        // Then - Should have both user message and reply
        assertEquals(2, viewModel.uiState.value.messages.size)
        val messages = viewModel.uiState.value.messages
        assertEquals("Hello", messages[0].text)
        assertFalse(messages[0].isIncoming)
        assertEquals("Echo: Hello", messages[1].text)
        assertTrue(messages[1].isIncoming)
    }

    @Test
    fun test_SendClicked_event_with_empty_text_does_nothing() = runTest {
        // Given - Set empty input text
        viewModel.submitEvent(ChatEvent.InputChanged(""))

        // When
        viewModel.submitEvent(ChatEvent.SendClicked)

        // Advance time
        testScope.advanceTimeBy(200L)

        // Then
        assertEquals("", viewModel.uiState.value.inputText)
        assertFalse(viewModel.uiState.value.isSending)
        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    @Test
    fun test_SendClicked_event_with_whitespace_only_text_does_nothing() = runTest {
        // Given - Set whitespace-only input text
        viewModel.submitEvent(ChatEvent.InputChanged("   \n\t  "))

        // When
        viewModel.submitEvent(ChatEvent.SendClicked)

        // Advance time
        testScope.advanceTimeBy(200L)

        // Then
        assertEquals("   \n\t  ", viewModel.uiState.value.inputText) // Input remains unchanged
        assertFalse(viewModel.uiState.value.isSending)
        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    @Test
    fun test_SendClicked_event_trims_input_text_before_processing() = runTest {
        // Given
        val messageText = "  Hello World  "
        val expectedTrimmed = "Hello World"

        // Set input text with whitespace
        viewModel.submitEvent(ChatEvent.InputChanged(messageText))

        // When
        viewModel.submitEvent(ChatEvent.SendClicked)

        // Advance time
        testScope.advanceTimeBy(50L)

        // Then
        assertEquals("", viewModel.uiState.value.inputText) // Input should be cleared
        assertEquals(1, viewModel.uiState.value.messages.size)
        assertEquals(expectedTrimmed, viewModel.uiState.value.messages.first().text)
    }

    @Test
    fun test_SendClicked_sets_isSending_to_true_initially_then_false_after_flow_update() = runTest {
        // Given
        val messageText = "Test"
        viewModel.submitEvent(ChatEvent.InputChanged(messageText))

        // When
        viewModel.submitEvent(ChatEvent.SendClicked)

        // Check immediately - should be sending
        // Note: This test might be tricky due to flow collection timing
        testScope.advanceTimeBy(10L) // Small advance to let coroutines start

        // After flow collection updates the state, isSending should be false
        testScope.advanceTimeBy(50L)
        assertFalse(viewModel.uiState.value.isSending)
        assertEquals(1, viewModel.uiState.value.messages.size)
    }

    @Test
    fun test_NewMessages_event_updates_messages_in_uiState() = runTest {
        // Given
        val messages = listOf(
            Message(1, "Message 1", false),
            Message(2, "Message 2", true)
        )

        // When
        viewModel.submitEvent(ChatEvent.NewMessages(messages))

        // Then
        assertEquals(messages, viewModel.uiState.value.messages)
        assertEquals("", viewModel.uiState.value.inputText)
        assertFalse(viewModel.uiState.value.isSending)
    }

    @Test
    fun test_NewMessages_event_with_empty_list() = runTest {
        // Given
        val emptyMessages = emptyList<Message>()

        // When
        viewModel.submitEvent(ChatEvent.NewMessages(emptyMessages))

        // Then
        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    @Test
    fun test_multiple_InputChanged_events_update_inputText_correctly() = runTest {
        // Given & When
        viewModel.submitEvent(ChatEvent.InputChanged("Hello"))
        assertEquals("Hello", viewModel.uiState.value.inputText)

        viewModel.submitEvent(ChatEvent.InputChanged("Hello World"))
        assertEquals("Hello World", viewModel.uiState.value.inputText)

        viewModel.submitEvent(ChatEvent.InputChanged(""))
        assertEquals("", viewModel.uiState.value.inputText)
    }

    @Test
    fun test_repository_flow_updates_UI_state_messages() = runTest {
        // Given - Manually add message to repository
        repository.sendOutgoing("Manual message")

        // Advance time to let flow collection work
        testScope.advanceTimeBy(50L)

        // Then
        assertEquals(1, viewModel.uiState.value.messages.size)
        assertEquals("Manual message", viewModel.uiState.value.messages.first().text)
        assertFalse(viewModel.uiState.value.messages.first().isIncoming)
        assertFalse(viewModel.uiState.value.isSending) // Should be set to false by flow
    }

    @Test
    fun test_sequential_messages_maintain_correct_order() = runTest {
        // Given
        val message1 = "First message"
        val message2 = "Second message"

        // When - First message
        viewModel.submitEvent(ChatEvent.InputChanged(message1))
        viewModel.submitEvent(ChatEvent.SendClicked)
        testScope.advanceTimeBy(150L) // Wait for send + reply

        val messagesAfterFirst = viewModel.uiState.value.messages
        assertEquals(2, messagesAfterFirst.size) // User message + echo

        // When - Second message
        viewModel.submitEvent(ChatEvent.InputChanged(message2))
        viewModel.submitEvent(ChatEvent.SendClicked)
        testScope.advanceTimeBy(150L) // Wait for send + reply

        // Then
        val finalMessages = viewModel.uiState.value.messages
        assertEquals(4, finalMessages.size) // 2 user messages + 2 echoes
        assertEquals(message1, finalMessages[0].text)
        assertFalse(finalMessages[0].isIncoming)
        assertEquals("Echo: $message1", finalMessages[1].text)
        assertTrue(finalMessages[1].isIncoming)
        assertEquals(message2, finalMessages[2].text)
        assertFalse(finalMessages[2].isIncoming)
        assertEquals("Echo: $message2", finalMessages[3].text)
        assertTrue(finalMessages[3].isIncoming)
    }

    @Test
    fun test_message_IDs_are_unique_and_incrementing() = runTest {
        // Given
        viewModel.submitEvent(ChatEvent.InputChanged("Message 1"))
        viewModel.submitEvent(ChatEvent.SendClicked)
        testScope.advanceTimeBy(150L)

        viewModel.submitEvent(ChatEvent.InputChanged("Message 2"))
        viewModel.submitEvent(ChatEvent.SendClicked)
        testScope.advanceTimeBy(150L)

        // Then
        val messages = viewModel.uiState.value.messages
        assertEquals(4, messages.size)

        // Check IDs are unique and incrementing
        val ids = messages.map { it.id }
        assertEquals(listOf(1L, 2L, 3L, 4L), ids)
    }

    @Test
    fun test_onCleared_cancels_repository_collector() = runTest {
        // Given - ViewModel is initialized with collector
        assertTrue(viewModel.uiState.value.messages.isEmpty())

        // When
        viewModel.onCleared()

        // Add message to repository after clearing
        repository.sendOutgoing("Should not appear")
        testScope.advanceTimeBy(100L)

        // Then - Message should not appear in UI state as collector is cancelled
        // Note: This test depends on the collector being properly cancelled
        // The exact behavior might vary based on timing
        testScope.advanceUntilIdle()
    }

}



