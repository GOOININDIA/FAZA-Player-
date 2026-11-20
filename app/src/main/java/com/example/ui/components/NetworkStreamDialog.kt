package com.example.ui.components

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.NetworkStreamValidator
import com.example.data.model.StreamProtocol
import com.example.data.model.StreamUrlValidation
import com.example.data.model.VideoItem

@Composable
fun NetworkStreamDialog(
    recentStreams: List<VideoItem> = emptyList(),
    onDismiss: () -> Unit,
    onPlayStream: (url: String, title: String?, protocol: StreamProtocol) -> Unit,
    onPlaySavedStream: (VideoItem) -> Unit = {}
) {
    val context = LocalContext.current
    var urlInput by remember { mutableStateOf("") }
    var titleInput by remember { mutableStateOf("") }
    var selectedProtocol by remember { mutableStateOf(StreamProtocol.AUTO) }
    var clipboardFeedback by remember { mutableStateOf<String?>(null) }

    val validation = remember(urlInput) {
        NetworkStreamValidator.validateUrl(urlInput)
    }
    val canSubmit = validation is StreamUrlValidation.Valid

    val submitStream = {
        if (validation is StreamUrlValidation.Valid) {
            onPlayStream(
                validation.normalizedUrl,
                titleInput.trim().takeIf { it.isNotBlank() },
                selectedProtocol
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        icon = {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.AddLink,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        },
        title = {
            Text(
                text = "Play Network Stream",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Stream HTTP/HTTPS, HLS (.m3u8), DASH (.mpd), MP4, MKV, or WebM directly in ExoPlayer.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = {
                        urlInput = it
                        clipboardFeedback = null
                    },
                    label = { Text("Stream URL") },
                    placeholder = { Text("https://example.com/stream.m3u8") },
                    singleLine = true,
                    isError = validation is StreamUrlValidation.Invalid,
                    supportingText = {
                        when (val v = validation) {
                            StreamUrlValidation.Empty -> {
                                Text(clipboardFeedback ?: "Paste or type a direct media or manifest URL.")
                            }
                            is StreamUrlValidation.Invalid -> {
                                Text(v.reason)
                            }
                            is StreamUrlValidation.Valid -> {
                                val effectiveLabel = if (selectedProtocol == StreamProtocol.AUTO) {
                                    if (v.detectedProtocol == StreamProtocol.AUTO) {
                                        "Ready • ${v.hostLabel}"
                                    } else {
                                        "Detected ${v.detectedProtocol.label} • ${v.hostLabel}"
                                    }
                                } else {
                                    "Forced ${selectedProtocol.label} • ${v.hostLabel}"
                                }
                                Text(
                                    text = effectiveLabel,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (urlInput.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        urlInput = ""
                                        clipboardFeedback = null
                                    },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("stream_clear_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Clear,
                                        contentDescription = "Clear URL"
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val clipText = clipboard?.primaryClip
                                        ?.takeIf { it.itemCount > 0 }
                                        ?.getItemAt(0)
                                        ?.coerceToText(context)
                                        ?.toString()
                                        ?.trim()
                                    if (!clipText.isNullOrBlank()) {
                                        urlInput = NetworkStreamValidator.normalizeUrl(clipText)
                                        clipboardFeedback = null
                                    } else {
                                        clipboardFeedback = "Clipboard does not contain text."
                                    }
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("stream_paste_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentPaste,
                                    contentDescription = "Paste URL from clipboard",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stream_url_input")
                )

                AnimatedVisibility(
                    visible = (validation as? StreamUrlValidation.Invalid)?.canPrependHttps == true
                ) {
                    AssistChip(
                        onClick = {
                            val normalized = NetworkStreamValidator.normalizeUrl(urlInput)
                            if (!normalized.contains("://")) {
                                urlInput = "https://$normalized"
                            }
                        },
                        shape = CircleShape,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Http,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        label = { Text("Prepend https://") },
                        modifier = Modifier.testTag("stream_prepend_https_chip")
                    )
                }

                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("Stream Title (Optional)") },
                    placeholder = { Text("My Live Channel or Video") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submitStream() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stream_title_input")
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Stream Format Hint",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StreamProtocol.entries.forEach { protocol ->
                            val selected = selectedProtocol == protocol
                            FilterChip(
                                selected = selected,
                                onClick = { selectedProtocol = protocol },
                                shape = CircleShape,
                                leadingIcon = if (selected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                label = { Text(protocol.shortLabel) },
                                modifier = Modifier.testTag("stream_protocol_${protocol.name.lowercase()}")
                            )
                        }
                    }
                }

                if (recentStreams.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Recent Network Streams",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        recentStreams.take(4).forEach { stream ->
                            Surface(
                                onClick = {
                                    urlInput = stream.uri
                                    titleInput = stream.title
                                },
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("recent_stream_item_${stream.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = stream.formatTag,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stream.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = stream.uri,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(
                                        onClick = { onPlaySavedStream(stream) },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("recent_stream_play_${stream.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.PlayArrow,
                                            contentDescription = "Play ${stream.title}",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = submitStream,
                enabled = canSubmit,
                shape = CircleShape,
                modifier = Modifier
                    .height(48.dp)
                    .testTag("stream_play_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Play Stream")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = CircleShape,
                modifier = Modifier
                    .height(48.dp)
                    .testTag("stream_cancel_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
