package com.app.chatbot.di


import com.app.chatbot.repo.ChatRepository
import com.app.chatbot.viewmodel.ChatViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin module for application-level dependencies.
 *
 * This module provides the following dependencies:
 * - [ChatRepository]: A singleton instance for managing chat data.
 * - [ChatViewModel]: A ViewModel instance for the chat screen, with [ChatRepository] injected.
 */
val appModule = module {
    single { ChatRepository() }
    viewModel { ChatViewModel(get()) }
}
