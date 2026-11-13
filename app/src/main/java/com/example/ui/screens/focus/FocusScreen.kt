package com.example.ui.screens.focus

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.SquareFlipClock
import com.example.ui.theme.RedExit
import com.example.ui.viewmodel.FocusMode
import com.example.ui.viewmodel.FocusState
import com.example.ui.viewmodel.FocusViewModel
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.SessionSummaryData
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(
    focusViewModel: FocusViewModel,
    mainViewModel: MainViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val focusMode by focusViewModel.focusMode.collectAsState()
    val focusState by focusViewModel.focusState.collectAsState()
    val remainingSeconds by focusViewModel.remainingSeconds.collectAsState()
    val breakRemainingSeconds by focusViewModel.breakRemainingSeconds.collectAsState()
    val customTargetMin by focusViewModel.customTargetMinutes.collectAsState()
    val sessionSummary by focusViewModel.sessionSummary.collectAsState()
    val prefs by mainViewModel.preferences.collectAsState()

    val isIdle = focusState == FocusState.IDLE
    val isBreak = focusState == FocusState.BREAK_RUNNING || focusState == FocusState.BREAK_PAUSED

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
                        text = "مساحة التركيز ⏳",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // App blocker status chip
                Surface(
                    color = if (prefs.appBlockerEnabled) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (prefs.appBlockerEnabled) Color(0xFF81C784) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .clickable { mainViewModel.navigateTo(Screen.AppBlockerSetup) }
                        .testTag("focus_app_blocker_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = if (prefs.appBlockerEnabled) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (prefs.appBlockerEnabled) "الحجب نشط" else "إدارة الحجب",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (prefs.appBlockerEnabled) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Mode Selector (Enabled only when IDLE)
                if (isIdle) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            FocusModeOption(
                                title = "بومودورو",
                                subtitle = "25 دقيقة",
                                isSelected = focusMode == FocusMode.POMODORO,
                                onClick = { focusViewModel.setMode(FocusMode.POMODORO) }
                            )
                            FocusModeOption(
                                title = "مؤقت مخصص",
                                subtitle = "$customTargetMin دقيقة",
                                isSelected = focusMode == FocusMode.CUSTOM,
                                onClick = { focusViewModel.setMode(FocusMode.CUSTOM) }
                            )
                            FocusModeOption(
                                title = "تركيز حر",
                                subtitle = "ساعة إيقاف",
                                isSelected = focusMode == FocusMode.FREE,
                                onClick = { focusViewModel.setMode(FocusMode.FREE) }
                            )
                        }
                    }

                    // If custom timer selected, show minutes slider
                    if (focusMode == FocusMode.CUSTOM) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                        ) {
                            Text(
                                text = "مدة الجلسة: $customTargetMin دقيقة",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Slider(
                                value = customTargetMin.toFloat(),
                                onValueChange = { focusViewModel.setCustomMinutes(it.toInt()) },
                                valueRange = 5f..120f,
                                steps = 22
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Square Digital Flip Clock
                val displaySeconds = if (isBreak) breakRemainingSeconds else remainingSeconds
                SquareFlipClock(
                    totalSeconds = displaySeconds,
                    isBreak = isBreak
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Control Buttons
                when (focusState) {
                    FocusState.IDLE -> {
                        Button(
                            onClick = { focusViewModel.startSession() },
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(56.dp)
                                .testTag("start_focus_button"),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("بدء جلسة التركيز", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    FocusState.RUNNING -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(0.85f),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { focusViewModel.pauseSession() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .testTag("pause_focus_button"),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Icon(Icons.Default.Pause, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إيقاف مؤقت", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { focusViewModel.stopSession(userTerminated = true) },
                                colors = ButtonDefaults.buttonColors(containerColor = RedExit),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .testTag("stop_focus_button"),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إنهاء", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    FocusState.PAUSED -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(0.85f),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Button(
                                onClick = { focusViewModel.resumeSession() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .testTag("resume_focus_button"),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("استئناف", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { focusViewModel.stopSession(userTerminated = true) },
                                colors = ButtonDefaults.buttonColors(containerColor = RedExit),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .testTag("stop_focus_button"),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إنهاء", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    FocusState.BREAK_RUNNING, FocusState.BREAK_PAUSED -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(0.85f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "استراحة هادئة لشرب الماء وتصفية الذهن ☕",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { focusViewModel.skipBreak() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(Icons.Default.SkipNext, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("تخطي الاستراحة")
                                }

                                Button(
                                    onClick = {
                                        focusViewModel.skipBreak()
                                        focusViewModel.startSession()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text("جلسة جديدة")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Session Summary Dialog (Strictly as specified in Requirement 5)
    sessionSummary?.let { summary ->
        SessionSummaryDialog(
            summary = summary,
            onTakeBreak = {
                focusViewModel.dismissSummary()
                focusViewModel.startBreak(5)
            },
            onNewSession = {
                focusViewModel.dismissSummary()
                focusViewModel.startSession()
            }
        )
    }
}

@Composable
private fun FocusModeOption(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) Color(0xFFFFD4A8) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SessionSummaryDialog(
    summary: SessionSummaryData,
    onTakeBreak: () -> Unit,
    onNewSession: () -> Unit
) {
    val actualMin = (summary.actualSeconds / 60).toInt()
    val actualSec = (summary.actualSeconds % 60).toInt()

    AlertDialog(
        onDismissRequest = {},
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFD99B26),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("ملخص الجلسة 🌟", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "مدة التركيز الفعلية: $actualMin دقيقة و $actualSec ثانية",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (summary.goalAchieved) "✅ تحقق الهدف المخطط للجلسة!" else "تم تسجيل وقت التركيز الفعلي بنجاح",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (summary.goalAchieved) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Daily & Weekly Progress Bars
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("الهدف اليومي", style = MaterialTheme.typography.labelSmall)
                        Text("${(summary.dailyProgressPct * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { summary.dailyProgressPct },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("الهدف الأسبوعي", style = MaterialTheme.typography.labelSmall)
                        Text("${(summary.weeklyProgressPct * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { summary.weeklyProgressPct },
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onNewSession,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("جلسة جديدة")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onTakeBreak,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("راحة ☕")
            }
        }
    )
}
