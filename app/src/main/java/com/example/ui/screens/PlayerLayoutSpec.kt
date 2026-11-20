package com.example.ui.screens

import com.example.data.model.AspectRatioMode

enum class PlayerScreenClass {
    ULTRA_COMPACT,
    COMPACT_PORTRAIT,
    LANDSCAPE_COMPACT,
    MEDIUM_TABLET,
    EXPANDED_TABLET
}

enum class PlayerTopAction(val priority: Int) {
    LOCK(0),
    SETTINGS(1),
    ROTATE(2),
    CUSTOMIZE_UI(3),
    PIP(4),
    FAVOURITE(5)
}

data class VideoSurfaceDimensions(
    val widthDp: Float,
    val heightDp: Float,
    val needsClipToBounds: Boolean
)

data class PlayerAdaptiveMetrics(
    val screenClass: PlayerScreenClass,
    val isCompactWidth: Boolean,
    val isShortHeight: Boolean,
    val isVeryShortHeight: Boolean,
    val isTabletOrExpanded: Boolean,
    val horizontalEdgePaddingDp: Float,
    val topBarTopOffsetDp: Float,
    val topBarBottomPaddingDp: Float,
    val maxTopRightSlots: Int,
    val bottomBarMaxWidthDp: Float?,
    val bottomBarVerticalPaddingDp: Float,
    val secondaryButtonBaseDp: Float,
    val secondaryIconBaseDp: Float,
    val primaryButtonBaseDp: Float,
    val primaryIconBaseDp: Float,
    val clusterSpacingDp: Float,
    val bottomBannerExtraDp: Float,
    val subtitleControlsExtraBottomDp: Float
)

data class TopActionPartition(
    val inlineActions: List<PlayerTopAction>,
    val overflowActions: List<PlayerTopAction>
)

fun calculateVideoSurfaceDimensions(
    containerWidthDp: Float,
    containerHeightDp: Float,
    naturalWidth: Int,
    naturalHeight: Int,
    aspectRatioMode: AspectRatioMode
): VideoSurfaceDimensions {
    val safeW = containerWidthDp.coerceAtLeast(1f)
    val safeH = containerHeightDp.coerceAtLeast(1f)
    val containerAspect = (safeW / safeH).coerceIn(0.1f, 10f)
    val naturalAspect = if (naturalWidth > 0 && naturalHeight > 0) {
        (naturalWidth.toFloat() / naturalHeight.toFloat()).coerceIn(0.1f, 10f)
    } else {
        16f / 9f
    }

    return when (aspectRatioMode) {
        AspectRatioMode.FILL -> VideoSurfaceDimensions(
            widthDp = safeW,
            heightDp = safeH,
            needsClipToBounds = false
        )
        AspectRatioMode.ZOOM -> {
            // Preserve exact natural video aspect ratio without stretching; scale to fill container and clip overflow
            if (containerAspect > naturalAspect) {
                VideoSurfaceDimensions(
                    widthDp = safeW,
                    heightDp = safeW / naturalAspect,
                    needsClipToBounds = true
                )
            } else {
                VideoSurfaceDimensions(
                    widthDp = safeH * naturalAspect,
                    heightDp = safeH,
                    needsClipToBounds = true
                )
            }
        }
        AspectRatioMode.FIT,
        AspectRatioMode.RATIO_16_9,
        AspectRatioMode.RATIO_4_3,
        AspectRatioMode.RATIO_21_9 -> {
            val targetAspect = when (aspectRatioMode) {
                AspectRatioMode.RATIO_16_9 -> 16f / 9f
                AspectRatioMode.RATIO_4_3 -> 4f / 3f
                AspectRatioMode.RATIO_21_9 -> 21f / 9f
                else -> naturalAspect
            }.coerceIn(0.1f, 10f)

            if (containerAspect > targetAspect) {
                VideoSurfaceDimensions(
                    widthDp = safeH * targetAspect,
                    heightDp = safeH,
                    needsClipToBounds = true
                )
            } else {
                VideoSurfaceDimensions(
                    widthDp = safeW,
                    heightDp = safeW / targetAspect,
                    needsClipToBounds = true
                )
            }
        }
    }
}

fun calculatePlayerAdaptiveMetrics(
    screenWidthDp: Float,
    screenHeightDp: Float,
    isControlsVisible: Boolean,
    isControlsLocked: Boolean,
    hasTopBanner: Boolean,
    isTwoLineBanner: Boolean,
    hasRunningTime: Boolean,
    isBottomCenterBanner: Boolean,
    hasAnyBottomButton: Boolean
): PlayerAdaptiveMetrics {
    val safeW = screenWidthDp.coerceAtLeast(1f)
    val safeH = screenHeightDp.coerceAtLeast(1f)

    val isCompactWidth = safeW < 380f
    val isShortHeight = safeH < 440f
    val isVeryShortHeight = safeH < 340f
    val isTabletOrExpanded = safeW >= 600f && safeH >= 480f

    val screenClass = when {
        safeW >= 840f && safeH >= 520f -> PlayerScreenClass.EXPANDED_TABLET
        safeW >= 600f && safeH >= 440f -> PlayerScreenClass.MEDIUM_TABLET
        isShortHeight -> PlayerScreenClass.LANDSCAPE_COMPACT
        safeW < 360f -> PlayerScreenClass.ULTRA_COMPACT
        else -> PlayerScreenClass.COMPACT_PORTRAIT
    }

    val topOverlayActive = hasTopBanner || hasRunningTime
    val topBarTopOffsetDp = when {
        isTwoLineBanner && isVeryShortHeight -> 42f
        isTwoLineBanner && isShortHeight -> 48f
        isTwoLineBanner -> 58f
        topOverlayActive && isVeryShortHeight -> 32f
        topOverlayActive && isShortHeight -> 38f
        topOverlayActive -> 44f
        isShortHeight -> 4f
        else -> 6f
    }

    val maxTopRightSlots = when {
        safeW < 340f -> 3
        safeW < 435f -> 4
        safeW < 540f -> 5
        else -> 6
    }

    val bottomBarMaxWidthDp = when (screenClass) {
        PlayerScreenClass.EXPANDED_TABLET -> 820f
        PlayerScreenClass.MEDIUM_TABLET -> 720f
        else -> null
    }

    val bottomBarVerticalPaddingDp = when {
        isVeryShortHeight -> 4f
        isShortHeight -> 6f
        isTabletOrExpanded -> 14f
        else -> 10f
    }

    val controlsActive = isControlsVisible && !isControlsLocked
    val activeBottomBarHeight = when {
        !controlsActive -> 0f
        hasAnyBottomButton && isVeryShortHeight -> 86f
        hasAnyBottomButton && isShortHeight -> 98f
        hasAnyBottomButton -> 114f
        isVeryShortHeight -> 46f
        isShortHeight -> 52f
        else -> 64f
    }

    val bottomBannerExtraDp = if (controlsActive) {
        activeBottomBarHeight + if (isTwoLineBanner) 10f else 4f
    } else {
        16f
    }

    val bannerOccupiedHeight = if (isBottomCenterBanner) {
        (if (isTwoLineBanner) 52f else 38f) + if (controlsActive) 4f else 14f
    } else {
        0f
    }

    val subtitleControlsExtraBottomDp = (activeBottomBarHeight + bannerOccupiedHeight)
        .coerceAtLeast(if (controlsActive) 64f else 0f)

    return PlayerAdaptiveMetrics(
        screenClass = screenClass,
        isCompactWidth = isCompactWidth,
        isShortHeight = isShortHeight,
        isVeryShortHeight = isVeryShortHeight,
        isTabletOrExpanded = isTabletOrExpanded,
        horizontalEdgePaddingDp = when {
            isCompactWidth -> 4f
            isTabletOrExpanded -> 14f
            else -> 8f
        },
        topBarTopOffsetDp = topBarTopOffsetDp,
        topBarBottomPaddingDp = if (isShortHeight) 6f else 12f,
        maxTopRightSlots = maxTopRightSlots,
        bottomBarMaxWidthDp = bottomBarMaxWidthDp,
        bottomBarVerticalPaddingDp = bottomBarVerticalPaddingDp,
        secondaryButtonBaseDp = when {
            isVeryShortHeight -> 42f
            isShortHeight -> 46f
            isTabletOrExpanded -> 60f
            else -> 54f
        },
        secondaryIconBaseDp = when {
            isVeryShortHeight -> 22f
            isShortHeight -> 24f
            isTabletOrExpanded -> 32f
            else -> 28f
        },
        primaryButtonBaseDp = when {
            isVeryShortHeight -> 48f
            isShortHeight -> 54f
            isTabletOrExpanded -> 72f
            else -> 64f
        },
        primaryIconBaseDp = when {
            isVeryShortHeight -> 26f
            isShortHeight -> 28f
            isTabletOrExpanded -> 38f
            else -> 34f
        },
        clusterSpacingDp = when {
            isVeryShortHeight -> 14f
            isShortHeight -> 18f
            isTabletOrExpanded -> 28f
            else -> 22f
        },
        bottomBannerExtraDp = bottomBannerExtraDp,
        subtitleControlsExtraBottomDp = subtitleControlsExtraBottomDp
    )
}

fun partitionTopBarActions(
    enabledActions: List<PlayerTopAction>,
    maxSlots: Int
): TopActionPartition {
    if (enabledActions.size <= maxSlots) {
        return TopActionPartition(
            inlineActions = enabledActions,
            overflowActions = emptyList()
        )
    }
    val directCount = (maxSlots - 1).coerceAtLeast(1)
    val chosenInlineSet = enabledActions
        .sortedBy { it.priority }
        .take(directCount)
        .toSet()

    val inline = enabledActions.filter { it in chosenInlineSet }
    val overflow = enabledActions.filter { it !in chosenInlineSet }
    return TopActionPartition(
        inlineActions = inline,
        overflowActions = overflow
    )
}
