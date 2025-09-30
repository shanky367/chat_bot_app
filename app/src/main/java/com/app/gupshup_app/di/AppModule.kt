package com.app.gupshup_app.di



import com.app.gupshup_app.repo.ChatRepository
import com.app.gupshup_app.viewmodel.ChatViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { ChatRepository() }
    viewModel { ChatViewModel(get()) }
}
