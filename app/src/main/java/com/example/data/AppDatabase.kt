package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.BadgeDao
import com.example.data.dao.ChallengeDao
import com.example.data.dao.ExamDao
import com.example.data.dao.FlashcardDao
import com.example.data.dao.FocusDao
import com.example.data.dao.LessonDao
import com.example.data.dao.SubjectDao
import com.example.data.dao.TaskDao
import com.example.data.model.BadgeEntity
import com.example.data.model.ChallengeEntity
import com.example.data.model.ExamEntity
import com.example.data.model.FlashcardEntity
import com.example.data.model.FocusSessionEntity
import com.example.data.model.LessonEntity
import com.example.data.model.SubjectEntity
import com.example.data.model.TaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        SubjectEntity::class,
        LessonEntity::class,
        TaskEntity::class,
        ExamEntity::class,
        FlashcardEntity::class,
        FocusSessionEntity::class,
        ChallengeEntity::class,
        BadgeEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun subjectDao(): SubjectDao
    abstract fun lessonDao(): LessonDao
    abstract fun taskDao(): TaskDao
    abstract fun examDao(): ExamDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun focusDao(): FocusDao
    abstract fun challengeDao(): ChallengeDao
    abstract fun badgeDao(): BadgeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `badges` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `isUnlocked` INTEGER NOT NULL,
                        `unlockedAt` INTEGER,
                        `hasCelebrated` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nox_study_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    INSTANCE?.let { database ->
                        populateInitialData(database)
                    }
                }
            }

            private suspend fun populateInitialData(db: AppDatabase) {
                val subjectDao = db.subjectDao()
                val mathId = subjectDao.insertSubject(
                    SubjectEntity(name = "الرياضيات والتحليل", colorHex = "#8A5836", iconName = "functions")
                )
                val physicsId = subjectDao.insertSubject(
                    SubjectEntity(name = "الفيزياء والعلوم", colorHex = "#5F6B55", iconName = "science")
                )
                val langId = subjectDao.insertSubject(
                    SubjectEntity(name = "الأدب واللغات", colorHex = "#B86D49", iconName = "menu_book")
                )

                val lessonDao = db.lessonDao()
                lessonDao.insertLesson(
                    LessonEntity(
                        subjectId = mathId,
                        title = "التفاضل والتكامل - الدرس الأول",
                        status = "in_progress",
                        notes = "التركيز على قواعد الاشتقاق الأساسية وحل التمارين الهندسية."
                    )
                )
                lessonDao.insertLesson(
                    LessonEntity(
                        subjectId = physicsId,
                        title = "الميكانيكا الكلاسيكية وقوانين الحركة",
                        status = "needs_revision",
                        nextRevisionDate = System.currentTimeMillis() + 86400000L,
                        notes = "مراجعة قانون نيوتن الثاني وتطبيقات الاحتكاك."
                    )
                )

                val taskDao = db.taskDao()
                taskDao.insertTask(
                    TaskEntity(
                        subjectId = mathId,
                        title = "حل المسائل من 12 إلى 24 في كراسة التمارين",
                        dueDate = System.currentTimeMillis() + 172800000L,
                        priority = "HIGH",
                        status = "PENDING",
                        notes = "تسليم الحلول مع الخطوات التفصيلية."
                    )
                )

                val examDao = db.examDao()
                examDao.insertExam(
                    ExamEntity(
                        subjectId = mathId,
                        title = "الاختبار النصفي لمادة التحليل",
                        examDate = System.currentTimeMillis() + (86400000L * 7),
                        linkedLessonIdsJson = "[]",
                        readinessLevel = "MEDIUM",
                        notes = "يشمل الفصول الثلاثة الأولى."
                    )
                )

                val challengeDao = db.challengeDao()
                challengeDao.insertChallenge(
                    ChallengeEntity(
                        title = "جلسة تركيز 25 دقيقة",
                        description = "أكمل 25 دقيقة من التركيز الفعلي لبناء عادة دراسية راسخة.",
                        targetType = "FOCUS_MINUTES",
                        targetValue = 25,
                        currentValue = 0,
                        isPredefined = true,
                        rewardBadge = "🎯"
                    )
                )
                challengeDao.insertChallenge(
                    ChallengeEntity(
                        title = "إنجاز جلستي تركيز مكتملتين",
                        description = "أكمل جلستين دراسيتين كاملتين بنجاح دون مقاطعة.",
                        targetType = "FOCUS_SESSIONS",
                        targetValue = 2,
                        currentValue = 0,
                        isPredefined = true,
                        rewardBadge = "⏳"
                    )
                )
                challengeDao.insertChallenge(
                    ChallengeEntity(
                        title = "ماراثون 50 دقيقة تركيز",
                        description = "أنجز ما مجموعه 50 دقيقة من التركيز العميق خلال جلساتك.",
                        targetType = "FOCUS_MINUTES",
                        targetValue = 50,
                        currentValue = 0,
                        isPredefined = true,
                        rewardBadge = "🔥"
                    )
                )
                challengeDao.insertChallenge(
                    ChallengeEntity(
                        title = "إكمال 4 جلسات تركيز",
                        description = "أنجز 4 جلسات دراسية مكتملة لتحقيق أعلى مستويات الانضباط.",
                        targetType = "FOCUS_SESSIONS",
                        targetValue = 4,
                        currentValue = 0,
                        isPredefined = true,
                        rewardBadge = "🏆"
                    )
                )

                val flashcardDao = db.flashcardDao()
                flashcardDao.insertFlashcard(
                    FlashcardEntity(
                        subjectId = mathId,
                        question = "ما هي قاعدة مشتقة حاصل ضرب دالتين (Product Rule)؟",
                        answer = "(u · v)' = u' · v + u · v'\nمشتقة الأولى في الثانية + الأولى في مشتقة الثانية.",
                        memoryLevel = "MEDIUM"
                    )
                )
                flashcardDao.insertFlashcard(
                    FlashcardEntity(
                        subjectId = physicsId,
                        question = "ما هو نص قانون نيوتن الثاني للحركة؟",
                        answer = "القوة المحصلة المؤثرة على جسم تساوي حاصل ضرب كتلته في تسارعه:\nF = m · a",
                        memoryLevel = "EASY"
                    )
                )
            }
        }
    }
}
