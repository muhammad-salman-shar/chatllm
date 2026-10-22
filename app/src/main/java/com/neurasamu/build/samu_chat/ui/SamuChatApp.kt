package com.neurasamu.build.samu_chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.asImageBitmap
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
fun SamuChatApp(
    settings: com.neurasamu.build.samu_chat.data.AppSettings,
    onSettingsChange: (com.neurasamu.build.samu_chat.data.AppSettings) -> Unit,
    vm: ChatViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showApiEditor by remember { mutableStateOf<ApiConfig?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showNewApi by remember { mutableStateOf(false) }
    var convMenuOpen by remember { mutableStateOf<String?>(null) }
    var renameConv by remember { mutableStateOf<com.neurasamu.build.samu_chat.data.Conversation?>(null) }
    var renameText by remember { mutableStateOf("") }
    var pendingExport by remember { mutableStateOf<com.neurasamu.build.samu_chat.data.Conversation?>(null) }
    val ctxApp = LocalContext.current
    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        val c = pendingExport
        if (uri != null && c != null) {
            scope.launch {
                val text = vm.exportConversationText(c)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    ctxApp.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
                }
            }
        }
        pendingExport = null
    }

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
                                Text(api.label, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                Text(api.modelName,
                                    fontSize = 9.sp,
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
                            Box {
                                IconButton(onClick = { convMenuOpen = conv.id }) {
                                    Icon(Icons.Default.MoreVert, "menu", Modifier.size(18.dp))
                                }
                                DropdownMenu(
                                    expanded = convMenuOpen == conv.id,
                                    onDismissRequest = { convMenuOpen = null }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Rename") },
                                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                                        onClick = {
                                            renameConv = conv
                                            renameText = conv.title
                                            convMenuOpen = null
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Download (txt)") },
                                        leadingIcon = { Icon(Icons.Default.Download, null) },
                                        onClick = {
                                            pendingExport = conv
                                            exportLauncher.launch(conv.title.replace(" ", "_") + ".txt")
                                            convMenuOpen = null
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete", color = Color(0xFFEF4444)) },
                                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color(0xFFEF4444)) },
                                        onClick = {
                                            vm.deleteConversation(conv)
                                            convMenuOpen = null
                                        }
                                    )
                                }
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
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp)
                            state.activeApi?.let {
                                Text("${it.label} · ${it.modelName}",
                                    fontSize = 10.sp,
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
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Default.Settings, "settings", Modifier.size(20.dp))
                        }
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

    renameConv?.let { c ->
        AlertDialog(
            onDismissRequest = { renameConv = null },
            title = { Text("Rename chat") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.renameConversation(c, renameText)
                        renameConv = null
                    },
                    enabled = renameText.isNotBlank()
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { renameConv = null }) { Text("Cancel") }
            }
        )
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

    if (showSettings) {
        SettingsDialog(
            current = settings,
            onSave = { onSettingsChange(it); showSettings = false },
            onDismiss = { showSettings = false }
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
    val ctxApp = LocalContext.current
    val scope = rememberCoroutineScope()
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
    var pendingImage by remember { mutableStateOf<android.net.Uri?>(null) }
    var pendingImageB64 by remember { mutableStateOf<String?>(null) }
    var pendingImageMime by remember { mutableStateOf("image/jpeg") }
    val imagePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            pendingImage = it
            pendingImageMime = ctxApp.contentResolver.getType(it) ?: "image/jpeg"
            scope.launch {
                pendingImageB64 = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        ctxApp.contentResolver.openInputStream(it)?.use { ins ->
                            val bytes = ins.readBytes()
                            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                        }
                    } catch (_: Exception) { null }
                }
            }
        }
    }
    val clipboard = LocalContext.current.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
        as android.content.ClipboardManager
    val tts = remember { android.speech.tts.TextToSpeech(ctxApp) { } }
    val shareText = { text: String ->
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, text)
        }
        ctxApp.startActivity(android.content.Intent.createChooser(intent, "Share via"))
    }

    LaunchedEffect(state.messages.size, state.streamingText.isNotEmpty(), state.isStreaming) {
        val total = state.messages.size +
            (if (state.streamingText.isNotEmpty() || state.isStreaming) 1 else 0)
        if (total > 0) {
            try { listState.animateScrollToItem(total - 1) } catch (_: Exception) {}
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
                    imageBase64 = m.imageBase64,
                    tts = tts,
                    onCopy = {
                        clipboard.setPrimaryClip(
                            android.content.ClipData.newPlainText("msg", m.content)
                        )
                    },
                    onShare = { shareText(m.content) },
                    onEdit = if (m.role == "user") {
                        { editingMessage = m }
                    } else null,
                    onRegenerate = if (m.role == "assistant") {
                        { vm.regenerateLast() }
                    } else null
                )
            }
            if (state.streamingText.isNotEmpty()) {
                item(key = "streaming") {
                    MessageBubble("assistant", state.streamingText, null, null, null, null, null, null)
                }
            } else if (state.isStreaming) {
                item(key = "thinking") {
                    ThinkingBubble()
                }
            }
            if (!state.isStreaming && state.messages.isNotEmpty() && state.messages.lastOrNull()?.role == "assistant") {
                item(key = "regen") {
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { vm.regenerateLast() }) {
                            Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Regenerate")
                        }
                    }
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
                pendingImageB64?.let {
                    Surface(
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                    ) {
                        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Image, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(6.dp))
                            Text("Image attached", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            IconButton(onClick = { pendingImage = null; pendingImageB64 = null }) {
                                Icon(Icons.Default.Close, null, Modifier.size(16.dp))
                            }
                        }
                    }
                }
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
                    IconButton(onClick = { filePicker.launch(arrayOf("*/*")) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.AttachFile, "attach", Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Image, "image", Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary)
                    }
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = { Text("Ask anything...") },
                        modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                        maxLines = 5,
                        shape = RoundedCornerShape(22.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )
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
                                if (t.isNotEmpty() || pendingAttachment != null || pendingImageB64 != null) {
                                    val msg = if (pendingAttachment != null)
                                        t + "\n\n[attachment: " + (pendingAttachmentName ?: "file") + "]"
                                    else t
                                    val fallback = "[attachment: " + (pendingAttachmentName ?: "file") + "]"
                                    vm.sendMessage(if (msg.isBlank()) fallback else msg, pendingImageB64, pendingImageMime)
                                    input = ""
                                    pendingAttachment = null
                                    pendingAttachmentName = null
                                    pendingImage = null
                                    pendingImageB64 = null
                                }
                            },
                            enabled = input.isNotBlank() || pendingAttachment != null || pendingImageB64 != null
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
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "dots")
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 4.dp, bottomEnd = 14.dp)
        ) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { i ->
                    val alpha by transition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                            animation = androidx.compose.animation.core.tween(600, easing = androidx.compose.animation.core.LinearEasing),
                            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
                            initialStartOffset = androidx.compose.animation.core.StartOffset(i * 180)
                        ),
                        label = "dot$i"
                    )
                    Box(
                        Modifier
                            .size(7.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha), RoundedCornerShape(50))
                    )
                    if (i < 2) Spacer(Modifier.width(5.dp))
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
    private fun MessageBubble(
        role: String,
        content: String,
        imageBase64: String? = null,
        tts: android.speech.tts.TextToSpeech? = null,
        onCopy: (() -> Unit)? = null,
        onShare: (() -> Unit)? = null,
        onEdit: (() -> Unit)? = null,
        onRegenerate: (() -> Unit)? = null
    ) {
        val isUser = role == "user"
        val align = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
        val bg = if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                 else MaterialTheme.colorScheme.surface
        var menuOpen by remember { mutableStateOf(false) }
        var isSelectMode by remember { mutableStateOf(false) }
        
        // Thinking tag parsing
        var showThink by remember { mutableStateOf(false) }
        val thinkRegex = Regex("<think>(.*?)</think>", RegexOption.DOT_MATCHES_ALL)
        val hasThink = thinkRegex.containsMatchIn(content)
        var displayContent = content
        var thinkContent = ""
        if (hasThink) {
            thinkContent = thinkRegex.find(content)?.groupValues?.get(1) ?: ""
            displayContent = thinkRegex.replace(content, "").trim()
        }
        
        // User input truncation
        var expanded by remember { mutableStateOf(false) }
        val words = displayContent.split("\\s+".toRegex())
        val displayText = if (isUser && !expanded && words.size > 10) {
            words.take(10).joinToString(" ") + "..."
        } else displayContent

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
                        .widthIn(max = if (isUser) 260.dp else 320.dp)
                        .combinedClickable(
                            onClick = { },
                            onLongClick = { menuOpen = true }
                        )
                ) {
                    Column(Modifier.padding(10.dp)) {
                        if (imageBase64 != null) {
                            val bitmap = remember(imageBase64) {
                                try {
                                    val bytes = android.util.Base64.decode(imageBase64, android.util.Base64.DEFAULT)
                                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                } catch (e: Exception) { null }
                            }
                            if (bitmap != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Attached Image",
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).padding(bottom = 8.dp),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                )
                            }
                        }
                        
                        if (hasThink) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { showThink = !showThink }) {
                                Text(
                                    if (showThink) "Hide thinking" else "Show thinking",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                Icon(
                                    if (showThink) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    null,
                                    Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                            if (showThink) {
                                Text(
                                    thinkContent.trim(),
                                    fontSize = 8.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                        
                        if (isSelectMode) {
                            androidx.compose.foundation.text.selection.SelectionContainer {
                                Text(displayText, fontSize = if (isUser) 11.sp else 13.sp)
                            }
                        } else {
                            Text(displayText, fontSize = if (isUser) 11.sp else 13.sp)
                        }
                        
                        if (isUser && words.size > 10) {
                            Text(
                                if (expanded) "Show less" else "Show more",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { expanded = !expanded }.padding(top = 4.dp)
                            )
                        }
                        
                        // Action Buttons
                        Row(
                            Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { isSelectMode = !isSelectMode }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.SelectAll, "Select", Modifier.size(14.dp), tint = if (isSelectMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                            if (onCopy != null) {
                                IconButton(onClick = onCopy, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.ContentCopy, "Copy", Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                            }
                            if (onShare != null) {
                                IconButton(onClick = onShare, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Share, "Share", Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                            }
                            if (onEdit != null) {
                                IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Edit, "Edit", Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                            }
                            if (!isUser && onRegenerate != null) {
                                IconButton(onClick = onRegenerate, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Refresh, "Regenerate", Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                            }
                            if (!isUser && tts != null) {
                                IconButton(onClick = { tts.speak(displayContent, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.VolumeUp, "Read Aloud", Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
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

