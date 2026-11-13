package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SquareFlipClock(
    totalSeconds: Int,
    isBreak: Boolean = false,
    modifier: Modifier = Modifier
) {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    val minStr = String.format(Locale.US, "%02d", minutes)
    val secStr = String.format(Locale.US, "%02d", seconds)
    val hourStr = String.format(Locale.US, "%02d", hours)

    // Breathing pulse for separator
    val infiniteTransition = rememberInfiniteTransition(label = "ClockPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SeparatorPulse"
    )

    // Square outer frame
    Surface(
        modifier = modifier
            .fillMaxWidth(0.85f)
            .aspectRatio(1.1f)
            .shadow(12.dp, RoundedCornerShape(32.dp), spotColor = Color(0x33281E15))
            .border(
                width = 2.dp,
                color = if (isBreak) Color(0xFF6F8B60) else Color(0xFFC79E78).copy(alpha = 0.45f),
                shape = RoundedCornerShape(32.dp)
            ),
        shape = RoundedCornerShape(32.dp),
        color = Color(0xFF221C18) // Cozy deep espresso study clock frame
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Label tag
            Text(
                text = if (isBreak) "✦ استراحة هادئة ✦" else "✦ وقت التركيز ✦",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp
                ),
                color = if (isBreak) Color(0xFFA5C495) else Color(0xFFE8AD7C).copy(alpha = 0.9f),
                modifier = Modifier.padding(bottom = 14.dp)
            )

            // Digits with aligned labels directly beneath
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (hours > 0) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            FlipDigitPair(digits = hourStr, isBreak = isBreak)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "ساعات",
                                color = Color(0xFFC7B299),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            text = ":",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE8AD7C).copy(alpha = pulseAlpha),
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .offset(y = (-11).dp)
                        )
                    }

                    // Minutes Column (دقائق)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        FlipDigitPair(digits = minStr, isBreak = isBreak)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "دقائق",
                            color = Color(0xFFC7B299),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Separator
                    Text(
                        text = ":",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE8AD7C).copy(alpha = pulseAlpha),
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .offset(y = (-11).dp)
                    )

                    // Seconds Column (ثوانٍ)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        FlipDigitPair(digits = secStr, isBreak = isBreak)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "ثوانٍ",
                            color = Color(0xFFC7B299),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun FlipDigitPair(
    digits: String,
    isBreak: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SingleFlipBox(digit = digits.getOrElse(0) { '0' }, isBreak = isBreak)
        SingleFlipBox(digit = digits.getOrElse(1) { '0' }, isBreak = isBreak)
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun SingleFlipBox(
    digit: Char,
    isBreak: Boolean
) {
    Box(
        modifier = Modifier
            .size(width = 46.dp, height = 68.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF2C241E))
            .border(1.dp, Color(0xFF4A3C31), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Horizontal middle groove of split-flip clock
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFF191411))
                .align(Alignment.Center)
        )

        // Animated digit transition
        AnimatedContent(
            targetState = digit,
            transitionSpec = {
                slideInVertically(
                    animationSpec = tween(280),
                    initialOffsetY = { -it }
                ) + fadeIn(tween(280)) togetherWith
                        slideOutVertically(
                            animationSpec = tween(280),
                            targetOffsetY = { it }
                        ) + fadeOut(tween(280))
            },
            label = "DigitAnimation"
        ) { targetDigit ->
            Text(
                text = targetDigit.toString(),
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (isBreak) Color(0xFFD6ECC8) else Color(0xFFFFD4A8)
            )
        }
    }
}
