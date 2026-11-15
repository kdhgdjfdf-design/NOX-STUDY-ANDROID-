package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.service.FocusSessionCoordinator
import kotlinx.coroutines.flow.StateFlow

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

/**
 * Thin ViewModel facade over [FocusSessionCoordinator] so UI state stays synchronized across
 * Activity recreations, backgrounding, BlockedAppActivity roundtrips, and FocusTimerService.
 */
class FocusViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext

    val focusMode: StateFlow<FocusMode> = FocusSessionCoordinator.focusMode
    val focusState: StateFlow<FocusState> = FocusSessionCoordinator.focusState
    val customTargetMinutes: StateFlow<Int> = FocusSessionCoordinator.customTargetMinutes

    val pomodoroFocusMinutes = FocusSessionCoordinator.POMODORO_FOCUS_MINUTES
    val pomodoroShortBreakMinutes = FocusSessionCoordinator.POMODORO_SHORT_BREAK_MINUTES
    val pomodoroLongBreakMinutes = FocusSessionCoordinator.POMODORO_LONG_BREAK_MINUTES

    val remainingSeconds: StateFlow<Int> = FocusSessionCoordinator.remainingSeconds
    val elapsedSeconds: StateFlow<Long> = FocusSessionCoordinator.elapsedSeconds
    val breakRemainingSeconds: StateFlow<Int> = FocusSessionCoordinator.breakRemainingSeconds
    val sessionSummary: StateFlow<SessionSummaryData?> = FocusSessionCoordinator.sessionSummary

    init {
        FocusSessionCoordinator.ensurePrefsObserved(appContext)
    }

    fun setMode(mode: FocusMode) = FocusSessionCoordinator.setMode(mode)

    fun setCustomMinutes(minutes: Int) = FocusSessionCoordinator.setCustomMinutes(minutes)

    fun startSession() = FocusSessionCoordinator.startSession(appContext)

    fun pauseSession() = FocusSessionCoordinator.pauseSession(appContext)

    fun resumeSession() = FocusSessionCoordinator.resumeSession(appContext)

    fun stopSession(userTerminated: Boolean = true) =
        FocusSessionCoordinator.stopSession(appContext, userTerminated)

    fun startBreak(breakMinutes: Int = pomodoroShortBreakMinutes) =
        FocusSessionCoordinator.startBreak(appContext, breakMinutes)

    fun pauseBreak() = FocusSessionCoordinator.pauseBreak(appContext)

    fun resumeBreak() = FocusSessionCoordinator.resumeBreak(appContext)

    fun skipBreak() = FocusSessionCoordinator.skipBreak(appContext)

    fun dismissSummary() = FocusSessionCoordinator.dismissSummary()
}
