package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.NoxStudyApp
import com.example.data.model.ChallengeEntity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChallengesViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as NoxStudyApp).database
    private val challengeDao = db.challengeDao()

    val challenges: StateFlow<List<ChallengeEntity>> = challengeDao.getAllChallenges().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _completedChallengeEvent = MutableSharedFlow<ChallengeEntity>()
    val completedChallengeEvent = _completedChallengeEvent.asSharedFlow()

    fun incrementProgress(challengeId: Long, incrementBy: Int = 1) {
        viewModelScope.launch {
            val challenge = challenges.value.firstOrNull { it.id == challengeId } ?: return@launch
            if (challenge.isCompleted) return@launch

            val newCurrent = challenge.currentValue + incrementBy
            val isNowCompleted = newCurrent >= challenge.targetValue
            val updated = challenge.copy(
                currentValue = newCurrent.coerceAtMost(challenge.targetValue),
                isCompleted = isNowCompleted,
                completedAt = if (isNowCompleted) System.currentTimeMillis() else null
            )
            challengeDao.updateChallenge(updated)

            if (isNowCompleted) {
                _completedChallengeEvent.emit(updated)
            }
        }
    }

    fun addCustomChallenge(
        title: String,
        description: String,
        targetType: String,
        targetValue: Int,
        deadlineDays: Int
    ) {
        viewModelScope.launch {
            val deadline = if (deadlineDays > 0) {
                System.currentTimeMillis() + (deadlineDays * 86400000L)
            } else null

            challengeDao.insertChallenge(
                ChallengeEntity(
                    title = title,
                    description = description,
                    targetType = targetType,
                    targetValue = targetValue,
                    currentValue = 0,
                    deadline = deadline,
                    isCompleted = false,
                    isPredefined = false,
                    rewardBadge = "🏆"
                )
            )
        }
    }

    fun deleteChallenge(challenge: ChallengeEntity) {
        viewModelScope.launch {
            challengeDao.deleteChallenge(challenge)
        }
    }
}
