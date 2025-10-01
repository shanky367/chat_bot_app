package com.app.chatbot

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.app.chatbot.model.Message
import com.app.chatbot.stateui.ChatUiState
import com.app.chatbot.ui.screen.ChatContent
import org.junit.Rule
import org.junit.Test

/**
 * Test class for the ChatScreen composable.
 * This class contains UI tests to verify the functionality of the chat screen,
 * including input field behavior, send button states, and message display.
 */
class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Tests that the input field accepts text input and updates the UI state accordingly.
     * It sets up the ChatContent with an initial empty state, simulates typing "Hi" into the input field,
     * and then asserts that the input field's displayed text matches the typed text.
     */
    @Test
    fun inputField_acceptsText() {
        var uiState by mutableStateOf(ChatUiState(
            messages = emptyList(),
            inputText = "",
            isSending = false
        ))

        composeTestRule.setContent {
            ChatContent(
                state = uiState,
                onInputChanged = { newText -> uiState = uiState.copy(inputText = newText) },
                onSend = {}
            )
        }

        val inputText = "Hi"
        composeTestRule.onNodeWithTag("InputField")
            .performTextInput(inputText)

        // Assert that the typed text is displayed in the InputField
        composeTestRule.onNodeWithTag("InputField")
            .assert(hasText(inputText))
    }

    /**
     * Tests that the send button is clickable when it is enabled (i.e., when inputText is not empty and isSending is false).
     * It sets an initial state where the send button should be enabled, then simulates a click on the button.
     * Finally, it asserts that the onSend callback was triggered.
     */
    @Test
    fun sendButton_clickableWhenEnabled() {
        var clicked = false
        // Use a non-mutable initial state for this test as it's simpler
        val initialState = ChatUiState(
            messages = emptyList(),
            inputText = "Hi", // inputText needs to be non-empty for Send to be enabled
            isSending = false
        )

        composeTestRule.setContent {
            ChatContent(
                state = initialState, // Directly use the copied state
                onInputChanged = {},
                onSend = { clicked = true }
            )
        }

        composeTestRule.onNodeWithTag("SendButton")
            .assertIsDisplayed()
            .performClick()

        assert(clicked) { "Send button should trigger onSend()" }
    }

    /**
     * Test case to verify that the SendButton is disabled when the `isSending` state is true.
     *
     * This test sets up the [ChatContent] with an initial state where `isSending` is true.
     * It then asserts that the SendButton exists and is displayed, but is not enabled,
     * reflecting the behavior that users cannot send a new message while a previous one is still being processed.
     */
    @Test
    fun sendButton_disabledWhenSending() {
         val initialState = ChatUiState(
            messages = emptyList(),
            inputText = "",
            isSending = true // isSending is true
        )
        composeTestRule.setContent {
            ChatContent(
                state = initialState, // Directly use the copied state
                onInputChanged = {},
                onSend = {}
            )
        }

        // SendButton should exist but be disabled
        composeTestRule.onNodeWithTag("SendButton")
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }

    /**
     * Tests that the messages list correctly displays the messages provided in the UI state.
     * It sets up the UI with a predefined list of messages and then asserts that
     * each message text is visible on the screen.
     */
    @Test
    fun messagesList_displaysMessages() {
        val messages = listOf(
            Message(1, "Hello", isIncoming = true, createdAtMs = 1234),
            Message(2, "Hi!", isIncoming = false, createdAtMs = 5678)
        )
        val initialState = ChatUiState(
            messages = messages,
            inputText = "",
            isSending = false
        )

        composeTestRule.setContent {
            ChatContent(
                state = initialState, // Use state with messages
                onInputChanged = {},
                onSend = {}
            )
        }

        composeTestRule.onNodeWithText("Hello").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hi!").assertIsDisplayed()
    }

    /**
     * Tests the interaction between the input field and the send button.
     * It verifies that text typed into the input field updates the UI state correctly.
     *
     * Steps:
     * 1. Sets up the [ChatContent] with an initial empty [ChatUiState].
     * 2. Simulates typing text into the "InputField".
     * 3. Asserts that the `inputText` in the `uiState` is updated to match the typed text.
     */
    @Test
    fun inputField_and_sendButton_workTogether() {
        var uiState by mutableStateOf(ChatUiState(
            messages = emptyList(),
            inputText = "",
            isSending = false
        ))

        composeTestRule.setContent {
            ChatContent(
                state = uiState,
                onInputChanged = { newText -> uiState = uiState.copy(inputText = newText) },
                onSend = { /* normally send message using uiState.inputText */ }
            )
        }

        val inputText = "This is a test"
        composeTestRule.onNodeWithTag("InputField").performTextInput(inputText)
        assert(uiState.inputText == inputText)
    }
}
