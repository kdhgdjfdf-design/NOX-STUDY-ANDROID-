package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.badges.BadgeEvaluator
import com.example.data.model.FocusSessionEntity
import com.example.data.model.SubjectEntity
import com.example.data.model.TaskEntity
import com.example.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BadgesAndDatabaseMigrationTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var prefsRepo: UserPreferencesRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        prefsRepo = UserPreferencesRepository(context)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `badges are not unlocked without real user activity and never grant twice`() = runTest {
        // Fresh database: zero badges unlocked
        val initialUnlocked = BadgeEvaluator.evaluateAll(context, db, prefsRepo)
        assertTrue(initialUnlocked.isEmpty())

        // Incomplete or <60s session must NOT unlock first_step
        db.focusDao().insertSession(
            FocusSessionEntity(mode = "POMODORO", targetMinutes = 25, actualDurationSeconds = 1200, completed = false)
        )
        db.focusDao().insertSession(
            FocusSessionEntity(mode = "POMODORO", targetMinutes = 25, actualDurationSeconds = 40, completed = true)
        )
        assertTrue(BadgeEvaluator.evaluateAll(context, db, prefsRepo).isEmpty())

        // Insert 1 real completed session -> unlocks ONLY first_step
        db.focusDao().insertSession(
            FocusSessionEntity(mode = "POMODORO", targetMinutes = 25, actualDurationSeconds = 1500, completed = true)
        )
        val firstUnlock = BadgeEvaluator.evaluateAll(context, db, prefsRepo)
        assertEquals(listOf("first_step"), firstUnlock.map { it.id })

        val savedBadge = db.badgeDao().getBadgeById("first_step")
        assertNotNull(savedBadge)
        assertTrue(savedBadge!!.isUnlocked)
        val originalTimestamp = savedBadge.unlockedAt

        // Second pass must NOT re-grant or change unlockedAt
        val duplicatePass = BadgeEvaluator.evaluateAll(context, db, prefsRepo)
        assertTrue(duplicatePass.isEmpty())
        assertEquals(originalTimestamp, db.badgeDao().getBadgeById("first_step")?.unlockedAt)

        // Complete 5 tasks -> unlocks task_conqueror
        val subjectId = db.subjectDao().insertSubject(SubjectEntity(name = "Math"))
        repeat(5) { idx ->
            db.taskDao().insertTask(
                TaskEntity(
                    subjectId = subjectId,
                    title = "Task $idx",
                    dueDate = System.currentTimeMillis(),
                    status = "COMPLETED"
                )
            )
        }
        val taskUnlock = BadgeEvaluator.evaluateAll(context, db, prefsRepo)
        assertEquals(listOf("task_conqueror"), taskUnlock.map { it.id })

        // Verify UI model progress reflects unlocked + locked accurately
        val uiModels = BadgeEvaluator.buildUiModels(context, db, prefsRepo, db.badgeDao().getAllBadgesList())
        val firstStepUi = uiModels.first { it.definition.id == "first_step" }
        val disciplinedUi = uiModels.first { it.definition.id == "disciplined_mind" }
        assertTrue(firstStepUi.isUnlocked)
        assertEquals(1f, firstStepUi.progressFraction)
        assertFalse(disciplinedUi.isUnlocked)
        assertEquals(1, disciplinedUi.currentValue)
        assertEquals(5, disciplinedUi.definition.targetValue)
    }

    @Test
    fun `migration 1 to 2 preserves existing v1 user data and creates badges table`() = runTest {
        val dbName = "migration_test_nox.db"
        context.deleteDatabase(dbName)

        // Create a genuine version 1 SQLite schema & insert pre-existing subject + session data
        val factory = FrameworkSQLiteOpenHelperFactory()
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `subjects` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `colorHex` TEXT NOT NULL, `iconName` TEXT NOT NULL, `imageUri` TEXT, `createdAt` INTEGER NOT NULL)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `lessons` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `subjectId` INTEGER NOT NULL, `title` TEXT NOT NULL, `status` TEXT NOT NULL, `addedAt` INTEGER NOT NULL, `nextRevisionDate` INTEGER, `notes` TEXT NOT NULL, `attachmentsJson` TEXT NOT NULL, `reminderEnabled` INTEGER NOT NULL, FOREIGN KEY(`subjectId`) REFERENCES `subjects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_lessons_subjectId` ON `lessons` (`subjectId`)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `tasks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `subjectId` INTEGER NOT NULL, `title` TEXT NOT NULL, `dueDate` INTEGER NOT NULL, `priority` TEXT NOT NULL, `status` TEXT NOT NULL, `notes` TEXT NOT NULL, `attachmentsJson` TEXT NOT NULL, `reminderMinutesBefore` INTEGER NOT NULL, FOREIGN KEY(`subjectId`) REFERENCES `subjects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_subjectId` ON `tasks` (`subjectId`)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `exams` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `subjectId` INTEGER NOT NULL, `title` TEXT NOT NULL, `examDate` INTEGER NOT NULL, `linkedLessonIdsJson` TEXT NOT NULL, `readinessLevel` TEXT NOT NULL, `notes` TEXT NOT NULL, `attachmentsJson` TEXT NOT NULL, `reminderDate` INTEGER, FOREIGN KEY(`subjectId`) REFERENCES `subjects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_exams_subjectId` ON `exams` (`subjectId`)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `flashcards` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `subjectId` INTEGER NOT NULL, `lessonId` INTEGER, `question` TEXT NOT NULL, `answer` TEXT NOT NULL, `imageUri` TEXT, `memoryLevel` TEXT NOT NULL, `lastReviewedAt` INTEGER NOT NULL, FOREIGN KEY(`subjectId`) REFERENCES `subjects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_flashcards_subjectId` ON `flashcards` (`subjectId`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_flashcards_lessonId` ON `flashcards` (`lessonId`)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `focus_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `mode` TEXT NOT NULL, `targetMinutes` INTEGER NOT NULL, `actualDurationSeconds` INTEGER NOT NULL, `completed` INTEGER NOT NULL, `dateTimestamp` INTEGER NOT NULL, `interruptedCount` INTEGER NOT NULL)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `challenges` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `targetType` TEXT NOT NULL, `targetValue` INTEGER NOT NULL, `currentValue` INTEGER NOT NULL, `deadline` INTEGER, `isCompleted` INTEGER NOT NULL, `isPredefined` INTEGER NOT NULL, `rewardBadge` TEXT NOT NULL, `completedAt` INTEGER)")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val v1Helper = factory.create(config)
        val v1Db = v1Helper.writableDatabase
        v1Db.execSQL("INSERT INTO subjects (id, name, colorHex, iconName, createdAt) VALUES (1, 'الكيمياء', '#8A5836', 'science', 1000)")
        v1Db.execSQL("INSERT INTO focus_sessions (id, mode, targetMinutes, actualDurationSeconds, completed, dateTimestamp, interruptedCount) VALUES (1, 'POMODORO', 25, 1500, 1, 2000, 0)")
        v1Db.close()
        v1Helper.close()

        // Reopen using Room v2 with MIGRATION_1_2
        val migratedDb = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()

        val subjects = migratedDb.subjectDao().getAllSubjects().first()
        assertEquals(1, subjects.size)
        assertEquals("الكيمياء", subjects.first().name)

        // Evaluating badges on migrated data unlocks first_step seamlessly
        val unlocked = BadgeEvaluator.evaluateAll(context, migratedDb, prefsRepo)
        assertEquals(listOf("first_step"), unlocked.map { it.id })

        migratedDb.close()
        context.deleteDatabase(dbName)
    }
}
