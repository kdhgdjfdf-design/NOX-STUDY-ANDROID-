package com.example.ui.screens.focus

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.provider.Settings
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
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.AppBlockerAccessibilityService
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class InstalledAppItem(
    val packageName: String,
    val appName: String,
    val iconBitmap: ImageBitmap? = null
)

private fun Drawable.toImageBitmap(): ImageBitmap? {
    return try {
        val width = if (intrinsicWidth > 0) intrinsicWidth.coerceAtMost(128) else 96
        val height = if (intrinsicHeight > 0) intrinsicHeight.coerceAtMost(128) else 96
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
        bitmap.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}

@Composable
fun AppBlockerScreen(
    mainViewModel: MainViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs by mainViewModel.preferences.collectAsState()

    var isAccessibilityEnabled by remember {
        mutableStateOf(AppBlockerAccessibilityService.isAccessibilitySettingsEnabled(context))
    }

    // Refresh permission status when screen appears
    LaunchedEffect(Unit) {
        isAccessibilityEnabled = AppBlockerAccessibilityService.isAccessibilitySettingsEnabled(context)
    }

    var appList by remember { mutableStateOf<List<InstalledAppItem>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val ownPackage = context.packageName
            val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(launcherIntent, 0)

            val loadedList = resolveInfos
                .mapNotNull { it.activityInfo }
                .filter { it.packageName != ownPackage }
                .distinctBy { it.packageName }
                .map { actInfo ->
                    val name = actInfo.loadLabel(pm).toString()
                    val iconDrawable = try {
                        actInfo.loadIcon(pm)
                    } catch (_: Exception) {
                        null
                    }
                    InstalledAppItem(
                        packageName = actInfo.packageName,
                        appName = name,
                        iconBitmap = iconDrawable?.toImageBitmap()
                    )
                }
                .sortedBy { it.appName.lowercase() }

            // If query returned few apps (e.g. minimal emulator), provide common study-distracting apps
            val finalList = if (loadedList.isNotEmpty()) {
                loadedList
            } else {
                listOf(
                    InstalledAppItem("com.instagram.android", "Instagram"),
                    InstalledAppItem("com.google.android.youtube", "YouTube"),
                    InstalledAppItem("com.zhiliaoapp.musically", "TikTok"),
                    InstalledAppItem("com.whatsapp", "WhatsApp"),
                    InstalledAppItem("com.twitter.android", "X (Twitter)"),
                    InstalledAppItem("com.facebook.katana", "Facebook"),
                    InstalledAppItem("com.snapchat.android", "Snapchat"),
                    InstalledAppItem("com.netflix.mediaclient", "Netflix")
                )
            }

            withContext(Dispatchers.Main) {
                appList = finalList
                isLoadingApps = false
            }
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "إدارة حجب التطبيقات 🛡️",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "اختر التطبيقات المشتتة لحجبها أثناء جلسة التركيز",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
            // Master Switch Card
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "تفعيل حجب التطبيقات",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "يحجب التطبيقات المحددة فقط أثناء جلسة التركيز النشطة ويتوقف تلقائياً عند انتهاء الجلسة أو إيقافها.",
                                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Switch(
                            checked = prefs.appBlockerEnabled,
                            onCheckedChange = { isChecked ->
                                val repo = (context.applicationContext as com.example.NoxStudyApp).preferencesRepository
                                scope.launch { repo.setAppBlockerEnabled(isChecked) }
                            },
                            modifier = Modifier.testTag("app_blocker_switch")
                        )
                    }
                }
            }

            // Permission Status & Explicit Disclosure Card
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isAccessibilityEnabled) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isAccessibilityEnabled) Color(0xFF81C784) else Color(0xFFFFB74D)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isAccessibilityEnabled) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isAccessibilityEnabled) Color(0xFF2E7D32) else Color(0xFFE65100),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isAccessibilityEnabled) "صلاحية الحجب مفعلة وجاهزة بنجاح" else "صلاحية إمكانية الوصول (Accessibility) مطلوبة",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isAccessibilityEnabled) Color(0xFF1B5E20) else Color(0xFFBF360C)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "إفصاح الخصوصية والأمان: تُستخدم خدمة إمكانية الوصول الأصلية لنظام Android حصرياً للتحقق من اسم التطبيق النشط في الواجهة أثناء جلسة الدراسة لتوجيهك بلطف إلى شاشة التحفيز. لا نقوم بقراءة الشاشة أو تسجيل أي مدخلات أو جمع أي بيانات شخصية إطلاقاً.",
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                            color = if (isAccessibilityEnabled) Color(0xFF2E7D32) else Color(0xFF5D4037)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("open_accessibility_settings_btn")
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isAccessibilityEnabled) "فحص الإعدادات في Android" else "تفعيل الصلاحية من إعدادات الهاتف")
                        }
                    }
                }
            }

            // Summary of Selected Apps
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "التطبيقات المثبتة (${appList.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "المحدد للحجب: ${prefs.blockedPackages.size}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            if (isLoadingApps) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            } else {
                items(appList, key = { it.packageName }) { appItem ->
                    val isChecked = prefs.blockedPackages.contains(appItem.packageName)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                val currentSet = prefs.blockedPackages.toMutableSet()
                                if (isChecked) {
                                    currentSet.remove(appItem.packageName)
                                } else {
                                    currentSet.add(appItem.packageName)
                                }
                                val repo = (context.applicationContext as com.example.NoxStudyApp).preferencesRepository
                                scope.launch { repo.updateBlockedPackages(currentSet) }
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isChecked) 1.5.dp else 1.dp,
                            color = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                // App Icon
                                if (appItem.iconBitmap != null) {
                                    Image(
                                        bitmap = appItem.iconBitmap,
                                        contentDescription = appItem.appName,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Apps,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column {
                                    Text(
                                        text = appItem.appName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = appItem.packageName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        maxLines = 1
                                    )
                                }
                            }

                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    val currentSet = prefs.blockedPackages.toMutableSet()
                                    if (checked) {
                                        currentSet.add(appItem.packageName)
                                    } else {
                                        currentSet.remove(appItem.packageName)
                                    }
                                    val repo = (context.applicationContext as com.example.NoxStudyApp).preferencesRepository
                                    scope.launch { repo.updateBlockedPackages(currentSet) }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
