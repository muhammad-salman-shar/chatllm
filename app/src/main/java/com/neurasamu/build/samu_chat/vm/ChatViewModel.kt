package com.neurasamu.build.samu_chat.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neurasamu.build.samu_chat.data.ApiConfig
import com.neurasamu.build.samu_chat.data.ChatRepository
import com.neurasamu.build.samu_chat.data.Conversation
import com.neurasamu.build.samu_chat.data.Message
import com.neurasamu.build.samu_chat.network.ChatClient
import com.neurasamu.build.samu_chat.network.ChatEvent
import com.neurasamu.build.samu_chat.network.ChatRequest
import com.neurasamu.build.samu_chat.smg.KcController
import com.neurasamu.build.samu_chat.smg.KeywordExtractor
import com.neurasamu.build.samu_chat.smg.SmgBundle
import com.neurasamu.build.samu_chat.smg.SmgPromptBuilder
import com.neurasamu.build.samu_chat.smg.SmgRetriever
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatUiState(
    val apis: List<ApiConfig> = emptyList(),
    val activeApi: ApiConfig? = null,
    val conversations: List<Conversation> = emptyList(),
    val activeConversation: Conversation? = null,
    val messages: List<Message> = emptyList(),
    val streamingText: String = "",
    val isStreaming: Boolean = false,
    val error: String? = null,
    val usedTokens: Int = 0,
    val maxTokens: Int = 4096,
    val showContextWarning: Boolean = false,
    val smgEnabled: Boolean = false,
    val bundleCount: Int = 0,
    val smgStatus: String = "",
    val bundles: List<SmgBundle> = emptyList()

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ChatRepository(app)
    private val kc = KcController(app)
    private val retriever = SmgRetriever(app)
    private val promptBuilder = SmgPromptBuilder(app)

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var streamJob: Job? = null
    private var convListJob: Job? = null
    private var msgListJob: Job? = null
    private var bundleListJob: Job? = null

    init {
        viewModelScope.launch {
            repo.observeApis().collect { apis ->
                _state.value = _state.value.copy(apis = apis)
                val active = _state.value.activeApi
                if (active == null && apis.isNotEmpty()) {
                    selectApi(apis.first())
                } else if (active != null && apis.none { it.id == active.id }) {
                    if (apis.isNotEmpty()) selectApi(apis.first())
                    else _state.value = _state.value.copy(activeApi = null)
                }
            }
        }
    }

    fun selectApi(api: ApiConfig) {
        _state.value = _state.value.copy(
            activeApi = api,
            activeConversation = null,
            messages = emptyList()
        )
        convListJob?.cancel()
        convListJob = viewModelScope.launch {
            repo.observeConversationsFor(api.id).collect { convs ->
                _state.value = _state.value.copy(conversations = convs)
            }
        }
    }

    fun saveApi(api: ApiConfig) {
        viewModelScope.launch { repo.saveApi(api) }
    }

    fun deleteApi(api: ApiConfig) {
        viewModelScope.launch {
            repo.deleteApi(api)
            if (_state.value.activeApi?.id == api.id) {
                _state.value = _state.value.copy(
                    activeApi = null,
                    activeConversation = null,
                    messages = emptyList(),
                    conversations = emptyList()
                )
            }
        }
    }

    fun newConversation() {
        val api = _state.value.activeApi ?: return
        viewModelScope.launch {
            val conv = Conversation(apiConfigId = api.id, title = "New chat")
            repo.saveConversation(conv)
            openConversation(conv)
        }
    }

    fun openConversation(conv: Conversation) {
        _state.value = _state.value.copy(
            activeConversation = conv,
            messages = emptyList(),
            streamingText = "",
            isStreaming = false,
            error = null,
            bundleCount = 0
        )
        msgListJob?.cancel()
        msgListJob = viewModelScope.launch {
            repo.observeMessages(conv.id).collect { msgs ->
                val api = _state.value.activeApi
                val maxCtx = api?.contextWindow ?: 4096
                val sysPrompt = api?.systemPrompt ?: ""
                val allText = (if (sysPrompt.isNotBlank()) listOf(sysPrompt) else emptyList()) + msgs.map { it.content }
                val used = TokenEstimator.estimateAll(allText)
                val warning = used >= (maxCtx * 0.9).toInt()
                _state.value = _state.value.copy(
                    messages = msgs,
                    usedTokens = used,
                    maxTokens = maxCtx,
                    showContextWarning = warning
                )
            }
        }
        bundleListJob?.cancel()
        bundleListJob = viewModelScope.launch {
            kc.observeBundles(conv.id).collect { list ->
                _state.value = _state.value.copy(
                    bundleCount = list.size,
                    bundles = list
                )
            }
        }
    }

    fun deleteConversation(conv: Conversation) {
        viewModelScope.launch {
            repo.deleteConversation(conv.id)
            if (_state.value.activeConversation?.id == conv.id) {
                msgListJob?.cancel()
                bundleListJob?.cancel()
                _state.value = _state.value.copy(
                    activeConversation = null,
                    messages = emptyList(),
                    bundleCount = 0
                )
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val api = _state.value.activeApi ?: return

        val conv = _state.value.activeConversation
        if (conv == null) {
            viewModelScope.launch {
                val newConv = Conversation(apiConfigId = api.id, title = "New chat")
                repo.saveConversation(newConv)
                openConversation(newConv)
                performSend(api, newConv, text)
            }
        } else {
            viewModelScope.launch {
                performSend(api, conv, text)
            }
        }
    }

    private suspend fun performSend(api: ApiConfig, conv: Conversation, text: String) {
        val userMsg = Message(conversationId = conv.id, role = "user", content = text)
        repo.insertMessage(userMsg)

        val msgs = repo.listMessages(conv.id)
        if (msgs.count { it.role == "user" } == 1) {
            val title = text.take(40).replace("\n", " ")
            repo.renameConversation(conv.id, title)
            _state.value = _state.value.copy(
                activeConversation = conv.copy(title = title)
            )
        }

        // Build system prompt parts
        val sysParts = mutableListOf<String>()
        api.systemPrompt.trim().takeIf { it.isNotBlank() }?.let { sysParts.add(it) }

        val smgOn = _state.value.smgEnabled
        if (smgOn) {
            try {
                _state.value = _state.value.copy(smgStatus = "Scanning memory…")
                val memIndex = promptBuilder.buildMemoryIndex(conv.id)
                if (memIndex.isNotBlank()) sysParts.add(memIndex)

                val hits = retriever.retrieve(conv.id, text, api, topK = 2)
                if (hits.isNotEmpty()) {
                    val recalled = promptBuilder.buildRecalledContext(hits)
                    if (recalled.isNotBlank()) sysParts.add(recalled)
                    _state.value = _state.value.copy(
                        smgStatus = "Recalled ${hits.size} slip(s)"
                    )
                } else {
                    _state.value = _state.value.copy(smgStatus = "Memory empty")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(smgStatus = "SMG error: ${e.message}")
            }
        }

        val base = if (sysParts.isNotEmpty())
            listOf("system" to sysParts.joinToString("\n\n"))
        else emptyList()

        // Bounded window when SMG on, full history when off
        val historyMsgs = if (smgOn) msgs.takeLast(6) else msgs
        val context = base + historyMsgs.map { it.role to it.content }

        runStream(api, conv, context, text)
    }

    private fun runStream(
        api: ApiConfig,
        conv: Conversation,
        context: List<Pair<String, String>>,
        userText: String
    ) {
        streamJob?.cancel()
        _state.value = _state.value.copy(isStreaming = true, streamingText = "", error = null)

        streamJob = viewModelScope.launch {
            val req = ChatRequest(
                baseUrl = api.baseUrl,
                apiKey = api.apiKey,
                model = api.modelName,
                messages = context,
                temperature = api.temperature.toDouble(),
                maxTokens = api.maxTokensPerReply,
                stream = true
            )
            var full = ""
            ChatClient.stream(req).collect { ev ->
                when (ev) {
                    is ChatEvent.Token -> {
                        full += ev.text
                        _state.value = _state.value.copy(streamingText = full)
                    }
                    is ChatEvent.Done -> {
                        val content = ev.fullText.ifBlank { full }
                        if (content.isNotBlank()) {
                            val assistantMsg = Message(
                                conversationId = conv.id,
                                role = "assistant",
                                content = content
                            )
                            repo.insertMessage(assistantMsg)

                            if (_state.value.smgEnabled) {
                                viewModelScope.launch {
                                    createBundleAsync(api, conv, userText, content)
                                }
                            }
                        }
                        _state.value = _state.value.copy(
                            isStreaming = false,
                            streamingText = ""
                        )
                    }
                    is ChatEvent.Error -> {
                        _state.value = _state.value.copy(
                            isStreaming = false,
                            streamingText = "",
                            error = ev.message
                        )
                    }
                }
            }
        }
    }

    private suspend fun createBundleAsync(
        api: ApiConfig,
        conv: Conversation,
        userText: String,
        assistantText: String
    ) {
        try {
            _state.value = _state.value.copy(smgStatus = "Indexing turn…")

            // Extract keywords (LLM preferred, fallback to heuristic)
            val keywords = try {
                val llmKw = KeywordExtractor.extract(
                    userText = userText,
                    assistantText = assistantText,
                    baseUrl = api.baseUrl,
                    apiKey = api.apiKey,
                    model = api.modelName
                )
                if (llmKw.isBlank())
                    KeywordExtractor.extractHeuristic("$userText $assistantText")
                else llmKw
            } catch (_: Exception) {
                KeywordExtractor.extractHeuristic("$userText $assistantText")
            }

            if (keywords.isBlank()) {
                _state.value = _state.value.copy(smgStatus = "No keywords")
                return
            }

            // Find the most recent user + assistant message IDs from DB
            val all = repo.listMessages(conv.id)
            val lastUser = all.lastOrNull { it.role == "user" && it.content == userText }
            val lastAssistant = all.lastOrNull { it.role == "assistant" && it.content == assistantText }
            if (lastUser == null || lastAssistant == null) {
                _state.value = _state.value.copy(smgStatus = "Bundle skip — messages not found")
                return
            }

            val bundle = kc.createBundle(
                convId = conv.id,
                userMessageId = lastUser.id,
                assistantMessageId = lastAssistant.id,
                keywords = keywords
            )
            _state.value = _state.value.copy(
                smgStatus = if (bundle != null) "${bundle.id} created" else "Bundle failed"
            )
        } catch (e: Exception) {
            _state.value = _state.value.copy(smgStatus = "Bundle error: ${e.message}")
        }
    }

    fun toggleSmg() {
        val next = !_state.value.smgEnabled
        _state.value = _state.value.copy(
            smgEnabled = next,
            smgStatus = if (next) "SMG ON" else "SMG OFF"
        )
    }

    fun cancelStream() {
        streamJob?.cancel()
        _state.value = _state.value.copy(isStreaming = false, streamingText = "")
    }

    fun deleteMessage(id: String) {
        viewModelScope.launch { repo.deleteMessage(id) }
    }

    fun deleteBundle(id: String) {
        viewModelScope.launch { kc.deleteBundle(id) }
    }

    fun clearAllBundles() {
        val conv = _state.value.activeConversation ?: return
        viewModelScope.launch { kc.deleteForConversation(conv.id) }
    }

    fun dismissContextWarning() {
        _state.value = _state.value.copy(showContextWarning = false)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
