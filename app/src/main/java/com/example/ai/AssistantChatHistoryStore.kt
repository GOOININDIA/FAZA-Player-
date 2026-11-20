package com.example.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

private const val HISTORY_STORE_TAG = "AssistantHistoryStore"
private const val HISTORY_FILE_NAME = "assistant_chat_history.json"
private const val MAX_PERSISTED_MESSAGES = 250

class AssistantChatHistoryStore(
    private val historyFile: File
) {
    constructor(context: Context) : this(File(context.applicationContext.filesDir, HISTORY_FILE_NAME))

    private val mutex = Mutex()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun loadMessages(): List<AssistantChatMessage> = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!historyFile.exists()) return@withLock emptyList()
            runCatching {
                val raw = historyFile.readText()
                if (raw.isBlank()) return@withLock emptyList()
                json.decodeFromString<List<AssistantChatMessage>>(raw)
                    .filter { it.text.isNotBlank() }
                    .map { it.copy(isStreaming = false) }
                    .distinctBy { it.id }
                    .sortedBy { it.timestampMs }
            }.getOrElse { error ->
                Log.e(HISTORY_STORE_TAG, "Failed to load assistant chat history", error)
                emptyList()
            }
        }
    }

    suspend fun saveMessages(messages: List<AssistantChatMessage>) = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                val sanitized = messages
                    .filter { !it.isStreaming && it.text.isNotBlank() }
                    .distinctBy { it.id }
                    .sortedBy { it.timestampMs }
                    .takeLast(MAX_PERSISTED_MESSAGES)
                if (sanitized.isEmpty()) {
                    if (historyFile.exists()) {
                        historyFile.delete()
                    }
                    return@withLock
                }
                historyFile.parentFile?.mkdirs()
                val tempFile = File(historyFile.parentFile, "$HISTORY_FILE_NAME.tmp")
                tempFile.writeText(json.encodeToString(sanitized))
                if (!tempFile.renameTo(historyFile)) {
                    historyFile.writeText(json.encodeToString(sanitized))
                    tempFile.delete()
                }
            }.onFailure { error ->
                Log.e(HISTORY_STORE_TAG, "Failed to save assistant chat history", error)
            }
        }
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                if (historyFile.exists()) {
                    historyFile.delete()
                }
            }.onFailure { error ->
                Log.e(HISTORY_STORE_TAG, "Failed to clear assistant chat history", error)
            }
        }
    }
}
