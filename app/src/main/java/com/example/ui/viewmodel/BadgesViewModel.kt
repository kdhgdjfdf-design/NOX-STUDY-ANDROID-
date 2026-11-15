package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.NoxStudyApp
import com.example.data.badges.BadgeCatalog
import com.example.data.badges.BadgeEvaluator
import com.example.data.badges.BadgeUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BadgesViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val app = application as NoxStudyApp
    private val db = app.database
    private val prefsRepo = app.preferencesRepository

    private val _badges = MutableStateFlow<List<BadgeUiModel>>(
        BadgeCatalog.allBadges.map {
            BadgeUiModel(
                definition = it,
                isUnlocked = false,
                unlockedAt = null,
                hasCelebrated = false,
                currentValue = 0,
                progressFraction = 0f
            )
        }
    )
    val badges: StateFlow<List<BadgeUiModel>> = _badges.asStateFlow()

    val newlyUnlockedQueue = BadgeEvaluator.newlyUnlockedQueue

    init {
        observeAndEvaluate()
    }

    private fun observeAndEvaluate() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                BadgeEvaluator.evaluateAll(appContext)
            }

            combine(
                db.badgeDao().getAllBadges(),
                db.focusDao().getAllSessions(),
                db.taskDao().getAllTasks(),
                db.lessonDao().getAllLessons(),
                db.flashcardDao().getAllFlashcards(),
                prefsRepo.userPreferencesFlow
            ) { values ->
                @Suppress("UNCHECKED_CAST")
                val badgeEntities = values[0] as List<com.example.data.model.BadgeEntity>
                badgeEntities
            }.collect { badgeEntities ->
                val uiModels = withContext(Dispatchers.IO) {
                    BadgeEvaluator.buildUiModels(
                        context = appContext,
                        db = db,
                        prefsRepo = prefsRepo,
                        entities = badgeEntities
                    )
                }
                _badges.value = uiModels
            }
        }
    }

    fun refreshEvaluation() {
        viewModelScope.launch(Dispatchers.IO) {
            BadgeEvaluator.evaluateAll(appContext)
        }
    }

    fun markCelebrated(badgeId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            BadgeEvaluator.markBadgeCelebrated(appContext, badgeId)
        }
    }
}
