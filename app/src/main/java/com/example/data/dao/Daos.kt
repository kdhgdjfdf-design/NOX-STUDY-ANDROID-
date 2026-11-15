package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BadgeEntity
import com.example.data.model.ChallengeEntity
import com.example.data.model.ExamEntity
import com.example.data.model.FlashcardEntity
import com.example.data.model.FocusSessionEntity
import com.example.data.model.LessonEntity
import com.example.data.model.SubjectEntity
import com.example.data.model.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects ORDER BY id ASC")
    fun getAllSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    suspend fun getSubjectById(id: Long): SubjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity): Long

    @Update
    suspend fun updateSubject(subject: SubjectEntity)

    @Delete
    suspend fun deleteSubject(subject: SubjectEntity)
}

@Dao
interface LessonDao {
    @Query("SELECT * FROM lessons WHERE subjectId = :subjectId ORDER BY id DESC")
    fun getLessonsBySubject(subjectId: Long): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons ORDER BY id DESC")
    fun getAllLessons(): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons WHERE nextRevisionDate IS NOT NULL AND nextRevisionDate <= :currentTime")
    fun getDueRevisions(currentTime: Long): Flow<List<LessonEntity>>

    @Query("SELECT COUNT(*) FROM lessons WHERE subjectId = :subjectId")
    fun getLessonsCountBySubject(subjectId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM lessons WHERE subjectId = :subjectId AND status = 'needs_revision'")
    fun getRevisionsDueCountBySubject(subjectId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM lessons WHERE status = 'reviewed'")
    suspend fun getReviewedLessonsCountSync(): Int

    @Query("SELECT * FROM lessons WHERE id = :id LIMIT 1")
    suspend fun getLessonById(id: Long): LessonEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLesson(lesson: LessonEntity): Long

    @Update
    suspend fun updateLesson(lesson: LessonEntity)

    @Delete
    suspend fun deleteLesson(lesson: LessonEntity)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY dueDate ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE subjectId = :subjectId ORDER BY dueDate ASC")
    fun getTasksBySubject(subjectId: Long): Flow<List<TaskEntity>>

    @Query("SELECT COUNT(*) FROM tasks WHERE status = 'COMPLETED'")
    suspend fun getCompletedTasksCountSync(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)
}

@Dao
interface ExamDao {
    @Query("SELECT * FROM exams ORDER BY examDate ASC")
    fun getAllExams(): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams WHERE subjectId = :subjectId ORDER BY examDate ASC")
    fun getExamsBySubject(subjectId: Long): Flow<List<ExamEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamEntity): Long

    @Update
    suspend fun updateExam(exam: ExamEntity)

    @Delete
    suspend fun deleteExam(exam: ExamEntity)
}

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM flashcards ORDER BY id DESC")
    fun getAllFlashcards(): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE subjectId = :subjectId ORDER BY id DESC")
    fun getFlashcardsBySubject(subjectId: Long): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE lessonId = :lessonId ORDER BY id DESC")
    fun getFlashcardsByLesson(lessonId: Long): Flow<List<FlashcardEntity>>

    @Query("SELECT COUNT(*) FROM flashcards WHERE lastReviewedAt > 0")
    suspend fun getReviewedFlashcardsCountSync(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcard(flashcard: FlashcardEntity): Long

    @Update
    suspend fun updateFlashcard(flashcard: FlashcardEntity)

    @Delete
    suspend fun deleteFlashcard(flashcard: FlashcardEntity)
}

@Dao
interface FocusDao {
    @Query("SELECT * FROM focus_sessions ORDER BY dateTimestamp DESC")
    fun getAllSessions(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE dateTimestamp >= :startOfDay AND dateTimestamp < :endOfDay")
    fun getSessionsForDay(startOfDay: Long, endOfDay: Long): Flow<List<FocusSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSessionEntity): Long

    @Query("SELECT SUM(actualDurationSeconds) FROM focus_sessions WHERE dateTimestamp >= :sinceTimestamp")
    suspend fun getTotalFocusSecondsSince(sinceTimestamp: Long): Long?

    @Query("SELECT * FROM focus_sessions WHERE completed = 1 ORDER BY dateTimestamp ASC")
    suspend fun getCompletedSessionsSync(): List<FocusSessionEntity>

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE completed = 1")
    suspend fun getCompletedSessionsCountSync(): Int
}

@Dao
interface ChallengeDao {
    @Query("SELECT * FROM challenges ORDER BY isCompleted ASC, id ASC")
    fun getAllChallenges(): Flow<List<ChallengeEntity>>

    @Query("SELECT * FROM challenges WHERE isCompleted = 0")
    suspend fun getActiveChallenges(): List<ChallengeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChallenge(challenge: ChallengeEntity): Long

    @Update
    suspend fun updateChallenge(challenge: ChallengeEntity)

    @Delete
    suspend fun deleteChallenge(challenge: ChallengeEntity)
}

@Dao
interface BadgeDao {
    @Query("SELECT * FROM badges")
    fun getAllBadges(): Flow<List<BadgeEntity>>

    @Query("SELECT * FROM badges")
    suspend fun getAllBadgesList(): List<BadgeEntity>

    @Query("SELECT * FROM badges WHERE id = :id LIMIT 1")
    suspend fun getBadgeById(id: String): BadgeEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBadge(badge: BadgeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBadge(badge: BadgeEntity)

    @Update
    suspend fun updateBadge(badge: BadgeEntity)
}
