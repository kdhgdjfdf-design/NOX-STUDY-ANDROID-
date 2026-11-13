package com.example.ui.screens.focus

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.MainActivity
import com.example.R
import com.example.ui.theme.NoxStudyTheme
import com.example.ui.theme.RedExit
import kotlin.random.Random

class BlockedAppActivity : ComponentActivity() {

    private val motivationalQuotes = listOf(
        "«النجاح لا يأتي بالصدفة، بل بالالتزام والتركيز الآن.»\nعد إلى دراستك، مستقبلك ينتظرك.",
        "«كل دقيقة تبتعد فيها عن المشتتات، تقربك خطوة من حلمك.»\nأنت أقوى من هذا الإشعار.",
        "«الصبر نصف العلم، والتركيز نصفه الآخر.»\nأكمل جلستك بكل هدوء وفخر.",
        "«لحظات الانضباط هذه ستصنع فارقاً ستفتخر به غداً.»\nأغلق هذا التطبيق واستمر في طريقك.",
        "«لا تدع رغبة لحظية تسرق إنجاز يوم كامل.»\nركّز على ما بنيته حتى الآن."
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val randomQuote = motivationalQuotes[Random.nextInt(motivationalQuotes.size)]

        setContent {
            NoxStudyTheme(darkTheme = true) {
                BlockedScreenContent(
                    quote = randomQuote,
                    onExitToStudy = {
                        val intent = Intent(this@BlockedAppActivity, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        }
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Prevent bypassing blocked screen easily, return to study
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
        finish()
    }
}

@Composable
private fun BlockedScreenContent(
    quote: String,
    onExitToStudy: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF161210))
    ) {
        // Cozy background photo
        Image(
            painter = painterResource(id = R.drawable.cozy_focus),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Dark gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xCC181310),
                            Color(0xF5181310),
                            Color(0xFF181310)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Brand & Icon
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0x33E5A638)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.HourglassTop,
                        contentDescription = "Focusing",
                        tint = Color(0xFFFFD4A8),
                        modifier = Modifier.size(42.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "NOX STUDY 📚",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    color = Color(0xFFFFD4A8)
                )

                Text(
                    text = "جلسة التركيز جارية الآن",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFC7B8AD)
                )
            }

            // Motivational quote card (2 lines random)
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0x6629201A),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4DE5A638)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = quote,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        lineHeight = 28.sp,
                        fontSize = 17.sp
                    ),
                    color = Color(0xFFF7F2EC),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp)
                )
            }

            // Red exit button at bottom
            Button(
                onClick = onExitToStudy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RedExit,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("exit_blocked_app_button")
            ) {
                Text(
                    text = "العودة إلى الدراسة 📚",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
