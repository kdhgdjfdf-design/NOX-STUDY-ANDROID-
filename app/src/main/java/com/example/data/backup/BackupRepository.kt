package com.example.data.backup

import android.content.Context
import android.net.Uri
import com.example.data.AppDatabase
import com.example.data.model.ChallengeEntity
import com.example.data.model.ExamEntity
import com.example.data.model.FlashcardEntity
import com.example.data.model.FocusSessionEntity
import com.example.data.model.LessonEntity
import com.example.data.model.SubjectEntity
import com.example.data.model.TaskEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class BackupRepository(
    private val context: Context,
    private val database: AppDatabase
) {

    suspend fun exportBackup(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject()
            root.put("app", "NOX STUDY")
            root.put("version", 1)
            root.put("timestamp", System.currentTimeMillis())

            // Subjects
            val subjects = database.subjectDao().getAllSubjects().first()
            val subjectsArray = JSONArray()
            subjects.forEach { s ->
                val obj = JSONObject()
                obj.put("id", s.id)
                obj.put("name", s.name)
                obj.put("colorHex", s.colorHex)
                obj.put("iconName", s.iconName)
                obj.put("createdAt", s.createdAt)
                subjectsArray.put(obj)
            }
            root.put("subjects", subjectsArray)

            // Lessons
            val lessons = database.lessonDao().getAllLessons().first()
            val lessonsArray = JSONArray()
            lessons.forEach { l ->
                val obj = JSONObject()
                obj.put("id", l.id)
                obj.put("subjectId", l.subjectId)
                obj.put("title", l.title)
                obj.put("status", l.status)
                obj.put("addedAt", l.addedAt)
                obj.put("nextRevisionDate", l.nextRevisionDate ?: -1L)
                obj.put("notes", l.notes)
                obj.put("attachmentsJson", l.attachmentsJson)
                obj.put("reminderEnabled", l.reminderEnabled)
                lessonsArray.put(obj)
            }
            root.put("lessons", lessonsArray)

            // Tasks
            val tasks = database.taskDao().getAllTasks().first()
            val tasksArray = JSONArray()
            tasks.forEach { t ->
                val obj = JSONObject()
                obj.put("id", t.id)
                obj.put("subjectId", t.subjectId)
                obj.put("title", t.title)
                obj.put("dueDate", t.dueDate)
                obj.put("priority", t.priority)
                obj.put("status", t.status)
                obj.put("notes", t.notes)
                obj.put("reminderMinutesBefore", t.reminderMinutesBefore)
                tasksArray.put(obj)
            }
            root.put("tasks", tasksArray)

            // Exams
            val exams = database.examDao().getAllExams().first()
            val examsArray = JSONArray()
            exams.forEach { e ->
                val obj = JSONObject()
                obj.put("id", e.id)
                obj.put("subjectId", e.subjectId)
                obj.put("title", e.title)
                obj.put("examDate", e.examDate)
                obj.put("linkedLessonIdsJson", e.linkedLessonIdsJson)
                obj.put("readinessLevel", e.readinessLevel)
                obj.put("notes", e.notes)
                obj.put("reminderDate", e.reminderDate ?: -1L)
                examsArray.put(obj)
            }
            root.put("exams", examsArray)

            // Flashcards
            val flashcards = database.flashcardDao().getAllFlashcards().first()
            val flashcardsArray = JSONArray()
            flashcards.forEach { f ->
                val obj = JSONObject()
                obj.put("id", f.id)
                obj.put("subjectId", f.subjectId)
                obj.put("lessonId", f.lessonId ?: -1L)
                obj.put("question", f.question)
                obj.put("answer", f.answer)
                obj.put("memoryLevel", f.memoryLevel)
                obj.put("lastReviewedAt", f.lastReviewedAt)
                flashcardsArray.put(obj)
            }
            root.put("flashcards", flashcardsArray)

            // Focus sessions
            val sessions = database.focusDao().getAllSessions().first()
            val sessionsArray = JSONArray()
            sessions.forEach { s ->
                val obj = JSONObject()
                obj.put("id", s.id)
                obj.put("mode", s.mode)
                obj.put("targetMinutes", s.targetMinutes)
                obj.put("actualDurationSeconds", s.actualDurationSeconds)
                obj.put("completed", s.completed)
                obj.put("dateTimestamp", s.dateTimestamp)
                obj.put("interruptedCount", s.interruptedCount)
                sessionsArray.put(obj)
            }
            root.put("focus_sessions", sessionsArray)

            // Challenges
            val challenges = database.challengeDao().getAllChallenges().first()
            val challengesArray = JSONArray()
            challenges.forEach { c ->
                val obj = JSONObject()
                obj.put("id", c.id)
                obj.put("title", c.title)
                obj.put("description", c.description)
                obj.put("targetType", c.targetType)
                obj.put("targetValue", c.targetValue)
                obj.put("currentValue", c.currentValue)
                obj.put("deadline", c.deadline ?: -1L)
                obj.put("isCompleted", c.isCompleted)
                obj.put("isPredefined", c.isPredefined)
                obj.put("rewardBadge", c.rewardBadge)
                challengesArray.put(obj)
            }
            root.put("challenges", challengesArray)

            context.contentResolver.openOutputStream(uri)?.use { os ->
                OutputStreamWriter(os).use { writer ->
                    writer.write(root.toString(2))
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importBackup(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val jsonText = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            } ?: return@withContext Result.failure(Exception("Failed to read file"))

            val root = JSONObject(jsonText)
            if (!root.has("app") || root.getString("app") != "NOX STUDY") {
                return@withContext Result.failure(Exception("Invalid NOX STUDY backup file format"))
            }

            // Restore subjects
            val subjectsArray = root.optJSONArray("subjects") ?: JSONArray()
            for (i in 0 until subjectsArray.length()) {
                val obj = subjectsArray.getJSONObject(i)
                val subject = SubjectEntity(
                    id = obj.optLong("id", 0L),
                    name = obj.getString("name"),
                    colorHex = obj.optString("colorHex", "#8A5836"),
                    iconName = obj.optString("iconName", "book"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
                database.subjectDao().insertSubject(subject)
            }

            // Restore lessons
            val lessonsArray = root.optJSONArray("lessons") ?: JSONArray()
            for (i in 0 until lessonsArray.length()) {
                val obj = lessonsArray.getJSONObject(i)
                val revDate = obj.optLong("nextRevisionDate", -1L)
                val lesson = LessonEntity(
                    id = obj.optLong("id", 0L),
                    subjectId = obj.getLong("subjectId"),
                    title = obj.getString("title"),
                    status = obj.optString("status", "not_started"),
                    addedAt = obj.optLong("addedAt", System.currentTimeMillis()),
                    nextRevisionDate = if (revDate > 0) revDate else null,
                    notes = obj.optString("notes", ""),
                    attachmentsJson = obj.optString("attachmentsJson", "[]"),
                    reminderEnabled = obj.optBoolean("reminderEnabled", true)
                )
                database.lessonDao().insertLesson(lesson)
            }

            // Restore tasks
            val tasksArray = root.optJSONArray("tasks") ?: JSONArray()
            for (i in 0 until tasksArray.length()) {
                val obj = tasksArray.getJSONObject(i)
                val task = TaskEntity(
                    id = obj.optLong("id", 0L),
                    subjectId = obj.getLong("subjectId"),
                    title = obj.getString("title"),
                    dueDate = obj.getLong("dueDate"),
                    priority = obj.optString("priority", "MEDIUM"),
                    status = obj.optString("status", "PENDING"),
                    notes = obj.optString("notes", ""),
                    reminderMinutesBefore = obj.optInt("reminderMinutesBefore", 60)
                )
                database.taskDao().insertTask(task)
            }

            // Restore exams
            val examsArray = root.optJSONArray("exams") ?: JSONArray()
            for (i in 0 until examsArray.length()) {
                val obj = examsArray.getJSONObject(i)
                val remDate = obj.optLong("reminderDate", -1L)
                val exam = ExamEntity(
                    id = obj.optLong("id", 0L),
                    subjectId = obj.getLong("subjectId"),
                    title = obj.getString("title"),
                    examDate = obj.getLong("examDate"),
                    linkedLessonIdsJson = obj.optString("linkedLessonIdsJson", "[]"),
                    readinessLevel = obj.optString("readinessLevel", "MEDIUM"),
                    notes = obj.optString("notes", ""),
                    reminderDate = if (remDate > 0) remDate else null
                )
                database.examDao().insertExam(exam)
            }

            // Restore flashcards
            val flashcardsArray = root.optJSONArray("flashcards") ?: JSONArray()
            for (i in 0 until flashcardsArray.length()) {
                val obj = flashcardsArray.getJSONObject(i)
                val lId = obj.optLong("lessonId", -1L)
                val card = FlashcardEntity(
                    id = obj.optLong("id", 0L),
                    subjectId = obj.getLong("subjectId"),
                    lessonId = if (lId > 0) lId else null,
                    question = obj.getString("question"),
                    answer = obj.getString("answer"),
                    memoryLevel = obj.optString("memoryLevel", "MEDIUM"),
                    lastReviewedAt = obj.optLong("lastReviewedAt", 0L)
                )
                database.flashcardDao().insertFlashcard(card)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
