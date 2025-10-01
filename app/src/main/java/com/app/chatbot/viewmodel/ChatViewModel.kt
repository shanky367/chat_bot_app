package com.app.chatbot.viewmodel


import androidx.lifecycle.ViewModel
import com.app.chatbot.events.ChatEvent
import com.app.chatbot.repo.ChatRepository
import com.app.chatbot.stateui.ChatUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


/**
 * ViewModel for the Chat screen.
 *
 * This ViewModel is responsible for managing the UI state of the chat screen and handling user interactions.
 * It interacts with the [ChatRepository] to send and receive messages.
 *
 * @param repository The [ChatRepository] used to interact with the chat data source.
 * @param replyDelayMs The delay in milliseconds for simulating a reply. This is parameterized for testing purposes.
 */
open class ChatViewModel(
    private val repository: ChatRepository,
    private val replyDelayMs: Long = 5000L, // parameterized for tests

) : ViewModel() {
    val chatIiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = chatIiState.asStateFlow()

    private var repoCollector: Job? = null
    private var testScope: CoroutineScope? = null

    private val viewModelScope: CoroutineScope
        get() = testScope ?: CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        // Observe repository messages and map into UI state
        initializeMessageCollection()
    }

    /**
     * Sets the [CoroutineScope] to be used for testing.
     * This allows for controlling the execution of coroutines in tests.
     * After setting the scope, it re-initializes the message collection
     * to ensure the new scope is used for observing repository messages.
     *
     * @param scope The [CoroutineScope] to be used for testing.
     */
    fun setTestScope(scope: CoroutineScope) {
        testScope = scope
        initializeMessageCollection()
    }

    /**
     * Initializes the collection of messages from the repository.
     *
     * This function launches a coroutine in the `viewModelScope` to observe the `messages` Flow
     * from the `repository`. Whenever new messages are emitted, it updates the `chatIiState`
     * with the new messages and sets `isSending` to false, indicating that no message is currently
     * being sent.
     */
    private fun initializeMessageCollection() {
        // Observe repository messages and map into UI state
        repoCollector = viewModelScope.launch {
            repository.messages.collectLatest { message ->
                chatIiState.value = chatIiState.value.copy(messages = message, isSending = false)
            }
        }
    }

    /**
     * Submits a chat event to be processed by the ViewModel.
     *
     * This function handles different types of chat events:
     * - [ChatEvent.InputChanged]: Updates the input text in the UI state.
     * - [ChatEvent.SendClicked]: Sends the current input text as an outgoing message,
     *   clears the input field, and simulates a reply from the repository.
     * - [ChatEvent.NewMessages]: Updates the list of messages in the UI state.
     *
     * @param event The [ChatEvent] to be processed.
     */
    fun submitEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.InputChanged -> {
                chatIiState.value = chatIiState.value.copy(inputText = event.text)
            }

            is ChatEvent.SendClicked -> {
                val text = chatIiState.value.inputText.trim()
                if (text.isEmpty()) return
                viewModelScope.launch {
                    chatIiState.value = chatIiState.value.copy(isSending = true)
                    repository.sendOutgoing(text)
                    chatIiState.value = chatIiState.value.copy(inputText = "")
                    // Simulate reply using repository; keep it off main thread
                    launch {
                        repository.simulateReplyFor("Echo: $text", replyDelayMs)
                    }
                }
            }

            is ChatEvent.NewMessages -> {
                chatIiState.value = chatIiState.value.copy(messages = event.messages)
            }
        }
    }

    public override fun onCleared() {
        repoCollector?.cancel()
        super.onCleared()
    }
}
