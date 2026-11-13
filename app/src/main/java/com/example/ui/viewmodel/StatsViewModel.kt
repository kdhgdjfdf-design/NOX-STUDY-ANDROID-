package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.NoxStudyApp
import com.example.data.model.FocusSessionEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DayFocusData(
    val dayLabel: String,
    val focusMinutes: Int
)

data class StatsData(
    val totalSessions: Int = 0,
    val completedSessions: Int = 0,
    val incompleteSessions: Int = 0,
    val totalFocusSeconds: Long = 0L,
    val avgSessionDurationMinutes: Int = 0,
    val bestStudyDay: String = "—",
    val dailyProgressPct: Float = 0f,
    val weeklyProgressPct: Float = 0f,
    val last7DaysData: List<DayFocusData> = emptyList()
)

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as NoxStudyApp).database
    private val prefsRepo = (application as NoxStudyApp).preferencesRepository

    private val _statsState = MutableStateFlow(StatsData())
    val statsState = _statsState.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            db.focusDao().getAllSessions().collect { sessions ->
                computeStats(sessions)
            }
        }
    }

    private fun computeStats(sessions: List<FocusSessionEntity>) {
        if (sessions.isEmpty()) {
            _statsState.value = StatsData()
            return
        }

        val completed = sessions.filter { it.completed }
        val incomplete = sessions.filter { !it.completed }
        val totalActualSeconds = sessions.sumOf { it.actualDurationSeconds }
        val avgDurationMin = if (sessions.isNotEmpty()) {
            (totalActualSeconds / sessions.size / 60).toInt()
        } else 0

        // Find best study day (by day of week name)
        val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
        val dayTotals = mutableMapOf<String, Long>()
        sessions.forEach { s ->
            val dayName = dayFormat.format(Date(s.dateTimestamp))
            dayTotals[dayName] = (dayTotals[dayName] ?: 0L) + s.actualDurationSeconds
        }
        val bestDay = dayTotals.maxByOrNull { it.value }?.key ?: "—"

        // Last 7 days chart data
        val cal = Calendar.getInstance()
        val shortDayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val last7Days = mutableListOf<DayFocusData>()

        for (i in 6 downTo 0) {
            val targetCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startMs = targetCal.timeInMillis
            val endMs = startMs + 86400000L
            val daySec = sessions
                .filter { it.dateTimestamp in startMs until endMs }
                .sumOf { it.actualDurationSeconds }

            last7Days.add(
                DayFocusData(
                    dayLabel = shortDayFormat.format(Date(startMs)),
                    focusMinutes = (daySec / 60).toInt()
                )
            )
        }

        // Daily and Weekly progress percentages
        val calToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val todaySec = sessions.filter { it.dateTimestamp >= calToday.timeInMillis }.sumOf { it.actualDurationSeconds }
        val dailyProgress = (todaySec.toFloat() / (100 * 60f)).coerceIn(0f, 1f)

        val calWeek = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
        }
        val weekSec = sessions.filter { it.dateTimestamp >= calWeek.timeInMillis }.sumOf { it.actualDurationSeconds }
        val weeklyProgress = (weekSec.toFloat() / (12 * 3600f)).coerceIn(0f, 1f)

        _statsState.value = StatsData(
            totalSessions = sessions.size,
            completedSessions = completed.size,
            incompleteSessions = incomplete.size,
            totalFocusSeconds = totalActualSeconds,
            avgSessionDurationMinutes = avgDurationMin,
            bestStudyDay = bestDay,
            dailyProgressPct = dailyProgress,
            weeklyProgressPct = weeklyProgress,
            last7DaysData = last7Days
        )
    }
}
