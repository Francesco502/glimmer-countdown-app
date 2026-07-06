package com.example.timeapk.ui.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.timeapk.TimeApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin

enum class SongSoundEffect(
    val durationMs: Int,
    val fundamentalHz: Double,
    val overtoneHz: Double,
    val volume: Float = 0.12f
) {
    Action(durationMs = 76, fundamentalHz = 740.0, overtoneHz = 1110.0, volume = 0.12f),
    Commit(durationMs = 132, fundamentalHz = 392.0, overtoneHz = 784.0, volume = 0.12f)
}

class SongSoundController internal constructor(
    private val enabled: Boolean
) {
    fun play(effect: SongSoundEffect) {
        SongSoundscape.play(effect, enabled)
    }
}

@Composable
fun rememberSongSoundscape(): SongSoundController {
    val context = LocalContext.current.applicationContext
    val app = context as? TimeApplication
    if (app == null) {
        return remember { SongSoundController(enabled = false) }
    }
    val enabled by app.userPrefs.songSoundEnabledFlow.collectAsState(initial = false)
    return remember(enabled) {
        SongSoundController(enabled)
    }
}

object SongSoundscape {
    private const val SampleRateHz = 22_050
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val pcmCache = ConcurrentHashMap<SongSoundEffect, ShortArray>()

    fun play(effect: SongSoundEffect, enabled: Boolean) {
        if (!enabled) return
        scope.launch {
            runCatching {
                playGeneratedEffect(effect)
            }
        }
    }

    private fun playGeneratedEffect(effect: SongSoundEffect) {
        val pcm = pcmCache.getOrPut(effect) { effect.buildPcm() }
        var track: AudioTrack? = null
        try {
            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SampleRateHz)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(pcm.size * Short.SIZE_BYTES)
                .build()
            track.write(pcm, 0, pcm.size)
            track.play()
            Thread.sleep(effect.durationMs.toLong() + 36L)
        } finally {
            track?.release()
        }
    }

    private fun SongSoundEffect.buildPcm(): ShortArray {
        val sampleCount = (SampleRateHz * durationMs / 1000.0).roundToInt().coerceAtLeast(1)
        return ShortArray(sampleCount) { index ->
            val t = index.toDouble() / SampleRateHz
            val progress = index.toDouble() / sampleCount
            val brushAttack = 1.0 - exp(-72.0 * t)
            val naturalDecay = exp(-5.8 * progress)
            val tone = sin(2.0 * PI * fundamentalHz * t) * 0.70 +
                sin(2.0 * PI * overtoneHz * t) * 0.22 +
                sin(2.0 * PI * fundamentalHz * 0.5 * t) * 0.08
            val value = (tone * brushAttack * naturalDecay * volume).coerceIn(-1.0, 1.0)
            (value * Short.MAX_VALUE).toInt().toShort()
        }
    }
}
