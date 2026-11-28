package com.example.auth

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.security.MessageDigest

/**
 * Represents an authenticated Google user linked to FAZA AI.
 *
 * [googleSub] holds Google's unique, immutable account subject identifier ("sub"),
 * ensuring FAZA AI sessions and isolated chat histories are never tied solely to a
 * mutable display name or email address.
 */
data class FazaGoogleAccount(
    val firebaseUid: String,
    val googleSub: String,
    val displayName: String,
    val email: String,
    val photoUrl: String? = null
) {
    /**
     * Canonical FAZA AI session identifier anchored to Google's unique subject ID ("sub").
     */
    val fazaAiAccountId: String
        get() = googleSub.trim().ifBlank { firebaseUid.trim() }

    val resolvedDisplayName: String
        get() = displayName.trim().ifBlank {
            email.substringBefore("@").ifBlank { "FAZA User" }
        }
}

sealed interface FazaAuthState {
    data object SignedOut : FazaAuthState
    data object SigningIn : FazaAuthState
    data class SignedIn(val account: FazaGoogleAccount) : FazaAuthState
    data class SessionExpired(
        val reason: String = "Your Google session has expired. Please sign in again to continue using FAZA AI."
    ) : FazaAuthState
}

/**
 * Real-time authentication lifecycle events emitted by the global [GlobalAuthStateListener].
 */
sealed interface FazaAuthEvent {
    data class SignedIn(val account: FazaGoogleAccount) : FazaAuthEvent
    data object SignedOut : FazaAuthEvent
    data class SessionExpired(
        val reason: String = "Your Google session has expired. Please sign in again to continue using FAZA AI."
    ) : FazaAuthEvent
}

fun interface GlobalAuthStateListener {
    fun onAuthStateChanged(event: FazaAuthEvent)
}

fun interface AuthListenerRegistration {
    fun remove()
}

/**
 * One-shot onboarding greeting emitted upon a successful Google Sign-In to drive the
 * Profile header "Welcome, [User Name]" animation.
 */
data class ProfileWelcomeGreeting(
    val id: Long = System.nanoTime(),
    val accountUid: String,
    val userName: String,
    val photoUrl: String? = null
)

/**
 * Marker exception raised when the user dismisses or cancels the Google Sign-In sheet.
 * Handled silently by resetting loading state without displaying an error banner.
 */
class AuthCancelledException(
    message: String = "Google Sign-In was cancelled.",
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Raised when a Google Sign-In verification or session check exceeds the network timeout window.
 */
class AuthNetworkTimeoutException(
    message: String = "Connection timed out while contacting Google. Check your internet connection and try again.",
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Raised when the persisted Google / Firebase authentication session has expired or been revoked.
 */
class AuthSessionExpiredException(
    message: String = "Your Google session has expired. Please sign in again to continue using FAZA AI.",
    cause: Throwable? = null
) : Exception(message, cause)

enum class AuthErrorCategory {
    SIGN_IN_FAILED,
    NETWORK_TIMEOUT,
    SESSION_EXPIRED,
    ACCOUNT_UNAVAILABLE,
    SIGN_OUT_FAILED
}

data class FazaAuthUiError(
    val id: Long = System.nanoTime(),
    val category: AuthErrorCategory,
    val title: String,
    val message: String,
    val canRetry: Boolean = true
)

/**
 * Classifies authentication, network timeout, and session expiration errors into user-friendly
 * structured notifications for the Profile error boundary and snackbar system.
 */
fun classifyAuthError(
    error: Throwable?,
    fallbackCategory: AuthErrorCategory = AuthErrorCategory.SIGN_IN_FAILED,
    id: Long = System.nanoTime()
): FazaAuthUiError {
    val rawMessage = error?.message?.trim().orEmpty()
    val lowerMessage = rawMessage.lowercase()

    if (fallbackCategory == AuthErrorCategory.SIGN_OUT_FAILED) {
        return FazaAuthUiError(
            id = id,
            category = AuthErrorCategory.SIGN_OUT_FAILED,
            title = "Sign-Out Issue",
            message = rawMessage.ifBlank { "Unable to complete sign-out right now. Please try again." },
            canRetry = false
        )
    }

    val isExpired = error is AuthSessionExpiredException ||
        lowerMessage.contains("expired") ||
        lowerMessage.contains("revoked") ||
        lowerMessage.contains("invalid_user") ||
        lowerMessage.contains("user-token-expired")
    if (isExpired) {
        return FazaAuthUiError(
            id = id,
            category = AuthErrorCategory.SESSION_EXPIRED,
            title = "Session Expired",
            message = rawMessage.ifBlank {
                "Your Google session has expired. Sign in again to keep using FAZA AI."
            },
            canRetry = true
        )
    }

    val isTimeoutOrNetwork = error is AuthNetworkTimeoutException ||
        error is kotlinx.coroutines.TimeoutCancellationException ||
        error is java.net.SocketTimeoutException ||
        error is java.net.UnknownHostException ||
        error is java.net.ConnectException ||
        lowerMessage.contains("timed out") ||
        lowerMessage.contains("timeout") ||
        lowerMessage.contains("network") ||
        lowerMessage.contains("offline") ||
        lowerMessage.contains("connection")
    if (isTimeoutOrNetwork) {
        val isExplicitTimeout = error is AuthNetworkTimeoutException ||
            error is kotlinx.coroutines.TimeoutCancellationException ||
            error is java.net.SocketTimeoutException ||
            lowerMessage.contains("timed out") ||
            lowerMessage.contains("timeout")
        return FazaAuthUiError(
            id = id,
            category = AuthErrorCategory.NETWORK_TIMEOUT,
            title = if (isExplicitTimeout) "Network Timeout" else "Connection Problem",
            message = rawMessage.ifBlank {
                "Connection timed out while contacting Google. Check your internet and try again."
            },
            canRetry = true
        )
    }

    val isNoAccount = lowerMessage.contains("no google account") ||
        lowerMessage.contains("no credential")
    if (isNoAccount) {
        return FazaAuthUiError(
            id = id,
            category = AuthErrorCategory.ACCOUNT_UNAVAILABLE,
            title = "No Google Account Found",
            message = rawMessage.ifBlank {
                "Add a Google account in Android Settings, then tap Sign in with Google again."
            },
            canRetry = true
        )
    }

    return FazaAuthUiError(
        id = id,
        category = AuthErrorCategory.SIGN_IN_FAILED,
        title = "Google Sign-In Failed",
        message = rawMessage.ifBlank {
            "We couldn't sign you in with Google right now. Please try again."
        },
        canRetry = true
    )
}

/**
 * Extracts the Google OIDC "sub" claim from a Google ID token AFTER server-side verification
 * through Firebase Authentication. Never used as an unverified trust decision on its own.
 */
internal fun extractGoogleSubFromIdToken(idToken: String): String? {
    val parts = idToken.split('.')
    if (parts.size < 2) return null
    return runCatching {
        val payloadBytes = decodeBase64Url(parts[1]) ?: return null
        val jsonObj = Json.parseToJsonElement(String(payloadBytes, Charsets.UTF_8)).jsonObject
        jsonObj["sub"]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
    }.getOrNull()
}

private fun decodeBase64Url(input: String): ByteArray? {
    val clean = input.trim().trimEnd('=')
    if (clean.isEmpty()) return null
    val output = ByteArray((clean.length * 3) / 4)
    var outIndex = 0
    var buffer = 0
    var bitsCollected = 0
    for (ch in clean) {
        val value = when (ch) {
            in 'A'..'Z' -> ch - 'A'
            in 'a'..'z' -> ch - 'a' + 26
            in '0'..'9' -> ch - '0' + 52
            '-', '+' -> 62
            '_', '/' -> 63
            else -> return null
        }
        buffer = (buffer shl 6) or value
        bitsCollected += 6
        if (bitsCollected >= 8) {
            bitsCollected -= 8
            if (outIndex < output.size) {
                output[outIndex++] = ((buffer shr bitsCollected) and 0xFF).toByte()
            }
        }
    }
    return output.copyOf(outIndex)
}

/**
 * Resolves the canonical Google subject ID ("sub") in priority order:
 * 1. Verified Google provider UID persisted by Firebase Auth (`providerData` for `google.com`).
 * 2. Verified ID token `sub` claim from the active Google Sign-In response.
 * 3. Fallback to Firebase Auth UID (guaranteed unique 1:1 per Google account).
 */
internal fun resolveGoogleSub(
    providerGoogleUid: String?,
    idTokenSub: String?,
    firebaseUid: String
): String {
    val cleanProviderSub = providerGoogleUid?.trim().orEmpty()
    if (cleanProviderSub.isNotEmpty()) return cleanProviderSub
    val cleanTokenSub = idTokenSub?.trim().orEmpty()
    if (cleanTokenSub.isNotEmpty()) return cleanTokenSub
    return firebaseUid.trim()
}

/**
 * Produces a deterministic, filesystem-safe, privacy-preserving storage key for local
 * per-account FAZA AI cache isolation.
 */
internal fun computeAccountStorageKey(accountId: String): String {
    val normalized = accountId.trim()
    require(normalized.isNotEmpty()) { "Account ID must not be blank" }
    val digest = MessageDigest.getInstance("SHA-256")
        .digest(normalized.toByteArray(Charsets.UTF_8))
    return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }.take(32)
}
