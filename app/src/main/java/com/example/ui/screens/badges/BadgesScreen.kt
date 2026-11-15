package com.example.ui.screens.badges

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.badges.BadgeDefinition
import com.example.data.badges.BadgeShape
import com.example.data.badges.BadgeUiModel
import com.example.ui.viewmodel.BadgesViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private enum class BadgeFilter {
    ALL,
    UNLOCKED,
    LOCKED
}

@Composable
fun BadgesScreen(
    badgesViewModel: BadgesViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    LaunchedEffect(Unit) {
        badgesViewModel.refreshEvaluation()
    }

    val badges by badgesViewModel.badges.collectAsState()
    var filter by remember { mutableStateOf(BadgeFilter.ALL) }
    var selectedBadge by remember { mutableStateOf<BadgeUiModel?>(null) }

    val unlockedList = badges.filter { it.isUnlocked }
    val lockedList = badges.filter { !it.isUnlocked }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("badges_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "الأوسمة والإنجازات 🏅",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "تقدير حقيقي لالتزامك وجلسات تركيزك المستمرة",
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
            // Luxury Summary Header Card
            item {
                val total = badges.size.coerceAtLeast(1)
                val unlockedCount = unlockedList.size
                val fraction = unlockedCount.toFloat() / total.toFloat()

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color(0xFFE5A638).copy(alpha = 0.35f)
                    ),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                        MaterialTheme.colorScheme.surface
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "حصيلتك من الأوسمة",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "تم فتح $unlockedCount من أصل $total أوسمة",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFE5A638).copy(alpha = 0.16f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5A638).copy(alpha = 0.45f))
                                ) {
                                    Text(
                                        text = "${(fraction * 100).toInt()}%",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        color = Color(0xFFD99B26),
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    )
                                }
                            }

                            LinearProgressIndicator(
                                progress = { fraction },
                                color = Color(0xFFD99B26),
                                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }
            }

            // Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filter == BadgeFilter.ALL,
                        onClick = { filter = BadgeFilter.ALL },
                        label = { Text("الكل (${badges.size})") }
                    )
                    FilterChip(
                        selected = filter == BadgeFilter.UNLOCKED,
                        onClick = { filter = BadgeFilter.UNLOCKED },
                        label = { Text("المفتوحة (${unlockedList.size})") }
                    )
                    FilterChip(
                        selected = filter == BadgeFilter.LOCKED,
                        onClick = { filter = BadgeFilter.LOCKED },
                        label = { Text("المغلقة (${lockedList.size})") }
                    )
                }
            }

            // Unlocked Section
            if (filter != BadgeFilter.LOCKED) {
                item {
                    SectionHeaderRow(
                        title = "الأوسمة المفتوحة (${unlockedList.size})",
                        tint = Color(0xFFD99B26)
                    )
                }
                if (unlockedList.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "لم تفتح أي وسام بعد — أكمل أول جلسة تركيز أو أتمّ مهامك ليظهر وسامك الأول هنا.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(unlockedList, key = { "unlocked_${it.definition.id}" }) { badge ->
                        BadgeCard(
                            badge = badge,
                            onClick = { selectedBadge = badge }
                        )
                    }
                }
            }

            // Locked Section
            if (filter != BadgeFilter.UNLOCKED && lockedList.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    SectionHeaderRow(
                        title = "أوسمة بانتظار الإنجاز (${lockedList.size})",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(lockedList, key = { "locked_${it.definition.id}" }) { badge ->
                    BadgeCard(
                        badge = badge,
                        onClick = { selectedBadge = badge }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }

    selectedBadge?.let { badge ->
        BadgeDetailsDialog(
            badge = badge,
            onDismiss = { selectedBadge = null }
        )
    }
}

@Composable
private fun SectionHeaderRow(title: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.EmojiEvents,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun BadgeCard(
    badge: BadgeUiModel,
    onClick: () -> Unit
) {
    val def = badge.definition
    val accent = if (badge.isUnlocked) Color(def.accentHex) else MaterialTheme.colorScheme.outline
    val containerColor = if (badge.isUnlocked) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(
            width = if (badge.isUnlocked) 1.5.dp else 1.dp,
            color = if (badge.isUnlocked) accent.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
        ),
        shadowElevation = if (badge.isUnlocked) 3.dp else 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .testTag("badge_card_${def.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .alpha(if (badge.isUnlocked) 1f else 0.72f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BadgeEmblemArtwork(
                shape = def.shape,
                accentColor = Color(def.accentHex),
                secondaryColor = Color(def.secondaryHex),
                isUnlocked = badge.isUnlocked,
                size = 64.dp
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = def.titleAr,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (badge.isUnlocked) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = accent.copy(alpha = 0.14f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "مفتوح",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = accent
                                )
                            }
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = def.criteriaAr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (badge.isUnlocked && badge.unlockedAt != null) {
                    val dateStr = remember(badge.unlockedAt) {
                        SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(badge.unlockedAt))
                    }
                    Text(
                        text = "تم الفوز به في $dateStr",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = accent
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "التقدم: ${badge.currentValue} / ${def.targetValue} ${def.unitAr}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(badge.progressFraction * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { badge.progressFraction },
                        color = Color(def.accentHex).copy(alpha = 0.7f),
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
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

@Composable
fun BadgeEmblemArtwork(
    shape: BadgeShape,
    accentColor: Color,
    secondaryColor: Color,
    isUnlocked: Boolean,
    size: Dp = 64.dp
) {
    val primary = if (isUnlocked) accentColor else Color(0xFF7D7873)
    val highlight = if (isUnlocked) secondaryColor else Color(0xFFA8A29C)

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val cx = w / 2f
            val cy = h / 2f
            val r = minOf(w, h) * 0.44f

            val fillBrush = Brush.linearGradient(
                colors = listOf(highlight, primary, primary.copy(alpha = 0.85f)),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )

            when (shape) {
                BadgeShape.STAR -> {
                    val path = Path()
                    val points = 8
                    val outerR = r
                    val innerR = r * 0.72f
                    for (i in 0 until points * 2) {
                        val angle = (i * PI / points) - PI / 2.0
                        val rad = if (i % 2 == 0) outerR else innerR
                        val px = cx + (rad * cos(angle)).toFloat()
                        val py = cy + (rad * sin(angle)).toFloat()
                        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                    path.close()
                    drawPath(path, brush = fillBrush)
                    drawCircle(
                        color = Color.White.copy(alpha = if (isUnlocked) 0.35f else 0.15f),
                        radius = r * 0.56f,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
                BadgeShape.SHIELD -> {
                    val path = Path().apply {
                        moveTo(cx, cy - r)
                        lineTo(cx + r * 0.88f, cy - r * 0.62f)
                        lineTo(cx + r * 0.78f, cy + r * 0.25f)
                        quadraticTo(cx + r * 0.45f, cy + r * 0.78f, cx, cy + r)
                        quadraticTo(cx - r * 0.45f, cy + r * 0.78f, cx - r * 0.78f, cy + r * 0.25f)
                        lineTo(cx - r * 0.88f, cy - r * 0.62f)
                        close()
                    }
                    drawPath(path, brush = fillBrush)
                    drawPath(
                        path = path,
                        color = Color.White.copy(alpha = if (isUnlocked) 0.45f else 0.18f),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
                BadgeShape.CROWN -> {
                    val hex = Path()
                    for (i in 0 until 6) {
                        val angle = (i * PI / 3.0) - PI / 2.0
                        val px = cx + (r * cos(angle)).toFloat()
                        val py = cy + (r * sin(angle)).toFloat()
                        if (i == 0) hex.moveTo(px, py) else hex.lineTo(px, py)
                    }
                    hex.close()
                    drawPath(hex, brush = fillBrush)
                    drawCircle(
                        color = Color.White.copy(alpha = if (isUnlocked) 0.4f else 0.15f),
                        radius = r * 0.72f,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
                BadgeShape.MEDAL -> {
                    // Ribbons at top
                    drawLine(
                        color = primary.copy(alpha = 0.75f),
                        start = Offset(cx - r * 0.45f, cy - r * 0.95f),
                        end = Offset(cx - r * 0.15f, cy - r * 0.35f),
                        strokeWidth = 6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = highlight.copy(alpha = 0.9f),
                        start = Offset(cx + r * 0.45f, cy - r * 0.95f),
                        end = Offset(cx + r * 0.15f, cy - r * 0.35f),
                        strokeWidth = 6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawCircle(brush = fillBrush, radius = r * 0.85f, center = Offset(cx, cy + r * 0.1f))
                    drawCircle(
                        color = Color.White.copy(alpha = if (isUnlocked) 0.45f else 0.18f),
                        radius = r * 0.65f,
                        center = Offset(cx, cy + r * 0.1f),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
                BadgeShape.FLAME -> {
                    drawCircle(
                        color = primary.copy(alpha = 0.18f),
                        radius = r
                    )
                    drawCircle(brush = fillBrush, radius = r * 0.82f)
                    drawCircle(
                        color = highlight,
                        radius = r * 0.82f,
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                }
                BadgeShape.LOTUS -> {
                    // Rounded diamond + soft halo
                    val diamond = Path().apply {
                        moveTo(cx, cy - r)
                        quadraticTo(cx + r * 0.85f, cy - r * 0.45f, cx + r, cy)
                        quadraticTo(cx + r * 0.85f, cy + r * 0.45f, cx, cy + r)
                        quadraticTo(cx - r * 0.85f, cy + r * 0.45f, cx - r, cy)
                        quadraticTo(cx - r * 0.85f, cy - r * 0.45f, cx, cy - r)
                        close()
                    }
                    drawPath(diamond, brush = fillBrush)
                }
            }
        }

        val icon = when (shape) {
            BadgeShape.STAR -> Icons.Default.Star
            BadgeShape.SHIELD -> Icons.Default.Shield
            BadgeShape.CROWN -> Icons.Default.WorkspacePremium
            BadgeShape.MEDAL -> Icons.Default.EmojiEvents
            BadgeShape.FLAME -> Icons.Default.LocalFireDepartment
            BadgeShape.LOTUS -> Icons.Default.Spa
        }
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = if (isUnlocked) 0.96f else 0.55f),
            modifier = Modifier.size(size * 0.4f)
        )
    }
}

@Composable
private fun BadgeDetailsDialog(
    badge: BadgeUiModel,
    onDismiss: () -> Unit
) {
    val def = badge.definition
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BadgeEmblemArtwork(
                    shape = def.shape,
                    accentColor = Color(def.accentHex),
                    secondaryColor = Color(def.secondaryHex),
                    isUnlocked = badge.isUnlocked,
                    size = 44.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = def.titleAr,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = def.descriptionAr,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "شرط الحصول على الوسام:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = def.criteriaAr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (badge.isUnlocked && badge.unlockedAt != null) {
                    val dateStr = SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(badge.unlockedAt))
                    Text(
                        text = "✅ مفتوح منذ $dateStr",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF2E7D32)
                    )
                } else {
                    Text(
                        text = "التقدم الحالي: ${badge.currentValue} من ${def.targetValue} ${def.unitAr}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    LinearProgressIndicator(
                        progress = { badge.progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("حسناً")
            }
        }
    )
}

@Composable
fun BadgeUnlockCelebrationDialog(
    badge: BadgeDefinition,
    onDismiss: () -> Unit
) {
    val scaleAnim = remember(badge.id) { Animatable(0.45f) }
    LaunchedEffect(badge.id) {
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "BadgeGlow")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowPulse"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                Color(badge.accentHex).copy(alpha = 0.65f)
            ),
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .scale(scaleAnim.value)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(badge.accentHex),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "وسام إنجاز جديد! 🏅",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(badge.accentHex)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Box(
                    modifier = Modifier
                        .size(116.dp)
                        .scale(glowPulse)
                        .clip(CircleShape)
                        .background(Color(badge.accentHex).copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    BadgeEmblemArtwork(
                        shape = badge.shape,
                        accentColor = Color(badge.accentHex),
                        secondaryColor = Color(badge.secondaryHex),
                        isUnlocked = true,
                        size = 92.dp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = badge.titleAr,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = badge.descriptionAr,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(badge.accentHex).copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "تم الإنجاز: ${badge.criteriaAr}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(badge.accentHex),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(badge.accentHex)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("رائع! متابعة الدراسة", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
