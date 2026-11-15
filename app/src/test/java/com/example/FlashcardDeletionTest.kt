package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.model.FlashcardEntity
import com.example.data.model.SubjectEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FlashcardDeletionTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `deleting a flashcard removes only that card and preserves siblings subject and disk files`() = runTest {
        val tempDeviceFile = File(context.cacheDir, "original_study_note_image.png").apply {
            writeText("mock-image-content")
        }

        val subjectId = db.subjectDao().insertSubject(
            SubjectEntity(name = "الفيزياء", colorHex = "#5F6B55", iconName = "science")
        )
        val c1Id = db.flashcardDao().insertFlashcard(
            FlashcardEntity(
                subjectId = subjectId,
                question = "السؤال الأول",
                answer = "الإجابة الأولى",
                imageUri = tempDeviceFile.toURI().toString(),
                memoryLevel = "EASY",
                lastReviewedAt = 1000L
            )
        )
        val c2Id = db.flashcardDao().insertFlashcard(
            FlashcardEntity(
                subjectId = subjectId,
                question = "السؤال الثاني",
                answer = "الإجابة الثانية",
                memoryLevel = "MEDIUM",
                lastReviewedAt = 2000L
            )
        )
        val c3Id = db.flashcardDao().insertFlashcard(
            FlashcardEntity(
                subjectId = subjectId,
                question = "السؤال الثالث",
                answer = "الإجابة الثالثة",
                memoryLevel = "HARD",
                lastReviewedAt = 0L
            )
        )

        val before = db.flashcardDao().getAllFlashcards().first()
        assertEquals(3, before.size)

        // Delete ONLY c1
        val targetCard = before.first { it.id == c1Id }
        db.flashcardDao().deleteFlashcard(targetCard)

        val after = db.flashcardDao().getAllFlashcards().first()
        assertEquals(2, after.size)
        assertEquals(setOf(c2Id, c3Id), after.map { it.id }.toSet())

        // Parent subject remains untouched
        val subject = db.subjectDao().getSubjectById(subjectId)
        assertNotNull(subject)
        assertEquals("الفيزياء", subject?.name)

        // Original file on disk remains untouched
        assertTrue(tempDeviceFile.exists())
        tempDeviceFile.delete()
    }
}
