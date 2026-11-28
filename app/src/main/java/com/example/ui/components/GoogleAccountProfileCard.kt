package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.auth.FazaGoogleAccount
import com.example.ui.theme.LocalFazaIsDark

private val GoogleButtonBgLight = Color(0xFFFFFFFF)
private val GoogleButtonBorderLight = Color(0xFF747775)
private val GoogleButtonTextLight = Color(0xFF1F1F1F)

private val GoogleButtonBgDark = Color(0xFF131314)
private val GoogleButtonBorderDark = Color(0xFF8E918F)
private val GoogleButtonTextDark = Color(0xFFE3E3E3)

/**
 * Official Google-styled "Sign in with Google" button following Google Identity Branding Guidelines
 * for both Light Mode and Dark Mode.
 */
@Composable
fun OfficialGoogleSignInButton(
    onClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    testTag: String = "google_sign_in_button"
) {
    val isDark = LocalFazaIsDark.current
    val containerColor = if (isDark) GoogleButtonBgDark else GoogleButtonBgLight
    val borderColor = if (isDark) GoogleButtonBorderDark else GoogleButtonBorderLight
    val contentColor = if (isDark) GoogleButtonTextDark else GoogleButtonTextLight

    Surface(
        onClick = onClick,
        enabled = !isLoading,
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = if (isDark) 0.dp else 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.google_auth_signing_in),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_brand_google_g),
                    contentDescription = stringResource(R.string.about_special_thanks_google_logo_cd),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.google_auth_sign_in_button),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun GoogleAccountAvatar(
    photoUrl: String?,
    displayName: String,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val initial = displayName.trim().firstOrNull()?.uppercaseChar()?.toString()

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (!photoUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(photoUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = stringResource(R.string.google_auth_avatar_cd, displayName),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    AvatarFallbackContent(initial = initial)
                },
                error = {
                    AvatarFallbackContent(initial = initial)
                }
            )
        } else {
            AvatarFallbackContent(initial = initial)
        }
    }
}

@Composable
private fun AvatarFallbackContent(initial: String?) {
    val colors = MaterialTheme.colorScheme
    if (initial != null) {
        Text(
            text = initial,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colors.onPrimaryContainer
        )
    } else {
        Icon(
            imageVector = Icons.Filled.Person,
            contentDescription = stringResource(R.string.google_auth_default_avatar_cd),
            tint = colors.onPrimaryContainer,
            modifier = Modifier.size(22.dp)
        )
    }
}

/**
 * Clean, minimalist Google Account card inside the Profile tab.
 * Shows the official Google Sign-In button when signed out, and the user's Google profile picture,
 * full name, Gmail address, FAZA AI connection status, and Sign-Out option when signed in.
 */
@Composable
fun ProfileGoogleAccountCard(
    account: FazaGoogleAccount?,
    isBusy: Boolean,
    authError: String?,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onDismissError: () -> Unit,
    showInlineError: Boolean = true,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    FazaGlassCard(
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(18.dp),
        modifier = modifier.testTag("profile_google_account_card")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (account == null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FazaGlassIconBadge(
                        icon = Icons.Filled.AutoAwesome,
                        size = 42.dp,
                        iconSize = 20.dp,
                        cornerRadius = 12.dp,
                        useSolidGradient = true
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.google_auth_card_title_signed_out),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface
                        )
                        Text(
                            text = stringResource(R.string.google_auth_card_subtitle_signed_out),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }

                OfficialGoogleSignInButton(
                    onClick = onSignInClick,
                    isLoading = isBusy,
                    testTag = "profile_google_sign_in_button"
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    GoogleAccountAvatar(
                        photoUrl = account.photoUrl,
                        displayName = account.resolvedDisplayName,
                        size = 52.dp,
                        modifier = Modifier.testTag("profile_google_avatar")
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = account.resolvedDisplayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("profile_google_display_name")
                        )
                        Text(
                            text = account.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("profile_google_email")
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VerifiedUser,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = stringResource(R.string.google_auth_connected_badge),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = colors.primary
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = onSignOutClick,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, colors.outlineVariant),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colors.onSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("profile_google_sign_out_button")
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.google_auth_sign_out_button),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (showInlineError && !authError.isNullOrBlank()) {
                AuthErrorInlineBanner(
                    message = authError,
                    onDismiss = onDismissError
                )
            }
        }
    }
}

/**
 * Friendly authentication gate rendered inside the FAZA AI Chat subtab whenever the user is
 * signed out. Prevents unauthenticated access to FAZA AI while offering one-tap Google Sign-In.
 */
@Composable
fun FazaAiSignInGateCard(
    isBusy: Boolean,
    authError: String?,
    onSignInClick: () -> Unit,
    onDismissError: () -> Unit,
    showInlineError: Boolean = true,
    bottomOverlayPadding: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 20.dp,
                end = 20.dp,
                top = 20.dp,
                bottom = 24.dp + bottomOverlayPadding
            )
            .testTag("assistant_auth_gate"),
        contentAlignment = Alignment.Center
    ) {
        FazaGlassCard(
            shape = RoundedCornerShape(26.dp),
            contentPadding = PaddingValues(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = colors.primaryContainer,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = colors.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.google_auth_ai_gate_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = stringResource(R.string.google_auth_ai_gate_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(2.dp))

                OfficialGoogleSignInButton(
                    onClick = onSignInClick,
                    isLoading = isBusy,
                    testTag = "assistant_google_sign_in_button"
                )

                if (showInlineError && !authError.isNullOrBlank()) {
                    AuthErrorInlineBanner(
                        message = authError,
                        onDismiss = onDismissError
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthErrorInlineBanner(
    message: String,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.errorContainer,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("google_auth_error_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = colors.onErrorContainer,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.google_auth_dismiss_error_cd),
                    tint = colors.onErrorContainer,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
