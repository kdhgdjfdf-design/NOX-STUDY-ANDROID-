package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.NoxStudyApp
import com.example.data.model.FocusSessionEntity
import com.example.data.preferences.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object StudyOrg : Screen("study_org")
    data class SubjectDetail(val subjectId: Long) : Screen("subject_detail")
    object Focus : Screen("focus")
    object AppBlockerSetup : Screen("app_blocker_setup")
    object Flashcards : Screen("flashcards")
    object Challenges : Screen("challenges")
    object Badges : Screen("badges")
    object Stats : Screen("stats")
    object Settings : Screen("settings")
}

data class TodaySummary(
    val focusMinutes: Int = 0,
    val completedSessions: Int = 0,
    val completedTasks: Int = 0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as NoxStudyApp).database
    private val prefsRepo = (application as NoxStudyApp).preferencesRepository

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _screenStack = mutableListOf<Screen>(Screen.Home)

    val preferences: StateFlow<UserPreferences> = prefsRepo.userPreferencesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserPreferences(
            userName = "طالب NOX المجتهد",
            avatarUri = null,
            language = "ar",
            themeMode = "light",
            dailyGoalSessions = 4,
            dailyGoalMinutes = 100,
            weeklyGoalHours = 12,
            appBlockerEnabled = false,
            blockedPackages = emptySet(),
            customWallpaperStudyOrg = null,
            customWallpaperFocus = null,
            customWallpaperFlashcards = null,
            customWallpaperChallenges = null,
            notificationsEnabled = true,
            timerSoundEnabled = true,
            lastTrackIndex = 0,
            lastTrackPositionMs = 0L
        )
    )

    private val _todaySummary = MutableStateFlow(TodaySummary())
    val todaySummary = _todaySummary.asStateFlow()

    // Daily inspirational quotes
    val dailyQuotes = listOf(
        "«الهدوء والتركيز يصنعان المستحيل، كل دقيقة تقضيها بوعي تقربك من هدفك.»",
        "«لا تقلق بشأن السرعة، بل حافظ على استمرارية المسير نحو أحلامك.»",
        "«جلسة دراسة هادئة مع كوب دافئ قادرة على تغيير مسارك الدراسي بالكامل.»",
        "«الانضباط هو الجسر الذي يربط بين الأهداف والإنجازات العظيمة.»",
        "«اجعل دراستك رحلة شغف واستكشاف، لا مجرد واجب ينتهي.»"
    )

    init {
        observeTodayStats()
    }

    fun navigateTo(screen: Screen) {
        if (_currentScreen.value != screen) {
            _screenStack.add(screen)
            _currentScreen.value = screen
        }
    }

    fun navigateToFocusFromExternalIntent() {
        if (_currentScreen.value == Screen.Focus) return
        _screenStack.removeAll { it == Screen.Focus }
        _screenStack.add(Screen.Focus)
        _currentScreen.value = Screen.Focus
    }

    fun navigateBack(): Boolean {
        if (_screenStack.size > 1) {
            _screenStack.removeAt(_screenStack.lastIndex)
            _currentScreen.value = _screenStack.last()
            return true
        }
        return false
    }

    private fun observeTodayStats() {
        viewModelScope.launch {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = cal.timeInMillis
            val endOfDay = startOfDay + 86400000L

            db.focusDao().getSessionsForDay(startOfDay, endOfDay).collect { sessions ->
                val completed = sessions.filter { it.completed }
                val totalSeconds = completed.sumOf { it.actualDurationSeconds }
                val current = _todaySummary.value
                _todaySummary.value = current.copy(
                    focusMinutes = (totalSeconds / 60).toInt(),
                    completedSessions = completed.size
                )
            }
        }

        viewModelScope.launch {
            db.taskDao().getAllTasks().collect { tasks ->
                val doneTasks = tasks.count { it.status == "COMPLETED" }
                val current = _todaySummary.value
                _todaySummary.value = current.copy(completedTasks = doneTasks)
            }
        }
    }

    fun updateProfile(name: String, avatarUri: String?) {
        viewModelScope.launch {
            prefsRepo.updateProfile(name, avatarUri)
        }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            prefsRepo.setLanguage(lang)
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            prefsRepo.setThemeMode(mode)
        }
    }

    fun setCustomWallpaper(sectionKey: String, uriString: String?) {
        viewModelScope.launch {
            prefsRepo.setSectionWallpaper(sectionKey, uriString)
        }
    }

    fun resetWallpapers() {
        viewModelScope.launch {
            prefsRepo.resetWallpapers()
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setNotificationsEnabled(enabled)
        }
    }

    fun setTimerSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setTimerSoundEnabled(enabled)
        }
    }

    fun updateGoals(dailySessions: Int, dailyMinutes: Int, weeklyHours: Int) {
        viewModelScope.launch {
            prefsRepo.updateGoals(dailySessions, dailyMinutes, weeklyHours)
        }
    }
}
