package com.example.data.badges

import android.content.Context
import com.example.NoxStudyApp
import com.example.data.AppDatabase
import com.example.data.model.BadgeEntity
import com.example.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AchievementMetrics(
    val completedSessionsCount: Int = 0,
    val totalFocusMinutes: Int = 0,
    val distinctStudyDays: Int = 0,
    val completedTasksCount: Int = 0,
    val focusBreakCyclesCount: Int = 0,
    val flashcardReviewsCount: Int = 0,
    val reviewedLessonsCount: Int = 0
) {
    fun valueFor(metricType: BadgeMetricType): Int = when (metricType) {
        BadgeMetricType.COMPLETED_SESSIONS -> completedSessionsCount
        BadgeMetricType.TOTAL_FOCUS_MINUTES -> totalFocusMinutes
        BadgeMetricType.DISTINCT_STUDY_DAYS -> distinctStudyDays
        BadgeMetricType.COMPLETED_TASKS -> completedTasksCount
        BadgeMetricType.FOCUS_BREAK_CYCLES -> focusBreakCyclesCount
        BadgeMetricType.FLASHCARD_REVIEWS -> flashcardReviewsCount
        BadgeMetricType.REVIEWED_LESSONS -> reviewedLessonsCount
    }
}

object BadgeEvaluator {

    private val mutex = Mutex()

    private val _newlyUnlockedQueue = MutableStateFlow<List<BadgeDefinition>>(emptyList())
    val newlyUnlockedQueue: StateFlow<List<BadgeDefinition>> = _newlyUnlockedQueue.asStateFlow()

    suspend fun collectMetrics(
        db: AppDatabase,
        prefsRepo: UserPreferencesRepository
    ): AchievementMetrics {
        val completedSessions = db.focusDao().getCompletedSessionsSync()
            .filter { it.actualDurationSeconds >= 60 }

        val totalFocusMinutes = (completedSessions.sumOf { it.actualDurationSeconds } / 60L).toInt()
        val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val distinctDays = completedSessions
            .map { dayFormat.format(Date(it.dateTimestamp)) }
            .distinct()
            .size

        val completedTasks = db.taskDao().getCompletedTasksCountSync()
        val reviewedLessons = db.lessonDao().getReviewedLessonsCountSync()
        val dbReviewedCards = db.flashcardDao().getReviewedFlashcardsCountSync()

        val prefs = prefsRepo.userPreferencesFlow.first()
        val flashcardReviews = maxOf(prefs.flashcardsReviewedCount, dbReviewedCards)

        return AchievementMetrics(
            completedSessionsCount = completedSessions.size,
            totalFocusMinutes = totalFocusMinutes,
            distinctStudyDays = distinctDays,
            completedTasksCount = completedTasks,
            focusBreakCyclesCount = prefs.completedFocusBreakCycles,
            flashcardReviewsCount = flashcardReviews,
            reviewedLessonsCount = reviewedLessons
        )
    }

    /**
     * Evaluates all badges against real user activity stored in Room + DataStore.
     * Grants newly earned badges once and never duplicates already unlocked badges.
     * Returns the list of newly unlocked badges from this evaluation pass.
     */
    suspend fun evaluateAll(
        context: Context,
        databaseOverride: AppDatabase? = null,
        prefsOverride: UserPreferencesRepository? = null
    ): List<BadgeDefinition> = mutex.withLock {
        val app = context.applicationContext as? NoxStudyApp
        val db = databaseOverride ?: app?.database ?: AppDatabase.getDatabase(context)
        val prefsRepo = prefsOverride ?: app?.preferencesRepository ?: UserPreferencesRepository(context)

        val metrics = collectMetrics(db, prefsRepo)
        val badgeDao = db.badgeDao()
        val existingMap = badgeDao.getAllBadgesList().associateBy { it.id }

        val newlyUnlocked = mutableListOf<BadgeDefinition>()
        val now = System.currentTimeMillis()

        for (def in BadgeCatalog.allBadges) {
            val currentVal = metrics.valueFor(def.metricType)
            val existing = existingMap[def.id]

            if (existing?.isUnlocked == true) {
                // Already unlocked previously: never grant twice or overwrite unlockedAt
                continue
            }

            if (currentVal >= def.targetValue) {
                val entity = BadgeEntity(
                    id = def.id,
                    isUnlocked = true,
                    unlockedAt = now,
                    hasCelebrated = false
                )
                badgeDao.upsertBadge(entity)
                newlyUnlocked.add(def)
            } else if (existing == null) {
                badgeDao.insertBadge(
                    BadgeEntity(
                        id = def.id,
                        isUnlocked = false,
                        unlockedAt = null,
                        hasCelebrated = false
                    )
                )
            }
        }

        if (newlyUnlocked.isNotEmpty()) {
            val currentQueue = _newlyUnlockedQueue.value.toMutableList()
            for (badge in newlyUnlocked) {
                if (currentQueue.none { it.id == badge.id }) {
                    currentQueue.add(badge)
                }
            }
            _newlyUnlockedQueue.value = currentQueue
        }

        return newlyUnlocked
    }

    suspend fun buildUiModels(
        context: Context,
        db: AppDatabase,
        prefsRepo: UserPreferencesRepository,
        entities: List<BadgeEntity>
    ): List<BadgeUiModel> {
        val metrics = collectMetrics(db, prefsRepo)
        val entityMap = entities.associateBy { it.id }
        return BadgeCatalog.allBadges.map { def ->
            val entity = entityMap[def.id]
            val unlocked = entity?.isUnlocked == true
            val current = metrics.valueFor(def.metricType).coerceAtMost(def.targetValue)
            val progress = if (unlocked) 1f else (current.toFloat() / def.targetValue.toFloat()).coerceIn(0f, 1f)
            BadgeUiModel(
                definition = def,
                isUnlocked = unlocked,
                unlockedAt = entity?.unlockedAt,
                hasCelebrated = entity?.hasCelebrated == true,
                currentValue = if (unlocked) def.targetValue else current,
                progressFraction = progress
            )
        }
    }

    suspend fun markBadgeCelebrated(context: Context, badgeId: String) {
        val app = context.applicationContext as? NoxStudyApp
        val db = app?.database ?: AppDatabase.getDatabase(context)
        val existing = db.badgeDao().getBadgeById(badgeId)
        if (existing != null && !existing.hasCelebrated) {
            db.badgeDao().updateBadge(existing.copy(hasCelebrated = true))
        }
        _newlyUnlockedQueue.value = _newlyUnlockedQueue.value.filterNot { it.id == badgeId }
    }

    fun dismissCelebrationBanner(badgeId: String) {
        _newlyUnlockedQueue.value = _newlyUnlockedQueue.value.filterNot { it.id == badgeId }
    }
}
