package com.example.ui.screens.study

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ExamEntity
import com.example.data.model.LessonEntity
import com.example.data.model.SubjectEntity
import com.example.data.model.TaskEntity
import com.example.ui.viewmodel.StudyOrgViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyOrgScreen(
    studyOrgViewModel: StudyOrgViewModel,
    onNavigateToSubjectDetail: (SubjectEntity) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("الدروس والمراجعة", "الواجبات والمهام", "الاختبارات")

    val subjects by studyOrgViewModel.subjects.collectAsState()
    val allLessons by studyOrgViewModel.allLessons.collectAsState()
    val allTasks by studyOrgViewModel.allTasks.collectAsState()
    val allExams by studyOrgViewModel.allExams.collectAsState()

    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddExamDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تنظيم الدراسة 📚",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (selectedTabIndex) {
                        0 -> showAddSubjectDialog = true
                        1 -> showAddTaskDialog = true
                        2 -> showAddExamDialog = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("study_org_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (selectedTabIndex) {
                // Tab 1: Subjects & Lessons
                0 -> {
                    SubjectsTabContent(
                        subjects = subjects,
                        allLessons = allLessons,
                        onSubjectClick = { subject -> onNavigateToSubjectDetail(subject) },
                        onAddSubject = { showAddSubjectDialog = true }
                    )
                }
                // Tab 2: Tasks & Assignments
                1 -> {
                    TasksTabContent(
                        tasks = allTasks,
                        subjects = subjects,
                        studyOrgViewModel = studyOrgViewModel
                    )
                }
                // Tab 3: Exams
                2 -> {
                    ExamsTabContent(
                        exams = allExams,
                        subjects = subjects,
                        allLessons = allLessons,
                        studyOrgViewModel = studyOrgViewModel
                    )
                }
            }
        }
    }

    if (showAddSubjectDialog) {
        AddSubjectDialog(
            onDismiss = { showAddSubjectDialog = false },
            onConfirm = { name, colorHex ->
                studyOrgViewModel.addSubject(name, colorHex, "book")
                showAddSubjectDialog = false
            }
        )
    }

    if (showAddTaskDialog) {
        AddTaskDialog(
            subjects = subjects,
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { subjectId, title, dueDate, priority, reminderMin, notes ->
                studyOrgViewModel.addTask(
                    subjectId = subjectId,
                    title = title,
                    dueDate = dueDate,
                    priority = priority,
                    status = "PENDING",
                    notes = notes,
                    attachmentsJson = "[]",
                    reminderMinutesBefore = reminderMin
                )
                showAddTaskDialog = false
            }
        )
    }

    if (showAddExamDialog) {
        AddExamDialog(
            subjects = subjects,
            allLessons = allLessons,
            onDismiss = { showAddExamDialog = false },
            onConfirm = { subjectId, title, examDate, readiness, linkedLessonsJson, reminderDate, notes ->
                studyOrgViewModel.addExam(
                    subjectId = subjectId,
                    title = title,
                    examDate = examDate,
                    linkedLessonIdsJson = linkedLessonsJson,
                    readinessLevel = readiness,
                    notes = notes,
                    attachmentsJson = "[]",
                    reminderDate = reminderDate
                )
                showAddExamDialog = false
            }
        )
    }
}

@Composable
private fun SubjectsTabContent(
    subjects: List<SubjectEntity>,
    allLessons: List<LessonEntity>,
    onSubjectClick: (SubjectEntity) -> Unit,
    onAddSubject: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "المواد الدراسية (انقر لدخول المادة وإدارة الدروس)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(subjects) { subject ->
            val subjectLessons = allLessons.filter { it.subjectId == subject.id }
            val revisionsDue = subjectLessons.count { it.status == "needs_revision" }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onSubjectClick(subject) }
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        RoundedCornerShape(20.dp)
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(subject.colorHex)).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = Color(android.graphics.Color.parseColor(subject.colorHex)),
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = subject.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "${subjectLessons.size} دروس",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (revisionsDue > 0) {
                                Text(
                                    text = "$revisionsDue مراجعات مستحقة",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFC04B37)
                                )
                            }
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onAddSubject,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("إضافة مادة دراسية جديدة")
            }
        }
    }
}

@Composable
private fun TasksTabContent(
    tasks: List<TaskEntity>,
    subjects: List<SubjectEntity>,
    studyOrgViewModel: StudyOrgViewModel
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }

    if (tasks.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "لا توجد واجبات أو مهام حالياً ✨",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(tasks) { task ->
                val subject = subjects.find { it.id == task.subjectId }
                val isCompleted = task.status == "COMPLETED"

                val (pColor, pText) = when (task.priority) {
                    "HIGH" -> Color(0xFFC04B37) to "أولوية عالية"
                    "LOW" -> Color(0xFF4A7C59) to "أولوية منخفضة"
                    else -> Color(0xFFD99B26) to "أولوية متوسطة"
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = subject?.name ?: "مادة عامة",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = pColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = pText,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = pColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                textDecoration = if (isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                            ),
                            color = if (isCompleted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                        )

                        if (task.notes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = task.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "التسليم: ${dateFormat.format(Date(task.dueDate))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row {
                                IconButton(
                                    onClick = {
                                        val newStatus = if (isCompleted) "PENDING" else "COMPLETED"
                                        studyOrgViewModel.updateTask(task.copy(status = newStatus))
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Check,
                                        contentDescription = "Toggle Complete",
                                        tint = if (isCompleted) Color(0xFF427A4E) else MaterialTheme.colorScheme.outline
                                    )
                                }

                                IconButton(
                                    onClick = { studyOrgViewModel.deleteTask(task) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color(0xFFBA1A1A),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExamsTabContent(
    exams: List<ExamEntity>,
    subjects: List<SubjectEntity>,
    allLessons: List<LessonEntity>,
    studyOrgViewModel: StudyOrgViewModel
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }

    if (exams.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "لا توجد اختبارات مجدولة حالياً 🎓",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(exams) { exam ->
                val subject = subjects.find { it.id == exam.subjectId }
                val (readinessText, readinessPct, readinessColor) = when (exam.readinessLevel) {
                    "HIGH" -> Triple("مستعد تماماً", 0.9f, Color(0xFF427A4E))
                    "LOW" -> Triple("منخفض", 0.3f, Color(0xFFC04B37))
                    else -> Triple("متوسط", 0.6f, Color(0xFFD99B26))
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = subject?.name ?: "مادة عامة",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "📅 ${dateFormat.format(Date(exam.examDate))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = exam.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Readiness indicator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مستوى الاستعداد: $readinessText",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(
                                onClick = { studyOrgViewModel.deleteExam(exam) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color(0xFFBA1A1A),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { readinessPct },
                            color = readinessColor,
                            trackColor = readinessColor.copy(alpha = 0.2f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddSubjectDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, colorHex: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#8A5836") }
    val presetColors = listOf("#8A5836", "#5F6B55", "#B86D49", "#4A7C9B", "#7B5E7B", "#5C6B73")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة مادة دراسية جديدة") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم المادة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("اختر لون المادة المفضل:", style = MaterialTheme.typography.bodySmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    presetColors.forEach { hex ->
                        val isSelected = selectedColor == hex
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(hex)))
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) onConfirm(name, selectedColor)
                }
            ) {
                Text("إضافة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onConfirm: (subjectId: Long, title: String, dueDate: Long, priority: String, reminderMin: Int, notes: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedSubjectId by remember { mutableStateOf(subjects.firstOrNull()?.id ?: 0L) }
    var priority by remember { mutableStateOf("MEDIUM") }
    var reminderMin by remember { mutableStateOf(60) }
    var dueDate by remember { mutableStateOf(System.currentTimeMillis() + 86400000L) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة واجب أو مهمة 📝") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان المهمة أو الواجب") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات إضافية") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                // Priority selector
                Text("الأولوية:", style = MaterialTheme.typography.bodySmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("HIGH" to "عالية", "MEDIUM" to "متوسطة", "LOW" to "منخفضة").forEach { (p, label) ->
                        val selected = priority == p
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { priority = p }
                        ) {
                            Text(
                                text = label,
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && selectedSubjectId > 0) {
                        onConfirm(selectedSubjectId, title, dueDate, priority, reminderMin, notes)
                    }
                }
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun AddExamDialog(
    subjects: List<SubjectEntity>,
    allLessons: List<LessonEntity>,
    onDismiss: () -> Unit,
    onConfirm: (subjectId: Long, title: String, examDate: Long, readiness: String, linkedLessonsJson: String, reminderDate: Long?, notes: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedSubjectId by remember { mutableStateOf(subjects.firstOrNull()?.id ?: 0L) }
    var readiness by remember { mutableStateOf("MEDIUM") }
    var examDate by remember { mutableStateOf(System.currentTimeMillis() + (86400000L * 7)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة موعد اختبار 🎓") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("اسم أو وصف الاختبار") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Readiness selector
                Text("مستوى الاستعداد الحالي:", style = MaterialTheme.typography.bodySmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("LOW" to "منخفض", "MEDIUM" to "متوسط", "HIGH" to "مستعد").forEach { (r, label) ->
                        val selected = readiness == r
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { readiness = r }
                        ) {
                            Text(
                                text = label,
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Suggest linking lessons button
                val subjectLessonsCount = allLessons.count { it.subjectId == selectedSubjectId }
                OutlinedButton(
                    onClick = { /* Auto-linked */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("اقتراح ربط $subjectLessonsCount دروس من نفس المادة")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && selectedSubjectId > 0) {
                        onConfirm(
                            selectedSubjectId,
                            title,
                            examDate,
                            readiness,
                            "[]",
                            examDate - 86400000L,
                            notes
                        )
                    }
                }
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
