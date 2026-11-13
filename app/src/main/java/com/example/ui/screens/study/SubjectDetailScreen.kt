package com.example.ui.screens.study

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.LessonEntity
import com.example.data.model.SubjectEntity
import com.example.ui.viewmodel.StudyOrgViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailScreen(
    subject: SubjectEntity,
    studyOrgViewModel: StudyOrgViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val allLessons by studyOrgViewModel.allLessons.collectAsState()
    val subjectLessons = allLessons.filter { it.subjectId == subject.id }
    val dueRevisionsCount = subjectLessons.count { it.status == "needs_revision" }

    var showAddLessonDialog by remember { mutableStateOf(false) }
    var lessonToEdit by remember { mutableStateOf<LessonEntity?>(null) }
    var lessonToDelete by remember { mutableStateOf<LessonEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddLessonDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_lesson_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Lesson")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Wide header image with subject name and stats
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.cozy_study_org),
                        contentDescription = "Subject Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x991C1613),
                                        Color(0xEE1C1613)
                                    )
                                )
                            )
                    )

                    // Back button & Title
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0x4D000000))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }

                        Column {
                            Text(
                                text = subject.name,
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text(
                                    text = "📚 ${subjectLessons.size} دروس",
                                    color = Color(0xFFFFD4A8),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "⏳ $dueRevisionsCount مراجعات مستحقة",
                                    color = Color(0xFFFFB4AB),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Section title
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "قائمة الدروس والمراجعات",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Empty state
            if (subjectLessons.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "لا توجد دروس مضافة بعد في هذه المادة",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { showAddLessonDialog = true }) {
                            Text("اضغط لإضافة الدرس الأول 📝")
                        }
                    }
                }
            } else {
                items(subjectLessons) { lesson ->
                    LessonCardItem(
                        lesson = lesson,
                        onEdit = { lessonToEdit = lesson },
                        onDelete = { lessonToDelete = lesson },
                        onStatusChange = { newStatus ->
                            studyOrgViewModel.updateLesson(lesson.copy(status = newStatus))
                        }
                    )
                }
            }
        }
    }

    if (showAddLessonDialog) {
        AddEditLessonDialog(
            subjectId = subject.id,
            existingLesson = null,
            onDismiss = { showAddLessonDialog = false },
            onSave = { title, status, notes, revDate, attachments, reminder ->
                studyOrgViewModel.addLesson(
                    subjectId = subject.id,
                    title = title,
                    status = status,
                    notes = notes,
                    nextRevisionDate = revDate,
                    attachmentsJson = attachments,
                    reminderEnabled = reminder
                )
                showAddLessonDialog = false
            }
        )
    }

    lessonToEdit?.let { lesson ->
        AddEditLessonDialog(
            subjectId = subject.id,
            existingLesson = lesson,
            onDismiss = { lessonToEdit = null },
            onSave = { title, status, notes, revDate, attachments, reminder ->
                studyOrgViewModel.updateLesson(
                    lesson.copy(
                        title = title,
                        status = status,
                        notes = notes,
                        nextRevisionDate = revDate,
                        attachmentsJson = attachments,
                        reminderEnabled = reminder
                    )
                )
                lessonToEdit = null
            }
        )
    }

    lessonToDelete?.let { lesson ->
        AlertDialog(
            onDismissRequest = { lessonToDelete = null },
            title = { Text("حذف الدرس") },
            text = { Text("هل أنت متأكد من حذف درس: ${lesson.title}؟") },
            confirmButton = {
                Button(
                    onClick = {
                        studyOrgViewModel.deleteLesson(lesson)
                        lessonToDelete = null
                    }
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { lessonToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun LessonCardItem(
    lesson: LessonEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStatusChange: (String) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }

    val (statusLabel, statusColor, statusBg) = when (lesson.status) {
        "not_started" -> Triple("لم أبدأ", Color(0xFF6B5E55), Color(0xFFEBE4DC))
        "in_progress" -> Triple("قيد الدراسة", Color(0xFF4A7C9B), Color(0xFFDCEAF2))
        "needs_revision" -> Triple("يحتاج إلى مراجعة", Color(0xFFB85D36), Color(0xFFF9E4DB))
        else -> Triple("تمت مراجعته", Color(0xFF427A4E), Color(0xFFDFF0E2))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Title
                Text(
                    text = lesson.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                // Status chip (clickable to cycle)
                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable {
                        val nextStatus = when (lesson.status) {
                            "not_started" -> "in_progress"
                            "in_progress" -> "needs_revision"
                            "needs_revision" -> "reviewed"
                            else -> "not_started"
                        }
                        onStatusChange(nextStatus)
                    }
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (lesson.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = lesson.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Revision Date & Reminders info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (lesson.nextRevisionDate != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "مراجعة: ${dateFormat.format(Date(lesson.nextRevisionDate))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = "لا يوجد موعد مراجعة محدد",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFBA1A1A), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditLessonDialog(
    subjectId: Long,
    existingLesson: LessonEntity?,
    onDismiss: () -> Unit,
    onSave: (title: String, status: String, notes: String, revDate: Long?, attachments: String, reminder: Boolean) -> Unit
) {
    var title by remember { mutableStateOf(existingLesson?.title ?: "") }
    var notes by remember { mutableStateOf(existingLesson?.notes ?: "") }
    var status by remember { mutableStateOf(existingLesson?.status ?: "not_started") }
    var reminderEnabled by remember { mutableStateOf(existingLesson?.reminderEnabled ?: true) }
    var nextRevisionDate by remember { mutableStateOf(existingLesson?.nextRevisionDate) }
    var attachmentCount by remember { mutableStateOf(0) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        attachmentCount = uris.size
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingLesson == null) "إضافة درس جديد 📖" else "تعديل الدرس") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("اسم الدرس") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("lesson_title_input")
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات شخصية") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                // Status Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(
                        "not_started" to "لم أبدأ",
                        "in_progress" to "دراسة",
                        "needs_revision" to "مراجعة",
                        "reviewed" to "تم"
                    ).forEach { (st, label) ->
                        val selected = status == st
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { status = st }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Revision date suggestion button
                OutlinedButton(
                    onClick = {
                        // Suggest default revision: 3 days from now
                        nextRevisionDate = System.currentTimeMillis() + (86400000L * 3)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (nextRevisionDate == null) "📅 اقتراح موعد مراجعة (+3 أيام)"
                        else "📅 تم تحديد المراجعة (+3 أيام)"
                    )
                }

                // Attachments button
                OutlinedButton(
                    onClick = { filePicker.launch("*/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (attachmentCount > 0) "تم إرفاق $attachmentCount ملفات" else "إرفاق ملفات PDF أو صور")
                }

                // Reminder toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = reminderEnabled,
                        onCheckedChange = { reminderEnabled = it }
                    )
                    Text("تفعيل التنبيه والإشعار للمراجعة", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(title, status, notes, nextRevisionDate, "[]", reminderEnabled)
                    }
                },
                modifier = Modifier.testTag("save_lesson_button")
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
