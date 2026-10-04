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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatUiState(
    val apis: List<ApiConfig> = emptyList(),
    val activeApi: ApiConfig? = null,
    val conversations: List<Conversation> = emptyList(),
    val activeConversation: Conversation? = null,
    val messages: List<Message> = emptyList(),
    val streamingText: String = "",
    val isStreaming: Boolean = false,
    val error: String? = null
)

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ChatRepository(app)
    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var streamJob: Job? = null

    init {
        viewModelScope.launch {
            repo.observeApis().collect { apis ->
                _state.value = _state.value.copy(apis = apis)
                if (_state.value.activeApi == null && apis.isNotEmpty()) {
                    selectApi(apis.first())
                }
            }
        }
    }

    fun selectApi(api: ApiConfig) {
        _state.value = _state.value.copy(activeApi = api)
        viewModelScope.launch {
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
                _state.value = _state.value.copy(activeApi = null, activeConversation = null, messages = emptyList())
            }
        }
    }

    fun newConversation() {
        val api = _state.value.activeApi ?: return
        val conv = Conversation(apiConfigId = api.id, title = "New chat")
        viewModelScope.launch {
            repo.saveConversation(conv)
            openConversation(conv)
        }
    }

    fun openConversation(conv: Conversation) {
        _state.value = _state.value.copy(activeConversation = conv, messages = emptyList())
        viewModelScope.launch {
            repo.observeMessages(conv.id).collect { msgs ->
                _state.value = _state.value.copy(messages = msgs)
            }
        }
    }

    fun deleteConversation(conv: Conversation) {
        viewModelScope.launch {
            repo.deleteConversation(conv.id)
            if (_state.value.activeConversation?.id == conv.id) {
                _state.value = _state.value.copy(activeConversation = null, messages = emptyList())
            }
        }
    }

    fun sendMessage(text: String) {
        val api = _state.value.activeApi ?: return
        val conv = _state.value.activeConversation ?: run {
            newConversation()
            return
        }
        if (text.isBlank()) return

        viewModelScope.launch {
            val userMsg = Message(conversationId = conv.id, role = "user", content = text)
            repo.insertMessage(userMsg)

            // Auto-title on first user message
            val msgs = repo.listMessages(conv.id)
            if (msgs.count { it.role == "user" } == 1) {
                val title = text.take(40).replace("\n", " ")
                repo.renameConversation(conv.id, title)
            }

            val context = msgs.map { it.role to it.content } + listOf("user" to text)
            runStream(api, conv, context)
        }
    }

    private fun runStream(api: ApiConfig, conv: Conversation, context: List<Pair<String, String>>) {
        streamJob?.cancel()
        _state.value = _state.value.copy(isStreaming = true, streamingText = "", error = null)

        streamJob = viewModelScope.launch {
            val req = ChatRequest(
                baseUrl = api.baseUrl,
                apiKey = api.apiKey,
                model = api.modelName,
                messages = context,
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
                            repo.insertMessage(
                                Message(conversationId = conv.id, role = "assistant", content = content)
                            )
                        }
                        _state.value = _state.value.copy(isStreaming = false, streamingText = "")
                    }
                    is ChatEvent.Error -> {
                        _state.value = _state.value.copy(isStreaming = false, streamingText = "", error = ev.message)
                    }
                }
            }
        }
    }

    fun cancelStream() {
        streamJob?.cancel()
        _state.value = _state.value.copy(isStreaming = false, streamingText = "")
    }

    fun deleteMessage(id: String) {
        viewModelScope.launch { repo.deleteMessage(id) }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
