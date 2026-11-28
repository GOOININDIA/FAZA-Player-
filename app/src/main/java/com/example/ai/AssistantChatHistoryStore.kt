package com.example.ai

import android.content.Context
import android.util.Log
import com.example.auth.computeAccountStorageKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

private const val HISTORY_STORE_TAG = "AssistantHistoryStore"
private const val HISTORY_FILE_NAME = "assistant_chat_history.json"
private const val ACCOUNT_HISTORY_DIR = "faza_ai_accounts"
private const val MAX_PERSISTED_MESSAGES = 250

class AssistantChatHistoryStore(
    private val historyFile: File,
    private val accountsBaseDir: File = File(historyFile.parentFile ?: File("."), ACCOUNT_HISTORY_DIR)
) {
    constructor(context: Context) : this(
        historyFile = File(context.applicationContext.filesDir, HISTORY_FILE_NAME),
        accountsBaseDir = File(context.applicationContext.filesDir, ACCOUNT_HISTORY_DIR)
    )

    private val mutex = Mutex()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Resolves an isolated, deterministic file strictly scoped to [accountId] (Google `sub`).
     * Never shares history between different Google accounts.
     */
    internal fun fileForAccount(accountId: String): File {
        val storageKey = computeAccountStorageKey(accountId)
        return File(accountsBaseDir, "chat_$storageKey.json")
    }

    suspend fun loadMessages(accountId: String): List<AssistantChatMessage> {
        if (accountId.isBlank()) return emptyList()
        return loadFromFile(fileForAccount(accountId))
    }

    suspend fun saveMessages(accountId: String, messages: List<AssistantChatMessage>) {
        if (accountId.isBlank()) return
        saveToFile(fileForAccount(accountId), messages)
    }

    suspend fun clear(accountId: String) {
        if (accountId.isBlank()) return
        clearFile(fileForAccount(accountId))
    }

    suspend fun loadMessages(): List<AssistantChatMessage> = loadFromFile(historyFile)

    suspend fun saveMessages(messages: List<AssistantChatMessage>) = saveToFile(historyFile, messages)

    suspend fun clear() = clearFile(historyFile)

    private suspend fun loadFromFile(targetFile: File): List<AssistantChatMessage> = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!targetFile.exists()) return@withLock emptyList()
            runCatching {
                val raw = targetFile.readText()
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

    private suspend fun saveToFile(
        targetFile: File,
        messages: List<AssistantChatMessage>
    ) = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                val sanitized = messages
                    .filter { !it.isStreaming && it.text.isNotBlank() }
                    .distinctBy { it.id }
                    .sortedBy { it.timestampMs }
                    .takeLast(MAX_PERSISTED_MESSAGES)
                if (sanitized.isEmpty()) {
                    if (targetFile.exists()) {
                        targetFile.delete()
                    }
                    return@withLock
                }
                targetFile.parentFile?.mkdirs()
                val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp")
                tempFile.writeText(json.encodeToString(sanitized))
                if (!tempFile.renameTo(targetFile)) {
                    targetFile.writeText(json.encodeToString(sanitized))
                    tempFile.delete()
                }
            }.onFailure { error ->
                Log.e(HISTORY_STORE_TAG, "Failed to save assistant chat history", error)
            }
        }
    }

    private suspend fun clearFile(targetFile: File) = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                if (targetFile.exists()) {
                    targetFile.delete()
                }
            }.onFailure { error ->
                Log.e(HISTORY_STORE_TAG, "Failed to clear assistant chat history", error)
            }
        }
    }
}
