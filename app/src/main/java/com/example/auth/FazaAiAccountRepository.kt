package com.example.auth

import android.content.Context
import androidx.annotation.Keep
import com.example.R
import com.example.ai.AssistantChatMessage
import com.example.ai.ChatRole
import com.example.ai.GroundingSource
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private const val COLLECTION_USERS = "users"
private const val SUBCOLLECTION_AI_MESSAGES = "ai_messages"
private const val MAX_DISPLAY_NAME_LEN = 120
private const val MAX_EMAIL_LEN = 254
private const val MAX_PHOTO_URL_LEN = 1024
private const val MAX_MESSAGE_TEXT_LEN = 32_000
private const val MAX_MODEL_NAME_LEN = 80
private const val MAX_MODE_LABEL_LEN = 40
private const val MAX_SEARCH_QUERIES = 20
private const val MAX_SOURCES = 10

interface FazaAiCloudStore {
    suspend fun upsertUserAccount(account: FazaGoogleAccount): Result<Unit>
    fun observeUserMessages(userId: String): Flow<List<AssistantChatMessage>>
    suspend fun saveMessage(account: FazaGoogleAccount, message: AssistantChatMessage): Result<Unit>
    suspend fun clearMessages(userId: String): Result<Unit>
}

@Keep
@IgnoreExtraProperties
data class FazaUserAccountDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val googleSub: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val provider: String = "google.com",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    @ServerTimestamp val lastSignInAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FazaAiMessageDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val googleSub: String = "",
    val role: String = "USER",
    val text: String = "",
    val modelName: String = "",
    val modeLabel: String = "",
    val usedGoogleSearch: Boolean = false,
    val searchQueries: List<String> = emptyList(),
    val sources: List<Map<String, Any>> = emptyList(),
    val timestampMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

class FazaAiAccountRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) : FazaAiCloudStore {

    // CRITICAL: Always resolve named database via R.string.firestore_database_id (never (default))
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    override suspend fun upsertUserAccount(account: FazaGoogleAccount): Result<Unit> {
        var docPath = "$COLLECTION_USERS/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            check(uid == account.firebaseUid) { "Signed-in user changed; refusing cross-user account write." }
            val safeSub = account.fazaAiAccountId.take(128)
            require(safeSub.isNotBlank()) { "Google account subject ID must not be blank." }
            val safeName = account.resolvedDisplayName.take(MAX_DISPLAY_NAME_LEN).ifBlank { "FAZA User" }
            val safeEmail = account.email.trim().take(MAX_EMAIL_LEN).ifBlank { "$uid@google.local" }
            val safePhoto = account.photoUrl?.trim()?.takeIf { it.isNotEmpty() && it.length <= MAX_PHOTO_URL_LEN }

            val docRef = db.collection(COLLECTION_USERS).document(uid)
            docPath = docRef.path
            val existingSnapshot = docRef.get().await()

            if (!existingSnapshot.exists()) {
                val createPayload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "googleSub" to safeSub,
                    "displayName" to safeName,
                    "email" to safeEmail,
                    "provider" to "google.com",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "lastSignInAt" to FieldValue.serverTimestamp()
                )
                if (safePhoto != null) {
                    createPayload["photoUrl"] = safePhoto
                }
                docRef.set(createPayload).await()
            } else {
                // Partial update preserves immutable userId, googleSub, provider, and createdAt.
                val updatePayload = mutableMapOf<String, Any>(
                    "displayName" to safeName,
                    "email" to safeEmail,
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "lastSignInAt" to FieldValue.serverTimestamp()
                )
                if (safePhoto != null) {
                    updatePayload["photoUrl"] = safePhoto
                }
                docRef.update(updatePayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    override fun observeUserMessages(userId: String): Flow<List<AssistantChatMessage>> = flow {
        if (userId.isBlank()) {
            emitAll(emptyFlow())
            return@flow
        }
        val currentUid = auth.currentUser?.uid
        if (currentUid.isNullOrBlank() || currentUid != userId) {
            emitAll(emptyFlow())
            return@flow
        }
        val path = "$COLLECTION_USERS/$userId/$SUBCOLLECTION_AI_MESSAGES"
        emitAll(
            db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(SUBCOLLECTION_AI_MESSAGES)
                .orderBy("timestampMs", Query.Direction.ASCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc -> doc.toAssistantChatMessage() }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    override suspend fun saveMessage(
        account: FazaGoogleAccount,
        message: AssistantChatMessage
    ): Result<Unit> {
        var docPath = "$COLLECTION_USERS/${auth.currentUser?.uid ?: "unauthenticated"}/$SUBCOLLECTION_AI_MESSAGES/${message.id}"
        return try {
            val uid = requireUserId()
            check(uid == account.firebaseUid) { "Signed-in user changed; refusing cross-user message write." }
            val cleanId = sanitizeFirestoreDocId(message.id)
            val cleanText = message.text.trim().take(MAX_MESSAGE_TEXT_LEN)
            require(cleanText.isNotEmpty()) { "Message text must not be blank." }
            val safeSub = account.fazaAiAccountId.take(128)
            require(safeSub.isNotBlank()) { "Google account subject ID must not be blank." }

            val docRef = db.collection(COLLECTION_USERS)
                .document(uid)
                .collection(SUBCOLLECTION_AI_MESSAGES)
                .document(cleanId)
            docPath = docRef.path

            val safeQueries = message.searchQueries
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .take(MAX_SEARCH_QUERIES)
            val safeSources = message.sources
                .take(MAX_SOURCES)
                .map { src ->
                    buildMap<String, Any> {
                        put("title", src.title.take(200))
                        put("uri", src.uri.take(1024))
                        src.domain?.takeIf { it.isNotBlank() }?.let { put("domain", it.take(200)) }
                    }
                }
            val safeTimestampMs = message.timestampMs.coerceAtLeast(1L)
            val safeModelName = message.modelName.take(MAX_MODEL_NAME_LEN)
            val safeModeLabel = message.modeLabel.take(MAX_MODE_LABEL_LEN)

            val existing = docRef.get().await()
            if (!existing.exists()) {
                // Never include `@DocumentId` key "id" inside write payload.
                val createPayload = mapOf<String, Any>(
                    "userId" to uid,
                    "googleSub" to safeSub,
                    "role" to message.role.name,
                    "text" to cleanText,
                    "modelName" to safeModelName,
                    "modeLabel" to safeModeLabel,
                    "usedGoogleSearch" to message.usedGoogleSearch,
                    "searchQueries" to safeQueries,
                    "sources" to safeSources,
                    "timestampMs" to safeTimestampMs,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            } else {
                val updatePayload = mapOf<String, Any>(
                    "text" to cleanText,
                    "modelName" to safeModelName,
                    "modeLabel" to safeModeLabel,
                    "usedGoogleSearch" to message.usedGoogleSearch,
                    "searchQueries" to safeQueries,
                    "sources" to safeSources,
                    "timestampMs" to safeTimestampMs,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updatePayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun clearMessages(userId: String): Result<Unit> {
        val path = "$COLLECTION_USERS/$userId/$SUBCOLLECTION_AI_MESSAGES"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user delete." }
            val snapshot = db.collection(COLLECTION_USERS)
                .document(uid)
                .collection(SUBCOLLECTION_AI_MESSAGES)
                .get()
                .await()
            val chunks = snapshot.documents.chunked(400)
            for (chunk in chunks) {
                val batch = db.batch()
                chunk.forEach { doc -> batch.delete(doc.reference) }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    private fun DocumentSnapshot.toAssistantChatMessage(): AssistantChatMessage? {
        val doc = toObject(FazaAiMessageDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?: return null
        val text = doc.text.trim()
        if (text.isEmpty()) return null
        val parsedRole = ChatRole.entries.firstOrNull { it.name == doc.role } ?: ChatRole.ASSISTANT
        val parsedSources = doc.sources.mapNotNull { raw ->
            val uri = (raw["uri"] as? String)?.trim().orEmpty()
            if (uri.isEmpty()) return@mapNotNull null
            val title = (raw["title"] as? String)?.trim().orEmpty().ifBlank { uri }
            val domain = (raw["domain"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
            GroundingSource(title = title, uri = uri, domain = domain)
        }
        val resolvedTimestamp = doc.timestampMs.takeIf { it > 0L }
            ?: doc.createdAt?.toDate()?.time
            ?: System.currentTimeMillis()
        return AssistantChatMessage(
            id = doc.id.ifBlank { id },
            role = parsedRole,
            text = text,
            modelName = doc.modelName,
            modeLabel = doc.modeLabel,
            usedGoogleSearch = doc.usedGoogleSearch,
            searchQueries = doc.searchQueries,
            sources = parsedSources,
            isStreaming = false,
            timestampMs = resolvedTimestamp
        )
    }

    private fun sanitizeFirestoreDocId(rawId: String): String {
        val cleaned = rawId.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128)
        require(cleaned.isNotEmpty()) { "Message ID must not be blank." }
        return cleaned
    }
}
