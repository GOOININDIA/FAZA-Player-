package com.example.ai

import com.google.firebase.ai.type.GroundingMetadata
import kotlinx.serialization.Serializable

enum class AssistantChatMode(
    val shortLabel: String,
    val modelName: String,
    val roleTitle: String,
    val roleSummary: String,
    val supportsGoogleSearch: Boolean,
    val defaultGoogleSearchEnabled: Boolean
) {
    GENERAL_SEARCH(
        shortLabel = "Search AI",
        modelName = "gemini-4-hydrogen",
        roleTitle = "Cinema & Web Search Assistant",
        roleSummary = "General help + live Google Search grounding",
        supportsGoogleSearch = true,
        defaultGoogleSearchEnabled = true
    ),
    FAST(
        shortLabel = "Fast",
        modelName = "gemini-3.1-flash-lite",
        roleTitle = "Quick Player Helper",
        roleSummary = "Fast answers for formats, controls & quick picks",
        supportsGoogleSearch = false,
        defaultGoogleSearchEnabled = false
    ),
    DEEP_REASONING(
        shortLabel = "Deep",
        modelName = "gemini-4-carbon",
        roleTitle = "Deep Media Analyst",
        roleSummary = "Deep reasoning for codecs, HDR, audio & film analysis",
        supportsGoogleSearch = false,
        defaultGoogleSearchEnabled = false
    ),
    COMPLEX_INTERACTIVE(
        shortLabel = "Planner",
        modelName = "gemini-4-argon",
        roleTitle = "Interactive Cinema Curator",
        roleSummary = "Multi-step watch planning & advanced troubleshooting",
        supportsGoogleSearch = false,
        defaultGoogleSearchEnabled = false
    )
}

@Serializable
enum class ChatRole {
    USER,
    ASSISTANT
}

@Serializable
data class GroundingSource(
    val title: String,
    val uri: String,
    val domain: String? = null
)

@Serializable
data class AssistantChatMessage(
    val id: String,
    val role: ChatRole,
    val text: String,
    val modelName: String = "",
    val modeLabel: String = "",
    val usedGoogleSearch: Boolean = false,
    val searchQueries: List<String> = emptyList(),
    val sources: List<GroundingSource> = emptyList(),
    val isStreaming: Boolean = false,
    val timestampMs: Long = System.currentTimeMillis()
)

sealed interface AssistantUiState {
    data object Idle : AssistantUiState
    data object Loading : AssistantUiState
    data class Streaming(val text: String) : AssistantUiState
    data class Success(val text: String) : AssistantUiState
    data class Error(val message: String) : AssistantUiState
}

data class LibraryContextSummary(
    val totalPublicVideos: Int = 0,
    val folderCount: Int = 0,
    val favouriteCount: Int = 0,
    val streamCount: Int = 0,
    val recentTitles: List<String> = emptyList(),
    val folderNames: List<String> = emptyList()
)

fun buildAssistantSystemInstruction(
    mode: AssistantChatMode,
    searchGroundingEnabled: Boolean,
    librarySummary: LibraryContextSummary
): String {
    val roleDirective = when (mode) {
        AssistantChatMode.GENERAL_SEARCH ->
            "You are FAZA Cinema Assistant inside FAZA Player. You help users with movies, shows, trailers, media formats, and playback tips." +
                if (searchGroundingEnabled) {
                    " Use Google Search grounding whenever up-to-date facts, recent releases, cast details, ratings, or current streaming/media news are helpful."
                } else {
                    ""
                }
        AssistantChatMode.FAST ->
            "You are FAZA Quick Helper inside FAZA Player. Provide concise, ultra-fast, easy-to-scan answers about playback controls, subtitles, aspect ratios, and quick recommendations."
        AssistantChatMode.DEEP_REASONING ->
            "You are FAZA Deep Media Analyst inside FAZA Player. Apply careful step-by-step reasoning for video engineering questions (HEVC, AV1, HDR10+, Dolby Vision, ExoPlayer hardware vs software decoding, audio passthrough) and in-depth film breakdowns."
        AssistantChatMode.COMPLEX_INTERACTIVE ->
            "You are FAZA Interactive Curator inside FAZA Player. Guide users through complex multi-step tasks such as curating themed watch marathons, organizing media folders, and diagnosing tricky network stream or subtitle sync issues interactively."
    }

    val librarySnippet = buildString {
        append(" User's public FAZA library summary: ")
        append("${librarySummary.totalPublicVideos} videos across ${librarySummary.folderCount} folders, ")
        append("${librarySummary.favouriteCount} favourites, ${librarySummary.streamCount} saved streams.")
        if (librarySummary.folderNames.isNotEmpty()) {
            append(" Folders: ${librarySummary.folderNames.take(6).joinToString(", ")}.")
        }
        if (librarySummary.recentTitles.isNotEmpty()) {
            append(" Recently watched: ${librarySummary.recentTitles.take(5).joinToString(", ")}.")
        }
    }

    return "$roleDirective Keep answers grounded, helpful, and formatted clearly for mobile reading.$librarySnippet"
}

fun extractGroundingDetails(
    metadata: GroundingMetadata?
): Pair<List<String>, List<GroundingSource>> {
    if (metadata == null) return emptyList<String>() to emptyList()
    val queries = metadata.webSearchQueries
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
    val sources = metadata.groundingChunks
        .mapNotNull { chunk ->
            val web = chunk.web ?: return@mapNotNull null
            val uri = web.uri?.trim().orEmpty()
            if (uri.isEmpty()) return@mapNotNull null
            val title = web.title?.trim().takeIf { !it.isNullOrEmpty() }
                ?: web.domain?.trim().takeIf { !it.isNullOrEmpty() }
                ?: uri
            GroundingSource(
                title = title,
                uri = uri,
                domain = web.domain?.trim()?.takeIf { it.isNotEmpty() }
            )
        }
        .distinctBy { it.uri }
    return queries to sources
}
