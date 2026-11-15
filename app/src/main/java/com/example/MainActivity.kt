package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import android.content.Intent
import com.example.data.model.SubjectEntity
import com.example.service.NotificationHelper
import com.example.ui.screens.badges.BadgeUnlockCelebrationDialog
import com.example.ui.screens.badges.BadgesScreen
import com.example.ui.screens.challenges.ChallengesScreen
import com.example.ui.screens.flashcards.FlashcardsScreen
import com.example.ui.screens.focus.AppBlockerScreen
import com.example.ui.screens.focus.FocusScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.stats.StatsScreen
import com.example.ui.screens.study.StudyOrgScreen
import com.example.ui.screens.study.SubjectDetailScreen
import com.example.ui.theme.NoxStudyTheme
import com.example.ui.viewmodel.BadgesViewModel
import com.example.ui.viewmodel.ChallengesViewModel
import com.example.ui.viewmodel.FlashcardsViewModel
import com.example.ui.viewmodel.FocusViewModel
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.StatsViewModel
import com.example.ui.viewmodel.StudyOrgViewModel

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val studyOrgViewModel: StudyOrgViewModel by viewModels()
    private val focusViewModel: FocusViewModel by viewModels()
    private val flashcardsViewModel: FlashcardsViewModel by viewModels()
    private val challengesViewModel: ChallengesViewModel by viewModels()
    private val statsViewModel: StatsViewModel by viewModels()
    private val badgesViewModel: BadgesViewModel by viewModels()

    private fun handleNavigationIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(NotificationHelper.EXTRA_NAVIGATE_TO_FOCUS, false) == true) {
            mainViewModel.navigateToFocusFromExternalIntent()
            intent.removeExtra(NotificationHelper.EXTRA_NAVIGATE_TO_FOCUS)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNavigationIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleNavigationIntent(intent)

        setContent {
            val prefs by mainViewModel.preferences.collectAsState()
            val currentScreen by mainViewModel.currentScreen.collectAsState()
            val newlyUnlockedQueue by badgesViewModel.newlyUnlockedQueue.collectAsState()

            // System dark vs user explicit selection
            val isDark = when (prefs.themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            // Direction: Arabic is RTL, English and French are LTR
            val layoutDirection = if (prefs.language == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr

            // Runtime Notification Permission for Android 13+
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { /* Granted or denied handled */ }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                NoxStudyTheme(darkTheme = isDark) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                    ) {
                        AppNavigation(
                            currentScreen = currentScreen,
                            mainViewModel = mainViewModel,
                            studyOrgViewModel = studyOrgViewModel,
                            focusViewModel = focusViewModel,
                            flashcardsViewModel = flashcardsViewModel,
                            challengesViewModel = challengesViewModel,
                            statsViewModel = statsViewModel,
                            badgesViewModel = badgesViewModel
                        )

                        newlyUnlockedQueue.firstOrNull()?.let { newlyUnlocked ->
                            BadgeUnlockCelebrationDialog(
                                badge = newlyUnlocked,
                                onDismiss = { badgesViewModel.markCelebrated(newlyUnlocked.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    currentScreen: Screen,
    mainViewModel: MainViewModel,
    studyOrgViewModel: StudyOrgViewModel,
    focusViewModel: FocusViewModel,
    flashcardsViewModel: FlashcardsViewModel,
    challengesViewModel: ChallengesViewModel,
    statsViewModel: StatsViewModel,
    badgesViewModel: BadgesViewModel
) {
    var activeSubjectForDetail by remember { mutableStateOf<SubjectEntity?>(null) }

    when (currentScreen) {
        Screen.Home -> {
            HomeScreen(
                mainViewModel = mainViewModel,
                badgesViewModel = badgesViewModel
            )
        }
        Screen.StudyOrg -> {
            StudyOrgScreen(
                studyOrgViewModel = studyOrgViewModel,
                onNavigateToSubjectDetail = { subject ->
                    activeSubjectForDetail = subject
                    mainViewModel.navigateTo(Screen.SubjectDetail(subject.id))
                },
                onBack = { mainViewModel.navigateBack() }
            )
        }
        is Screen.SubjectDetail -> {
            val subjects by studyOrgViewModel.subjects.collectAsState()
            val targetSubject = activeSubjectForDetail ?: subjects.find { it.id == currentScreen.subjectId }
            if (targetSubject != null) {
                SubjectDetailScreen(
                    subject = targetSubject,
                    studyOrgViewModel = studyOrgViewModel,
                    onBack = { mainViewModel.navigateBack() }
                )
            } else {
                HomeScreen(
                    mainViewModel = mainViewModel,
                    badgesViewModel = badgesViewModel
                )
            }
        }
        Screen.Focus -> {
            FocusScreen(
                focusViewModel = focusViewModel,
                mainViewModel = mainViewModel,
                onBack = { mainViewModel.navigateBack() }
            )
        }
        Screen.AppBlockerSetup -> {
            AppBlockerScreen(
                mainViewModel = mainViewModel,
                onBack = { mainViewModel.navigateBack() }
            )
        }
        Screen.Flashcards -> {
            FlashcardsScreen(
                flashcardsViewModel = flashcardsViewModel,
                studyOrgViewModel = studyOrgViewModel,
                onBack = { mainViewModel.navigateBack() }
            )
        }
        Screen.Challenges -> {
            ChallengesScreen(
                challengesViewModel = challengesViewModel,
                onBack = { mainViewModel.navigateBack() }
            )
        }
        Screen.Badges -> {
            BadgesScreen(
                badgesViewModel = badgesViewModel,
                onBack = { mainViewModel.navigateBack() }
            )
        }
        Screen.Stats -> {
            StatsScreen(
                statsViewModel = statsViewModel,
                onBack = { mainViewModel.navigateBack() }
            )
        }
        Screen.Settings -> {
            SettingsScreen(
                mainViewModel = mainViewModel,
                badgesViewModel = badgesViewModel,
                onBack = { mainViewModel.navigateBack() }
            )
        }
    }
}
