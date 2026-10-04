package com.neurasamu.build.samu_chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neurasamu.build.samu_chat.data.ApiConfig
import com.neurasamu.build.samu_chat.data.Message
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
            ModalDrawerSheet(Modifier.width(320.dp)) {
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
                            val pct = if (state.maxTokens > 0)
                                state.usedTokens * 100 / state.maxTokens else 0
                            val chipColor = when {
                                pct >= 90 -> Color(0xFFEF4444)
                                pct >= 70 -> Color(0xFFF59E0B)
                                else -> MaterialTheme.colorScheme.primary
                            }
                            Surface(
                                color = chipColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Text("${state.usedTokens}/${state.maxTokens}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = chipColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
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
            Box(Modifier.fillMaxSize().padding(pad).imePadding()) {
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
    if (state.showContextWarning) {
        AlertDialog(
            onDismissRequest = { },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Chat full") },
            text = { Text("This conversation has used ~${state.usedTokens} of ${state.maxTokens} tokens. Replies may get cut off. Start a new chat to keep things fast and clean.") },
            confirmButton = { TextButton(onClick = { vm.newConversation() }) { Text("New chat") } },
            dismissButton = { TextButton(onClick = { vm.dismissContextWarning() }) { Text("Continue") } }
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
    var editingMessage by remember { mutableStateOf<Message?>(null) }
    var pendingAttachment by remember { mutableStateOf<android.net.Uri?>(null) }
    var pendingAttachmentName by remember { mutableStateOf<String?>(null) }
    val filePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            pendingAttachment = it
            pendingAttachmentName = it.lastPathSegment
        }
    }
    val clipboard = LocalContext.current.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
        as android.content.ClipboardManager

    LaunchedEffect(state.messages.size, state.streamingText.isNotEmpty(), state.isStreaming) {
        val total = state.messages.size +
            (if (state.streamingText.isNotEmpty() || state.isStreaming) 1 else 0)
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
                MessageBubble(
                    role = m.role,
                    content = m.content,
                    onCopy = {
                        clipboard.setPrimaryClip(
                            android.content.ClipData.newPlainText("msg", m.content)
                        )
                    },
                    onEdit = if (m.role == "user") {
                        { editingMessage = m }
                    } else null
                )
            }
            if (state.streamingText.isNotEmpty()) {
                item(key = "streaming") {
                    MessageBubble("assistant", state.streamingText, null, null)
                }
            } else if (state.isStreaming) {
                item(key = "thinking") {
                    ThinkingBubble()
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
            Column(Modifier.fillMaxWidth().padding(10.dp)) {
                pendingAttachment?.let { uri ->
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                    ) {
                        Row(
                            Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AttachFile, null, Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                pendingAttachmentName ?: uri.toString(),
                                Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1
                            )
                            IconButton(onClick = {
                                pendingAttachment = null
                                pendingAttachmentName = null
                            }) { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { filePicker.launch(arrayOf("*/*")) }) {
                        Icon(Icons.Default.AttachFile, "attach",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = { Text("Message…") },
                        modifier = Modifier.weight(1f),
                        maxLines = 5
                    )
                    Spacer(Modifier.width(8.dp))
                    if (state.isStreaming) {
                        FilledIconButton(
                            onClick = { vm.cancelStream() },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color(0xFFEF4444)
                            )
                        ) { Icon(Icons.Default.Stop, "stop") }
                    } else {
                        FilledIconButton(
                            onClick = {
                                val t = input.trim()
                                if (t.isNotEmpty() || pendingAttachment != null) {
                                    val msg = if (pendingAttachment != null)
                                        t + "\n\n[attachment: " + (pendingAttachmentName ?: "file") + "]"
                                    else t
                                    val fallback = "[attachment: " + (pendingAttachmentName ?: "file") + "]"
                                    vm.sendMessage(if (msg.isBlank()) fallback else msg)
                                    input = ""
                                    pendingAttachment = null
                                    pendingAttachmentName = null
                                }
                            },
                            enabled = input.isNotBlank() || pendingAttachment != null
                        ) { Icon(Icons.Default.Send, "send") }
                    }
                }
            }
        }
    }

    editingMessage?.let { msg ->
        var editText by remember(msg.id) { mutableStateOf(msg.content) }
        AlertDialog(
            onDismissRequest = { editingMessage = null },
            title = { Text("Edit message") },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 8
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val t = editText.trim()
                        if (t.isNotBlank() && t != msg.content) {
                            vm.editAndResend(msg, t)
                        }
                        editingMessage = null
                    },
                    enabled = editText.isNotBlank()
                ) { Text("Resend") }
            },
            dismissButton = {
                TextButton(onClick = { editingMessage = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ThinkingBubble() {
    val dots = remember { androidx.compose.runtime.mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(400)
            dots.intValue = (dots.intValue + 1) % 4
        }
    }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp,
                bottomStart = 4.dp, bottomEnd = 14.dp),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Thinking",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Spacer(Modifier.width(2.dp))
                Text(".".repeat(dots.intValue),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    role: String,
    content: String,
    onCopy: (() -> Unit)?,
    onEdit: (() -> Unit)?
) {
    val isUser = role == "user"
    val align = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bg = if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
             else MaterialTheme.colorScheme.surface
    var menuOpen by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxWidth(), contentAlignment = align) {
        Box {
            Surface(
                color = bg,
                shape = RoundedCornerShape(
                    topStart = 14.dp, topEnd = 14.dp,
                    bottomStart = if (isUser) 14.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 14.dp
                ),
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .combinedClickable(
                        onClick = { },
                        onLongClick = {
                            if (onCopy != null || onEdit != null) menuOpen = true
                        }
                    )
            ) {
                Text(content, Modifier.padding(10.dp),
                    style = MaterialTheme.typography.bodyMedium)
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false }
            ) {
                if (onCopy != null) {
                    DropdownMenuItem(
                        text = { Text("Copy") },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                        onClick = { onCopy(); menuOpen = false }
                    )
                }
                if (onEdit != null) {
                    DropdownMenuItem(
                        text = { Text("Edit & Resend") },
                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                        onClick = { onEdit(); menuOpen = false }
                    )
                }
            }
        }
    }
}
