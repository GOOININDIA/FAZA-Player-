package com.example.util

import java.util.Locale
import java.util.concurrent.TimeUnit

object Formatters {
    fun formatDuration(durationMs: Long): String {
        if (durationMs <= 0L) return "00:00"
        val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(durationMs)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    fun formatSignedDelta(deltaMs: Long): String {
        val sign = if (deltaMs >= 0) "+" else "-"
        return "$sign${formatDuration(kotlin.math.abs(deltaMs))}"
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return "Stream"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            else -> String.format(Locale.US, "%.0f KB", kb)
        }
    }

    fun formatResolution(width: Int, height: Int): String {
        return if (width > 0 && height > 0) "${width}×${height}" else "Adaptive"
    }
}
