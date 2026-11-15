package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.NoxStudyApp
import com.example.data.badges.BadgeEvaluator
import com.example.data.model.ExamEntity
import com.example.data.model.LessonEntity
import com.example.data.model.SubjectEntity
import com.example.data.model.TaskEntity
import com.example.service.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StudyOrgViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val db = (application as NoxStudyApp).database
    private val subjectDao = db.subjectDao()
    private val lessonDao = db.lessonDao()
    private val taskDao = db.taskDao()
    private val examDao = db.examDao()

    val subjects: StateFlow<List<SubjectEntity>> = subjectDao.getAllSubjects().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allLessons: StateFlow<List<LessonEntity>> = lessonDao.getAllLessons().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allTasks: StateFlow<List<TaskEntity>> = taskDao.getAllTasks().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allExams: StateFlow<List<ExamEntity>> = examDao.getAllExams().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Current selected subject for detail view
    private val _selectedSubjectId = MutableStateFlow<Long?>(null)
    val selectedSubjectId = _selectedSubjectId.asStateFlow()

    fun selectSubject(id: Long) {
        _selectedSubjectId.value = id
    }

    fun addSubject(name: String, colorHex: String, iconName: String) {
        viewModelScope.launch {
            subjectDao.insertSubject(
                SubjectEntity(name = name, colorHex = colorHex, iconName = iconName)
            )
        }
    }

    fun updateSubject(subject: SubjectEntity) {
        viewModelScope.launch {
            subjectDao.updateSubject(subject)
        }
    }

    fun deleteSubject(subject: SubjectEntity) {
        viewModelScope.launch {
            subjectDao.deleteSubject(subject)
        }
    }

    fun addLesson(
        subjectId: Long,
        title: String,
        status: String,
        notes: String,
        nextRevisionDate: Long?,
        attachmentsJson: String,
        reminderEnabled: Boolean
    ) {
        viewModelScope.launch {
            val lessonId = lessonDao.insertLesson(
                LessonEntity(
                    subjectId = subjectId,
                    title = title,
                    status = status,
                    notes = notes,
                    nextRevisionDate = nextRevisionDate,
                    attachmentsJson = attachmentsJson,
                    reminderEnabled = reminderEnabled
                )
            )
            if (reminderEnabled && nextRevisionDate != null && nextRevisionDate > System.currentTimeMillis()) {
                NotificationHelper.scheduleAlarm(
                    context = context,
                    requestCode = (10000 + lessonId).toInt(),
                    triggerTimeMillis = nextRevisionDate,
                    title = "تذكير بمراجعة الدرس 📚",
                    message = "حان موعد مراجعة درس: $title"
                )
            }
        }
    }

    fun updateLesson(lesson: LessonEntity) {
        viewModelScope.launch {
            lessonDao.updateLesson(lesson)
            if (lesson.reminderEnabled && lesson.nextRevisionDate != null && lesson.nextRevisionDate > System.currentTimeMillis()) {
                NotificationHelper.scheduleAlarm(
                    context = context,
                    requestCode = (10000 + lesson.id).toInt(),
                    triggerTimeMillis = lesson.nextRevisionDate,
                    title = "تذكير بمراجعة الدرس 📚",
                    message = "حان موعد مراجعة درس: ${lesson.title}"
                )
            } else {
                NotificationHelper.cancelAlarm(context, (10000 + lesson.id).toInt())
            }
            if (lesson.status == "reviewed") {
                BadgeEvaluator.evaluateAll(context)
            }
        }
    }

    fun deleteLesson(lesson: LessonEntity) {
        viewModelScope.launch {
            NotificationHelper.cancelAlarm(context, (10000 + lesson.id).toInt())
            lessonDao.deleteLesson(lesson)
        }
    }

    fun addTask(
        subjectId: Long,
        title: String,
        dueDate: Long,
        priority: String,
        status: String,
        notes: String,
        attachmentsJson: String,
        reminderMinutesBefore: Int
    ) {
        viewModelScope.launch {
            val taskId = taskDao.insertTask(
                TaskEntity(
                    subjectId = subjectId,
                    title = title,
                    dueDate = dueDate,
                    priority = priority,
                    status = status,
                    notes = notes,
                    attachmentsJson = attachmentsJson,
                    reminderMinutesBefore = reminderMinutesBefore
                )
            )
            val triggerTime = dueDate - (reminderMinutesBefore * 60 * 1000L)
            if (triggerTime > System.currentTimeMillis()) {
                NotificationHelper.scheduleAlarm(
                    context = context,
                    requestCode = (20000 + taskId).toInt(),
                    triggerTimeMillis = triggerTime,
                    title = "تذكير بتسليم الواجب ⏳",
                    message = "اقترب موعد تسليم: $title (متبقي $reminderMinutesBefore دقيقة)"
                )
            }
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            taskDao.updateTask(task)
            if (task.status == "COMPLETED") {
                NotificationHelper.cancelAlarm(context, (20000 + task.id).toInt())
                BadgeEvaluator.evaluateAll(context)
            }
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            NotificationHelper.cancelAlarm(context, (20000 + task.id).toInt())
            taskDao.deleteTask(task)
        }
    }

    fun addExam(
        subjectId: Long,
        title: String,
        examDate: Long,
        linkedLessonIdsJson: String,
        readinessLevel: String,
        notes: String,
        attachmentsJson: String,
        reminderDate: Long?
    ) {
        viewModelScope.launch {
            val examId = examDao.insertExam(
                ExamEntity(
                    subjectId = subjectId,
                    title = title,
                    examDate = examDate,
                    linkedLessonIdsJson = linkedLessonIdsJson,
                    readinessLevel = readinessLevel,
                    notes = notes,
                    attachmentsJson = attachmentsJson,
                    reminderDate = reminderDate
                )
            )
            if (reminderDate != null && reminderDate > System.currentTimeMillis()) {
                NotificationHelper.scheduleAlarm(
                    context = context,
                    requestCode = (30000 + examId).toInt(),
                    triggerTimeMillis = reminderDate,
                    title = "تنبيه قبل الاختبار القادم 📝",
                    message = "موعد اختبار $title يقترب! حظاً موفقاً."
                )
            }
        }
    }

    fun updateExam(exam: ExamEntity) {
        viewModelScope.launch {
            examDao.updateExam(exam)
        }
    }

    fun deleteExam(exam: ExamEntity) {
        viewModelScope.launch {
            NotificationHelper.cancelAlarm(context, (30000 + exam.id).toInt())
            examDao.deleteExam(exam)
        }
    }
}
