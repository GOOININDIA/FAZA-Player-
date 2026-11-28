package com.example.ui.viewmodel

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.auth.AuthCancelledException
import com.example.auth.AuthErrorCategory
import com.example.auth.AuthListenerRegistration
import com.example.auth.AuthSessionExpiredException
import com.example.auth.FazaAiCloudStore
import com.example.auth.FazaAuthEvent
import com.example.auth.FazaAuthGateway
import com.example.auth.FazaAuthState
import com.example.auth.FazaAuthUiError
import com.example.auth.FazaGoogleAccount
import com.example.auth.GlobalAuthStateListener
import com.example.auth.ProfileWelcomeGreeting
import com.example.auth.classifyAuthError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

private const val AUTH_VM_TAG = "FazaAuthVM"
private const val RESUME_SESSION_CHECK_INTERVAL_MS = 30_000L

/**
 * Profile authentication ViewModel backed by a global [GlobalAuthStateListener].
 *
 * Automatically observes real-time authentication transitions (sign-in, explicit logout,
 * external sign-out, and session expiration/revocation) and keeps Profile UI state continuously
 * synchronized until [onCleared] cleanly removes the listener registration.
 */
class FazaAuthViewModel(
    private val authGateway: FazaAuthGateway,
    private val accountStore: FazaAiCloudStore? = null
) : ViewModel() {

    private val errorSequence = AtomicLong(0L)
    private val greetingSequence = AtomicLong(0L)
    private val explicitSignOutInProgress = AtomicBoolean(false)
    private val lastSessionCheckAtMs = AtomicLong(0L)

    private val _authState = MutableStateFlow<FazaAuthState>(
        authGateway.getCurrentAccount()?.let { FazaAuthState.SignedIn(it) } ?: FazaAuthState.SignedOut
    )
    val authState: StateFlow<FazaAuthState> = _authState.asStateFlow()

    private val _currentAccount = MutableStateFlow(authGateway.getCurrentAccount())
    val currentAccount: StateFlow<FazaGoogleAccount?> = _currentAccount.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _authUiError = MutableStateFlow<FazaAuthUiError?>(null)
    val authUiError: StateFlow<FazaAuthUiError?> = _authUiError.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _welcomeGreeting = MutableStateFlow<ProfileWelcomeGreeting?>(null)
    val welcomeGreeting: StateFlow<ProfileWelcomeGreeting?> = _welcomeGreeting.asStateFlow()

    private val _isAuthListenerActive = MutableStateFlow(false)
    val isAuthListenerActive: StateFlow<Boolean> = _isAuthListenerActive.asStateFlow()

    private var hasAttemptedSilentSignIn = false
    private var lastSyncedUid: String? = null
    private var lastGreetedUid: String? = authGateway.getCurrentAccount()?.firebaseUid
    private var globalAuthListenerRegistration: AuthListenerRegistration? = null

    private val globalAuthStateListener = GlobalAuthStateListener { event ->
        handleGlobalAuthEvent(event)
    }

    init {
        globalAuthListenerRegistration = authGateway.addGlobalAuthStateListener(globalAuthStateListener)
        _isAuthListenerActive.value = true
    }

    private fun handleGlobalAuthEvent(event: FazaAuthEvent) {
        when (event) {
            is FazaAuthEvent.SignedIn -> {
                val account = event.account
                val shouldGreet = _isAuthListenerActive.value && lastGreetedUid != account.firebaseUid
                _currentAccount.value = account
                _authState.value = FazaAuthState.SignedIn(account)
                _isBusy.value = false
                if (_authUiError.value?.category == AuthErrorCategory.SESSION_EXPIRED) {
                    clearError()
                }
                if (shouldGreet) {
                    triggerWelcomeGreeting(account)
                } else {
                    lastGreetedUid = account.firebaseUid
                }
                syncAccountRecordIfNeeded(account)
            }

            is FazaAuthEvent.SignedOut -> {
                lastSyncedUid = null
                lastGreetedUid = null
                _welcomeGreeting.value = null
                _currentAccount.value = null
                if (_authState.value !is FazaAuthState.SigningIn &&
                    _authState.value !is FazaAuthState.SessionExpired
                ) {
                    _authState.value = FazaAuthState.SignedOut
                }
                if (explicitSignOutInProgress.get()) {
                    clearError()
                }
            }

            is FazaAuthEvent.SessionExpired -> {
                lastSyncedUid = null
                lastGreetedUid = null
                _welcomeGreeting.value = null
                _currentAccount.value = null
                _isBusy.value = false
                _authState.value = FazaAuthState.SessionExpired(event.reason)
                publishUiError(
                    classifyAuthError(
                        error = AuthSessionExpiredException(event.reason),
                        id = errorSequence.incrementAndGet()
                    )
                )
            }
        }
    }

    fun attemptSilentSignInIfNeeded(context: Context) {
        if (hasAttemptedSilentSignIn) return
        hasAttemptedSilentSignIn = true

        viewModelScope.launch {
            val existing = authGateway.getCurrentAccount()
            if (existing != null) {
                verifySessionAndUpdateState()
                return@launch
            }

            try {
                val result = authGateway.attemptSilentSignIn(context)
                result.getOrNull()?.let { account ->
                    _currentAccount.value = account
                    _authState.value = FazaAuthState.SignedIn(account)
                    clearError()
                    syncAccountRecordIfNeeded(account)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.d(AUTH_VM_TAG, "Silent sign-in skipped: ${e.message}")
            }
        }
    }

    /**
     * Re-verifies an active session when the app returns to foreground so expired/revoked tokens
     * update Profile UI immediately rather than waiting for the user's next API call.
     */
    fun refreshSessionStatusIfSignedIn(force: Boolean = false) {
        if (_isBusy.value) return
        if (_currentAccount.value == null && authGateway.getCurrentAccount() == null) return

        val now = System.currentTimeMillis()
        val lastCheck = lastSessionCheckAtMs.get()
        if (!force && lastCheck > 0L && (now - lastCheck) < RESUME_SESSION_CHECK_INTERVAL_MS) {
            return
        }

        viewModelScope.launch {
            verifySessionAndUpdateState()
        }
    }

    private suspend fun verifySessionAndUpdateState() {
        lastSessionCheckAtMs.set(System.currentTimeMillis())
        val verification = authGateway.verifyActiveSession()
        verification.onSuccess { verifiedAccount ->
            if (verifiedAccount != null) {
                _currentAccount.value = verifiedAccount
                _authState.value = FazaAuthState.SignedIn(verifiedAccount)
            }
        }.onFailure { err ->
            val classified = classifyAuthError(
                error = err,
                id = errorSequence.incrementAndGet()
            )
            if (classified.category == AuthErrorCategory.SESSION_EXPIRED) {
                lastSyncedUid = null
                _currentAccount.value = null
                _authState.value = FazaAuthState.SessionExpired(classified.message)
            }
            publishUiError(classified)
        }
    }

    fun signInWithGoogle(activity: Activity) {
        if (_isBusy.value) return
        _isBusy.value = true
        clearError()
        _authState.value = FazaAuthState.SigningIn

        viewModelScope.launch {
            try {
                val result = authGateway.signInWithGoogle(activity)
                result.onSuccess { account ->
                    _currentAccount.value = account
                    _authState.value = FazaAuthState.SignedIn(account)
                    clearError()
                    triggerWelcomeGreeting(account)
                    lastSessionCheckAtMs.set(System.currentTimeMillis())
                    syncAccountRecordIfNeeded(account, force = true)
                }.onFailure { error ->
                    _currentAccount.value = authGateway.getCurrentAccount()
                    val active = _currentAccount.value
                    if (error !is AuthCancelledException) {
                        val classified = classifyAuthError(
                            error = error,
                            id = errorSequence.incrementAndGet()
                        )
                        _authState.value = when {
                            active != null -> FazaAuthState.SignedIn(active)
                            classified.category == AuthErrorCategory.SESSION_EXPIRED ->
                                FazaAuthState.SessionExpired(classified.message)
                            else -> FazaAuthState.SignedOut
                        }
                        publishUiError(classified)
                    } else {
                        _authState.value = if (active != null) {
                            FazaAuthState.SignedIn(active)
                        } else {
                            FazaAuthState.SignedOut
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(AUTH_VM_TAG, "Unexpected sign-in error", e)
                _authState.value = _currentAccount.value?.let { FazaAuthState.SignedIn(it) }
                    ?: FazaAuthState.SignedOut
                publishUiError(
                    classifyAuthError(
                        error = e,
                        id = errorSequence.incrementAndGet()
                    )
                )
            } finally {
                _isBusy.value = false
            }
        }
    }

    /**
     * Detaches active Firestore/AI session listeners inside [NonCancellable] BEFORE clearing
     * Credential Manager state and signing out of Firebase Auth, preventing PERMISSION_DENIED races.
     */
    fun signOut(
        context: Context,
        onBeforeSignOut: suspend () -> Unit = {}
    ) {
        if (_isBusy.value) return
        _isBusy.value = true
        explicitSignOutInProgress.set(true)
        clearError()

        viewModelScope.launch {
            withContext(NonCancellable) {
                try {
                    onBeforeSignOut()
                    val result = authGateway.signOut(context)
                    result.onSuccess {
                        lastSyncedUid = null
                        lastGreetedUid = null
                        _welcomeGreeting.value = null
                        _currentAccount.value = null
                        _authState.value = FazaAuthState.SignedOut
                    }.onFailure { err ->
                        publishUiError(
                            classifyAuthError(
                                error = err,
                                fallbackCategory = AuthErrorCategory.SIGN_OUT_FAILED,
                                id = errorSequence.incrementAndGet()
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.e(AUTH_VM_TAG, "Sign-out failed", e)
                    publishUiError(
                        classifyAuthError(
                            error = e,
                            fallbackCategory = AuthErrorCategory.SIGN_OUT_FAILED,
                            id = errorSequence.incrementAndGet()
                        )
                    )
                } finally {
                    explicitSignOutInProgress.set(false)
                    _isBusy.value = false
                }
            }
        }
    }

    fun dismissWelcomeGreeting() {
        _welcomeGreeting.value = null
    }

    private fun triggerWelcomeGreeting(account: FazaGoogleAccount) {
        if (lastGreetedUid == account.firebaseUid && _welcomeGreeting.value != null) {
            return
        }
        lastGreetedUid = account.firebaseUid
        _welcomeGreeting.value = ProfileWelcomeGreeting(
            id = greetingSequence.incrementAndGet(),
            accountUid = account.firebaseUid,
            userName = account.resolvedDisplayName,
            photoUrl = account.photoUrl
        )
    }

    fun clearError() {
        _authUiError.value = null
        _authError.value = null
        if (_authState.value is FazaAuthState.SessionExpired && _currentAccount.value == null) {
            _authState.value = FazaAuthState.SignedOut
        }
    }

    private fun publishUiError(uiError: FazaAuthUiError) {
        _authUiError.value = uiError
        _authError.value = uiError.message
    }

    private fun syncAccountRecordIfNeeded(account: FazaGoogleAccount, force: Boolean = false) {
        val store = accountStore ?: return
        if (!force && lastSyncedUid == account.firebaseUid) return
        lastSyncedUid = account.firebaseUid
        viewModelScope.launch {
            store.upsertUserAccount(account).onFailure { err ->
                Log.w(AUTH_VM_TAG, "Deferred user profile sync failed: ${err.message}")
            }
        }
    }

    override fun onCleared() {
        globalAuthListenerRegistration?.remove()
        globalAuthListenerRegistration = null
        _isAuthListenerActive.value = false
        super.onCleared()
    }

    companion object {
        fun factory(
            authGateway: FazaAuthGateway,
            accountStore: FazaAiCloudStore?
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return FazaAuthViewModel(authGateway, accountStore) as T
            }
        }
    }
}
