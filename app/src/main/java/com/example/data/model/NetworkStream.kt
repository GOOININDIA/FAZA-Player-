package com.example.data.model

import java.net.URI

enum class StreamProtocol(
    val label: String,
    val shortLabel: String,
    val badge: String,
    val mimeType: String?
) {
    AUTO("Auto Detect", "Auto", "STREAM", null),
    HLS("HLS (.m3u8)", "HLS", "HLS", "application/x-mpegURL"),
    DASH("MPEG-DASH (.mpd)", "DASH", "DASH", "application/dash+xml"),
    PROGRESSIVE("Direct (MP4 / MKV)", "Direct", "DIRECT", null)
}

sealed interface StreamUrlValidation {
    data object Empty : StreamUrlValidation
    data class Valid(
        val normalizedUrl: String,
        val detectedProtocol: StreamProtocol,
        val hostLabel: String
    ) : StreamUrlValidation

    data class Invalid(
        val reason: String,
        val canPrependHttps: Boolean = false
    ) : StreamUrlValidation
}

data class ResolvedStreamMeta(
    val normalizedUrl: String,
    val title: String,
    val formatTag: String,
    val mimeType: String
)

object NetworkStreamValidator {

    fun normalizeUrl(raw: String): String {
        return raw.trim()
            .removeSurrounding("<", ">")
            .removeSurrounding("\"", "\"")
            .removeSurrounding("'", "'")
            .trim()
    }

    fun validateUrl(raw: String): StreamUrlValidation {
        val cleaned = normalizeUrl(raw)
        if (cleaned.isEmpty()) return StreamUrlValidation.Empty
        if (cleaned.any { it.isWhitespace() }) {
            return StreamUrlValidation.Invalid("URL cannot contain spaces or line breaks.")
        }

        val hasScheme = cleaned.contains("://")
        if (!hasScheme) {
            val looksLikeHost = cleaned.contains('.') && !cleaned.startsWith(".")
            return StreamUrlValidation.Invalid(
                reason = "Include http:// or https:// at the start of the URL.",
                canPrependHttps = looksLikeHost
            )
        }

        val parsed = try {
            URI(cleaned)
        } catch (_: Exception) {
            return StreamUrlValidation.Invalid("Enter a valid network URL.")
        }

        val scheme = parsed.scheme?.lowercase().orEmpty()
        if (scheme != "http" && scheme != "https") {
            return StreamUrlValidation.Invalid(
                "Only http:// and https:// video streams are supported."
            )
        }

        val host = parsed.host?.takeIf { it.isNotBlank() }
            ?: parsed.authority?.substringBefore(':')?.takeIf { it.isNotBlank() }
            ?: return StreamUrlValidation.Invalid("URL is missing a valid server hostname.")

        return StreamUrlValidation.Valid(
            normalizedUrl = cleaned,
            detectedProtocol = detectProtocol(cleaned),
            hostLabel = host
        )
    }

    fun detectProtocol(url: String): StreamProtocol {
        val pathOnly = url.substringBefore('?').substringBefore('#').lowercase()
        val query = url.substringAfter('?', "").lowercase()
        return when {
            pathOnly.endsWith(".m3u8") ||
                pathOnly.endsWith(".m3u") ||
                pathOnly.contains("/hls/") ||
                query.contains("format=m3u8") ||
                query.contains("type=hls") -> StreamProtocol.HLS

            pathOnly.endsWith(".mpd") ||
                pathOnly.contains("/dash/") ||
                query.contains("format=mpd") ||
                query.contains("type=dash") -> StreamProtocol.DASH

            pathOnly.endsWith(".mp4") ||
                pathOnly.endsWith(".mkv") ||
                pathOnly.endsWith(".webm") ||
                pathOnly.endsWith(".mov") ||
                pathOnly.endsWith(".m4v") ||
                pathOnly.endsWith(".ts") ||
                pathOnly.endsWith(".flv") ||
                pathOnly.endsWith(".avi") -> StreamProtocol.PROGRESSIVE

            else -> StreamProtocol.AUTO
        }
    }

    fun resolveStreamMetadata(
        url: String,
        selectedProtocol: StreamProtocol,
        customTitle: String?
    ): ResolvedStreamMeta {
        val cleaned = normalizeUrl(url)
        val pathOnly = cleaned.substringBefore('?').substringBefore('#')
        val lastSegment = pathOnly.trimEnd('/').substringAfterLast('/', "").takeIf { it.isNotBlank() }
        val host = try {
            URI(cleaned).host?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
        val title = customTitle?.trim()?.takeIf { it.isNotBlank() }
            ?: lastSegment
            ?: host?.let { "Stream ($it)" }
            ?: "Network Stream"

        val ext = (lastSegment ?: "").substringAfterLast('.', "").uppercase()
        val effectiveProtocol = if (selectedProtocol == StreamProtocol.AUTO) {
            detectProtocol(cleaned)
        } else {
            selectedProtocol
        }

        val formatTag = when (effectiveProtocol) {
            StreamProtocol.HLS -> "HLS"
            StreamProtocol.DASH -> "DASH"
            StreamProtocol.PROGRESSIVE -> when (ext) {
                "MKV", "MP4", "WEBM", "MOV", "AVI", "FLV", "M4V", "TS" -> ext
                else -> "DIRECT"
            }
            StreamProtocol.AUTO -> when (ext) {
                "M3U8", "M3U" -> "HLS"
                "MPD" -> "DASH"
                "MKV", "MP4", "WEBM", "MOV", "AVI", "FLV", "M4V", "TS" -> ext
                else -> "STREAM"
            }
        }

        val mimeType = when (formatTag) {
            "HLS" -> "application/x-mpegURL"
            "DASH" -> "application/dash+xml"
            "MKV" -> "video/x-matroska"
            "WEBM" -> "video/webm"
            "MOV" -> "video/quicktime"
            "AVI" -> "video/x-msvideo"
            "FLV" -> "video/x-flv"
            "TS" -> "video/mp2t"
            "MP4", "M4V" -> "video/mp4"
            else -> effectiveProtocol.mimeType ?: "video/*"
        }

        return ResolvedStreamMeta(
            normalizedUrl = cleaned,
            title = title,
            formatTag = formatTag,
            mimeType = mimeType
        )
    }
}
