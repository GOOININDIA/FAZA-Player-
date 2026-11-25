package com.example.ui.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ads.AdFreeAccessPolicy
import com.example.ads.FREE_ACCESS_CONGRATULATIONS_MESSAGE
import com.example.ads.FazaAdsManager
import com.example.ads.FreeAccessStatus
import com.example.ads.REQUIRED_REWARDED_ADS
import com.example.ads.RewardSessionGuard
import com.example.ads.RewardedAdCallbacks
import com.example.data.local.UserPreferencesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class FreeAccessFeedbackType {
    SUCCESS,
    INFO,
    WARNING
}

data class FreeAccessFeedback(
    val message: String,
    val type: FreeAccessFeedbackType
)

class AdFreeAccessViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {

    private val sessionGuard = RewardSessionGuard()
    private val nowMsState = MutableStateFlow(clock())

    private val _isLoadingRewardedAd = MutableStateFlow(false)
    val isLoadingRewardedAd: StateFlow<Boolean> = _isLoadingRewardedAd.asStateFlow()

    private val _feedback = MutableStateFlow<FreeAccessFeedback?>(null)
    val feedback: StateFlow<FreeAccessFeedback?> = _feedback.asStateFlow()

    val freeAccessStatus: StateFlow<FreeAccessStatus> = combine(
        preferencesRepository.preferencesFlow
            .map { it.rewardedAdsWatchedCount to it.freeAccessExpiresAtMs }
            .distinctUntilChanged(),
        nowMsState
    ) { (watchedCount, expiresAtMs), nowMs ->
        AdFreeAccessPolicy.resolveStatus(
            watchedCount = watchedCount,
            expiresAtMs = expiresAtMs,
            nowMs = nowMs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = AdFreeAccessPolicy.resolveStatus(
            watchedCount = 0,
            expiresAtMs = 0L,
            nowMs = clock()
        )
    )

    init {
        // Lightweight countdown ticker: only ticks while 24-hour free access is actively running.
        viewModelScope.launch {
            preferencesRepository.preferencesFlow
                .map { it.freeAccessExpiresAtMs }
                .distinctUntilChanged()
                .collect { expiresAtMs ->
                    val currentNow = clock()
                    nowMsState.value = currentNow
                    if (expiresAtMs > currentNow) {
                        var now = currentNow
                        while (expiresAtMs > now) {
                            val remaining = expiresAtMs - now
                            val nextStepMs = minOf(30_000L, remaining + 50L)
                            delay(nextStepMs)
                            now = clock()
                            nowMsState.value = now
                        }
                    }
                }
        }
    }

    fun refreshTimer(nowMs: Long = clock()) {
        nowMsState.value = nowMs
    }

    fun clearFeedback() {
        _feedback.value = null
    }

    fun onConnectivityChanged(isOnline: Boolean) {
        if (!isOnline) {
            _isLoadingRewardedAd.value = false
            // Never display ad-loading errors when offline
            if (_feedback.value?.type == FreeAccessFeedbackType.WARNING) {
                _feedback.value = null
            }
        }
    }

    fun requestRewardedAd(activity: Activity, isOnline: Boolean) {
        val nowMs = clock()
        nowMsState.value = nowMs
        val currentStatus = freeAccessStatus.value

        if (!AdFreeAccessPolicy.shouldAllowRewardedAdRequest(isOnline, currentStatus.expiresAtMs, nowMs)) {
            _isLoadingRewardedAd.value = false
            return
        }
        if (_isLoadingRewardedAd.value) return

        val sessionId = sessionGuard.startNewSession()
        _feedback.value = null

        FazaAdsManager.loadAndShowRewardedAd(
            activity = activity,
            sessionId = sessionId,
            callbacks = object : RewardedAdCallbacks {
                override fun onLoadingChanged(isLoading: Boolean) {
                    _isLoadingRewardedAd.value = isLoading
                }

                override fun onRewardEarned(sessionId: String) {
                    handleVerifiedReward(sessionId)
                }

                override fun onDismissedWithoutReward(sessionId: String) {
                    _isLoadingRewardedAd.value = false
                    if (!sessionGuard.hasClaimedReward(sessionId)) {
                        sessionGuard.finishSession(sessionId)
                        _feedback.value = FreeAccessFeedback(
                            message = "Ad was closed early. Watch the full ad to count toward free access.",
                            type = FreeAccessFeedbackType.INFO
                        )
                    } else {
                        sessionGuard.finishSession(sessionId)
                    }
                }

                override fun onFailed(sessionId: String, reason: String) {
                    _isLoadingRewardedAd.value = false
                    sessionGuard.finishSession(sessionId)
                    if (FazaAdsManager.isInternetConnected(activity)) {
                        _feedback.value = FreeAccessFeedback(
                            message = reason,
                            type = FreeAccessFeedbackType.WARNING
                        )
                    }
                }

                override fun onOfflineBlocked(sessionId: String) {
                    _isLoadingRewardedAd.value = false
                    sessionGuard.finishSession(sessionId)
                    _feedback.value = null
                }
            }
        )
    }

    internal fun handleVerifiedReward(sessionId: String) {
        if (!sessionGuard.tryClaimReward(sessionId)) {
            return
        }
        viewModelScope.launch {
            val nowMs = clock()
            val result = preferencesRepository.recordCompletedRewardedAd(nowMs)
            nowMsState.value = nowMs
            sessionGuard.finishSession(sessionId)
            _isLoadingRewardedAd.value = false

            if (result.unlocked24HourAccess) {
                _feedback.value = FreeAccessFeedback(
                    message = FREE_ACCESS_CONGRATULATIONS_MESSAGE,
                    type = FreeAccessFeedbackType.SUCCESS
                )
            } else if (!result.wasAlreadyActive) {
                _feedback.value = FreeAccessFeedback(
                    message = "Reward counted! ${result.newWatchedCount}/$REQUIRED_REWARDED_ADS ads watched (${result.remainingAds} remaining).",
                    type = FreeAccessFeedbackType.INFO
                )
            }
        }
    }

    companion object {
        fun factory(
            preferencesRepository: UserPreferencesRepository,
            clock: () -> Long = System::currentTimeMillis
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AdFreeAccessViewModel(preferencesRepository, clock) as T
            }
        }
    }
}
