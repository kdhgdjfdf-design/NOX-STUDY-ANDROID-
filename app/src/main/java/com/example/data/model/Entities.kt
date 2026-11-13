package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String = "#8A5836",
    val iconName: String = "book",
    val imageUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "lessons",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("subjectId")]
)
data class LessonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val title: String,
    val status: String = "not_started", // not_started, in_progress, needs_revision, reviewed
    val addedAt: Long = System.currentTimeMillis(),
    val nextRevisionDate: Long? = null,
    val notes: String = "",
    val attachmentsJson: String = "[]", // List of Uri strings serialized as JSON array
    val reminderEnabled: Boolean = true
)

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("subjectId")]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val title: String,
    val dueDate: Long,
    val priority: String = "MEDIUM", // HIGH, MEDIUM, LOW
    val status: String = "PENDING",  // PENDING, IN_PROGRESS, COMPLETED
    val notes: String = "",
    val attachmentsJson: String = "[]",
    val reminderMinutesBefore: Int = 60
)

@Entity(
    tableName = "exams",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("subjectId")]
)
data class ExamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val title: String,
    val examDate: Long,
    val linkedLessonIdsJson: String = "[]", // JSON array of Long lesson IDs
    val readinessLevel: String = "MEDIUM", // LOW, MEDIUM, HIGH
    val notes: String = "",
    val attachmentsJson: String = "[]",
    val reminderDate: Long? = null
)

@Entity(
    tableName = "flashcards",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("subjectId"), Index("lessonId")]
)
data class FlashcardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val lessonId: Long? = null,
    val question: String,
    val answer: String,
    val imageUri: String? = null,
    val memoryLevel: String = "MEDIUM", // EASY, MEDIUM, HARD
    val lastReviewedAt: Long = 0
)

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mode: String = "POMODORO", // POMODORO, CUSTOM, FREE
    val targetMinutes: Int = 25,
    val actualDurationSeconds: Long = 0,
    val completed: Boolean = false,
    val dateTimestamp: Long = System.currentTimeMillis(),
    val interruptedCount: Int = 0
)

@Entity(tableName = "challenges")
data class ChallengeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val targetType: String, // LESSONS, REVISIONS, FOCUS_SESSIONS, TASKS
    val targetValue: Int,
    val currentValue: Int = 0,
    val deadline: Long? = null,
    val isCompleted: Boolean = false,
    val isPredefined: Boolean = false,
    val rewardBadge: String = "🌟",
    val completedAt: Long? = null
)
