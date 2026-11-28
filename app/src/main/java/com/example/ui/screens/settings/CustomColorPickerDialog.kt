package com.example.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ui.theme.LocalFazaIsDark
import com.example.ui.theme.buildCustomAppColorScheme
import com.example.ui.theme.formatColorHexLabel
import com.example.ui.theme.hsvToRgbHex
import com.example.ui.theme.isMonochromeSeedColor
import com.example.ui.theme.parseColorHexInput
import com.example.ui.theme.rgbHexToHsv
import kotlin.math.roundToInt

private val HueSpectrumStops = listOf(
    Color(0xFFFF0000),
    Color(0xFFFFFF00),
    Color(0xFF00FF00),
    Color(0xFF00FFFF),
    Color(0xFF0000FF),
    Color(0xFFFF00FF),
    Color(0xFFFF0000)
)

@Composable
internal fun CustomColorPickerDialog(
    initialColorHex: Long,
    onDismiss: () -> Unit,
    onApplyColor: (Long) -> Unit
) {
    val isDark = LocalFazaIsDark.current
    val (initH, initS, initV) = remember(initialColorHex) { rgbHexToHsv(initialColorHex) }
    var hue by remember(initialColorHex) { mutableFloatStateOf(initH) }
    var saturation by remember(initialColorHex) { mutableFloatStateOf(initS) }
    var brightness by remember(initialColorHex) { mutableFloatStateOf(initV) }
    var draftColorHex by remember(initialColorHex) { mutableStateOf(initialColorHex) }
    var hexInput by remember(initialColorHex) { mutableStateOf(formatColorHexLabel(initialColorHex)) }
    var hexError by remember(initialColorHex) { mutableStateOf(false) }

    val previewColorScheme = remember(draftColorHex, isDark) {
        buildCustomAppColorScheme(draftColorHex, isDark)
    }

    fun updateFromHsv(newH: Float, newS: Float, newV: Float) {
        hue = newH.coerceIn(0f, 360f)
        saturation = newS.coerceIn(0f, 1f)
        brightness = newV.coerceIn(0f, 1f)
        val computed = hsvToRgbHex(hue, saturation, brightness)
        draftColorHex = computed
        hexInput = formatColorHexLabel(computed)
        hexError = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .widthIn(max = 440.dp)
                .testTag("custom_color_picker_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = previewColorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Palette,
                                contentDescription = null,
                                tint = previewColorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.custom_color_picker_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.custom_color_picker_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 2D Interactive Color Spectrum (Saturation x Brightness)
                ColorSpectrumPanel(
                    hue = hue,
                    saturation = saturation,
                    brightness = brightness,
                    selectedColor = Color(draftColorHex),
                    onSelectSv = { s, v -> updateFromHsv(hue, s, v) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(176.dp)
                        .testTag("custom_color_spectrum_panel")
                )

                // Hue Bar Slider
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.custom_color_hue_label),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${hue.roundToInt()}°",
                            style = MaterialTheme.typography.labelMedium,
                            color = previewColorScheme.primary
                        )
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Brush.horizontalGradient(HueSpectrumStops))
                        )
                        Slider(
                            value = hue,
                            onValueChange = { newHue -> updateFromHsv(newHue, saturation, brightness) },
                            valueRange = 0f..360f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(hsvToRgbHex(hue, 1f, 1f)),
                                activeTrackColor = Color.Transparent,
                                inactiveTrackColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_color_dialog_hue_slider")
                        )
                    }
                }

                // HEX Color Code Input Field
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { raw ->
                        val sanitized = raw.take(9)
                        hexInput = sanitized
                        val parsed = parseColorHexInput(sanitized)
                        if (parsed != null) {
                            draftColorHex = parsed
                            val (h, s, v) = rgbHexToHsv(parsed)
                            hue = h
                            saturation = s
                            brightness = v
                            hexError = false
                        } else {
                            hexError = true
                        }
                    },
                    label = { Text(stringResource(R.string.custom_color_hex_label)) },
                    placeholder = { Text("#6750A4") },
                    singleLine = true,
                    isError = hexError,
                    supportingText = {
                        Text(
                            text = if (hexError) {
                                stringResource(R.string.custom_color_hex_invalid)
                            } else {
                                stringResource(R.string.custom_color_hex_hint)
                            }
                        )
                    },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(draftColorHex))
                                .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        )
                    },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_color_hex_input")
                )

                // Live Preview Before Applying
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.custom_color_preview_title),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isMonochromeSeedColor(draftColorHex)) {
                                "Monochrome · ${formatColorHexLabel(draftColorHex)}"
                            } else {
                                formatColorHexLabel(draftColorHex)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    MaterialTheme(colorScheme = previewColorScheme) {
                        DialogThemePreviewCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_color_dialog_preview")
                        )
                    }
                }

                // Cancel / Apply Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("custom_color_cancel_button")
                    ) {
                        Text(stringResource(R.string.custom_color_cancel))
                    }
                    Button(
                        onClick = { onApplyColor(draftColorHex) },
                        enabled = !hexError,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = previewColorScheme.primary,
                            contentColor = previewColorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("custom_color_apply_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.custom_color_apply))
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorSpectrumPanel(
    hue: Float,
    saturation: Float,
    brightness: Float,
    selectedColor: Color,
    onSelectSv: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val pureHueColor = Color(hsvToRgbHex(hue, 1f, 1f))
    val shape = RoundedCornerShape(18.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val s = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                    val v = (1f - (offset.y / size.height.coerceAtLeast(1))).coerceIn(0f, 1f)
                    onSelectSv(s, v)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val pos = change.position
                    val s = (pos.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                    val v = (1f - (pos.y / size.height.coerceAtLeast(1))).coerceIn(0f, 1f)
                    onSelectSv(s, v)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Horizontal white -> pure hue gradient (Saturation)
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.White, pureHueColor)
                )
            )
            // Vertical transparent -> black gradient (Brightness / Value)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black)
                )
            )

            // Crosshair selector ring
            val thumbX = (saturation.coerceIn(0f, 1f) * size.width)
            val thumbY = ((1f - brightness.coerceIn(0f, 1f)) * size.height)
            val center = Offset(thumbX, thumbY)
            drawCircle(
                color = Color.Black.copy(alpha = 0.45f),
                radius = 12.dp.toPx(),
                center = center
            )
            drawCircle(
                color = selectedColor,
                radius = 9.dp.toPx(),
                center = center
            )
            drawCircle(
                color = Color.White,
                radius = 10.dp.toPx(),
                center = center,
                style = Stroke(width = 2.5.dp.toPx())
            )
        }
    }
}

@Composable
private fun DialogThemePreviewCard(
    modifier: Modifier = Modifier
) {
    val previewColors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = previewColors.surfaceContainerHigh,
        border = BorderStroke(1.dp, previewColors.primary.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = previewColors.primary,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = previewColors.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "FAZA Player",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = previewColors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Buttons, tabs & sliders",
                        style = MaterialTheme.typography.labelSmall,
                        color = previewColors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Switch(
                    checked = true,
                    onCheckedChange = null
                )
            }

            LinearProgressIndicator(
                progress = { 0.68f },
                color = previewColors.primary,
                trackColor = previewColors.surfaceContainerHighest,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = previewColors.primary
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Home,
                            contentDescription = null,
                            tint = previewColors.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Home",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = previewColors.onPrimary
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        previewColors.primary,
                        previewColors.secondary,
                        previewColors.tertiary
                    ).forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }
            }
        }
    }
}
