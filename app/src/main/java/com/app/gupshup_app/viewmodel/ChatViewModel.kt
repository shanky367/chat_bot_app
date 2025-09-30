package com.app.gupshup_app.viewmodel


import androidx.lifecycle.ViewModel
import com.app.gupshup_app.events.ChatEvent
import com.app.gupshup_app.repo.ChatRepository
import com.app.gupshup_app.stateui.ChatUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


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
    fun setTestScope(scope: CoroutineScope) {
        testScope = scope
        initializeMessageCollection()
    }

    private fun initializeMessageCollection() {
        // Observe repository messages and map into UI state
        repoCollector = viewModelScope.launch {
            repository.messages.collectLatest { message ->
                chatIiState.value = chatIiState.value.copy(messages = message, isSending = false)
            }
        }
    }

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
