package com.example.ui.components

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale
import com.example.data.model.FzQuickToolbarItem
import com.example.data.model.PlayerUiThemeStyle
import com.example.player.FZ_EQUALIZER_PRESETS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

private val EQ_BAND_LABELS = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")
private val SPEED_PRESETS = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 2.5f, 3.0f)
private val SLEEP_PRESET_MINUTES = listOf(15, 30, 45, 60, 90)

suspend fun saveBitmapScreenshotToGallery(
    context: Context,
    bitmap: Bitmap,
    videoTitle: String
): Boolean = withContext(Dispatchers.IO) {
    val safeName = videoTitle.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(36).ifBlank { "video" }
    val fileName = "FAZA_${safeName}_${System.currentTimeMillis()}.png"
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/FAZA Player")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return@withContext false
            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            true
        } else {
            val dir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "FAZA Player"
            ).apply { mkdirs() }
            val outFile = File(dir, fileName)
            FileOutputStream(outFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            true
        }
    }.getOrDefault(false)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FzEqualizerBottomSheet(
    equalizerEnabled: Boolean,
    selectedPresetIndex: Int,
    bandLevelsMb: List<Int>,
    onSelectPreset: (Int) -> Unit,
    onBandLevelChange: (bandIndex: Int, levelMb: Int) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Equalizer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Equalizer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FZ_EQUALIZER_PRESETS.forEachIndexed { idx, preset ->
                    FilterChip(
                        selected = (equalizerEnabled && selectedPresetIndex == idx) || (!equalizerEnabled && idx == 0),
                        onClick = { onSelectPreset(idx) },
                        label = { Text(preset.label) },
                        modifier = Modifier.testTag("fz_eq_preset_${preset.id}")
                    )
                }
            }

            bandLevelsMb.forEachIndexed { index, levelMb ->
                val label = EQ_BAND_LABELS.getOrElse(index) { "Band ${index + 1}" }
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label, style = MaterialTheme.typography.labelLarge)
                        Text(
                            text = "${if (levelMb > 0) "+" else ""}${(levelMb / 100f).roundToInt()} dB",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = levelMb.toFloat(),
                        onValueChange = { onBandLevelChange(index, it.roundToInt()) },
                        valueRange = -1200f..1200f
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FzSleepTimerBottomSheet(
    remainingSec: Int?,
    sleepAtEndOfVideo: Boolean,
    onSelectMinutes: (Int?) -> Unit,
    onToggleEndOfVideo: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Bedtime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sleep Timer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }

            if (remainingSec != null) {
                val mins = remainingSec / 60
                val secs = remainingSec % 60
                Text(
                    text = String.format(Locale.US, "Playback pauses in %02d:%02d", mins, secs),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (sleepAtEndOfVideo) {
                Text(
                    text = "Playback will pause at the end of this video",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = remainingSec == null && !sleepAtEndOfVideo,
                    onClick = {
                        onSelectMinutes(null)
                        onDismiss()
                    },
                    label = { Text("Off") },
                    modifier = Modifier.testTag("fz_sleep_off")
                )
                SLEEP_PRESET_MINUTES.forEach { min ->
                    FilterChip(
                        selected = remainingSec != null && ((remainingSec + 59) / 60) == min,
                        onClick = {
                            onSelectMinutes(min)
                            onDismiss()
                        },
                        label = { Text("$min min") },
                        modifier = Modifier.testTag("fz_sleep_$min")
                    )
                }
                FilterChip(
                    selected = sleepAtEndOfVideo,
                    onClick = {
                        onToggleEndOfVideo(!sleepAtEndOfVideo)
                        onDismiss()
                    },
                    label = { Text("End of video") },
                    modifier = Modifier.testTag("fz_sleep_end_of_video")
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FzSpeedPickerBottomSheet(
    currentSpeed: Float,
    onSelectSpeed: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Playback Speed", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SPEED_PRESETS.forEach { spd ->
                    FilterChip(
                        selected = kotlin.math.abs(currentSpeed - spd) < 0.05f,
                        onClick = {
                            onSelectSpeed(spd)
                            onDismiss()
                        },
                        label = { Text(formatFzSpeedLabel(spd)) },
                        modifier = Modifier.testTag("fz_speed_${spd}")
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FzCustomiseItemsBottomSheet(
    themeStyle: PlayerUiThemeStyle,
    hiddenItems: Set<String>,
    onThemeStyleChange: (PlayerUiThemeStyle) -> Unit,
    onHiddenItemsChange: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.DashboardCustomize, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Customise FZ Ui Items", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Player Interface Theme", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlayerUiThemeStyle.entries.forEach { style ->
                        FilterChip(
                            selected = themeStyle == style,
                            onClick = { onThemeStyleChange(style) },
                            label = { Text(style.label) },
                            modifier = Modifier.testTag("fz_sheet_theme_${style.name.lowercase()}")
                        )
                    }
                }
            }

            HorizontalDivider()

            Text("Quick-Access Toolbar Items", style = MaterialTheme.typography.titleSmall)
            FzQuickToolbarItem.entries
                .filter { it != FzQuickToolbarItem.CUSTOMISE_ITEMS }
                .forEach { item ->
                    val visible = item.name !in hiddenItems
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.label, style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = visible,
                            onCheckedChange = { shouldShow ->
                                val next = if (shouldShow) {
                                    hiddenItems - item.name
                                } else {
                                    hiddenItems + item.name
                                }
                                onHiddenItemsChange(next)
                            },
                            modifier = Modifier.testTag("fz_customise_toggle_${item.name.lowercase()}")
                        )
                    }
                }

            OutlinedButton(
                onClick = { onHiddenItemsChange(emptySet()) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Show All Toolbar Items")
            }
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}
