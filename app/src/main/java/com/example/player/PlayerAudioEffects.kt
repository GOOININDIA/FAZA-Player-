package com.example.player

import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FzEqualizerPreset(
    val id: String,
    val label: String,
    val bandOffsetsMb: List<Int>
)

val FZ_EQUALIZER_PRESETS = listOf(
    FzEqualizerPreset("flat", "Flat", listOf(0, 0, 0, 0, 0)),
    FzEqualizerPreset("cinema", "Cinema", listOf(350, 150, -50, 200, 300)),
    FzEqualizerPreset("bass", "Bass Boost", listOf(650, 420, 100, 0, 50)),
    FzEqualizerPreset("vocal", "Clear Voice", listOf(-250, -50, 450, 500, 200)),
    FzEqualizerPreset("night", "Late Night", listOf(-200, 100, 350, 250, -150))
)

class PlayerAudioEffectsController {

    private var equalizer: Equalizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var attachedAudioSessionId: Int = 0

    private val _audioEffectEnabled = MutableStateFlow(true)
    val audioEffectEnabled: StateFlow<Boolean> = _audioEffectEnabled.asStateFlow()

    private val _equalizerEnabled = MutableStateFlow(false)
    val equalizerEnabled: StateFlow<Boolean> = _equalizerEnabled.asStateFlow()

    private val _selectedPresetIndex = MutableStateFlow(0)
    val selectedPresetIndex: StateFlow<Int> = _selectedPresetIndex.asStateFlow()

    private val _bandLevelsMb = MutableStateFlow(listOf(0, 0, 0, 0, 0))
    val bandLevelsMb: StateFlow<List<Int>> = _bandLevelsMb.asStateFlow()

    fun attachToAudioSession(audioSessionId: Int) {
        if (audioSessionId <= 0 || audioSessionId == attachedAudioSessionId) return
        release()
        attachedAudioSessionId = audioSessionId
        runCatching {
            loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                setTargetGain(350)
                enabled = _audioEffectEnabled.value
            }
        }
        runCatching {
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = _equalizerEnabled.value
            }
            applyBandsToHardware(_bandLevelsMb.value)
        }
    }

    fun setAudioEffectEnabled(enabled: Boolean) {
        _audioEffectEnabled.value = enabled
        runCatching {
            loudnessEnhancer?.apply {
                setTargetGain(if (enabled) 350 else 0)
                this.enabled = enabled
            }
        }
    }

    fun toggleAudioEffect(): Boolean {
        val next = !_audioEffectEnabled.value
        setAudioEffectEnabled(next)
        return next
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        _equalizerEnabled.value = enabled
        runCatching {
            equalizer?.enabled = enabled
        }
    }

    fun selectPreset(index: Int) {
        val safeIndex = index.coerceIn(0, FZ_EQUALIZER_PRESETS.lastIndex)
        _selectedPresetIndex.value = safeIndex
        val levels = FZ_EQUALIZER_PRESETS[safeIndex].bandOffsetsMb
        _bandLevelsMb.value = levels
        val shouldEnable = safeIndex != 0
        _equalizerEnabled.value = shouldEnable
        runCatching {
            equalizer?.enabled = shouldEnable
            applyBandsToHardware(levels)
        }
    }

    fun setBandLevel(bandIndex: Int, levelMb: Int) {
        val current = _bandLevelsMb.value.toMutableList()
        if (bandIndex !in current.indices) return
        current[bandIndex] = levelMb.coerceIn(-1200, 1200)
        _bandLevelsMb.value = current
        _equalizerEnabled.value = true
        runCatching {
            equalizer?.enabled = true
            applyBandsToHardware(current)
        }
    }

    private fun applyBandsToHardware(levelsMb: List<Int>) {
        val eq = equalizer ?: return
        val numBands = eq.numberOfBands.toInt().coerceAtMost(levelsMb.size)
        val range = eq.bandLevelRange
        val minMb = range.getOrNull(0)?.toInt() ?: -1200
        val maxMb = range.getOrNull(1)?.toInt() ?: 1200
        for (i in 0 until numBands) {
            val clamped = levelsMb[i].coerceIn(minMb, maxMb).toShort()
            eq.setBandLevel(i.toShort(), clamped)
        }
    }

    fun release() {
        runCatching { loudnessEnhancer?.release() }
        runCatching { equalizer?.release() }
        loudnessEnhancer = null
        equalizer = null
        attachedAudioSessionId = 0
    }
}
