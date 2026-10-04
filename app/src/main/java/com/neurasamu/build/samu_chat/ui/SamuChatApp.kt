package com.neurasamu.build.samu_chat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neurasamu.build.samu_chat.data.ApiConfig
import com.neurasamu.build.samu_chat.data.Conversation
import com.neurasamu.build.samu_chat.vm.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SamuChatApp(vm: ChatViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showApiEditor by remember { mutableStateOf<ApiConfig?>(null) }
    var showNewApi by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(Modifier.width(300.dp)) {
                Spacer(Modifier.height(16.dp))
                Text("SaMu Chat", Modifier.padding(16.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold)
                Text("NeuraSamu", Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary)
                HorizontalDivider(Modifier.padding(vertical = 12.dp))

                Text("APIs", Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)
                state.apis.forEach { api ->
                    NavigationDrawerItem(
                        label = {
                            Column {
                                Text(api.label, fontWeight = FontWeight.SemiBold)
                                Text(api.modelName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        },
                        selected = api.id == state.activeApi?.id,
                        onClick = {
                            vm.selectApi(api)
                            scope.launch { drawerState.close() }
                        },
                        badge = {
                            IconButton(onClick = { showApiEditor = api }) {
                                Icon(Icons.Default.Edit, "edit", Modifier.size(16.dp))
                            }
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                NavigationDrawerItem(
                    label = { Text("Add new API") },
                    icon = { Icon(Icons.Default.Add, null) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showNewApi = true
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )

                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                Text("Chats", Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)
                state.conversations.forEach { conv ->
                    NavigationDrawerItem(
                        label = { Text(conv.title, maxLines = 1) },
                        selected = conv.id == state.activeConversation?.id,
                        onClick = {
                            vm.openConversation(conv)
                            scope.launch { drawerState.close() }
                        },
                        badge = {
                            IconButton(onClick = { vm.deleteConversation(conv) }) {
                                Icon(Icons.Default.Delete, "del", Modifier.size(16.dp))
                            }
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp)
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(state.activeConversation?.title ?: "SaMu Chat",
                                fontWeight = FontWeight.Bold)
                            state.activeApi?.let {
                                Text("${it.label} · ${it.modelName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, "menu")
                        }
                    },
                    actions = {
                        if (state.activeConversation != null) {
                            IconButton(onClick = { vm.newConversation() }) {
                                Icon(Icons.Default.Add, "new")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            floatingActionButton = {
                if (state.activeApi != null && state.activeConversation == null) {
                    ExtendedFloatingActionButton(
                        onClick = { vm.newConversation() },
                        icon = { Icon(Icons.Default.Chat, null) },
                        text = { Text("New chat") }
                    )
                }
            }
        ) { pad ->
            Box(Modifier.fillMaxSize().padding(pad)) {
                if (state.activeApi == null) {
                    EmptyApiState(onAddApi = { showNewApi = true })
                } else if (state.activeConversation == null) {
                    EmptyChatState(apiLabel = state.activeApi?.label ?: "")
                } else {
                    ChatScreen(vm)
                }
            }
        }
    }

    if (showNewApi) {
        ApiEditDialog(
            existing = null,
            onSave = { vm.saveApi(it); showNewApi = false },
            onDismiss = { showNewApi = false }
        )
    }
    showApiEditor?.let { api ->
        ApiEditDialog(
            existing = api,
            onSave = { vm.saveApi(it); showApiEditor = null },
            onDelete = { vm.deleteApi(it); showApiEditor = null },
            onDismiss = { showApiEditor = null }
        )
    }
}

@Composable
private fun EmptyApiState(onAddApi: () -> Unit) {
    Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
        Text("No API configured", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text("Add a server URL to start chatting",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Spacer(Modifier.height(16.dp))
        Button(onClick = onAddApi) { Text("Add API") }
    }
}

@Composable
private fun EmptyChatState(apiLabel: String) {
    Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
        Text("Ready", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text("Using $apiLabel — start a new chat",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

@Composable
private fun ChatScreen(vm: ChatViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }

    LaunchedEffect(state.messages.size, state.streamingText.isNotEmpty()) {
        val total = state.messages.size + if (state.streamingText.isNotEmpty()) 1 else 0
        if (total > 0) {
            try { listState.scrollToItem(total - 1) } catch (_: Exception) {}
        }
    }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.messages, key = { it.id }) { m ->
                MessageBubble(m.role, m.content)
            }
            if (state.streamingText.isNotEmpty()) {
                item(key = "streaming") {
                    MessageBubble("assistant", state.streamingText)
                }
            }
        }

        state.error?.let { err ->
            Surface(
                Modifier.fillMaxWidth().padding(8.dp),
                color = Color(0xFF3B1212),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(err, Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFF8080), maxLines = 4)
                    TextButton(onClick = { vm.clearError() }) { Text("Dismiss") }
                }
            }
        }

        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 4.dp) {
            Row(
                Modifier.fillMaxWidth().padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("Message…") },
                    modifier = Modifier.weight(1f),
                    maxLines = 5
                )
                Spacer(Modifier.width(8.dp))
                if (state.isStreaming) {
                    IconButton(onClick = { vm.cancelStream() }) {
                        Icon(Icons.Default.Close, "stop",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    FilledIconButton(
                        onClick = {
                            val t = input.trim()
                            if (t.isNotEmpty()) {
                                vm.sendMessage(t)
                                input = ""
                            }
                        },
                        enabled = input.isNotBlank()
                    ) {
                        Icon(Icons.Default.Send, "send")
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(role: String, content: String) {
    val isUser = role == "user"
    val align = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bg = if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
             else MaterialTheme.colorScheme.surface
    Box(Modifier.fillMaxWidth(), contentAlignment = align) {
        Surface(
            color = bg,
            shape = RoundedCornerShape(
                topStart = 14.dp, topEnd = 14.dp,
                bottomStart = if (isUser) 14.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 14.dp
            ),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Text(content, Modifier.padding(10.dp),
                style = MaterialTheme.typography.bodyMedium)
        }
    }
}
