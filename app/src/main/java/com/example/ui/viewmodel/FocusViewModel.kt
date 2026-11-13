package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.os.SystemClock
import com.example.NoxStudyApp
import com.example.data.model.FocusSessionEntity
import com.example.service.AppBlockerAccessibilityService
import com.example.service.NotificationHelper
import com.example.service.TimerSoundHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar

enum class FocusMode {
    POMODORO,
    CUSTOM,
    FREE
}

enum class FocusState {
    IDLE,
    RUNNING,
    PAUSED,
    BREAK_RUNNING,
    BREAK_PAUSED
}

data class SessionSummaryData(
    val actualSeconds: Long,
    val targetMinutes: Int,
    val goalAchieved: Boolean,
    val completedSessionsToday: Int,
    val dailyProgressPct: Float,
    val weeklyProgressPct: Float
)

class FocusViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val db = (application as NoxStudyApp).database
    private val prefsRepo = (application as NoxStudyApp).preferencesRepository

    private val _focusMode = MutableStateFlow(FocusMode.POMODORO)
    val focusMode = _focusMode.asStateFlow()

    private val _focusState = MutableStateFlow(FocusState.IDLE)
    val focusState = _focusState.asStateFlow()

    private val _customTargetMinutes = MutableStateFlow(45)
    val customTargetMinutes = _customTargetMinutes.asStateFlow()

    // Pomodoro settings
    val pomodoroFocusMinutes = 25
    val pomodoroShortBreakMinutes = 5
    val pomodoroLongBreakMinutes = 15

    private val _remainingSeconds = MutableStateFlow(25 * 60)
    val remainingSeconds = _remainingSeconds.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0L) // Actual focused time in current session
    val elapsedSeconds = _elapsedSeconds.asStateFlow()

    private val _breakRemainingSeconds = MutableStateFlow(5 * 60)
    val breakRemainingSeconds = _breakRemainingSeconds.asStateFlow()

    private val _sessionSummary = MutableStateFlow<SessionSummaryData?>(null)
    val sessionSummary = _sessionSummary.asStateFlow()

    private val timerSoundHelper = TimerSoundHelper()
    private var timerSoundEnabled: Boolean = true

    private var timerJob: Job? = null

    init {
        // Observe preferences for blocked apps and timer tick sound
        viewModelScope.launch {
            prefsRepo.userPreferencesFlow.collect { prefs ->
                timerSoundEnabled = prefs.timerSoundEnabled
                if (prefs.appBlockerEnabled) {
                    AppBlockerAccessibilityService.blockedPackages = prefs.blockedPackages
                } else {
                    AppBlockerAccessibilityService.blockedPackages = emptySet()
                }
            }
        }
    }

    fun setMode(mode: FocusMode) {
        if (_focusState.value != FocusState.IDLE) return
        _focusMode.value = mode
        resetTimerToMode()
    }

    fun setCustomMinutes(minutes: Int) {
        if (_focusState.value != FocusState.IDLE) return
        _customTargetMinutes.value = minutes.coerceIn(5, 180)
        if (_focusMode.value == FocusMode.CUSTOM) {
            _remainingSeconds.value = _customTargetMinutes.value * 60
        }
    }

    private fun resetTimerToMode() {
        when (_focusMode.value) {
            FocusMode.POMODORO -> {
                _remainingSeconds.value = pomodoroFocusMinutes * 60
            }
            FocusMode.CUSTOM -> {
                _remainingSeconds.value = _customTargetMinutes.value * 60
            }
            FocusMode.FREE -> {
                _remainingSeconds.value = 0
            }
        }
        _elapsedSeconds.value = 0L
    }

    fun startSession() {
        if (_focusState.value == FocusState.IDLE) {
            resetTimerToMode()
        }
        _focusState.value = FocusState.RUNNING
        AppBlockerAccessibilityService.isFocusActive = true

        startTimerLoop()
    }

    fun pauseSession() {
        _focusState.value = FocusState.PAUSED
        AppBlockerAccessibilityService.isFocusActive = false
        timerJob?.cancel()
    }

    fun resumeSession() {
        _focusState.value = FocusState.RUNNING
        AppBlockerAccessibilityService.isFocusActive = true
        startTimerLoop()
    }

    fun stopSession(userTerminated: Boolean = true) {
        val actualSec = _elapsedSeconds.value
        val completed = !userTerminated || (_focusMode.value != FocusMode.FREE && _remainingSeconds.value <= 0)

        timerJob?.cancel()
        _focusState.value = FocusState.IDLE
        AppBlockerAccessibilityService.isFocusActive = false

        if (actualSec > 10) { // Only save if more than 10 seconds of actual focus
            saveSessionAndShowSummary(actualSec, completed)
        } else {
            resetTimerToMode()
        }
    }

    private fun startTimerLoop() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            val loopStartTime = SystemClock.elapsedRealtime()
            val initialElapsed = _elapsedSeconds.value
            val initialRemaining = _remainingSeconds.value
            var tickCount = 0L

            while (isActive && _focusState.value == FocusState.RUNNING) {
                tickCount++
                val nextTargetTime = loopStartTime + (tickCount * 1000L)
                val delayMs = (nextTargetTime - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
                delay(delayMs)

                _elapsedSeconds.value = initialElapsed + tickCount

                if (timerSoundEnabled) {
                    timerSoundHelper.playTick()
                }

                when (_focusMode.value) {
                    FocusMode.FREE -> {
                        _remainingSeconds.value = initialRemaining + tickCount.toInt()
                    }
                    FocusMode.POMODORO, FocusMode.CUSTOM -> {
                        val currentRem = initialRemaining - tickCount.toInt()
                        if (currentRem <= 0) {
                            _remainingSeconds.value = 0
                            // Focus period ended!
                            onFocusPeriodFinished()
                            break
                        } else {
                            _remainingSeconds.value = currentRem
                        }
                    }
                }
            }
        }
    }

    private fun onFocusPeriodFinished() {
        AppBlockerAccessibilityService.isFocusActive = false
        NotificationHelper.showNotification(
            context = context,
            id = 5001,
            title = "أحسنت! انتهت جلسة التركيز 🌟",
            message = "حان وقت الاستراحة الآن لتجديد طاقتك.",
            channelId = NotificationHelper.CHANNEL_FOCUS
        )

        // Save session
        val actualSec = _elapsedSeconds.value
        saveSessionToDb(actualSec, completed = true)

        // Automatically start break
        startBreak(pomodoroShortBreakMinutes)
    }

    fun startBreak(breakMinutes: Int = 5) {
        timerJob?.cancel()
        _focusState.value = FocusState.BREAK_RUNNING
        _breakRemainingSeconds.value = breakMinutes * 60
        AppBlockerAccessibilityService.isFocusActive = false

        timerJob = viewModelScope.launch {
            while (isActive && _focusState.value == FocusState.BREAK_RUNNING) {
                delay(1000)
                val cur = _breakRemainingSeconds.value - 1
                if (cur <= 0) {
                    _breakRemainingSeconds.value = 0
                    onBreakFinished()
                    break
                } else {
                    _breakRemainingSeconds.value = cur
                }
            }
        }
    }

    fun pauseBreak() {
        _focusState.value = FocusState.BREAK_PAUSED
        timerJob?.cancel()
    }

    fun resumeBreak() {
        _focusState.value = FocusState.BREAK_RUNNING
        startBreak(_breakRemainingSeconds.value / 60)
    }

    fun skipBreak() {
        timerJob?.cancel()
        _focusState.value = FocusState.IDLE
        resetTimerToMode()
    }

    private fun onBreakFinished() {
        _focusState.value = FocusState.IDLE
        NotificationHelper.showNotification(
            context = context,
            id = 5002,
            title = "انتهت الاستراحة ☕",
            message = "مستعد لجلسة جديدة؟ افتح NOX STUDY وابدأ بتركيز.",
            channelId = NotificationHelper.CHANNEL_FOCUS
        )
        resetTimerToMode()
    }

    private fun saveSessionToDb(actualSec: Long, completed: Boolean) {
        viewModelScope.launch {
            val targetMin = when (_focusMode.value) {
                FocusMode.POMODORO -> pomodoroFocusMinutes
                FocusMode.CUSTOM -> _customTargetMinutes.value
                FocusMode.FREE -> (actualSec / 60).toInt()
            }

            db.focusDao().insertSession(
                FocusSessionEntity(
                    mode = _focusMode.value.name,
                    targetMinutes = targetMin,
                    actualDurationSeconds = actualSec,
                    completed = completed
                )
            )

            // Update challenges only if session was successfully completed
            if (completed && actualSec >= 60) {
                updateChallengesOnSessionCompleted(actualSec)
            }
        }
    }

    private fun saveSessionAndShowSummary(actualSec: Long, completed: Boolean) {
        viewModelScope.launch {
            val targetMin = when (_focusMode.value) {
                FocusMode.POMODORO -> pomodoroFocusMinutes
                FocusMode.CUSTOM -> _customTargetMinutes.value
                FocusMode.FREE -> (actualSec / 60).toInt()
            }

            db.focusDao().insertSession(
                FocusSessionEntity(
                    mode = _focusMode.value.name,
                    targetMinutes = targetMin,
                    actualDurationSeconds = actualSec,
                    completed = completed
                )
            )

            // Update challenges only if session was successfully completed (never if cancelled/incomplete)
            if (completed && actualSec >= 60) {
                updateChallengesOnSessionCompleted(actualSec)
            }

            // Compute daily & weekly progress
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            val startOfDay = cal.timeInMillis
            val todayTotalSec = db.focusDao().getTotalFocusSecondsSince(startOfDay) ?: actualSec

            val calWeek = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
            }
            val startOfWeek = calWeek.timeInMillis
            val weekTotalSec = db.focusDao().getTotalFocusSecondsSince(startOfWeek) ?: actualSec

            // Daily goal: 100 mins default, Weekly goal: 12 hrs default
            val dailyProgress = (todayTotalSec.toFloat() / (100 * 60f)).coerceIn(0f, 1f)
            val weeklyProgress = (weekTotalSec.toFloat() / (12 * 3600f)).coerceIn(0f, 1f)

            _sessionSummary.value = SessionSummaryData(
                actualSeconds = actualSec,
                targetMinutes = targetMin,
                goalAchieved = completed || actualSec >= (targetMin * 60L),
                completedSessionsToday = 1,
                dailyProgressPct = dailyProgress,
                weeklyProgressPct = weeklyProgress
            )
        }
    }

    private suspend fun updateChallengesOnSessionCompleted(actualDurationSeconds: Long) {
        try {
            val activeChallenges = db.challengeDao().getActiveChallenges()
            val minutesFocused = (actualDurationSeconds / 60).toInt()

            for (challenge in activeChallenges) {
                var shouldUpdate = false
                var newCurrent = challenge.currentValue

                if (challenge.targetType == "FOCUS_MINUTES" && minutesFocused > 0) {
                    newCurrent = (challenge.currentValue + minutesFocused).coerceAtMost(challenge.targetValue)
                    shouldUpdate = true
                } else if (challenge.targetType == "FOCUS_SESSIONS") {
                    newCurrent = (challenge.currentValue + 1).coerceAtMost(challenge.targetValue)
                    shouldUpdate = true
                }

                if (shouldUpdate) {
                    val isNowCompleted = newCurrent >= challenge.targetValue
                    db.challengeDao().updateChallenge(
                        challenge.copy(
                            currentValue = newCurrent,
                            isCompleted = isNowCompleted,
                            completedAt = if (isNowCompleted) System.currentTimeMillis() else null
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // Fail gracefully
        }
    }

    fun dismissSummary() {
        _sessionSummary.value = null
        resetTimerToMode()
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        timerSoundHelper.release()
    }
}
