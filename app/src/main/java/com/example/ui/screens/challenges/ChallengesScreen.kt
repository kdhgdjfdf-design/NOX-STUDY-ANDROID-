package com.example.ui.screens.challenges

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChallengeEntity
import com.example.ui.viewmodel.ChallengesViewModel

@Composable
fun ChallengesScreen(
    challengesViewModel: ChallengesViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val challenges by challengesViewModel.challenges.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var completedChallengeToCelebrate by remember { mutableStateOf<ChallengeEntity?>(null) }

    LaunchedEffect(Unit) {
        challengesViewModel.completedChallengeEvent.collect { challenge ->
            completedChallengeToCelebrate = challenge
        }
    }

    val predefinedList = challenges.filter { it.isPredefined }
    val customList = challenges.filter { !it.isPredefined }

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
                        text = "التحديات الفردية 🏆",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "New Challenge", tint = MaterialTheme.colorScheme.primary)
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_challenge_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Challenge")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header info
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFFD99B26),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "تحديات مصممة لبناء الاستمرارية ومكافأتك بشارات إنجاز أنيقة دون مبالغة في النقاط.",
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Section 1: Predefined challenges
            item {
                Text(
                    text = "تحديات NOX الجاهزة",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            items(predefinedList) { challenge ->
                ChallengeCardItem(
                    challenge = challenge,
                    onDelete = null
                )
            }

            // Section 2: Custom challenges
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تحدياتي الخاصة",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = { showAddDialog = true }) {
                        Text("إنشاء تحدٍ جديد +")
                    }
                }
            }

            if (customList.isEmpty()) {
                item {
                    Text(
                        text = "لم تنشئ أي تحدٍ خاص بك حتى الآن.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(customList) { challenge ->
                    ChallengeCardItem(
                        challenge = challenge,
                        onDelete = { challengesViewModel.deleteChallenge(challenge) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddChallengeDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, desc, targetType, targetVal, days ->
                challengesViewModel.addCustomChallenge(title, desc, targetType, targetVal, days)
                showAddDialog = false
            }
        )
    }

    completedChallengeToCelebrate?.let { challenge ->
        AlertDialog(
            onDismissRequest = { completedChallengeToCelebrate = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(challenge.rewardBadge, fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("تهانينا! أكملت التحدي 🌟", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "لقد نجحت في إنجاز تحدي: «${challenge.title}»",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "حصلت على شارة التميز وأثبتّ التزامك الرائع بالدراسة الهادئة.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { completedChallengeToCelebrate = null }) {
                    Text("رائع! شكرًا")
                }
            }
        )
    }
}

@Composable
fun ChallengeCardItem(
    challenge: ChallengeEntity,
    onDelete: (() -> Unit)?
) {
    val progress = (challenge.currentValue.toFloat() / challenge.targetValue.toFloat()).coerceIn(0f, 1f)

    val progressDisplay = if (challenge.isCompleted) {
        "مكتمل 🏆"
    } else {
        when (challenge.targetType) {
            "FOCUS_MINUTES" -> "${challenge.currentValue}/${challenge.targetValue} دقيقة"
            "FOCUS_SESSIONS" -> "${challenge.currentValue}/${challenge.targetValue} جلسة"
            else -> "${challenge.currentValue}/${challenge.targetValue}"
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            width = if (challenge.isCompleted) 1.5.dp else 1.dp,
            color = if (challenge.isCompleted) Color(0xFF4C8C56) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (challenge.isCompleted) Color(0xFFDFF0E2) else Color(0xFFFAF0E3)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = challenge.rewardBadge, fontSize = 22.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = challenge.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = challenge.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (onDelete != null) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFBA1A1A), modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress bar and counter
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = progressDisplay,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (challenge.isCompleted) Color(0xFF4C8C56) else MaterialTheme.colorScheme.primary
                )

                Text(
                    text = if (challenge.isCompleted) "شارة مستحقة ✨" else "تحديث تلقائي مع الجلسات ⏱️",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (challenge.isCompleted) Color(0xFF4C8C56) else MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { progress },
                color = if (challenge.isCompleted) Color(0xFF4C8C56) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )
        }
    }
}

@Composable
fun AddChallengeDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, desc: String, targetType: String, targetVal: Int, days: Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var targetValueStr by remember { mutableStateOf("25") }
    var targetType by remember { mutableStateOf("FOCUS_MINUTES") }
    var daysStr by remember { mutableStateOf("7") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إنشاء تحدٍ دراسي ذكي 🎯") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("اسم التحدي") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("الوصف أو الحافز") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                // Select target type: Minutes vs Sessions
                Text(
                    text = "نوع الهدف:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "FOCUS_MINUTES" to "دقائق تركيز",
                        "FOCUS_SESSIONS" to "جلسات مكتملة"
                    ).forEach { (type, label) ->
                        val isSelected = targetType == type
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    targetType = type
                                    if (type == "FOCUS_MINUTES" && targetValueStr == "2") {
                                        targetValueStr = "25"
                                    } else if (type == "FOCUS_SESSIONS" && targetValueStr == "25") {
                                        targetValueStr = "2"
                                    }
                                }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = targetValueStr,
                        onValueChange = { targetValueStr = it },
                        label = {
                            Text(if (targetType == "FOCUS_MINUTES") "الهدف (دقائق)" else "الهدف (جلسات)")
                        },
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = daysStr,
                        onValueChange = { daysStr = it },
                        label = { Text("المدة (بالأيام)") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val targetVal = targetValueStr.toIntOrNull() ?: 25
                    val days = daysStr.toIntOrNull() ?: 7
                    if (title.isNotBlank()) {
                        onConfirm(title, description, targetType, targetVal, days)
                    }
                }
            ) {
                Text("بدء التحدي")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
