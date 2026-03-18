package com.studypulse.app.ui.screens.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studypulse.app.ui.theme.*
import com.studypulse.app.sensors.NoiseStatus
import com.studypulse.app.sensors.LightSensorManager

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isPhoneStill by viewModel.isPhoneStill.collectAsState()
    val noiseDb by viewModel.noiseDb.collectAsState()
    val noiseStatus by viewModel.noiseStatus.collectAsState()
    val lightStatus by viewModel.lightStatus.collectAsState()
    val onShake by viewModel.onShake.collectAsState()

    // React to shake — start/pause session
    LaunchedEffect(onShake) {
        if (onShake) viewModel.toggleSessionOnShake()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Header
        item {
            DashboardHeader(
                userName = uiState.userName,
                onStartSession = { onNavigate("timer") }
            )
        }

        // ── Sensor Status Bar
        item {
            SensorStatusBar(
                isPhoneStill = isPhoneStill,
                noiseDb = noiseDb,
                noiseStatus = noiseStatus,
                lightStatus = lightStatus
            )
        }

        // ── Noise Warning (shows when loud)
        if (noiseStatus == NoiseStatus.LOUD) {
            item {
                NoiseWarningBanner(noiseDb = noiseDb)
            }
        }

        // ── Active Session Card
        if (uiState.isSessionActive) {
            item {
                ActiveSessionCard(
                    subject = uiState.activeSubject,
                    elapsedSeconds = uiState.elapsedSeconds,
                    onPause = { viewModel.pauseSession() },
                    onStop = { viewModel.stopSession() }
                )
            }
        }

        // ── Stats Row
        item {
            StatsRow(
                todayHours = uiState.todayHours,
                streakDays = uiState.streakDays,
                weeklyProgress = uiState.weeklyProgress,
                focusScore = uiState.focusScore
            )
        }

        // ── Subject Breakdown
        item {
            SubjectBreakdownCard(subjects = uiState.subjects)
        }

        // ── Motivational Quote
        item {
            MotivationalQuoteCard(
                quote = uiState.currentQuote,
                author = uiState.quoteAuthor,
                onRefresh = { viewModel.refreshQuote() }
            )
        }

        // ── Streak Calendar
        item {
            StreakCalendarCard(
                streakDays = uiState.streakDays,
                studiedDays = uiState.studiedDaysThisMonth
            )
        }

        // ── Recent Sessions
        item {
            RecentSessionsCard(
                sessions = uiState.recentSessions,
                onViewAll = { onNavigate("sessions") }
            )
        }

        item { Spacer(Modifier.height(80.dp)) } // Bottom nav padding
    }
}

// ─────────────────────────────────────────────
@Composable
fun DashboardHeader(userName: String, onStartSession: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Good evening, $userName 👋",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Text(
                text = "3 goals remaining today",
                fontSize = 13.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Button(
            onClick = onStartSession,
            colors = ButtonDefaults.buttonColors(containerColor = Purple),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Start Session", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
}

// ─────────────────────────────────────────────
@Composable
fun SensorStatusBar(
    isPhoneStill: Boolean,
    noiseDb: Float,
    noiseStatus: NoiseStatus,
    lightStatus: LightSensorManager.LightStatus
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Accelerometer pill
        SensorPill(
            icon = if (isPhoneStill) "📳" else "📲",
            label = if (isPhoneStill) "Still · Focused" else "Moving",
            isActive = isPhoneStill,
            modifier = Modifier.weight(1f)
        )
        // Microphone pill
        SensorPill(
            icon = "🎙️",
            label = "${noiseDb.toInt()} dB · ${noiseStatus.name.lowercase().replaceFirstChar { it.uppercase() }}",
            isActive = noiseStatus == NoiseStatus.QUIET,
            isWarning = noiseStatus == NoiseStatus.LOUD,
            modifier = Modifier.weight(1f)
        )
        // Light pill
        SensorPill(
            icon = "💡",
            label = when (lightStatus) {
                LightSensorManager.LightStatus.DARK -> "Dark Mode"
                LightSensorManager.LightStatus.DIM -> "Dim Light"
                LightSensorManager.LightStatus.NORMAL -> "Normal Light"
                LightSensorManager.LightStatus.BRIGHT -> "Bright"
            },
            isActive = lightStatus != LightSensorManager.LightStatus.DARK,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SensorPill(
    icon: String,
    label: String,
    isActive: Boolean,
    isWarning: Boolean = false,
    modifier: Modifier = Modifier
) {
    val borderColor = when {
        isWarning -> Orange.copy(alpha = 0.5f)
        isActive -> Green.copy(alpha = 0.4f)
        else -> Navy4
    }
    val textColor = when {
        isWarning -> Orange
        isActive -> Green
        else -> TextMuted
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50.dp))
            .background(Navy3)
            .border(1.dp, borderColor, RoundedCornerShape(50.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(icon, fontSize = 12.sp)
        Text(label, fontSize = 10.sp, color = textColor, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

// ─────────────────────────────────────────────
@Composable
fun NoiseWarningBanner(noiseDb: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Orange.copy(alpha = 0.15f))
            .border(1.dp, Orange.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("🔊", fontSize = 20.sp)
        Column {
            Text("Noisy Environment Detected", fontWeight = FontWeight.Bold, color = Orange, fontSize = 13.sp)
            Text("${noiseDb.toInt()} dB detected — consider moving to a quieter place", fontSize = 12.sp, color = TextSecondary)
        }
    }
}

// ─────────────────────────────────────────────
@Composable
fun ActiveSessionCard(
    subject: String,
    elapsedSeconds: Int,
    onPause: () -> Unit,
    onStop: () -> Unit
) {
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Green.copy(alpha = 0.1f))
            .border(1.dp, Green.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Green.copy(alpha = alpha * 0.2f))
                .border(2.dp, Green.copy(alpha = alpha), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Green))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text("📖 $subject", fontWeight = FontWeight.Bold, color = Green, fontSize = 14.sp)
            Text("Shake to pause · Tap to stop", fontSize = 11.sp, color = TextSecondary)
        }

        Text(
            text = "%02d:%02d".format(minutes, seconds),
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Green,
            letterSpacing = 1.sp
        )

        IconButton(onClick = onPause) {
            Icon(Icons.Default.Pause, contentDescription = "Pause", tint = Green)
        }
    }
}

// ─────────────────────────────────────────────
@Composable
fun StatsRow(
    todayHours: Float,
    streakDays: Int,
    weeklyProgress: Float,
    focusScore: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard("Today", "%.1fh".format(todayHours), "↑ vs yesterday", Purple3, Modifier.weight(1f))
        StatCard("Streak", "$streakDays 🔥", "Day streak", Green, Modifier.weight(1f))
        StatCard("Weekly", "${(weeklyProgress * 100).toInt()}%", "of goal", Orange, Modifier.weight(1f))
        StatCard("Focus", "$focusScore", "Score", Accent, Modifier.weight(1f))
    }
}

@Composable
fun StatCard(label: String, value: String, sub: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Navy2)
            .border(1.dp, Navy4, RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = valueColor, modifier = Modifier.padding(vertical = 4.dp))
        Text(sub, fontSize = 10.sp, color = TextSecondary, textAlign = TextAlign.Center)
    }
}

// ─────────────────────────────────────────────
@Composable
fun SubjectBreakdownCard(subjects: List<SubjectProgress>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Navy2),
        border = BorderStroke(1.dp, Navy4)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("📚 Subject Breakdown", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
            Spacer(Modifier.height(16.dp))
            subjects.forEach { subject ->
                SubjectRow(subject = subject)
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun SubjectRow(subject: SubjectProgress) {
    val animatedProgress by animateFloatAsState(
        targetValue = subject.progress,
        animationSpec = tween(1000, easing = EaseOutCubic),
        label = "progress"
    )
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(subject.color))
                Text(subject.name, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            }
            Text(subject.hoursLabel, fontSize = 12.sp, color = TextSecondary)
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(10.dp)),
            color = subject.color,
            trackColor = Navy4,
        )
    }
}

// ─────────────────────────────────────────────
@Composable
fun MotivationalQuoteCard(quote: String, author: String, onRefresh: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(Purple.copy(alpha = 0.25f), Purple2.copy(alpha = 0.1f))
                )
            )
            .border(1.dp, Purple.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("💬 Daily Quote", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                IconButton(onClick = onRefresh, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "New quote", tint = Accent, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "\"$quote\"",
                fontSize = 14.sp,
                color = TextPrimary,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Light
            )
            Spacer(Modifier.height(10.dp))
            Text("— $author", fontSize = 12.sp, color = Purple3, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─────────────────────────────────────────────
@Composable
fun StreakCalendarCard(streakDays: Int, studiedDaysThisMonth: Set<Int>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Navy2),
        border = BorderStroke(1.dp, Navy4)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("🔥 Study Streak", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("$streakDays", fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, color = Purple3)
                Column {
                    Text("day streak 🔥", fontSize = 14.sp, color = TextSecondary)
                    Spacer(Modifier.height(4.dp))
                    Text("${studiedDaysThisMonth.size} sessions this month", fontSize = 12.sp, color = TextMuted)
                }
            }
            Spacer(Modifier.height(16.dp))
            // 4-week mini calendar
            val days = listOf("M","T","W","T","F","S","S")
            repeat(4) { week ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    days.forEachIndexed { dayIndex, dayLabel ->
                        val dayNum = week * 7 + dayIndex + 1
                        val isDone = dayNum in studiedDaysThisMonth
                        val isToday = dayNum == 11
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isDone) Purple.copy(alpha = 0.8f) else Navy4)
                                .then(if (isToday) Modifier.border(2.dp, Purple3, RoundedCornerShape(6.dp)) else Modifier),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(dayLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                color = if (isDone) Color.White else TextMuted)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────
@Composable
fun RecentSessionsCard(sessions: List<StudySession>, onViewAll: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Navy2),
        border = BorderStroke(1.dp, Navy4)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("🕐 Recent Sessions", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                TextButton(onClick = onViewAll) {
                    Text("All →", fontSize = 12.sp, color = Purple3)
                }
            }
            Spacer(Modifier.height(8.dp))
            sessions.forEach { session ->
                SessionItem(session = session)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun SessionItem(session: StudySession) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Navy3)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.width(3.dp).height(36.dp).clip(RoundedCornerShape(4.dp)).background(session.color))
        Column(modifier = Modifier.weight(1f)) {
            Text(session.subject, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(session.meta, fontSize = 11.sp, color = TextMuted)
        }
        Text(session.duration, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Accent)
    }
}

// ─────────────────────────────────────────────
//  Data Models
// ─────────────────────────────────────────────

data class SubjectProgress(
    val name: String,
    val progress: Float,
    val hoursLabel: String,
    val color: Color
)

data class StudySession(
    val subject: String,
    val meta: String,
    val duration: String,
    val color: Color
)
