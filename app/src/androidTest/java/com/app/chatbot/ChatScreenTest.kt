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

class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

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
