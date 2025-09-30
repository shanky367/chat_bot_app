package com.app.chatbot.di


import com.app.chatbot.repo.ChatRepository
import com.app.chatbot.viewmodel.ChatViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { ChatRepository() }
    viewModel { ChatViewModel(get()) }
}
