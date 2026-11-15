package com.example.ui.screens.flashcards

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.ButtonDefaults
import com.example.R
import com.example.data.model.FlashcardEntity
import com.example.data.model.SubjectEntity
import com.example.ui.theme.RedExit
import com.example.ui.viewmodel.FlashcardsViewModel
import com.example.ui.viewmodel.StudyOrgViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardsScreen(
    flashcardsViewModel: FlashcardsViewModel,
    studyOrgViewModel: StudyOrgViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val allCards by flashcardsViewModel.allFlashcards.collectAsState()
    val subjects by studyOrgViewModel.subjects.collectAsState()
    val selectedSubjectId by flashcardsViewModel.selectedSubjectFilter.collectAsState()
    val currentIndex by flashcardsViewModel.currentCardIndex.collectAsState()
    val isFlipped by flashcardsViewModel.isFlipped.collectAsState()

    val filteredCards = if (selectedSubjectId == null) {
        allCards
    } else {
        allCards.filter { it.subjectId == selectedSubjectId }
    }

    val safeIndex = if (filteredCards.isNotEmpty()) currentIndex.coerceIn(0, filteredCards.size - 1) else 0
    val currentCard = filteredCards.getOrNull(safeIndex)

    var showAddCardDialog by remember { mutableStateOf(false) }
    var cardPendingDelete by remember { mutableStateOf<FlashcardEntity?>(null) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "بطاقات المراجعة 🗂️",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(onClick = { showAddCardDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Card", tint = MaterialTheme.colorScheme.primary)
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddCardDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_flashcard_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Card")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Subject Filter Bar
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "تصفية حسب المادة:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedSubjectId == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { flashcardsViewModel.setSubjectFilter(null) }
                        ) {
                            Text(
                                text = "الكل (${allCards.size})",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (selectedSubjectId == null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    items(subjects) { subject ->
                        val count = allCards.count { it.subjectId == subject.id }
                        val isSelected = selectedSubjectId == subject.id
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { flashcardsViewModel.setSubjectFilter(subject.id) }
                        ) {
                            Text(
                                text = "${subject.name} ($count)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Flashcard Area
            if (filteredCards.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Style,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "لا توجد بطاقات مراجعة مضافة",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { showAddCardDialog = true }) {
                            Text("أنشئ بطاقتك الأولى الآن ✨")
                        }
                    }
                }
            } else if (currentCard != null) {
                // Card Counter & Live Review Stats
                val reviewedInFilter = filteredCards.count { it.lastReviewedAt > 0 }
                Row(
                    modifier = Modifier.fillMaxWidth(0.92f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "بطاقة ${safeIndex + 1} من ${filteredCards.size}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "تمت مراجعة $reviewedInFilter / ${filteredCards.size}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3D Flip Card (moderately scaled for comfortable reading)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Interactive3DFlipCard(
                        card = currentCard,
                        isFlipped = isFlipped,
                        onFlip = { flashcardsViewModel.flipCard() },
                        onRequestDelete = { cardPendingDelete = currentCard },
                        onMemoryLevelSelect = { level ->
                            flashcardsViewModel.updateMemoryLevel(currentCard, level)
                        },
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .height(310.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Navigation controls between cards
                Row(
                    modifier = Modifier.fillMaxWidth(0.8f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = { flashcardsViewModel.prevCard(filteredCards.size) },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("السابق")
                    }

                    // Flip hint button
                    IconButton(
                        onClick = { flashcardsViewModel.flipCard() },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(Icons.Default.Flip, contentDescription = "Flip", tint = MaterialTheme.colorScheme.primary)
                    }

                    FilledTonalButton(
                        onClick = { flashcardsViewModel.nextCard(filteredCards.size) },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("التالي")
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showAddCardDialog) {
        AddFlashcardDialog(
            subjects = subjects,
            onDismiss = { showAddCardDialog = false },
            onConfirm = { subjectId, question, answer, imgUri ->
                flashcardsViewModel.addFlashcard(
                    subjectId = subjectId,
                    lessonId = null,
                    question = question,
                    answer = answer,
                    imageUri = imgUri
                )
                showAddCardDialog = false
            }
        )
    }

    cardPendingDelete?.let { cardToDelete ->
        AlertDialog(
            onDismissRequest = { cardPendingDelete = null },
            title = {
                Text(
                    text = "هل تريد حذف بطاقة المراجعة هذه؟",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "سيتم حذف هذه البطاقة فقط من القائمة دون حذف مجلد المادة أو أي بطاقات أخرى أو ملفات من الهاتف.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        flashcardsViewModel.deleteFlashcard(cardToDelete, filteredCards.size)
                        cardPendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedExit),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("confirm_delete_flashcard_button")
                ) {
                    Text("حذف", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { cardPendingDelete = null },
                    modifier = Modifier.testTag("cancel_delete_flashcard_button")
                ) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun Interactive3DFlipCard(
    card: FlashcardEntity,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    onRequestDelete: () -> Unit,
    onMemoryLevelSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(450),
        label = "Card3DFlip"
    )

    Card(
        modifier = modifier
            .testTag("flashcard_3d_card")
            .clip(RoundedCornerShape(22.dp))
            .clickable { onFlip() }
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 14f * density
            }
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(22.dp)
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (rotation <= 90f) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        if (rotation <= 90f) {
            // Front Side (Question)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Tag with optional recall status indicator & neat trash icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "السؤال",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        if (card.lastReviewedAt > 0) {
                            val (lvlText, lvlColor) = when (card.memoryLevel) {
                                "EASY" -> "سهل" to Color(0xFF427A4E)
                                "HARD" -> "صعب" to Color(0xFFC04B37)
                                else -> "متوسط" to Color(0xFFD99B26)
                            }
                            Surface(
                                color = lvlColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "التذكّر: $lvlText",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    color = lvlColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onRequestDelete,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("delete_flashcard_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "حذف البطاقة",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Question Text
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = card.question,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            lineHeight = 26.sp
                        ),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Image if attached
                if (card.imageUri != null) {
                    AsyncImage(
                        model = card.imageUri,
                        contentDescription = "Card Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Bottom Hint
                Text(
                    text = "انقر لقلب البطاقة وإظهار الإجابة 🔄",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            // Back Side (Answer + Recall Header)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f } // Counter-rotate so text is normal
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Elegant "التذكّر" Bar (Top of Card with Easy, Medium, Hard options)
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "التذكّر:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            RecallButton(
                                label = "سهل",
                                isSelected = card.memoryLevel == "EASY",
                                color = Color(0xFF427A4E),
                                onClick = { onMemoryLevelSelect("EASY") }
                            )
                            RecallButton(
                                label = "متوسط",
                                isSelected = card.memoryLevel == "MEDIUM",
                                color = Color(0xFFD99B26),
                                onClick = { onMemoryLevelSelect("MEDIUM") }
                            )
                            RecallButton(
                                label = "صعب",
                                isSelected = card.memoryLevel == "HARD",
                                color = Color(0xFFC04B37),
                                onClick = { onMemoryLevelSelect("HARD") }
                            )
                        }
                    }
                }

                // Answer Text
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = card.answer,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Normal,
                            fontSize = 18.sp,
                            lineHeight = 26.sp
                        ),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Bottom Hint
                Text(
                    text = "انقر للعودة إلى السؤال",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun RecallButton(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) color else color.copy(alpha = 0.15f),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = if (isSelected) Color.White else color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun AddFlashcardDialog(
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onConfirm: (subjectId: Long, question: String, answer: String, imageUri: String?) -> Unit
) {
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var selectedSubjectId by remember { mutableStateOf(subjects.firstOrNull()?.id ?: 0L) }
    var imageUri by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        imageUri = uri?.toString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إنشاء بطاقة مراجعة جديدة 🗂️") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("السؤال / المصطلح") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("flashcard_question_input")
                )

                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    label = { Text("الإجابة / الشرح") },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth().testTag("flashcard_answer_input")
                )

                // Optional Image
                OutlinedButton(
                    onClick = { photoPicker.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (imageUri != null) "تم إرفاق صورة توضيحية" else "إضافة صورة توضيحية (اختياري)")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (question.isNotBlank() && answer.isNotBlank() && selectedSubjectId > 0) {
                        onConfirm(selectedSubjectId, question, answer, imageUri)
                    }
                },
                modifier = Modifier.testTag("save_flashcard_button")
            ) {
                Text("حفظ البطاقة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
