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
import com.example.auth.FazaAiCloudStore
import com.example.auth.FazaGoogleAccount
import com.google.firebase.Firebase
import com.google.firebase.ai.Chat
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GroundingMetadata
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.UUID

private const val TAG = "ProfileAssistantVM"
private const val INFERENCE_TIMEOUT_MS = 60_000L

class ProfileAssistantViewModel(
    private val historyStore: AssistantChatHistoryStore? = null,
    private val cloudStore: FazaAiCloudStore? = null
) : ViewModel() {

    private val _activeAccount = MutableStateFlow<FazaGoogleAccount?>(null)
    val activeAccount: StateFlow<FazaGoogleAccount?> = _activeAccount.asStateFlow()

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
    private var cloudObserveJob: Job? = null
    private var localLoadJob: Job? = null

    /**
     * Binds FAZA AI strictly to [account].
     * - When [account] is null (signed out), all in-memory chat history, active streams, and
     *   Firestore listeners are cleared immediately so logged-out state never exposes user data.
     * - When switching between Google accounts, previous listeners/chat context are torn down and
     *   only the newly authenticated account's isolated history (keyed by Google `sub` / Firebase UID)
     *   is loaded.
     */
    fun bindAccount(account: FazaGoogleAccount?) {
        val previous = _activeAccount.value
        if (account == null) {
            if (previous != null || _messages.value.isNotEmpty() || cloudObserveJob != null) {
                resetSessionState()
            }
            _activeAccount.value = null
            return
        }

        if (previous != null &&
            previous.firebaseUid == account.firebaseUid &&
            previous.fazaAiAccountId == account.fazaAiAccountId
        ) {
            // Same Google account refreshed metadata — keep active chat session intact without duplicating.
            _activeAccount.value = account
            return
        }

        resetSessionState()
        _activeAccount.value = account
        val boundAccountId = account.fazaAiAccountId
        val boundUid = account.firebaseUid

        if (historyStore != null) {
            localLoadJob = viewModelScope.launch {
                val saved = historyStore.loadMessages(boundAccountId)
                if (_activeAccount.value?.fazaAiAccountId == boundAccountId && saved.isNotEmpty()) {
                    _messages.update { current ->
                        (saved + current)
                            .distinctBy { it.id }
                            .sortedBy { it.timestampMs }
                    }
                    activeChat = null
                }
            }
        }

        if (cloudStore != null && boundUid.isNotBlank()) {
            cloudObserveJob = viewModelScope.launch {
                cloudStore.observeUserMessages(boundUid)
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        Log.w(TAG, "Cloud chat sync paused for $boundUid", firestoreError ?: error)
                    }
                    .collect { remoteMessages ->
                        if (_activeAccount.value?.firebaseUid != boundUid) return@collect
                        if (remoteMessages.isNotEmpty()) {
                            _messages.update { current ->
                                val activeStreaming = current.filter { it.isStreaming }
                                (remoteMessages + current.filterNot { it.isStreaming } + activeStreaming)
                                    .distinctBy { it.id }
                                    .sortedBy { it.timestampMs }
                            }
                            activeChat = null
                            historyStore?.saveMessages(boundAccountId, _messages.value)
                        }
                    }
            }
        }
    }

    /**
     * Cancels active Firestore listeners and streaming jobs cleanly BEFORE Firebase sign-out.
     */
    fun detachBeforeSignOut() {
        resetSessionState()
        _activeAccount.value = null
    }

    private fun resetSessionState() {
        activeStreamJob?.cancel()
        activeStreamJob = null
        cloudObserveJob?.cancel()
        cloudObserveJob = null
        localLoadJob?.cancel()
        localLoadJob = null
        activeChat = null
        activeChatConfigKey = null
        _lastFailedPrompt.value = null
        _messages.value = emptyList()
        _uiState.value = AssistantUiState.Idle
    }

    fun updateLibraryContext(summary: LibraryContextSummary) {
        if (_librarySummary.value != summary) {
            _librarySummary.value = summary
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
        val account = _activeAccount.value ?: return
        activeStreamJob?.cancel()
        activeStreamJob = null
        activeChat = null
        activeChatConfigKey = null
        _lastFailedPrompt.value = null
        _messages.value = emptyList()
        _uiState.value = AssistantUiState.Idle
        viewModelScope.launch {
            historyStore?.clear(account.fazaAiAccountId)
            cloudStore?.clearMessages(account.firebaseUid)
        }
    }

    fun stopGenerating() {
        val account = _activeAccount.value
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
        if (account != null) {
            persistCurrentHistory(account)
        }
    }

    fun retryLastFailed() {
        val prompt = _lastFailedPrompt.value ?: return
        _lastFailedPrompt.value = null
        sendMessage(prompt)
    }

    fun sendMessage(rawPrompt: String) {
        val account = _activeAccount.value
        if (account == null) {
            _uiState.value = AssistantUiState.Error(
                "Please sign in with your Google account in the Profile tab to use FAZA AI Chat."
            )
            return
        }

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

        // Persist user prompt immediately to isolated local & cloud stores for this account.
        persistSingleMessage(account, userMsg)

        activeStreamJob = viewModelScope.launch {
            val accumulatedText = StringBuilder()
            var latestGrounding: GroundingMetadata? = null

            try {
                val chat = getOrCreateChat(
                    account = account,
                    mode = mode,
                    useSearch = useSearch,
                    historySnapshot = historyBeforeTurn
                )

                withTimeout(INFERENCE_TIMEOUT_MS) {
                    chat.sendMessageStream(prompt).collect { chunk ->
                        if (_activeAccount.value?.firebaseUid != account.firebaseUid) {
                            throw CancellationException("Account changed during generation")
                        }
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

                if (_activeAccount.value?.firebaseUid != account.firebaseUid) return@launch

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
                    val completedAssistant = _messages.value.firstOrNull { it.id == assistantMsgId }
                    if (completedAssistant != null) {
                        persistSingleMessage(account, completedAssistant)
                    }
                    persistCurrentHistory(account)
                } else {
                    removeMessage(assistantMsgId)
                    activeChat = null
                    _lastFailedPrompt.value = prompt
                    _uiState.value = AssistantUiState.Error("Gemini returned an empty response. Please try again.")
                    persistCurrentHistory(account)
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Gemini chat request timed out", e)
                finalizeOrRemovePartial(assistantMsgId, accumulatedText.toString(), latestGrounding)
                activeChat = null
                _lastFailedPrompt.value = prompt
                _uiState.value = AssistantUiState.Error(
                    "Response timed out after 60 seconds. Check your connection and try again."
                )
                persistCurrentHistory(account)
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
                persistCurrentHistory(account)
            }
        }
    }

    private fun getOrCreateChat(
        account: FazaGoogleAccount,
        mode: AssistantChatMode,
        useSearch: Boolean,
        historySnapshot: List<Content>
    ): Chat {
        val summary = _librarySummary.value
        val configKey = "${account.fazaAiAccountId}|${mode.name}|$useSearch|${summary.hashCode()}"
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

    private fun persistSingleMessage(account: FazaGoogleAccount, message: AssistantChatMessage) {
        val snapshot = _messages.value
        viewModelScope.launch {
            historyStore?.saveMessages(account.fazaAiAccountId, snapshot)
            cloudStore?.saveMessage(account, message)
        }
    }

    private fun persistCurrentHistory(account: FazaGoogleAccount) {
        val store = historyStore ?: return
        val snapshot = _messages.value
        viewModelScope.launch {
            store.saveMessages(account.fazaAiAccountId, snapshot)
        }
    }

    companion object {
        fun factory(
            historyStore: AssistantChatHistoryStore,
            cloudStore: FazaAiCloudStore? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ProfileAssistantViewModel(historyStore, cloudStore) as T
                }
            }
    }
}
