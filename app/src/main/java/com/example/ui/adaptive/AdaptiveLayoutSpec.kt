package com.example.ui.adaptive

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class DeviceWidthClass {
    COMPACT,
    MEDIUM,
    EXPANDED
}

enum class DeviceHeightClass {
    COMPACT,
    MEDIUM,
    EXPANDED
}

data class AdaptiveLayoutSpec(
    val widthClass: DeviceWidthClass,
    val heightClass: DeviceHeightClass,
    val isUltraCompactWidth: Boolean,
    val isShortHeight: Boolean,
    val useNavigationRail: Boolean,
    val useTwoPane: Boolean,
    val videoGridColumns: Int,
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val contentSpacing: Dp,
    val maxContentWidth: Dp,
    val twoPaneMasterWidth: Dp
)

fun calculateAdaptiveLayoutSpec(
    availableWidth: Dp,
    availableHeight: Dp
): AdaptiveLayoutSpec {
    val widthClass = when {
        availableWidth >= 840.dp -> DeviceWidthClass.EXPANDED
        availableWidth >= 600.dp -> DeviceWidthClass.MEDIUM
        else -> DeviceWidthClass.COMPACT
    }

    val heightClass = when {
        availableHeight >= 900.dp -> DeviceHeightClass.EXPANDED
        availableHeight >= 480.dp -> DeviceHeightClass.MEDIUM
        else -> DeviceHeightClass.COMPACT
    }

    val isUltraCompactWidth = availableWidth < 360.dp
    val isShortHeight = availableHeight < 480.dp
    val useNavigationRail = widthClass != DeviceWidthClass.COMPACT
    val useTwoPane = availableWidth >= 720.dp && availableHeight >= 420.dp

    val videoGridColumns = when {
        availableWidth >= 1000.dp -> 3
        availableWidth >= 600.dp -> 2
        else -> 1
    }

    val horizontalPadding = when {
        isUltraCompactWidth -> 12.dp
        widthClass == DeviceWidthClass.COMPACT -> 16.dp
        widthClass == DeviceWidthClass.MEDIUM -> 24.dp
        else -> 32.dp
    }

    val verticalPadding = when {
        isShortHeight -> 8.dp
        widthClass == DeviceWidthClass.EXPANDED -> 16.dp
        else -> 12.dp
    }

    val contentSpacing = when {
        isUltraCompactWidth -> 8.dp
        widthClass == DeviceWidthClass.COMPACT -> 10.dp
        widthClass == DeviceWidthClass.MEDIUM -> 12.dp
        else -> 14.dp
    }

    val maxContentWidth = when (videoGridColumns) {
        3 -> 1200.dp
        2 -> 960.dp
        else -> 840.dp
    }

    val twoPaneMasterWidth = (availableWidth * 0.38f).coerceIn(300.dp, 400.dp)

    return AdaptiveLayoutSpec(
        widthClass = widthClass,
        heightClass = heightClass,
        isUltraCompactWidth = isUltraCompactWidth,
        isShortHeight = isShortHeight,
        useNavigationRail = useNavigationRail,
        useTwoPane = useTwoPane,
        videoGridColumns = videoGridColumns,
        horizontalPadding = horizontalPadding,
        verticalPadding = verticalPadding,
        contentSpacing = contentSpacing,
        maxContentWidth = maxContentWidth,
        twoPaneMasterWidth = twoPaneMasterWidth
    )
}
