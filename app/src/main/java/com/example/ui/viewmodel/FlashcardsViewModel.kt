package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.NoxStudyApp
import com.example.data.model.FlashcardEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FlashcardsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as NoxStudyApp).database
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
        viewModelScope.launch {
            val updated = card.copy(
                memoryLevel = level,
                lastReviewedAt = System.currentTimeMillis()
            )
            flashcardDao.updateFlashcard(updated)
        }
    }

    fun addFlashcard(
        subjectId: Long,
        lessonId: Long?,
        question: String,
        answer: String,
        imageUri: String? = null
    ) {
        viewModelScope.launch {
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
        }
    }

    fun deleteFlashcard(card: FlashcardEntity) {
        viewModelScope.launch {
            flashcardDao.deleteFlashcard(card)
        }
    }
}
