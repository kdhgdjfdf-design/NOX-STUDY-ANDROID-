package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Random
import kotlin.math.PI
import kotlin.math.sin

data class StudyTrack(
    val id: String,
    val title: String,
    val subtitle: String,
    val coverRes: Int = R.drawable.cozy_vinyl_cover,
    val uriString: String? = null,
    val isBuiltIn: Boolean = true
)

class StudyAudioService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())

    private var mediaPlayer: MediaPlayer? = null
    private var audioTrack: AudioTrack? = null
    private var generatorJob: Job? = null
    private var progressJob: Job? = null

    inner class LocalBinder : Binder() {
        fun getService(): StudyAudioService = this@StudyAudioService
    }

    companion object {
        const val ACTION_PLAY = "com.aistudio.noxstudy.ACTION_PLAY"
        const val ACTION_PAUSE = "com.aistudio.noxstudy.ACTION_PAUSE"
        const val ACTION_NEXT = "com.aistudio.noxstudy.ACTION_NEXT"
        const val ACTION_PREV = "com.aistudio.noxstudy.ACTION_PREV"
        const val NOTIFICATION_ID = 404

        val defaultTracks = listOf(
            StudyTrack(
                id = "builtin_rain",
                title = "مطر هادئ وموقد دافئ",
                subtitle = "أصوات الطبيعة الهادئة"
            ),
            StudyTrack(
                id = "builtin_lofi",
                title = "نغمات لوفاي للدراسة والتركيز",
                subtitle = "Cozy Lo-Fi Study Chords"
            ),
            StudyTrack(
                id = "builtin_cafe",
                title = "أجواء مقهى الكتب الدافئ",
                subtitle = "Book Cafe Ambience"
            ),
            StudyTrack(
                id = "builtin_alpha",
                title = "موجات ألفا للتركيز العميق",
                subtitle = "Deep Alpha 10Hz Waves"
            )
        )

        private val _tracks = MutableStateFlow(defaultTracks)
        val tracks = _tracks.asStateFlow()

        private val _currentTrackIndex = MutableStateFlow(0)
        val currentTrackIndex = _currentTrackIndex.asStateFlow()

        private val _isPlaying = MutableStateFlow(false)
        val isPlaying = _isPlaying.asStateFlow()

        private val _currentPositionSec = MutableStateFlow(0)
        val currentPositionSec = _currentPositionSec.asStateFlow()

        private val _durationSec = MutableStateFlow(180) // 3 mins display per cycle
        val durationSec = _durationSec.asStateFlow()

        fun addTrack(track: StudyTrack) {
            _tracks.value = _tracks.value + track
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
        startProgressCounter()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> resumePlayback()
            ACTION_PAUSE -> pausePlayback()
            ACTION_NEXT -> nextTrack()
            ACTION_PREV -> prevTrack()
        }
        return START_STICKY
    }

    fun playTrack(index: Int) {
        val list = _tracks.value
        if (index !in list.indices) return
        _currentTrackIndex.value = index
        _currentPositionSec.value = 0
        startCurrentTrack()
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pausePlayback()
        } else {
            resumePlayback()
        }
    }

    fun resumePlayback() {
        startCurrentTrack()
    }

    fun pausePlayback() {
        _isPlaying.value = false
        stopCurrentAudioEngines()
        updateNotification()
    }

    fun nextTrack() {
        val nextIdx = (_currentTrackIndex.value + 1) % _tracks.value.size
        playTrack(nextIdx)
    }

    fun prevTrack() {
        val prevIdx = if (_currentTrackIndex.value - 1 < 0) _tracks.value.size - 1 else _currentTrackIndex.value - 1
        playTrack(prevIdx)
    }

    fun seekTo(seconds: Int) {
        _currentPositionSec.value = seconds
        mediaPlayer?.seekTo(seconds * 1000)
    }

    private fun startCurrentTrack() {
        stopCurrentAudioEngines()
        val current = _tracks.value.getOrNull(_currentTrackIndex.value) ?: return

        _isPlaying.value = true

        if (current.isBuiltIn || current.uriString == null) {
            startProceduralAtmosphere(current.id)
        } else {
            startMediaUri(current.uriString)
        }

        startForeground(NOTIFICATION_ID, buildNotification())
    }

    private fun startMediaUri(uriString: String) {
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@StudyAudioService, Uri.parse(uriString))
                isLooping = true
                prepareAsync()
                setOnPreparedListener { mp ->
                    mp.start()
                    _durationSec.value = (mp.duration / 1000).coerceAtLeast(30)
                }
                setOnCompletionListener {
                    nextTrack()
                }
            }
        } catch (_: Exception) {
            // Fallback to procedural
            startProceduralAtmosphere("builtin_lofi")
        }
    }

    private fun startProceduralAtmosphere(trackId: String) {
        _durationSec.value = 300 // 5 minutes loop cycle
        val sampleRate = 22050
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack = track
            track.play()

            generatorJob = serviceScope.launch {
                val buffer = ShortArray(bufferSize / 2)
                val random = Random()
                var phase1 = 0.0
                var phase2 = 0.0
                var lastNoise = 0.0

                // Ambient synthesis loops
                while (isActive && _isPlaying.value) {
                    when (trackId) {
                        "builtin_rain" -> {
                            // Pink noise with soft random crackles
                            for (i in buffer.indices) {
                                val white = (random.nextDouble() * 2.0 - 1.0)
                                lastNoise = (lastNoise * 0.95) + (white * 0.05)
                                val crackle = if (random.nextInt(800) == 0) (random.nextDouble() * 0.6) else 0.0
                                val sample = ((lastNoise * 0.25 + crackle) * 32767.0).toInt().coerceIn(-32768, 32767)
                                buffer[i] = sample.toShort()
                            }
                        }
                        "builtin_lofi" -> {
                            // Warm gentle chord tones (C major 7th / F maj 7th)
                            val f1 = 261.63 // C4
                            val f2 = 329.63 // E4
                            val f3 = 392.00 // G4
                            val step1 = 2.0 * PI * f1 / sampleRate
                            val step2 = 2.0 * PI * f2 / sampleRate
                            for (i in buffer.indices) {
                                phase1 += step1
                                phase2 += step2
                                val tone = (sin(phase1) * 0.12) + (sin(phase2) * 0.08)
                                val vinylHiss = (random.nextDouble() * 2.0 - 1.0) * 0.015
                                val sample = ((tone + vinylHiss) * 32767.0).toInt().coerceIn(-32768, 32767)
                                buffer[i] = sample.toShort()
                            }
                        }
                        "builtin_alpha" -> {
                            // 10Hz Alpha beat over 200Hz carrier
                            val fCarrier = 200.0
                            val fAlpha = 10.0
                            val stepC = 2.0 * PI * fCarrier / sampleRate
                            val stepA = 2.0 * PI * fAlpha / sampleRate
                            for (i in buffer.indices) {
                                phase1 += stepC
                                phase2 += stepA
                                val mod = 0.5 * (1.0 + sin(phase2))
                                val tone = sin(phase1) * mod * 0.18
                                val sample = (tone * 32767.0).toInt().coerceIn(-32768, 32767)
                                buffer[i] = sample.toShort()
                            }
                        }
                        else -> {
                            // Cafe murmur: low-pass pink noise with warm drone
                            for (i in buffer.indices) {
                                val white = (random.nextDouble() * 2.0 - 1.0)
                                lastNoise = (lastNoise * 0.98) + (white * 0.02)
                                val sample = (lastNoise * 0.22 * 32767.0).toInt().coerceIn(-32768, 32767)
                                buffer[i] = sample.toShort()
                            }
                        }
                    }
                    track.write(buffer, 0, buffer.size)
                }
            }
        } catch (_: Exception) {
            // Ignore audio generation failures gracefully
        }
    }

    private fun startProgressCounter() {
        progressJob?.cancel()
        progressJob = serviceScope.launch {
            while (isActive) {
                if (_isPlaying.value) {
                    val currentPos = _currentPositionSec.value + 1
                    val dur = _durationSec.value
                    if (currentPos >= dur) {
                        _currentPositionSec.value = 0
                    } else {
                        _currentPositionSec.value = currentPos
                    }
                }
                delay(1000)
            }
        }
    }

    private fun stopCurrentAudioEngines() {
        generatorJob?.cancel()
        generatorJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null

        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
    }

    private fun buildNotification(): Notification {
        val current = _tracks.value.getOrNull(_currentTrackIndex.value)
        val title = current?.title ?: "NOX STUDY 📚"
        val text = current?.subtitle ?: "تشغيل الموسيقى الهادئة للدراسة"

        val openIntent = Intent(this, MainActivity::class.java)
        val pendingOpen = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val prevIntent = Intent(this, StudyAudioService::class.java).apply { action = ACTION_PREV }
        val pPrev = PendingIntent.getService(this, 1, prevIntent, PendingIntent.FLAG_IMMUTABLE)

        val playPauseIntent = Intent(this, StudyAudioService::class.java).apply {
            action = if (_isPlaying.value) ACTION_PAUSE else ACTION_PLAY
        }
        val pPlayPause = PendingIntent.getService(this, 2, playPauseIntent, PendingIntent.FLAG_IMMUTABLE)

        val nextIntent = Intent(this, StudyAudioService::class.java).apply { action = ACTION_NEXT }
        val pNext = PendingIntent.getService(this, 3, nextIntent, PendingIntent.FLAG_IMMUTABLE)

        val playPauseIcon = if (_isPlaying.value) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play

        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_AUDIO)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingOpen)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.ic_media_previous, "السابق", pPrev)
            .addAction(playPauseIcon, if (_isPlaying.value) "إيقاف" else "تشغيل", pPlayPause)
            .addAction(android.R.drawable.ic_media_next, "التالي", pNext)
            .setOngoing(_isPlaying.value)
            .build()
    }

    private fun updateNotification() {
        val notification = buildNotification()
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
        nm?.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCurrentAudioEngines()
        progressJob?.cancel()
    }
}
