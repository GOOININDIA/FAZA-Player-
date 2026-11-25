package com.example.ads

import java.util.Locale
import java.util.UUID

const val REQUIRED_REWARDED_ADS: Int = 8
const val FREE_ACCESS_DURATION_MS: Long = 24L * 60L * 60L * 1000L
const val FREE_ACCESS_CONGRATULATIONS_MESSAGE: String =
    "Congratulations! You have unlocked 24 hours of ad-free experience."

data class FreeAccessStatus(
    val watchedCount: Int,
    val requiredCount: Int = REQUIRED_REWARDED_ADS,
    val expiresAtMs: Long,
    val evaluatedAtMs: Long
) {
    val clampedWatchedCount: Int
        get() = watchedCount.coerceIn(0, requiredCount)

    val isActive: Boolean
        get() = expiresAtMs > evaluatedAtMs

    val isExpired: Boolean
        get() = expiresAtMs in 1L..evaluatedAtMs

    val remainingMs: Long
        get() = (expiresAtMs - evaluatedAtMs).coerceAtLeast(0L)

    val remainingAds: Int
        get() = (requiredCount - clampedWatchedCount).coerceAtLeast(0)

    val progressFraction: Float
        get() = if (requiredCount <= 0) {
            0f
        } else {
            (clampedWatchedCount.toFloat() / requiredCount.toFloat()).coerceIn(0f, 1f)
        }

    val progressPercentText: String
        get() = AdFreeAccessPolicy.formatProgressPercent(clampedWatchedCount, requiredCount)

    val remainingTimeText: String
        get() = AdFreeAccessPolicy.formatRemainingTime(remainingMs)

    val remainingTimeFraction: Float
        get() = (remainingMs.toFloat() / FREE_ACCESS_DURATION_MS.toFloat()).coerceIn(0f, 1f)
}

data class RewardCompletionResult(
    val newWatchedCount: Int,
    val newExpiresAtMs: Long,
    val unlocked24HourAccess: Boolean,
    val remainingAds: Int,
    val wasAlreadyActive: Boolean = false
)

object AdFreeAccessPolicy {

    fun isFreeAccessActive(expiresAtMs: Long, nowMs: Long): Boolean {
        return expiresAtMs > nowMs
    }

    fun resolveStatus(
        watchedCount: Int,
        expiresAtMs: Long,
        nowMs: Long
    ): FreeAccessStatus {
        return FreeAccessStatus(
            watchedCount = watchedCount.coerceIn(0, REQUIRED_REWARDED_ADS),
            requiredCount = REQUIRED_REWARDED_ADS,
            expiresAtMs = expiresAtMs.coerceAtLeast(0L),
            evaluatedAtMs = nowMs
        )
    }

    /**
     * Ad-Free Access priority rules:
     * 1. Active 24-hour free access -> hide all advertisements.
     * 2. Offline -> do not show or request any advertisements.
     * 3. Online with no active free access -> show banner advertisements on Home & Profile.
     */
    fun shouldShowBannerAds(
        isOnline: Boolean,
        expiresAtMs: Long,
        nowMs: Long
    ): Boolean {
        if (isFreeAccessActive(expiresAtMs, nowMs)) return false
        if (!isOnline) return false
        return true
    }

    /**
     * Rewarded ads may only be requested when the user explicitly asks for free access,
     * the device is online, and 24-hour free access is not already active.
     */
    fun shouldAllowRewardedAdRequest(
        isOnline: Boolean,
        expiresAtMs: Long,
        nowMs: Long
    ): Boolean {
        if (!isOnline) return false
        if (isFreeAccessActive(expiresAtMs, nowMs)) return false
        return true
    }

    /**
     * Pure state transition for a single verified rewarded-ad completion:
     * - Increments progress 0/8 -> 1/8 ... 7/8 -> unlocks 24h access and resets progress to 0/8.
     * - Starts the 24-hour timer strictly when the 8th valid reward is earned.
     */
    fun applyCompletedReward(
        currentWatchedCount: Int,
        currentExpiresAtMs: Long,
        nowMs: Long
    ): RewardCompletionResult {
        if (isFreeAccessActive(currentExpiresAtMs, nowMs)) {
            return RewardCompletionResult(
                newWatchedCount = currentWatchedCount.coerceIn(0, REQUIRED_REWARDED_ADS),
                newExpiresAtMs = currentExpiresAtMs,
                unlocked24HourAccess = false,
                remainingAds = (REQUIRED_REWARDED_ADS - currentWatchedCount.coerceIn(0, REQUIRED_REWARDED_ADS))
                    .coerceAtLeast(0),
                wasAlreadyActive = true
            )
        }

        val nextCount = currentWatchedCount.coerceIn(0, REQUIRED_REWARDED_ADS) + 1
        return if (nextCount >= REQUIRED_REWARDED_ADS) {
            RewardCompletionResult(
                newWatchedCount = 0,
                newExpiresAtMs = nowMs + FREE_ACCESS_DURATION_MS,
                unlocked24HourAccess = true,
                remainingAds = 0,
                wasAlreadyActive = false
            )
        } else {
            RewardCompletionResult(
                newWatchedCount = nextCount,
                newExpiresAtMs = 0L,
                unlocked24HourAccess = false,
                remainingAds = (REQUIRED_REWARDED_ADS - nextCount).coerceAtLeast(0),
                wasAlreadyActive = false
            )
        }
    }

    fun formatRemainingTime(remainingMs: Long): String {
        if (remainingMs <= 0L) return "0h 00m"
        val totalMinutes = remainingMs / 60_000L
        if (totalMinutes <= 0L) return "< 1m"
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        return String.format(Locale.US, "%dh %02dm", hours, minutes)
    }

    fun formatProgressPercent(
        watchedCount: Int,
        requiredCount: Int = REQUIRED_REWARDED_ADS
    ): String {
        if (requiredCount <= 0) return "0%"
        val clamped = watchedCount.coerceIn(0, requiredCount)
        val percent = (clamped * 100.0) / requiredCount.toDouble()
        return if (percent % 1.0 == 0.0) {
            "${percent.toInt()}%"
        } else {
            String.format(Locale.US, "%.1f%%", percent)
        }
    }
}

/**
 * Ensures each shown rewarded ad session can grant at most ONE reward credit,
 * preventing duplicate callbacks or late/stale callbacks from inflating progress.
 */
class RewardSessionGuard {
    private val lock = Any()
    private var activeSessionId: String? = null
    private var rewardClaimedForActiveSession: Boolean = false
    private val claimedSessions = HashSet<String>()

    fun startNewSession(): String = synchronized(lock) {
        val id = UUID.randomUUID().toString()
        activeSessionId = id
        rewardClaimedForActiveSession = false
        id
    }

    fun tryClaimReward(sessionId: String): Boolean = synchronized(lock) {
        if (sessionId.isBlank()) return false
        if (activeSessionId != sessionId) return false
        if (rewardClaimedForActiveSession || claimedSessions.contains(sessionId)) return false
        rewardClaimedForActiveSession = true
        claimedSessions.add(sessionId)
        true
    }

    fun hasClaimedReward(sessionId: String): Boolean = synchronized(lock) {
        claimedSessions.contains(sessionId)
    }

    fun finishSession(sessionId: String) = synchronized(lock) {
        if (activeSessionId == sessionId) {
            activeSessionId = null
            rewardClaimedForActiveSession = false
        }
    }
}
