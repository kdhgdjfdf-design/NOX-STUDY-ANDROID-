package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.NoxStudyApp
import com.example.data.badges.BadgeEvaluator
import com.example.data.model.FlashcardEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FlashcardsViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val app = application as NoxStudyApp
    private val db = app.database
    private val prefsRepo = app.preferencesRepository
    private val flashcardDao = db.flashcardDao()

    val allFlashcards: StateFlow<List<FlashcardEntity>> = flashcardDao.getAllFlashcards().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedSubjectFilter = MutableStateFlow<Long?>(null)
    val selectedSubjectFilter = _selectedSubjectFilter.asStateFlow()

    private val _currentCardIndex = MutableStateFlow(0)
    val currentCardIndex = _currentCardIndex.asStateFlow()

    private val _isFlipped = MutableStateFlow(false)
    val isFlipped = _isFlipped.asStateFlow()

    fun setSubjectFilter(subjectId: Long?) {
        _selectedSubjectFilter.value = subjectId
        _currentCardIndex.value = 0
        _isFlipped.value = false
    }

    fun flipCard() {
        _isFlipped.value = !_isFlipped.value
    }

    fun nextCard(totalSize: Int) {
        if (totalSize > 0) {
            _currentCardIndex.value = (_currentCardIndex.value + 1) % totalSize
            _isFlipped.value = false
        }
    }

    fun prevCard(totalSize: Int) {
        if (totalSize > 0) {
            _currentCardIndex.value = if (_currentCardIndex.value - 1 < 0) totalSize - 1 else _currentCardIndex.value - 1
            _isFlipped.value = false
        }
    }

    fun updateMemoryLevel(card: FlashcardEntity, level: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = card.copy(
                memoryLevel = level,
                lastReviewedAt = System.currentTimeMillis()
            )
            flashcardDao.updateFlashcard(updated)
            prefsRepo.incrementFlashcardsReviewed()
            BadgeEvaluator.evaluateAll(appContext)
        }
    }

    fun addFlashcard(
        subjectId: Long,
        lessonId: Long?,
        question: String,
        answer: String,
        imageUri: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            flashcardDao.insertFlashcard(
                FlashcardEntity(
                    subjectId = subjectId,
                    lessonId = lessonId,
                    question = question,
                    answer = answer,
                    imageUri = imageUri,
                    memoryLevel = "MEDIUM"
                )
            )
            _currentCardIndex.value = 0
            _isFlipped.value = false
        }
    }

    /**
     * Deletes ONLY the specified flashcard from Room without affecting any sibling flashcards,
     * the parent subject folder, or any original files on the device.
     */
    fun deleteFlashcard(card: FlashcardEntity, currentFilteredSize: Int = 1) {
        _isFlipped.value = false
        val remainingCount = (currentFilteredSize - 1).coerceAtLeast(0)
        if (remainingCount == 0) {
            _currentCardIndex.value = 0
        } else if (_currentCardIndex.value >= remainingCount) {
            _currentCardIndex.value = remainingCount - 1
        }
        viewModelScope.launch(Dispatchers.IO) {
            flashcardDao.deleteFlashcard(card)
        }
    }
}
