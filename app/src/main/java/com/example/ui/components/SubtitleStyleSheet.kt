package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SubtitleEdgeStyle
import com.example.data.model.SubtitleStyleConfig
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubtitleStyleEditor(
    style: SubtitleStyleConfig,
    onStyleChange: (SubtitleStyleConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorPresets = listOf(
        0xFFFFFFFF to "White",
        0xFF00E5FF to "Cyan",
        0xFFFFD54F to "Amber",
        0xFF69F0AE to "Emerald"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Live preview theater box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(106.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF0E172A), Color(0xFF1B2A4A), Color(0xFF0D1117))
                    )
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(
                text = if (style.enabled) {
                    "FAZA Player • Crystal-clear 8K Ultra HD Subtitles"
                } else {
                    "Subtitles disabled"
                },
                textAlign = TextAlign.Center,
                style = buildSubtitleTextStyle(style),
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .background(
                        Color.Black.copy(alpha = if (style.enabled) style.backgroundOpacity else 0.3f),
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Enable Subtitles", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Show embedded or external SRT/VTT captions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = style.enabled,
                onCheckedChange = { onStyleChange(style.copy(enabled = it)) },
                modifier = Modifier.testTag("subtitle_enabled_switch")
            )
        }

        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Font Size", style = MaterialTheme.typography.titleSmall)
                Text("${style.fontSizeSp.roundToInt()} sp", style = MaterialTheme.typography.labelMedium)
            }
            Slider(
                value = style.fontSizeSp,
                onValueChange = { onStyleChange(style.copy(fontSizeSp = it)) },
                valueRange = 12f..32f
            )
        }

        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Backdrop Opacity", style = MaterialTheme.typography.titleSmall)
                Text("${(style.backgroundOpacity * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
            }
            Slider(
                value = style.backgroundOpacity,
                onValueChange = { onStyleChange(style.copy(backgroundOpacity = it)) },
                valueRange = 0f..0.9f
            )
        }

        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Bottom Position Offset", style = MaterialTheme.typography.titleSmall)
                Text("${style.bottomPaddingDp.roundToInt()} dp", style = MaterialTheme.typography.labelMedium)
            }
            Slider(
                value = style.bottomPaddingDp,
                onValueChange = { onStyleChange(style.copy(bottomPaddingDp = it)) },
                valueRange = 16f..110f
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Text Color", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                colorPresets.forEach { (hex, label) ->
                    val selected = style.textColorHex == hex
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(hex))
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                            .clickable { onStyleChange(style.copy(textColorHex = hex)) }
                            .testTag("sub_color_$label")
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Edge & Weight", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SubtitleEdgeStyle.entries.forEach { edge ->
                    FilterChip(
                        selected = style.edgeStyle == edge,
                        onClick = { onStyleChange(style.copy(edgeStyle = edge)) },
                        label = { Text(edge.label) }
                    )
                }
                FilterChip(
                    selected = style.bold,
                    onClick = { onStyleChange(style.copy(bold = !style.bold)) },
                    label = { Text("Bold Text") }
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

fun buildSubtitleTextStyle(style: SubtitleStyleConfig): TextStyle {
    val shadow = when (style.edgeStyle) {
        SubtitleEdgeStyle.NONE -> null
        SubtitleEdgeStyle.OUTLINE -> Shadow(
            color = Color.Black,
            offset = Offset(0f, 0f),
            blurRadius = 6f
        )
        SubtitleEdgeStyle.DROP_SHADOW -> Shadow(
            color = Color.Black.copy(alpha = 0.9f),
            offset = Offset(3f, 3f),
            blurRadius = 5f
        )
    }
    return TextStyle(
        color = Color(style.textColorHex),
        fontSize = style.fontSizeSp.sp,
        fontWeight = if (style.bold) FontWeight.Bold else FontWeight.Medium,
        shadow = shadow
    )
}
