package com.example.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.Firebase
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

private const val AUTH_TAG = "Auth"
private const val SIGN_IN_VERIFY_TIMEOUT_MS = 25_000L
private const val SESSION_CHECK_TIMEOUT_MS = 12_000L

interface FazaAuthGateway {
    val currentAccountFlow: Flow<FazaGoogleAccount?>
    fun getCurrentAccount(): FazaGoogleAccount?
    fun addGlobalAuthStateListener(listener: GlobalAuthStateListener): AuthListenerRegistration
    suspend fun attemptSilentSignIn(context: Context): Result<FazaGoogleAccount?>
    suspend fun signInWithGoogle(activity: Activity): Result<FazaGoogleAccount>
    suspend fun signOut(context: Context): Result<Unit>
    suspend fun verifyActiveSession(): Result<FazaGoogleAccount?>
}

/**
 * Production Google Sign-In implementation backed by Jetpack Credential Manager
 * (`GetSignInWithGoogleOption` for interactive sign-in, `GetGoogleIdOption` for silent startup)
 * and Firebase Authentication for server-side Google ID token verification & session persistence.
 */
class FirebaseGoogleAuthGateway : FazaAuthGateway {

    private val explicitSignOutInProgress = AtomicBoolean(false)

    private fun firebaseAuthOrNull(): FirebaseAuth? = runCatching { Firebase.auth }.getOrNull()

    override val currentAccountFlow: Flow<FazaGoogleAccount?> = callbackFlow {
        val auth = firebaseAuthOrNull()
        if (auth == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.toFazaGoogleAccount())
        }
        auth.addAuthStateListener(listener)
        trySend(auth.currentUser?.toFazaGoogleAccount())
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override fun getCurrentAccount(): FazaGoogleAccount? {
        return firebaseAuthOrNull()?.currentUser?.toFazaGoogleAccount()
    }

    /**
     * Attaches global Firebase [FirebaseAuth.AuthStateListener] and [FirebaseAuth.IdTokenListener]
     * callbacks so user logout, account switches, and real-time token revocations / expirations
     * immediately propagate to the Profile ViewModel UI state.
     */
    override fun addGlobalAuthStateListener(
        listener: GlobalAuthStateListener
    ): AuthListenerRegistration {
        val auth = firebaseAuthOrNull()
        if (auth == null) {
            listener.onAuthStateChanged(FazaAuthEvent.SignedOut)
            return AuthListenerRegistration {}
        }

        val isDisposed = AtomicBoolean(false)
        val lastKnownUid = AtomicReference<String?>(auth.currentUser?.uid)
        val lastDispatchedEvent = AtomicReference<FazaAuthEvent?>(null)

        fun dispatchIfChanged(event: FazaAuthEvent) {
            if (isDisposed.get()) return
            val previous = lastDispatchedEvent.getAndSet(event)
            if (previous != event) {
                listener.onAuthStateChanged(event)
            }
        }

        fun evaluateAuthState(firebaseAuth: FirebaseAuth) {
            val currentUser = firebaseAuth.currentUser
            if (currentUser != null) {
                lastKnownUid.set(currentUser.uid)
                dispatchIfChanged(FazaAuthEvent.SignedIn(currentUser.toFazaGoogleAccount()))
            } else {
                val previousUid = lastKnownUid.getAndSet(null)
                if (previousUid != null && !explicitSignOutInProgress.get()) {
                    Log.w(AUTH_TAG, "Global AuthStateListener detected session expiration for uid=$previousUid")
                    dispatchIfChanged(FazaAuthEvent.SessionExpired())
                } else {
                    dispatchIfChanged(FazaAuthEvent.SignedOut)
                }
            }
        }

        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            evaluateAuthState(firebaseAuth)
        }

        val idTokenListener = FirebaseAuth.IdTokenListener { firebaseAuth ->
            val currentUser = firebaseAuth.currentUser
            if (currentUser == null) {
                evaluateAuthState(firebaseAuth)
                return@IdTokenListener
            }
            // Lightweight token check catches server-side token revocation / expiration in real time.
            currentUser.getIdToken(false).addOnFailureListener { error ->
                if (isDisposed.get()) return@addOnFailureListener
                if (error is FirebaseAuthInvalidUserException ||
                    error is FirebaseAuthInvalidCredentialsException
                ) {
                    Log.w(AUTH_TAG, "Global IdTokenListener detected expired/revoked token", error)
                    lastKnownUid.set(null)
                    runCatching { firebaseAuth.signOut() }
                    dispatchIfChanged(FazaAuthEvent.SessionExpired())
                }
            }
        }

        auth.addAuthStateListener(authStateListener)
        auth.addIdTokenListener(idTokenListener)
        evaluateAuthState(auth)

        return AuthListenerRegistration {
            if (isDisposed.compareAndSet(false, true)) {
                runCatching { auth.removeAuthStateListener(authStateListener) }
                runCatching { auth.removeIdTokenListener(idTokenListener) }
            }
        }
    }

    override suspend fun attemptSilentSignIn(context: Context): Result<FazaGoogleAccount?> {
        val existing = getCurrentAccount()
        if (existing != null) {
            return Result.success(existing)
        }
        val auth = firebaseAuthOrNull() ?: return Result.success(null)
        val clientId = try {
            context.getString(R.string.default_web_client_id).trim()
        } catch (_: Exception) {
            return Result.success(null)
        }
        if (clientId.isEmpty()) return Result.success(null)

        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val cred = credentialManager.getCredential(context, request).credential
            if (cred is CustomCredential &&
                cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val idToken = GoogleIdTokenCredential.createFrom(cred.data).idToken
                val firebaseResult = auth.signInWithCredential(
                    GoogleAuthProvider.getCredential(idToken, null)
                ).await()
                val user = firebaseResult.user
                Result.success(user?.toFazaGoogleAccount(verifiedIdToken = idToken))
            } else {
                Result.success(null)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: NoCredentialException) {
            Result.success(null)
        } catch (_: GetCredentialCancellationException) {
            Result.success(null)
        } catch (e: Exception) {
            Log.d(AUTH_TAG, "Silent Google Sign-In unavailable: ${e.message}")
            Result.success(null)
        }
    }

    override suspend fun signInWithGoogle(activity: Activity): Result<FazaGoogleAccount> {
        val auth = firebaseAuthOrNull()
            ?: return Result.failure(
                IllegalStateException("Authentication service is unavailable. Please try again.")
            )

        val clientId = try {
            activity.getString(R.string.default_web_client_id).trim()
        } catch (_: Exception) {
            return Result.failure(
                IllegalStateException(
                    "Google Sign-In configuration missing: default_web_client_id not found."
                )
            )
        }
        if (clientId.isEmpty()) {
            return Result.failure(
                IllegalStateException(
                    "Google Sign-In configuration missing: default_web_client_id is empty."
                )
            )
        }

        val credentialManager = CredentialManager.create(activity)
        // One credential option per GetCredentialRequest keeps Android API 36 bottom sheet responsive.
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        return try {
            val cred = credentialManager.getCredential(activity, request).credential
            if (cred is CustomCredential &&
                cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCredential = GoogleIdTokenCredential.createFrom(cred.data)
                val idToken = googleCredential.idToken
                val authResult = withTimeout(SIGN_IN_VERIFY_TIMEOUT_MS) {
                    auth.signInWithCredential(
                        GoogleAuthProvider.getCredential(idToken, null)
                    ).await()
                }
                val firebaseUser = authResult.user
                    ?: return Result.failure(
                        IllegalStateException("Google Sign-In completed without a user profile.")
                    )
                val account = firebaseUser.toFazaGoogleAccount(
                    verifiedIdToken = idToken,
                    fallbackDisplayName = googleCredential.displayName,
                    fallbackEmail = googleCredential.id,
                    fallbackPhotoUrl = googleCredential.profilePictureUri?.toString()
                )
                Result.success(account)
            } else {
                Result.failure(
                    IllegalStateException("Unexpected credential type returned by Google Sign-In.")
                )
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(AUTH_TAG, "Google Sign-In verification timed out", e)
            Result.failure(
                AuthNetworkTimeoutException(
                    message = "Google Sign-In timed out. Check your internet connection and try again.",
                    cause = e
                )
            )
        } catch (e: GetCredentialCancellationException) {
            Log.w(AUTH_TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            Result.failure(AuthCancelledException(cause = e))
        } catch (e: CancellationException) {
            throw e
        } catch (e: NoCredentialException) {
            Log.w(AUTH_TAG, "No Google account available on device: ${e.message}", e)
            Result.failure(
                IllegalStateException(
                    "No Google account was found on this device. Add a Google account in Android Settings and try again."
                )
            )
        } catch (e: GoogleIdTokenParsingException) {
            Log.e(AUTH_TAG, "Invalid Google ID token response", e)
            Result.failure(
                IllegalStateException("Could not verify Google account credentials. Please try again.")
            )
        } catch (e: FirebaseNetworkException) {
            Log.e(AUTH_TAG, "Network error during Google Sign-In", e)
            Result.failure(
                IllegalStateException("Network connection required to sign in with Google. Check your connection and try again.")
            )
        } catch (e: FirebaseAuthException) {
            Log.e(AUTH_TAG, "Firebase authentication error: ${e.errorCode}", e)
            Result.failure(
                IllegalStateException(
                    "Unable to verify your Google account right now. Please try signing in again."
                )
            )
        } catch (e: GetCredentialException) {
            Log.e(AUTH_TAG, "Credential Manager error during Google Sign-In", e)
            Result.failure(
                IllegalStateException(
                    e.errorMessage?.toString()?.takeIf { it.isNotBlank() }
                        ?: "Google Sign-In failed. Please try again."
                )
            )
        } catch (e: Exception) {
            Log.e(AUTH_TAG, "Google Sign-In failed", e)
            Result.failure(
                IllegalStateException(
                    e.localizedMessage?.takeIf { it.isNotBlank() }
                        ?: "Google Sign-In failed. Please try again."
                )
            )
        }
    }

    override suspend fun signOut(context: Context): Result<Unit> = withContext(NonCancellable) {
        explicitSignOutInProgress.set(true)
        val credentialManager = CredentialManager.create(context)
        try {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(AUTH_TAG, "Failed to clear credential state: ${e.message}")
            } finally {
                runCatching { firebaseAuthOrNull()?.signOut() }
            }
            Result.success(Unit)
        } finally {
            explicitSignOutInProgress.set(false)
        }
    }

    override suspend fun verifyActiveSession(): Result<FazaGoogleAccount?> {
        val auth = firebaseAuthOrNull() ?: return Result.success(null)
        val user = auth.currentUser ?: return Result.success(null)
        return try {
            withTimeout(SESSION_CHECK_TIMEOUT_MS) {
                user.reload().await()
                user.getIdToken(false).await()
            }
            Result.success(auth.currentUser?.toFazaGoogleAccount())
        } catch (e: TimeoutCancellationException) {
            Log.w(AUTH_TAG, "Session verification timed out; keeping cached session and notifying user.", e)
            Result.failure(
                AuthNetworkTimeoutException(
                    message = "Connection timed out while verifying your Google session. Check your connection and try again.",
                    cause = e
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseAuthInvalidUserException) {
            Log.w(AUTH_TAG, "Session expired or user revoked; signing out cleanly.", e)
            withContext(NonCancellable) {
                runCatching { auth.signOut() }
            }
            Result.failure(
                AuthSessionExpiredException(
                    message = "Your Google session expired. Please sign in again to continue using FAZA AI.",
                    cause = e
                )
            )
        } catch (e: FirebaseNetworkException) {
            // Keep persisted session usable while offline; FAZA AI calls will check connectivity.
            Result.success(user.toFazaGoogleAccount())
        } catch (e: Exception) {
            Log.w(AUTH_TAG, "Session check warning: ${e.message}")
            Result.success(user.toFazaGoogleAccount())
        }
    }
}

private fun FirebaseUser.toFazaGoogleAccount(
    verifiedIdToken: String? = null,
    fallbackDisplayName: String? = null,
    fallbackEmail: String? = null,
    fallbackPhotoUrl: String? = null
): FazaGoogleAccount {
    val googleProviderInfo = providerData.firstOrNull {
        it.providerId == GoogleAuthProvider.PROVIDER_ID
    }
    val tokenSub = verifiedIdToken?.let { extractGoogleSubFromIdToken(it) }
    val resolvedSub = resolveGoogleSub(
        providerGoogleUid = googleProviderInfo?.uid,
        idTokenSub = tokenSub,
        firebaseUid = uid
    )
    val resolvedEmail = email?.takeIf { it.isNotBlank() }
        ?: googleProviderInfo?.email?.takeIf { it.isNotBlank() }
        ?: fallbackEmail?.takeIf { it.isNotBlank() }
        ?: ""
    val resolvedName = displayName?.takeIf { it.isNotBlank() }
        ?: googleProviderInfo?.displayName?.takeIf { it.isNotBlank() }
        ?: fallbackDisplayName?.takeIf { it.isNotBlank() }
        ?: resolvedEmail.substringBefore("@").ifBlank { "FAZA User" }
    val resolvedPhoto = photoUrl?.toString()?.takeIf { it.isNotBlank() }
        ?: googleProviderInfo?.photoUrl?.toString()?.takeIf { it.isNotBlank() }
        ?: fallbackPhotoUrl?.takeIf { it.isNotBlank() }

    return FazaGoogleAccount(
        firebaseUid = uid,
        googleSub = resolvedSub,
        displayName = resolvedName,
        email = resolvedEmail,
        photoUrl = resolvedPhoto
    )
}
