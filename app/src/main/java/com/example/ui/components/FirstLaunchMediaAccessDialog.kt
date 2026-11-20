package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R

@Composable
fun FirstLaunchMediaAccessDialog(
    onGrantAccess: () -> Unit,
    onSkip: () -> Unit
) {
    Dialog(
        onDismissRequest = onSkip,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        AllFileAccessPermissionScreen(
            onAllowAccess = onGrantAccess,
            onDismiss = onSkip
        )
    }
}

@Composable
fun AllFileAccessPermissionScreen(
    onAllowAccess: () -> Unit,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showWhyDialog by rememberSaveable { mutableStateOf(false) }

    if (onDismiss != null) {
        BackHandler(onBack = onDismiss)
    }

    val colors = MaterialTheme.colorScheme
    val backgroundColor = colors.surface
    val isDarkSurface = backgroundColor.luminance() < 0.5f
    val headlineTextColor = colors.onSurface
    val primaryActionColor = colors.primary
    val primaryActionContent = colors.onPrimary
    val secondaryLinkColor = colors.primary

    val buttonInteractionSource = remember { MutableInteractionSource() }
    val isButtonPressed by buttonInteractionSource.collectIsPressedAsState()
    val isButtonHovered by buttonInteractionSource.collectIsHoveredAsState()

    val buttonScale by animateFloatAsState(
        targetValue = when {
            isButtonPressed -> 0.94f
            isButtonHovered -> 1.015f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "allow_access_scale"
    )

    val buttonColor by animateColorAsState(
        targetValue = when {
            isButtonPressed -> primaryActionColor.copy(alpha = 0.88f)
            isButtonHovered -> primaryActionColor.copy(alpha = 0.94f)
            else -> primaryActionColor
        },
        animationSpec = tween(durationMillis = 160),
        label = "allow_access_color"
    )

    val linkInteractionSource = remember { MutableInteractionSource() }
    val isLinkPressed by linkInteractionSource.collectIsPressedAsState()
    val isLinkHovered by linkInteractionSource.collectIsHoveredAsState()
    val linkScale by animateFloatAsState(
        targetValue = if (isLinkPressed) 0.95f else if (isLinkHovered) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "why_link_scale"
    )

    Surface(
        color = backgroundColor,
        modifier = modifier
            .fillMaxSize()
            .testTag("first_launch_media_access_dialog")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            if (onDismiss != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        color = colors.surfaceContainerHigh,
                        shape = CircleShape
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("media_access_close_icon")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.all_file_access_skip_cd),
                                tint = colors.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp)
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    FolderPadlockIllustration(
                        isDarkSurface = isDarkSurface,
                        modifier = Modifier
                            .size(176.dp)
                            .testTag("all_file_access_illustration")
                    )

                    Spacer(modifier = Modifier.height(26.dp))

                Text(
                    text = stringResource(R.string.all_file_access_message),
                    color = headlineTextColor,
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp,
                    lineHeight = 27.sp,
                    letterSpacing = 0.15.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .testTag("all_file_access_message")
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onAllowAccess,
                    interactionSource = buttonInteractionSource,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonColor,
                        contentColor = primaryActionContent
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                        hoveredElevation = 2.dp
                    ),
                    contentPadding = PaddingValues(horizontal = 28.dp, vertical = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .graphicsLayer {
                            scaleX = buttonScale
                            scaleY = buttonScale
                        }
                        .testTag("media_access_grant_button")
                ) {
                    Text(
                        text = stringResource(R.string.all_file_access_allow_button),
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 0.6.sp,
                        color = primaryActionContent
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                val whyLabel = stringResource(R.string.all_file_access_why_link)
                Row(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = linkScale
                            scaleY = linkScale
                        }
                        .heightIn(min = 48.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = linkInteractionSource,
                            indication = ripple(bounded = true, color = secondaryLinkColor),
                            onClick = { showWhyDialog = true }
                        )
                        .semantics {
                            role = Role.Button
                            contentDescription = whyLabel
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .testTag("media_access_why_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = secondaryLinkColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = whyLabel,
                        color = secondaryLinkColor,
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
                }
            }
        }
    }

    if (showWhyDialog) {
        AlertDialog(
            onDismissRequest = { showWhyDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = secondaryLinkColor
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.all_file_access_why_title),
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.all_file_access_why_body),
                    fontFamily = FontFamily.Default,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 21.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showWhyDialog = false },
                    shape = CircleShape,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("media_access_why_close_button")
                ) {
                    Text(
                        text = stringResource(R.string.all_file_access_why_close),
                        color = secondaryLinkColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            modifier = Modifier.testTag("media_access_why_dialog")
        )
    }
}

@Composable
private fun FolderPadlockIllustration(
    isDarkSurface: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "folder_sparkle_anim")
    val twinkle by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkle_twinkle"
    )

    val colors = MaterialTheme.colorScheme
    val goldPrimary = colors.tertiary
    val yellowSparkle = colors.secondary
    val royalBlue = colors.primary
    val lightBlue = colors.primaryContainer
    val deepBlue = colors.onPrimaryContainer
    val lockFill = colors.surfaceContainerLowest

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.52f

        // Soft ambient glow behind the folder
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    royalBlue.copy(alpha = if (isDarkSurface) 0.28f else 0.16f),
                    goldPrimary.copy(alpha = if (isDarkSurface) 0.08f else 0.06f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = w * 0.48f
            ),
            radius = w * 0.48f,
            center = Offset(cx, cy)
        )

        // Subtle golden & yellow decorative dots
        drawCircle(
            color = goldPrimary.copy(alpha = 0.90f * twinkle),
            radius = w * 0.022f,
            center = Offset(w * 0.22f, h * 0.18f)
        )
        drawCircle(
            color = yellowSparkle.copy(alpha = 0.85f * twinkle),
            radius = w * 0.014f,
            center = Offset(w * 0.84f, h * 0.31f)
        )
        drawCircle(
            color = goldPrimary.copy(alpha = 0.72f),
            radius = w * 0.016f,
            center = Offset(w * 0.14f, h * 0.64f)
        )
        drawCircle(
            color = yellowSparkle.copy(alpha = 0.78f),
            radius = w * 0.013f,
            center = Offset(w * 0.77f, h * 0.81f)
        )

        // 4-point star sparkles
        drawFourPointStar(
            center = Offset(w * 0.80f, h * 0.20f),
            outerRadius = w * 0.055f * (0.9f + 0.12f * twinkle),
            color = yellowSparkle.copy(alpha = 0.95f * twinkle)
        )
        drawFourPointStar(
            center = Offset(w * 0.17f, h * 0.36f),
            outerRadius = w * 0.038f * (0.88f + 0.15f * twinkle),
            color = goldPrimary.copy(alpha = 0.90f * twinkle)
        )
        drawFourPointStar(
            center = Offset(w * 0.85f, h * 0.69f),
            outerRadius = w * 0.036f * (0.9f + 0.1f * twinkle),
            color = goldPrimary.copy(alpha = 0.85f)
        )

        // Minimalist Vector Folder Back Plate + Tab
        val folderLeft = w * 0.18f
        val folderRight = w * 0.82f
        val folderTop = h * 0.26f
        val folderBottom = h * 0.76f
        val folderWidth = folderRight - folderLeft
        val corner = w * 0.065f
        val tabWidth = folderWidth * 0.40f
        val tabHeight = h * 0.085f

        val backPath = Path().apply {
            moveTo(folderLeft + corner, folderTop)
            lineTo(folderLeft + tabWidth - corner * 0.4f, folderTop)
            quadraticTo(
                folderLeft + tabWidth + corner * 0.1f,
                folderTop,
                folderLeft + tabWidth + corner * 0.45f,
                folderTop + tabHeight * 0.55f
            )
            lineTo(folderLeft + tabWidth + corner * 0.7f, folderTop + tabHeight)
            lineTo(folderRight - corner, folderTop + tabHeight)
            quadraticTo(folderRight, folderTop + tabHeight, folderRight, folderTop + tabHeight + corner)
            lineTo(folderRight, folderBottom - corner)
            quadraticTo(folderRight, folderBottom, folderRight - corner, folderBottom)
            lineTo(folderLeft + corner, folderBottom)
            quadraticTo(folderLeft, folderBottom, folderLeft, folderBottom - corner)
            lineTo(folderLeft, folderTop + corner)
            quadraticTo(folderLeft, folderTop, folderLeft + corner, folderTop)
            close()
        }

        drawPath(
            path = backPath,
            brush = Brush.linearGradient(
                colors = listOf(royalBlue, deepBlue),
                start = Offset(folderLeft, folderTop),
                end = Offset(folderRight, folderBottom)
            )
        )

        // Front Folder Body
        val frontTop = folderTop + tabHeight + h * 0.025f
        val frontRectTopLeft = Offset(folderLeft, frontTop)
        val frontSize = Size(folderWidth, folderBottom - frontTop)
        val frontCorner = CornerRadius(corner, corner)

        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(lightBlue, royalBlue),
                start = Offset(folderLeft, frontTop),
                end = Offset(folderRight, folderBottom)
            ),
            topLeft = frontRectTopLeft,
            size = frontSize,
            cornerRadius = frontCorner
        )

        drawRoundRect(
            color = Color.White.copy(alpha = 0.28f),
            topLeft = frontRectTopLeft,
            size = frontSize,
            cornerRadius = frontCorner,
            style = Stroke(width = w * 0.011f)
        )

        // Centered Padlock Emblem inside the Folder
        val lockCenterX = cx
        val lockCenterY = (frontTop + folderBottom) * 0.52f
        val badgeRadius = w * 0.135f

        drawCircle(
            color = deepBlue.copy(alpha = 0.52f),
            radius = badgeRadius,
            center = Offset(lockCenterX, lockCenterY)
        )
        drawCircle(
            color = goldPrimary.copy(alpha = 0.45f),
            radius = badgeRadius,
            center = Offset(lockCenterX, lockCenterY),
            style = Stroke(width = w * 0.01f)
        )

        // Padlock Shackle
        val shackleW = w * 0.095f
        val shackleH = h * 0.085f
        val bodyW = w * 0.145f
        val bodyH = h * 0.105f
        val bodyTop = lockCenterY - bodyH * 0.25f

        val shacklePath = Path().apply {
            moveTo(lockCenterX - shackleW * 0.5f, bodyTop + h * 0.01f)
            lineTo(lockCenterX - shackleW * 0.5f, bodyTop - shackleH * 0.35f)
            arcTo(
                rect = Rect(
                    left = lockCenterX - shackleW * 0.5f,
                    top = bodyTop - shackleH * 0.85f,
                    right = lockCenterX + shackleW * 0.5f,
                    bottom = bodyTop + shackleH * 0.15f
                ),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            lineTo(lockCenterX + shackleW * 0.5f, bodyTop + h * 0.01f)
        }
        drawPath(
            path = shacklePath,
            color = lockFill,
            style = Stroke(
                width = w * 0.022f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Padlock Body
        drawRoundRect(
            color = lockFill,
            topLeft = Offset(lockCenterX - bodyW * 0.5f, bodyTop),
            size = Size(bodyW, bodyH),
            cornerRadius = CornerRadius(w * 0.026f, w * 0.026f)
        )

        // Padlock Keyhole
        val keyholeCenter = Offset(lockCenterX, bodyTop + bodyH * 0.44f)
        drawCircle(
            color = royalBlue,
            radius = w * 0.016f,
            center = keyholeCenter
        )
        drawLine(
            color = royalBlue,
            start = keyholeCenter,
            end = Offset(lockCenterX, bodyTop + bodyH * 0.74f),
            strokeWidth = w * 0.016f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawFourPointStar(
    center: Offset,
    outerRadius: Float,
    color: Color
) {
    val inner = outerRadius * 0.28f
    val path = Path().apply {
        moveTo(center.x, center.y - outerRadius)
        quadraticTo(center.x + inner * 0.3f, center.y - inner * 0.3f, center.x + outerRadius, center.y)
        quadraticTo(center.x + inner * 0.3f, center.y + inner * 0.3f, center.x, center.y + outerRadius)
        quadraticTo(center.x - inner * 0.3f, center.y + inner * 0.3f, center.x - outerRadius, center.y)
        quadraticTo(center.x - inner * 0.3f, center.y - inner * 0.3f, center.x, center.y - outerRadius)
        close()
    }
    drawPath(path = path, color = color)
}
