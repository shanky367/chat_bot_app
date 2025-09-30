package com.app.gupshup_app.ui.screen


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.gupshup_app.events.ChatEvent
import com.app.gupshup_app.model.Message
import com.app.gupshup_app.stateui.ChatUiState
import com.app.gupshup_app.ui.theme.Purple40
import com.app.gupshup_app.ui.theme.Purple80
import com.app.gupshup_app.viewmodel.ChatViewModel
import kotlinx.datetime.Clock


@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val state by viewModel.uiState.collectAsState()
    ChatContent(
        state = state,
        onInputChanged = { viewModel.submitEvent(ChatEvent.InputChanged(it)) },
        onSend = { viewModel.submitEvent(ChatEvent.SendClicked) }
    )
}

@Composable
fun ChatContent(
    state: ChatUiState,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        LazyColumn(
            modifier = Modifier .testTag("MessageList")
                .weight(1f)
                .fillMaxWidth(),
            reverseLayout = true
        ) {
            items(state.messages.reversed()) { msg ->
                MessageRow(msg)
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .weight(1f)
                    .wrapContentSize()
            ) {
                TextField(
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.White,
                        focusedContainerColor = Color.White,
                        unfocusedIndicatorColor = Color.White,
                        disabledIndicatorColor = Color.White,
                        errorIndicatorColor = Color.White,
                        focusedPlaceholderColor = Color.White,
                    ),
                    value = state.inputText,
                    onValueChange = onInputChanged,
                    modifier = Modifier.testTag("InputField"),
                    placeholder = { Text("Type a message") }
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                modifier = Modifier.testTag("SendButton"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Purple40,
                    contentColor = Color.White,
                    disabledContainerColor = Color.Gray,
                    disabledContentColor = Color.DarkGray
                ),
                onClick = onSend,
                enabled = !state.isSending) { Text("Send") }
        }
        Spacer(modifier = Modifier.height(8.dp))

    }
}

@Composable
fun MessageRow(msg: Message) {
    val bgShape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = if (msg.isIncoming) Arrangement.Start else Arrangement.End
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (msg.isIncoming) Purple80.copy(alpha = 0.2f) else Purple80.copy(alpha = 0.5f),
                ),
            shape = bgShape,
            elevation =
                CardDefaults.cardElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    hoveredElevation = 0.dp,
                    focusedElevation = 0.dp,
                    disabledElevation = 0.dp,
                    draggedElevation = 0.dp
                )
        ) {
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .widthIn(max = 280.dp)
            ) {
                Text(
                    msg.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Black,
                )
                Text(
                    text = android.text.format.DateFormat.format("hh:mm a", msg.createdAtMs)
                        .toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun ChatPreview() {
    val now = Clock.System.now().toEpochMilliseconds()
    val messages = listOf(
        Message(1, "Hello from Alice", isIncoming = true, createdAtMs = now - 60000),
        Message(2, "Hi! this is a reply", isIncoming = false, createdAtMs = now - 30000),
        Message(3, "Another incoming", isIncoming = true, createdAtMs = now - 10000)
    )
    ChatContent(
        state = ChatUiState(messages = messages, inputText = ""),
        onInputChanged = {},
        onSend = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTopBar() {
    TopAppBar(
        title = {
            Text("Chat application ")
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Purple40,
            titleContentColor = Color.White
        )
    )
}
