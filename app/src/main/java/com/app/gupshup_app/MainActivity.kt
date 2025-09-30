package com.app.gupshup_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.app.gupshup_app.di.appModule
import com.app.gupshup_app.ui.screen.ChatScreen
import com.app.gupshup_app.ui.screen.CustomTopBar
import com.app.gupshup_app.ui.theme.Gupshup_appTheme
import com.app.gupshup_app.viewmodel.ChatViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.compose.koinViewModel
import org.koin.core.context.startKoin


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startKoin {
            androidContext(this@MainActivity)
            modules(appModule)
        }

        enableEdgeToEdge()
        setContent {
            Gupshup_appTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = { CustomTopBar() }) { innerPadding ->
                    val vm: ChatViewModel = koinViewModel()
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)) {
                        ChatScreen(viewModel = vm)
                    }
                }
            }
        }
    }
}