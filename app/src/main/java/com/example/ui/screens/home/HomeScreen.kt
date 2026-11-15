package com.example.ui.screens.home

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.components.MiniMusicPlayerDisk
import com.example.ui.components.SectionCard
import com.example.ui.music.MusicLibrarySheet
import com.example.ui.profile.ProfileDialog
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.ui.text.style.TextOverflow
import com.example.ui.viewmodel.BadgesViewModel
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.Screen
import java.util.Calendar

@Composable
fun HomeScreen(
    mainViewModel: MainViewModel,
    badgesViewModel: BadgesViewModel,
    modifier: Modifier = Modifier
) {
    val prefs by mainViewModel.preferences.collectAsState()
    val todaySummary by mainViewModel.todaySummary.collectAsState()
    val badges by badgesViewModel.badges.collectAsState()
    val unlockedBadgeCount = badges.count { it.isUnlocked }

    var showMusicSheet by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }

    // Dynamic greeting according to hour of day
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 5..11 -> "صباح النور والدراسة ☀️"
        in 12..16 -> "مساء التركيز والإنجاز 📖"
        in 17..21 -> "أمسية هادئة ومثمرة ☕"
        else -> "ليلة دراسية دافئة 🌙"
    }

    // Daily quote
    val quoteIndex = Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % mainViewModel.dailyQuotes.size
    val dailyQuote = mainViewModel.dailyQuotes[quoteIndex]

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Top Bar: Profile & Mini Music Player
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Profile Chip
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .clickable { showProfileDialog = true }
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (prefs.avatarUri != null) {
                                AsyncImage(
                                    model = prefs.avatarUri,
                                    contentDescription = "Profile",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_nox_launcher),
                                    contentDescription = "Default Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = greeting,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = prefs.userName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Mini Music Player Disk
                    MiniMusicPlayerDisk(
                        onClick = { showMusicSheet = true }
                    )
                }
            }

            // Hero Cozy Card with Daily Quote
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(24.dp)),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.cozy_home_hero),
                            contentDescription = "Cozy Desk",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0x331E1815),
                                            Color(0xE61E1815)
                                        )
                                    )
                                )
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                                .align(Alignment.BottomStart)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FormatQuote,
                                    contentDescription = null,
                                    tint = Color(0xFFF3C79A),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "إلهام اليوم",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFF3C79A)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = dailyQuote,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 20.sp
                                ),
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Quick Stats, Badges & Settings Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Statistics Button
                    ElevatedButton(
                        onClick = { mainViewModel.navigateTo(Screen.Stats) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_stats_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("الإحصائيات", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    // Badges Button
                    ElevatedButton(
                        onClick = { mainViewModel.navigateTo(Screen.Badges) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_badges_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFD99B26))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (unlockedBadgeCount > 0) "الأوسمة ($unlockedBadgeCount)" else "الأوسمة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Settings Button
                    OutlinedButton(
                        onClick = { mainViewModel.navigateTo(Screen.Settings) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_settings_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("الإعدادات", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            // Today's Highlights Summary
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "ملخص إنجازات اليوم ✨",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            SummaryItem(
                                icon = Icons.Default.Timer,
                                title = "التركيز",
                                value = "${todaySummary.focusMinutes} دقيقة"
                            )
                            SummaryItem(
                                icon = Icons.Default.HourglassTop,
                                title = "الجلسات",
                                value = "${todaySummary.completedSessions} مكتملة"
                            )
                            SummaryItem(
                                icon = Icons.Default.CheckCircle,
                                title = "المهام",
                                value = "${todaySummary.completedTasks} منجزة"
                            )
                        }
                    }
                }
            }

            // The 4 Main Sections (Strictly As Specified in Requirement 3)
            item {
                Text(
                    text = "الأقسام الرئيسية",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )
            }

            // 1. تنظيم الدراسة
            item {
                SectionCard(
                    title = "تنظيم الدراسة",
                    description = "الدروس والمراجعات، الواجبات والمهام، ومواعيد الاختبارات",
                    imagePainter = painterResource(id = R.drawable.cozy_study_org),
                    badgeText = "تنظيم شامل",
                    testTag = "section_study_org",
                    onClick = { mainViewModel.navigateTo(Screen.StudyOrg) }
                )
            }

            // 2. التركيز
            item {
                SectionCard(
                    title = "مساحة التركيز",
                    description = "بومودورو، مؤقت مخصص، تركيز حر، مع ميزة حجب التطبيقات المشتتة",
                    imagePainter = painterResource(id = R.drawable.cozy_focus),
                    badgeText = "ساعة رقمية وحجب",
                    testTag = "section_focus",
                    onClick = { mainViewModel.navigateTo(Screen.Focus) }
                )
            }

            // 3. بطاقات المراجعة
            item {
                SectionCard(
                    title = "بطاقات المراجعة",
                    description = "بطاقات دراسية ثلاثية الأبعاد مع خانة ذكية لتقييم مستوى التذكّر",
                    imagePainter = painterResource(id = R.drawable.cozy_flashcards),
                    badgeText = "تفاعل 3D",
                    testTag = "section_flashcards",
                    onClick = { mainViewModel.navigateTo(Screen.Flashcards) }
                )
            }

            // 4. التحديات الفردية
            item {
                SectionCard(
                    title = "التحديات الفردية",
                    description = "تحديات NOX الجاهزة وأهدافك الخاصة مع شارات إنجاز مميزة",
                    imagePainter = painterResource(id = R.drawable.cozy_challenges),
                    badgeText = "شارات وجوائز",
                    testTag = "section_challenges",
                    onClick = { mainViewModel.navigateTo(Screen.Challenges) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showMusicSheet) {
        MusicLibrarySheet(onDismiss = { showMusicSheet = false })
    }

    if (showProfileDialog) {
        ProfileDialog(
            mainViewModel = mainViewModel,
            badges = badges,
            onOpenBadges = {
                showProfileDialog = false
                mainViewModel.navigateTo(Screen.Badges)
            },
            onDismiss = { showProfileDialog = false }
        )
    }
}

@Composable
private fun SummaryItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
