package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import com.example.NoxStudyApp
import com.example.data.badges.BadgeEvaluator
import com.example.data.model.FocusSessionEntity
import com.example.ui.viewmodel.FocusMode
import com.example.ui.viewmodel.FocusState
import com.example.ui.viewmodel.SessionSummaryData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Single source of truth for active Focus / Break session state across MainActivity,
 * FocusViewModel, FocusTimerService, and AppBlockerAccessibilityService.
 * Survives Activity recreation, navigation, and returning from BlockedAppActivity.
 */
object FocusSessionCoordinator {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    const val POMODORO_FOCUS_MINUTES = 25
    const val POMODORO_SHORT_BREAK_MINUTES = 5
    const val POMODORO_LONG_BREAK_MINUTES = 15

    private val _focusMode = MutableStateFlow(FocusMode.POMODORO)
    val focusMode: StateFlow<FocusMode> = _focusMode.asStateFlow()

    private val _focusState = MutableStateFlow(FocusState.IDLE)
    val focusState: StateFlow<FocusState> = _focusState.asStateFlow()

    private val _customTargetMinutes = MutableStateFlow(45)
    val customTargetMinutes: StateFlow<Int> = _customTargetMinutes.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(POMODORO_FOCUS_MINUTES * 60)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private val _breakRemainingSeconds = MutableStateFlow(POMODORO_SHORT_BREAK_MINUTES * 60)
    val breakRemainingSeconds: StateFlow<Int> = _breakRemainingSeconds.asStateFlow()

    private val _sessionSummary = MutableStateFlow<SessionSummaryData?>(null)
    val sessionSummary: StateFlow<SessionSummaryData?> = _sessionSummary.asStateFlow()

    private val timerSoundHelper = TimerSoundHelper()
    @Volatile
    private var timerSoundEnabled: Boolean = true

    private var timerJob: Job? = null
    private var prefsObserverStarted = false

    fun ensurePrefsObserved(context: Context) {
        if (prefsObserverStarted) return
        prefsObserverStarted = true
        val appContext = context.applicationContext
        val prefsRepo = (appContext as? NoxStudyApp)?.preferencesRepository ?: return
        scope.launch {
            prefsRepo.userPreferencesFlow.collect { prefs ->
                timerSoundEnabled = prefs.timerSoundEnabled
                BlockerStateStore.syncBlockerConfig(
                    appContext,
                    prefs.appBlockerEnabled,
                    prefs.blockedPackages
                )
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
            FocusMode.POMODORO -> _remainingSeconds.value = POMODORO_FOCUS_MINUTES * 60
            FocusMode.CUSTOM -> _remainingSeconds.value = _customTargetMinutes.value * 60
            FocusMode.FREE -> _remainingSeconds.value = 0
        }
        _elapsedSeconds.value = 0L
    }

    fun startSession(context: Context) {
        val appContext = context.applicationContext
        ensurePrefsObserved(appContext)
        timerJob?.cancel()

        if (_focusState.value == FocusState.IDLE ||
            _focusState.value == FocusState.BREAK_RUNNING ||
            _focusState.value == FocusState.BREAK_PAUSED
        ) {
            resetTimerToMode()
        }

        _focusState.value = FocusState.RUNNING
        val expectedEndMs = computeExpectedEndWallClockMs()
        BlockerStateStore.setFocusSessionActive(
            context = appContext,
            mode = _focusMode.value.name,
            phase = "FOCUS_RUNNING",
            expectedEndWallClockMs = expectedEndMs
        )
        startOrUpdateForegroundService(appContext, FocusTimerService.ACTION_START_FOCUS)
        startFocusTimerLoop(appContext)
    }

    fun pauseSession(context: Context) {
        val appContext = context.applicationContext
        timerJob?.cancel()
        _focusState.value = FocusState.PAUSED
        BlockerStateStore.setFocusSessionInactive(appContext, phase = "FOCUS_PAUSED")
        startOrUpdateForegroundService(appContext, FocusTimerService.ACTION_UPDATE_NOTIFICATION)
    }

    fun resumeSession(context: Context) {
        val appContext = context.applicationContext
        ensurePrefsObserved(appContext)
        timerJob?.cancel()
        _focusState.value = FocusState.RUNNING
        val expectedEndMs = computeExpectedEndWallClockMs()
        BlockerStateStore.setFocusSessionActive(
            context = appContext,
            mode = _focusMode.value.name,
            phase = "FOCUS_RUNNING",
            expectedEndWallClockMs = expectedEndMs
        )
        startOrUpdateForegroundService(appContext, FocusTimerService.ACTION_START_FOCUS)
        startFocusTimerLoop(appContext)
    }

    fun stopSession(context: Context, userTerminated: Boolean = true) {
        val appContext = context.applicationContext
        val actualSec = _elapsedSeconds.value
        val completed = !userTerminated || (_focusMode.value != FocusMode.FREE && _remainingSeconds.value <= 0)

        timerJob?.cancel()
        _focusState.value = FocusState.IDLE
        BlockerStateStore.setFocusSessionInactive(appContext, phase = "IDLE")
        stopForegroundService(appContext)

        if (actualSec > 10) {
            saveSessionAndShowSummary(appContext, actualSec, completed)
        } else {
            resetTimerToMode()
        }
    }

    fun startBreak(context: Context, breakMinutes: Int = POMODORO_SHORT_BREAK_MINUTES) {
        val appContext = context.applicationContext
        ensurePrefsObserved(appContext)
        timerJob?.cancel()

        val safeMinutes = breakMinutes.coerceAtLeast(1)
        _focusState.value = FocusState.BREAK_RUNNING
        _breakRemainingSeconds.value = safeMinutes * 60
        BlockerStateStore.setFocusSessionInactive(appContext, phase = "BREAK_RUNNING")
        startOrUpdateForegroundService(appContext, FocusTimerService.ACTION_START_BREAK)
        startBreakTimerLoop(appContext)
    }

    fun pauseBreak(context: Context) {
        val appContext = context.applicationContext
        timerJob?.cancel()
        _focusState.value = FocusState.BREAK_PAUSED
        BlockerStateStore.setFocusSessionInactive(appContext, phase = "BREAK_PAUSED")
        startOrUpdateForegroundService(appContext, FocusTimerService.ACTION_UPDATE_NOTIFICATION)
    }

    fun resumeBreak(context: Context) {
        val appContext = context.applicationContext
        if (_breakRemainingSeconds.value <= 0) {
            onBreakFinished(appContext)
            return
        }
        timerJob?.cancel()
        _focusState.value = FocusState.BREAK_RUNNING
        BlockerStateStore.setFocusSessionInactive(appContext, phase = "BREAK_RUNNING")
        startOrUpdateForegroundService(appContext, FocusTimerService.ACTION_START_BREAK)
        startBreakTimerLoop(appContext)
    }

    fun skipBreak(context: Context) {
        val appContext = context.applicationContext
        timerJob?.cancel()
        _focusState.value = FocusState.IDLE
        BlockerStateStore.setFocusSessionInactive(appContext, phase = "IDLE")
        stopForegroundService(appContext)
        resetTimerToMode()
    }

    fun dismissSummary() {
        _sessionSummary.value = null
        resetTimerToMode()
    }

    fun handleAppTaskRemoved(context: Context) {
        val appContext = context.applicationContext
        timerJob?.cancel()
        _focusState.value = FocusState.IDLE
        BlockerStateStore.setFocusSessionInactive(appContext, phase = "IDLE")
        resetTimerToMode()
    }

    private fun computeExpectedEndWallClockMs(): Long {
        return when (_focusMode.value) {
            FocusMode.POMODORO, FocusMode.CUSTOM -> {
                System.currentTimeMillis() + (_remainingSeconds.value.coerceAtLeast(0) * 1000L)
            }
            FocusMode.FREE -> 0L
        }
    }

    private fun startFocusTimerLoop(appContext: Context) {
        timerJob?.cancel()
        timerJob = scope.launch {
            val loopStartTime = SystemClock.elapsedRealtime()
            val initialElapsed = _elapsedSeconds.value
            val initialRemaining = _remainingSeconds.value
            var tickCount = 0L

            while (isActive && _focusState.value == FocusState.RUNNING) {
                tickCount++
                val nextTargetTime = loopStartTime + (tickCount * 1000L)
                val delayMs = (nextTargetTime - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
                delay(delayMs)

                if (_focusState.value != FocusState.RUNNING) break

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
                            onFocusPeriodFinished(appContext)
                            break
                        } else {
                            _remainingSeconds.value = currentRem
                        }
                    }
                }

                // Periodic heartbeat & ongoing notification update every 5 seconds
                if (tickCount % 5L == 0L) {
                    BlockerStateStore.updateHeartbeat(
                        appContext,
                        computeExpectedEndWallClockMs()
                    )
                    FocusTimerService.refreshOngoingNotification(appContext)
                }
            }
        }
    }

    private fun startBreakTimerLoop(appContext: Context) {
        timerJob?.cancel()
        timerJob = scope.launch {
            val loopStartTime = SystemClock.elapsedRealtime()
            val initialBreakRemaining = _breakRemainingSeconds.value
            var tickCount = 0L

            while (isActive && _focusState.value == FocusState.BREAK_RUNNING) {
                tickCount++
                val nextTargetTime = loopStartTime + (tickCount * 1000L)
                val delayMs = (nextTargetTime - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
                delay(delayMs)

                if (_focusState.value != FocusState.BREAK_RUNNING) break

                val cur = initialBreakRemaining - tickCount.toInt()
                if (cur <= 0) {
                    _breakRemainingSeconds.value = 0
                    onBreakFinished(appContext)
                    break
                } else {
                    _breakRemainingSeconds.value = cur
                    if (tickCount % 5L == 0L) {
                        FocusTimerService.refreshOngoingNotification(appContext)
                    }
                }
            }
        }
    }

    private fun onFocusPeriodFinished(appContext: Context) {
        BlockerStateStore.setFocusSessionInactive(appContext, phase = "BREAK_RUNNING")
        NotificationHelper.showNotification(
            context = appContext,
            id = 5001,
            title = "أحسنت! انتهت جلسة التركيز 🌟",
            message = "حان وقت الاستراحة الآن لتجديد طاقتك.",
            channelId = NotificationHelper.CHANNEL_FOCUS
        )

        val actualSec = _elapsedSeconds.value
        saveCompletedSessionOnly(appContext, actualSec)
        startBreak(appContext, POMODORO_SHORT_BREAK_MINUTES)
    }

    private fun onBreakFinished(appContext: Context) {
        _focusState.value = FocusState.IDLE
        BlockerStateStore.setFocusSessionInactive(appContext, phase = "IDLE")
        stopForegroundService(appContext)

        scope.launch(Dispatchers.IO) {
            try {
                val app = appContext as? NoxStudyApp ?: return@launch
                app.preferencesRepository.incrementFocusBreakCycles()
                BadgeEvaluator.evaluateAll(appContext)
            } catch (_: Exception) {
            }
        }

        NotificationHelper.showNotification(
            context = appContext,
            id = 5002,
            title = "انتهت الاستراحة ☕",
            message = "مستعد لجلسة جديدة؟ افتح NOX STUDY وابدأ بتركيز.",
            channelId = NotificationHelper.CHANNEL_FOCUS
        )
        resetTimerToMode()
    }

    private fun saveCompletedSessionOnly(appContext: Context, actualSec: Long) {
        scope.launch(Dispatchers.IO) {
            try {
                val app = appContext as? NoxStudyApp ?: return@launch
                val db = app.database
                val targetMin = when (_focusMode.value) {
                    FocusMode.POMODORO -> POMODORO_FOCUS_MINUTES
                    FocusMode.CUSTOM -> _customTargetMinutes.value
                    FocusMode.FREE -> (actualSec / 60).toInt()
                }

                db.focusDao().insertSession(
                    FocusSessionEntity(
                        mode = _focusMode.value.name,
                        targetMinutes = targetMin,
                        actualDurationSeconds = actualSec,
                        completed = true
                    )
                )

                if (actualSec >= 60) {
                    updateChallengesOnSessionCompleted(app, actualSec)
                    BadgeEvaluator.evaluateAll(appContext)
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun saveSessionAndShowSummary(appContext: Context, actualSec: Long, completed: Boolean) {
        scope.launch(Dispatchers.IO) {
            try {
                val app = appContext as? NoxStudyApp ?: return@launch
                val db = app.database
                val targetMin = when (_focusMode.value) {
                    FocusMode.POMODORO -> POMODORO_FOCUS_MINUTES
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

                if (completed && actualSec >= 60) {
                    updateChallengesOnSessionCompleted(app, actualSec)
                    BadgeEvaluator.evaluateAll(appContext)
                }

                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }
                val todayTotalSec = db.focusDao().getTotalFocusSecondsSince(cal.timeInMillis) ?: actualSec

                val calWeek = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                }
                val weekTotalSec = db.focusDao().getTotalFocusSecondsSince(calWeek.timeInMillis) ?: actualSec

                val prefs = app.preferencesRepository.userPreferencesFlow.first()
                val dailyTargetSec = (prefs.dailyGoalMinutes.coerceAtLeast(10)) * 60f
                val weeklyTargetSec = (prefs.weeklyGoalHours.coerceAtLeast(1)) * 3600f

                val dailyProgress = (todayTotalSec.toFloat() / dailyTargetSec).coerceIn(0f, 1f)
                val weeklyProgress = (weekTotalSec.toFloat() / weeklyTargetSec).coerceIn(0f, 1f)

                _sessionSummary.value = SessionSummaryData(
                    actualSeconds = actualSec,
                    targetMinutes = targetMin,
                    goalAchieved = completed || actualSec >= (targetMin * 60L),
                    completedSessionsToday = 1,
                    dailyProgressPct = dailyProgress,
                    weeklyProgressPct = weeklyProgress
                )
            } catch (_: Exception) {
            }
        }
    }

    private suspend fun updateChallengesOnSessionCompleted(app: NoxStudyApp, actualDurationSeconds: Long) {
        try {
            val activeChallenges = app.database.challengeDao().getActiveChallenges()
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
                    app.database.challengeDao().updateChallenge(
                        challenge.copy(
                            currentValue = newCurrent,
                            isCompleted = isNowCompleted,
                            completedAt = if (isNowCompleted) System.currentTimeMillis() else null
                        )
                    )
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun startOrUpdateForegroundService(appContext: Context, action: String) {
        try {
            val intent = Intent(appContext, FocusTimerService::class.java).apply {
                this.action = action
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appContext.startForegroundService(intent)
            } else {
                appContext.startService(intent)
            }
        } catch (_: Exception) {
            // In headless Robolectric or restricted contexts, coordinator timer continues cleanly
        }
    }

    private fun stopForegroundService(appContext: Context) {
        try {
            val intent = Intent(appContext, FocusTimerService::class.java).apply {
                action = FocusTimerService.ACTION_STOP_SERVICE
            }
            appContext.startService(intent)
        } catch (_: Exception) {
        }
    }
}
