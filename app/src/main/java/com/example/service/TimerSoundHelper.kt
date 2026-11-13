package com.example.service

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import kotlin.math.exp
import kotlin.math.sin

class TimerSoundHelper {

    private val sampleRate = 22050
    private val audioData: ShortArray
    private var audioTrack: AudioTrack? = null

    init {
        // 14ms subtle wooden clock tick
        val durationMs = 14
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        audioData = ShortArray(numSamples)

        val freq = 550.0 // Warm woody frequency
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            // Exponential rapid decay for soft subtle click
            val envelope = exp(-i.toDouble() / (numSamples * 0.25))
            val wave = sin(2.0 * Math.PI * freq * t)
            // Low volume (cozy, quiet tap)
            val sample = (wave * envelope * 0.18 * Short.MAX_VALUE).toInt().toShort()
            audioData[i] = sample
        }

        try {
            audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(audioData.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                AudioTrack(
                    AudioManager.STREAM_SYSTEM,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    audioData.size * 2,
                    AudioTrack.MODE_STATIC
                )
            }
            audioTrack?.write(audioData, 0, audioData.size)
        } catch (_: Exception) {
            audioTrack = null
        }
    }

    fun playTick() {
        try {
            audioTrack?.let { track ->
                if (track.state == AudioTrack.STATE_INITIALIZED) {
                    track.stop()
                    track.reloadStaticData()
                    track.play()
                }
            }
        } catch (_: Exception) {
            // Fail safely without interrupting timer
        }
    }

    fun release() {
        try {
            audioTrack?.release()
            audioTrack = null
        } catch (_: Exception) {}
    }
}
