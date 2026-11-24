package com.example.util

import com.example.player.TrackOption
import java.util.Locale

/**
 * Resolves the active video's audio language into its native display name
 * (e.g. "हिंदी", "English", "বাংলা", "தமிழ்") from ExoPlayer audio track metadata
 * with a word-boundary fallback on the track label and video title.
 */
object VideoLanguageResolver {

    private val nativeLanguageMap: Map<String, String> = mapOf(
        // Indian languages
        "hi" to "हिंदी",
        "hin" to "हिंदी",
        "hindi" to "हिंदी",
        "bn" to "বাংলা",
        "ben" to "বাংলা",
        "bengali" to "বাংলা",
        "bangla" to "বাংলা",
        "en" to "English",
        "eng" to "English",
        "english" to "English",
        "ta" to "தமிழ்",
        "tam" to "தமிழ்",
        "tamil" to "தமிழ்",
        "te" to "తెలుగు",
        "tel" to "తెలుగు",
        "telugu" to "తెలుగు",
        "mr" to "मराठी",
        "mar" to "मराठी",
        "marathi" to "मराठी",
        "pa" to "ਪੰਜਾਬੀ",
        "pan" to "ਪੰਜਾਬੀ",
        "punjabi" to "ਪੰਜਾਬੀ",
        "gu" to "ગુજરાતી",
        "guj" to "ગુજરાતી",
        "gujarati" to "ગુજરાતી",
        "kn" to "ಕನ್ನಡ",
        "kan" to "ಕನ್ನಡ",
        "kannada" to "ಕನ್ನಡ",
        "ml" to "മലയാളം",
        "mal" to "മലയാളം",
        "malayalam" to "മലയാളം",
        "ur" to "اردو",
        "urd" to "اردو",
        "urdu" to "اردو",
        "or" to "ଓଡ଼ିଆ",
        "ori" to "ଓଡ଼ିଆ",
        "odia" to "ଓଡ଼ିଆ",
        "oriya" to "ଓଡ଼ିଆ",
        "as" to "অসমীয়া",
        "asm" to "অসমীয়া",
        "assamese" to "অসমীয়া",
        "bho" to "भोजपुरी",
        "bhojpuri" to "भोजपुरी",
        "ne" to "नेपाली",
        "nep" to "नेपाली",
        "nepali" to "नेपाली",
        "si" to "සිංහල",
        "sin" to "සිංහල",
        "sinhala" to "සිංහල",

        // Global languages
        "ja" to "日本語",
        "jpn" to "日本語",
        "japanese" to "日本語",
        "ko" to "한국어",
        "kor" to "한국어",
        "korean" to "한국어",
        "es" to "Español",
        "spa" to "Español",
        "spanish" to "Español",
        "fr" to "Français",
        "fra" to "Français",
        "fre" to "Français",
        "french" to "Français",
        "de" to "Deutsch",
        "deu" to "Deutsch",
        "ger" to "Deutsch",
        "german" to "Deutsch",
        "ar" to "العربية",
        "ara" to "العربية",
        "arabic" to "العربية",
        "ru" to "Русский",
        "rus" to "Русский",
        "russian" to "Русский",
        "zh" to "中文",
        "zho" to "中文",
        "chi" to "中文",
        "chinese" to "中文",
        "mandarin" to "中文",
        "pt" to "Português",
        "por" to "Português",
        "portuguese" to "Português",
        "it" to "Italiano",
        "ita" to "Italiano",
        "italian" to "Italiano",
        "tr" to "Türkçe",
        "tur" to "Türkçe",
        "turkish" to "Türkçe",
        "th" to "ไทย",
        "tha" to "ไทย",
        "thai" to "ไทย",
        "id" to "Bahasa",
        "ind" to "Bahasa",
        "indonesian" to "Bahasa",
        "vi" to "Tiếng Việt",
        "vie" to "Tiếng Việt",
        "vietnamese" to "Tiếng Việt",
        "fa" to "فارسی",
        "fas" to "فارسی",
        "per" to "فارسی",
        "persian" to "فارسی"
    )

    private val titleLanguageKeywords: List<Pair<String, String>> = listOf(
        "hindi" to "हिंदी",
        "bengali" to "বাংলা",
        "bangla" to "বাংলা",
        "english" to "English",
        "tamil" to "தமிழ்",
        "telugu" to "తెలుగు",
        "marathi" to "मराठी",
        "punjabi" to "ਪੰਜਾਬੀ",
        "gujarati" to "ગુજરાતી",
        "kannada" to "ಕನ್ನಡ",
        "malayalam" to "മലയാളം",
        "urdu" to "اردو",
        "odia" to "ଓଡ଼ିଆ",
        "oriya" to "ଓଡ଼ିଆ",
        "assamese" to "অসমীয়া",
        "bhojpuri" to "भोजपुरी",
        "nepali" to "नेपाली",
        "japanese" to "日本語",
        "korean" to "한국어",
        "spanish" to "Español",
        "french" to "Français",
        "german" to "Deutsch",
        "arabic" to "العربية",
        "russian" to "Русский",
        "chinese" to "中文",
        "portuguese" to "Português",
        "italian" to "Italiano",
        "turkish" to "Türkçe",
        "thai" to "ไทย",
        "indonesian" to "Bahasa",
        "vietnamese" to "Tiếng Việt"
    )

    private val unknownCodes = setOf("", "und", "unknown", "none", "null", "zxx", "mul", "mis", "qaa")

    /**
     * Resolves a single ISO language code or language name into its native display string.
     */
    fun resolveLanguageCodeOrName(raw: String?): String? {
        val cleaned = raw?.trim().orEmpty()
        if (cleaned.isEmpty()) return null

        val lower = cleaned.lowercase(Locale.ROOT)
        if (lower in unknownCodes) return null

        // Direct match (e.g., "hi", "hin", "hindi", "en", "bn")
        nativeLanguageMap[lower]?.let { return it }

        // BCP-47 primary subtag match (e.g., "hi-IN", "en_US", "bn-BD")
        val primarySubtag = lower.substringBefore('-').substringBefore('_').trim()
        if (primarySubtag in unknownCodes) return null
        nativeLanguageMap[primarySubtag]?.let { return it }

        // Keyword search inside descriptive track labels (e.g., "Hindi 5.1 • AAC", "Bengali Stereo")
        matchLanguageKeyword(cleaned)?.let { return it }

        // Fallback to JVM/Android Locale native display name for any valid 2- or 3-letter ISO code
        if (primarySubtag.length in 2..3 && primarySubtag.all { it in 'a'..'z' }) {
            val locale = Locale.forLanguageTag(primarySubtag)
            val display = locale.getDisplayLanguage(locale).trim()
            if (display.isNotEmpty() && !display.equals(primarySubtag, ignoreCase = true) &&
                !display.equals("und", ignoreCase = true)
            ) {
                return display.replaceFirstChar { ch ->
                    if (ch.isLowerCase()) ch.titlecase(locale) else ch.toString()
                }
            }
        }

        return null
    }

    /**
     * Resolves the currently active video audio language from track options with a title fallback.
     */
    fun resolveFromAudioTracks(
        audioTracks: List<TrackOption>,
        fallbackTitle: String = ""
    ): String? {
        val activeTrack = audioTracks.firstOrNull { it.isSelected } ?: audioTracks.firstOrNull()
        if (activeTrack != null) {
            resolveLanguageCodeOrName(activeTrack.language)?.let { return it }
            resolveLanguageCodeOrName(activeTrack.label)?.let { return it }
        }
        return matchLanguageKeyword(fallbackTitle)
    }

    private fun matchLanguageKeyword(text: String): String? {
        if (text.isBlank()) return null
        val tokens = text
            .lowercase(Locale.ROOT)
            .split(Regex("[^a-z\\p{L}]+"))
            .filter { it.isNotEmpty() }
            .toSet()

        // Also check native scripts directly in track labels / filenames
        if (tokens.any { it.contains("हिंदी") || it.contains("हिन्दी") }) return "हिंदी"
        if (tokens.any { it.contains("বাংলা") }) return "বাংলা"
        if (tokens.any { it.contains("தமிழ்") }) return "தமிழ்"
        if (tokens.any { it.contains("తెలుగు") }) return "తెలుగు"
        if (tokens.any { it.contains("मराठी") }) return "मराठी"
        if (tokens.any { it.contains("ਪੰਜਾਬੀ") }) return "ਪੰਜਾਬੀ"

        for ((keyword, nativeName) in titleLanguageKeywords) {
            if (keyword in tokens) return nativeName
        }
        return null
    }
}
