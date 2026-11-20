package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ai.AssistantChatHistoryStore
import com.example.ai.AssistantChatMessage
import com.example.ai.AssistantChatMode
import com.example.ai.AssistantUiState
import com.example.ai.ChatRole
import com.example.ai.GroundingSource
import com.example.ai.LibraryContextSummary
import com.example.ai.buildAssistantSystemInstruction
import com.example.ai.extractGroundingDetails
import com.google.firebase.Firebase
import com.google.firebase.ai.Chat
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GroundingMetadata
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.UUID

private const val TAG = "ProfileAssistantVM"
private const val INFERENCE_TIMEOUT_MS = 60_000L

class ProfileAssistantViewModel(
    private val historyStore: AssistantChatHistoryStore? = null
) : ViewModel() {

    private val _messages = MutableStateFlow<List<AssistantChatMessage>>(emptyList())
    val messages: StateFlow<List<AssistantChatMessage>> = _messages.asStateFlow()

    private val _uiState = MutableStateFlow<AssistantUiState>(AssistantUiState.Idle)
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    private val _selectedMode = MutableStateFlow(AssistantChatMode.GENERAL_SEARCH)
    val selectedMode: StateFlow<AssistantChatMode> = _selectedMode.asStateFlow()

    private val _searchGroundingEnabled = MutableStateFlow(true)
    val searchGroundingEnabled: StateFlow<Boolean> = _searchGroundingEnabled.asStateFlow()

    private val _librarySummary = MutableStateFlow(LibraryContextSummary())
    val librarySummary: StateFlow<LibraryContextSummary> = _librarySummary.asStateFlow()

    private val _lastFailedPrompt = MutableStateFlow<String?>(null)
    val lastFailedPrompt: StateFlow<String?> = _lastFailedPrompt.asStateFlow()

    private var activeChat: Chat? = null
    private var activeChatConfigKey: String? = null
    private var activeStreamJob: Job? = null

    init {
        if (historyStore != null) {
            viewModelScope.launch {
                val saved = historyStore.loadMessages()
                if (saved.isNotEmpty()) {
                    _messages.update { current ->
                        (saved + current)
                            .distinctBy { it.id }
                            .sortedBy { it.timestampMs }
                    }
                    activeChat = null
                }
            }
        }
    }

    fun updateLibraryContext(summary: LibraryContextSummary) {
        if (_librarySummary.value != summary) {
            _librarySummary.value = summary
            // Rebuild chat session on next message so updated library context is in system instruction
            activeChat = null
        }
    }

    fun selectMode(mode: AssistantChatMode) {
        if (_selectedMode.value == mode) return
        _selectedMode.value = mode
        _searchGroundingEnabled.value = mode.defaultGoogleSearchEnabled
        activeChat = null
    }

    fun setSearchGroundingEnabled(enabled: Boolean) {
        if (!_selectedMode.value.supportsGoogleSearch) return
        if (_searchGroundingEnabled.value == enabled) return
        _searchGroundingEnabled.value = enabled
        activeChat = null
    }

    fun clearConversation() {
        activeStreamJob?.cancel()
        activeStreamJob = null
        activeChat = null
        activeChatConfigKey = null
        _lastFailedPrompt.value = null
        _messages.value = emptyList()
        _uiState.value = AssistantUiState.Idle
        if (historyStore != null) {
            viewModelScope.launch {
                historyStore.clear()
            }
        }
    }

    fun stopGenerating() {
        activeStreamJob?.cancel()
        activeStreamJob = null
        _messages.update { list ->
            list.map { msg ->
                if (msg.isStreaming) msg.copy(isStreaming = false) else msg
            }.filterNot { msg ->
                msg.role == ChatRole.ASSISTANT && msg.text.isBlank()
            }
        }
        _uiState.value = AssistantUiState.Idle
        persistCurrentHistory()
    }

    fun retryLastFailed() {
        val prompt = _lastFailedPrompt.value ?: return
        _lastFailedPrompt.value = null
        sendMessage(prompt)
    }

    fun sendMessage(rawPrompt: String) {
        val prompt = rawPrompt.trim()
        if (prompt.isEmpty()) return
        if (_uiState.value is AssistantUiState.Loading || _uiState.value is AssistantUiState.Streaming) {
            return
        }

        val mode = _selectedMode.value
        val useSearch = mode.supportsGoogleSearch && _searchGroundingEnabled.value
        val userMsg = AssistantChatMessage(
            id = UUID.randomUUID().toString(),
            role = ChatRole.USER,
            text = prompt
        )
        val assistantMsgId = UUID.randomUUID().toString()
        val placeholderAssistantMsg = AssistantChatMessage(
            id = assistantMsgId,
            role = ChatRole.ASSISTANT,
            text = "",
            modelName = mode.modelName,
            modeLabel = mode.shortLabel,
            usedGoogleSearch = useSearch,
            isStreaming = true
        )

        val historyBeforeTurn = buildHistoryFromMessages(_messages.value)
        _lastFailedPrompt.value = null
        _messages.update { it + userMsg + placeholderAssistantMsg }
        _uiState.value = AssistantUiState.Loading

        activeStreamJob = viewModelScope.launch {
            val accumulatedText = StringBuilder()
            var latestGrounding: GroundingMetadata? = null

            try {
                val chat = getOrCreateChat(
                    mode = mode,
                    useSearch = useSearch,
                    historySnapshot = historyBeforeTurn
                )

                withTimeout(INFERENCE_TIMEOUT_MS) {
                    chat.sendMessageStream(prompt).collect { chunk ->
                        val chunkText = chunk.text.orEmpty()
                        if (chunkText.isNotEmpty()) {
                            accumulatedText.append(chunkText)
                        }
                        val chunkGrounding = chunk.candidates.firstOrNull()?.groundingMetadata
                        if (chunkGrounding != null) {
                            latestGrounding = chunkGrounding
                        }

                        val partialText = accumulatedText.toString()
                        val (queries, sources) = extractGroundingDetails(latestGrounding)
                        _uiState.value = AssistantUiState.Streaming(partialText)
                        updateAssistantMessage(
                            id = assistantMsgId,
                            text = partialText,
                            queries = queries,
                            sources = sources,
                            isStreaming = true
                        )
                    }
                }

                val finalText = accumulatedText.toString().trim()
                val (finalQueries, finalSources) = extractGroundingDetails(latestGrounding)
                if (finalText.isNotEmpty()) {
                    updateAssistantMessage(
                        id = assistantMsgId,
                        text = finalText,
                        queries = finalQueries,
                        sources = finalSources,
                        isStreaming = false
                    )
                    _uiState.value = AssistantUiState.Success(finalText)
                    persistCurrentHistory()
                } else {
                    removeMessage(assistantMsgId)
                    activeChat = null
                    _lastFailedPrompt.value = prompt
                    _uiState.value = AssistantUiState.Error("Gemini returned an empty response. Please try again.")
                    persistCurrentHistory()
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Gemini chat request timed out", e)
                finalizeOrRemovePartial(assistantMsgId, accumulatedText.toString(), latestGrounding)
                activeChat = null
                _lastFailedPrompt.value = prompt
                _uiState.value = AssistantUiState.Error(
                    "Response timed out after 60 seconds. Check your connection and try again."
                )
                persistCurrentHistory()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Firebase AI Logic chat failed", e)
                finalizeOrRemovePartial(assistantMsgId, accumulatedText.toString(), latestGrounding)
                activeChat = null
                _lastFailedPrompt.value = prompt
                _uiState.value = AssistantUiState.Error(
                    e.localizedMessage?.takeIf { it.isNotBlank() }
                        ?: "Unable to reach Gemini right now. Please try again."
                )
                persistCurrentHistory()
            }
        }
    }

    private fun getOrCreateChat(
        mode: AssistantChatMode,
        useSearch: Boolean,
        historySnapshot: List<Content>
    ): Chat {
        val summary = _librarySummary.value
        val configKey = "${mode.name}|$useSearch|${summary.hashCode()}"
        val existing = activeChat
        if (existing != null && activeChatConfigKey == configKey) {
            return existing
        }

        val model = createGenerativeModel(mode, useSearch, summary)
        val created = model.startChat(history = historySnapshot)
        activeChat = created
        activeChatConfigKey = configKey
        return created
    }

    private fun createGenerativeModel(
        mode: AssistantChatMode,
        useSearch: Boolean,
        summary: LibraryContextSummary
    ): GenerativeModel {
        val systemText = buildAssistantSystemInstruction(
            mode = mode,
            searchGroundingEnabled = useSearch,
            librarySummary = summary
        )
        val tools = if (useSearch) listOf(Tool.googleSearch()) else emptyList()
        return Firebase.ai.generativeModel(
            modelName = mode.modelName,
            tools = tools,
            systemInstruction = content { text(systemText) }
        )
    }

    private fun buildHistoryFromMessages(messages: List<AssistantChatMessage>): List<Content> =
        messages
            .filter { !it.isStreaming && it.text.isNotBlank() }
            .map { msg ->
                val role = if (msg.role == ChatRole.USER) "user" else "model"
                content(role) { text(msg.text) }
            }

    private fun updateAssistantMessage(
        id: String,
        text: String,
        queries: List<String>,
        sources: List<GroundingSource>,
        isStreaming: Boolean
    ) {
        _messages.update { list ->
            list.map { msg ->
                if (msg.id == id) {
                    msg.copy(
                        text = text,
                        searchQueries = queries,
                        sources = sources,
                        isStreaming = isStreaming
                    )
                } else {
                    msg
                }
            }
        }
    }

    private fun removeMessage(id: String) {
        _messages.update { list -> list.filterNot { it.id == id } }
    }

    private fun finalizeOrRemovePartial(
        id: String,
        partial: String,
        grounding: GroundingMetadata?
    ) {
        val trimmed = partial.trim()
        if (trimmed.isEmpty()) {
            removeMessage(id)
        } else {
            val (queries, sources) = extractGroundingDetails(grounding)
            updateAssistantMessage(
                id = id,
                text = trimmed,
                queries = queries,
                sources = sources,
                isStreaming = false
            )
        }
    }

    private fun persistCurrentHistory() {
        val store = historyStore ?: return
        val snapshot = _messages.value
        viewModelScope.launch {
            store.saveMessages(snapshot)
        }
    }

    companion object {
        fun factory(historyStore: AssistantChatHistoryStore): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ProfileAssistantViewModel(historyStore) as T
                }
            }
    }
}
