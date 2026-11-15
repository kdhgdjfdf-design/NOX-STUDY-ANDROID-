package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.service.BlockerStateStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nox_study_preferences")

data class UserPreferences(
    val userName: String,
    val avatarUri: String?,
    val language: String,
    val themeMode: String,
    val dailyGoalSessions: Int,
    val dailyGoalMinutes: Int,
    val weeklyGoalHours: Int,
    val appBlockerEnabled: Boolean,
    val blockedPackages: Set<String>,
    val customWallpaperStudyOrg: String?,
    val customWallpaperFocus: String?,
    val customWallpaperFlashcards: String?,
    val customWallpaperChallenges: String?,
    val notificationsEnabled: Boolean,
    val timerSoundEnabled: Boolean,
    val lastTrackIndex: Int,
    val lastTrackPositionMs: Long,
    val completedFocusBreakCycles: Int = 0,
    val flashcardsReviewedCount: Int = 0
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val USER_NAME = stringPreferencesKey("user_name")
        val AVATAR_URI = stringPreferencesKey("avatar_uri")
        val LANGUAGE = stringPreferencesKey("app_language")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DAILY_GOAL_SESSIONS = intPreferencesKey("daily_goal_sessions")
        val DAILY_GOAL_MINUTES = intPreferencesKey("daily_goal_minutes")
        val WEEKLY_GOAL_HOURS = intPreferencesKey("weekly_goal_hours")
        val APP_BLOCKER_ENABLED = booleanPreferencesKey("app_blocker_enabled")
        val BLOCKED_PACKAGES = stringSetPreferencesKey("blocked_packages")
        val WALLPAPER_STUDY_ORG = stringPreferencesKey("wallpaper_study_org")
        val WALLPAPER_FOCUS = stringPreferencesKey("wallpaper_focus")
        val WALLPAPER_FLASHCARDS = stringPreferencesKey("wallpaper_flashcards")
        val WALLPAPER_CHALLENGES = stringPreferencesKey("wallpaper_challenges")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val TIMER_SOUND_ENABLED = booleanPreferencesKey("timer_sound_enabled")
        val LAST_TRACK_INDEX = intPreferencesKey("last_track_index")
        val LAST_TRACK_POSITION = longPreferencesKey("last_track_position")
        val COMPLETED_FOCUS_BREAK_CYCLES = intPreferencesKey("completed_focus_break_cycles")
        val FLASHCARDS_REVIEWED_COUNT = intPreferencesKey("flashcards_reviewed_count")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            userName = prefs[Keys.USER_NAME] ?: "طالب NOX المجتهد",
            avatarUri = prefs[Keys.AVATAR_URI],
            language = prefs[Keys.LANGUAGE] ?: "ar",
            themeMode = prefs[Keys.THEME_MODE] ?: "light",
            dailyGoalSessions = prefs[Keys.DAILY_GOAL_SESSIONS] ?: 4,
            dailyGoalMinutes = prefs[Keys.DAILY_GOAL_MINUTES] ?: 100,
            weeklyGoalHours = prefs[Keys.WEEKLY_GOAL_HOURS] ?: 12,
            appBlockerEnabled = prefs[Keys.APP_BLOCKER_ENABLED] ?: false,
            blockedPackages = prefs[Keys.BLOCKED_PACKAGES] ?: emptySet(),
            customWallpaperStudyOrg = prefs[Keys.WALLPAPER_STUDY_ORG],
            customWallpaperFocus = prefs[Keys.WALLPAPER_FOCUS],
            customWallpaperFlashcards = prefs[Keys.WALLPAPER_FLASHCARDS],
            customWallpaperChallenges = prefs[Keys.WALLPAPER_CHALLENGES],
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            timerSoundEnabled = prefs[Keys.TIMER_SOUND_ENABLED] ?: true,
            lastTrackIndex = prefs[Keys.LAST_TRACK_INDEX] ?: 0,
            lastTrackPositionMs = prefs[Keys.LAST_TRACK_POSITION] ?: 0L,
            completedFocusBreakCycles = prefs[Keys.COMPLETED_FOCUS_BREAK_CYCLES] ?: 0,
            flashcardsReviewedCount = prefs[Keys.FLASHCARDS_REVIEWED_COUNT] ?: 0
        )
    }

    suspend fun updateProfile(name: String, avatarUri: String?) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USER_NAME] = name
            if (avatarUri != null) {
                prefs[Keys.AVATAR_URI] = avatarUri
            } else {
                prefs.remove(Keys.AVATAR_URI)
            }
        }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LANGUAGE] = lang
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode
        }
    }

    suspend fun updateGoals(dailySessions: Int, dailyMinutes: Int, weeklyHours: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DAILY_GOAL_SESSIONS] = dailySessions
            prefs[Keys.DAILY_GOAL_MINUTES] = dailyMinutes
            prefs[Keys.WEEKLY_GOAL_HOURS] = weeklyHours
        }
    }

    suspend fun setAppBlockerEnabled(enabled: Boolean) {
        val updatedPrefs = context.dataStore.edit { prefs ->
            prefs[Keys.APP_BLOCKER_ENABLED] = enabled
        }
        val pkgs = updatedPrefs[Keys.BLOCKED_PACKAGES] ?: emptySet()
        BlockerStateStore.syncBlockerConfig(context, enabled, pkgs)
    }

    suspend fun updateBlockedPackages(packages: Set<String>) {
        val updatedPrefs = context.dataStore.edit { prefs ->
            prefs[Keys.BLOCKED_PACKAGES] = packages
        }
        val enabled = updatedPrefs[Keys.APP_BLOCKER_ENABLED] ?: false
        BlockerStateStore.syncBlockerConfig(context, enabled, packages)
    }

    suspend fun syncBlockerPreferencesToStore() {
        val current = userPreferencesFlow.first()
        BlockerStateStore.syncBlockerConfig(context, current.appBlockerEnabled, current.blockedPackages)
    }

    suspend fun incrementFocusBreakCycles(): Int {
        var newVal = 0
        context.dataStore.edit { prefs ->
            newVal = (prefs[Keys.COMPLETED_FOCUS_BREAK_CYCLES] ?: 0) + 1
            prefs[Keys.COMPLETED_FOCUS_BREAK_CYCLES] = newVal
        }
        return newVal
    }

    suspend fun incrementFlashcardsReviewed(): Int {
        var newVal = 0
        context.dataStore.edit { prefs ->
            newVal = (prefs[Keys.FLASHCARDS_REVIEWED_COUNT] ?: 0) + 1
            prefs[Keys.FLASHCARDS_REVIEWED_COUNT] = newVal
        }
        return newVal
    }

    suspend fun setSectionWallpaper(sectionKey: String, uriString: String?) {
        context.dataStore.edit { prefs ->
            val key = when (sectionKey) {
                "study_org" -> Keys.WALLPAPER_STUDY_ORG
                "focus" -> Keys.WALLPAPER_FOCUS
                "flashcards" -> Keys.WALLPAPER_FLASHCARDS
                "challenges" -> Keys.WALLPAPER_CHALLENGES
                else -> null
            }
            if (key != null) {
                if (uriString != null) prefs[key] = uriString else prefs.remove(key)
            }
        }
    }

    suspend fun resetWallpapers() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.WALLPAPER_STUDY_ORG)
            prefs.remove(Keys.WALLPAPER_FOCUS)
            prefs.remove(Keys.WALLPAPER_FLASHCARDS)
            prefs.remove(Keys.WALLPAPER_CHALLENGES)
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setTimerSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TIMER_SOUND_ENABLED] = enabled
        }
    }

    suspend fun saveLastPlayback(trackIndex: Int, positionMs: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_TRACK_INDEX] = trackIndex
            prefs[Keys.LAST_TRACK_POSITION] = positionMs
        }
    }
}
