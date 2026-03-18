package com.studypulse.app.ui.screens.timer

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studypulse.app.sensors.NoiseStatus
import com.studypulse.app.ui.theme.*

// ─────────────────────────────────────────────
//  Timer Screen
// ─────────────────────────────────────────────

@Composable
fun TimerScreen(
    viewModel: TimerViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val noiseStatus by viewModel.noiseStatus.collectAsState()
    val noiseDb by viewModel.noiseDb.collectAsState()
    val isPhoneStill by viewModel.isPhoneStill.collectAsState()
    val onShake by viewModel.onShake.collectAsState()

    // Shake to toggle timer
    LaunchedEffect(onShake) {
        if (onShake) viewModel.toggleTimer()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        // Top bar
        TimerTopBar(
            selectedCourse = state.selectedCourse,
            onBack = onBack
        )

        Spacer(Modifier.height(16.dp))

        // Mode selector
        TimerModeTabs(
            selectedMode = state.mode,
            onModeSelected = { viewModel.setMode(it) }
        )

        Spacer(Modifier.height(12.dp))

        // Duration selector
        DurationRow(
            selectedDuration = state.totalMinutes,
            mode = state.mode,
            onDurationSelected = { viewModel.setDuration(it) }
        )

        Spacer(Modifier.height(20.dp))

        // Noise warning
        AnimatedVisibility(
            visible = noiseStatus == NoiseStatus.LOUD && state.isRunning
        ) {
            NoiseWarning(noiseDb = noiseDb)
            Spacer(Modifier.height(12.dp))
        }

        // Pomodoro dots
        PomodoroProgressDots(
            currentRound = state.currentRound,
            totalRounds = 4
        )

        Spacer(Modifier.height(20.dp))

        // Timer ring
        TimerRing(
            progress = state.progress,
            timeText = state.timeDisplay,
            courseName = state.selectedCourse,
            roundText = "Round ${state.currentRound} of 4",
            mode = state.mode,
            isRunning = state.isRunning
        )

        Spacer(Modifier.height(28.dp))

        // Controls
        TimerControls(
            isRunning = state.isRunning,
            onToggle = { viewModel.toggleTimer() },
            onReset = { viewModel.resetTimer() },
            onSkip = { viewModel.skipToNext() }
        )

        Spacer(Modifier.height(16.dp))

        // Sensor status
        SensorStatusRow(
            isPhoneStill = isPhoneStill,
            noiseDb = noiseDb,
            noiseStatus = noiseStatus
        )

        Spacer(Modifier.height(20.dp))

        // Today's stats
        TodayStatsRow(
            sessions = state.sessionsToday,
            minutesFocused = state.minutesToday,
            breaks = state.breaksToday
        )
    }
}

// ─────────────────────────────────────────────
@Composable
fun TimerTopBar(selectedCourse: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextSecondary)
        }
        Text(
            "Pomodoro Timer",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        IconButton(onClick = {}) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextSecondary)
        }
    }
}

// ─────────────────────────────────────────────
@Composable
fun TimerModeTabs(selectedMode: TimerMode, onModeSelected: (TimerMode) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(Navy3)
            .border(1.dp, Navy4, RoundedCornerShape(50.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TimerMode.values().forEach { mode ->
            val isSelected = mode == selectedMode
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(if (isSelected) Navy4 else Color.Transparent)
                    .clickable { onModeSelected(mode) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    mode.label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) mode.color else TextMuted
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
@Composable
fun DurationRow(selectedDuration: Int, mode: TimerMode, onDurationSelected: (Int) -> Unit) {
    val options = when (mode) {
        TimerMode.FOCUS -> listOf(25, 30, 45, 60)
        TimerMode.SHORT_BREAK -> listOf(5, 10)
        TimerMode.LONG_BREAK -> listOf(15, 20, 30)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { mins ->
            val isSelected = mins == selectedDuration
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) Purple.copy(alpha = 0.2f) else Navy3)
                    .border(1.dp, if (isSelected) Purple.copy(alpha = 0.5f) else Navy4, RoundedCornerShape(8.dp))
                    .clickable { onDurationSelected(mins) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("$mins min", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) Accent else TextSecondary)
            }
        }
    }
}

// ─────────────────────────────────────────────
@Composable
fun TimerRing(
    progress: Float,
    timeText: String,
    courseName: String,
    roundText: String,
    mode: TimerMode,
    isRunning: Boolean
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(900, easing = LinearEasing),
        label = "ring-progress"
    )

    val glowAlpha by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "glow-alpha"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(260.dp)) {
        // Ring
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val startAngle = -90f
            val sweep = 360f * animatedProgress

            // Track
            drawArc(
                color = Navy4,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(strokeWidth, cap = StrokeCap.Round)
            )

            // Glow layer
            drawArc(
                color = mode.color.copy(alpha = glowAlpha * 0.4f),
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(strokeWidth + 8.dp.toPx(), cap = StrokeCap.Round)
            )

            // Progress arc
            drawArc(
                brush = Brush.sweepGradient(listOf(mode.color.copy(alpha = 0.6f), mode.color)),
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Inner content
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                courseName.take(16).uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                timeText,
                fontSize = 54.sp,
                fontWeight = FontWeight.Light,
                color = mode.color,
                fontFamily = FontFamily.Monospace,
                letterSpacing = (-1).sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                roundText,
                fontSize = 11.sp,
                color = TextMuted,
                letterSpacing = 0.5.sp
            )
        }
    }
}

// ─────────────────────────────────────────────
@Composable
fun PomodoroProgressDots(currentRound: Int, totalRounds: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(totalRounds) { i ->
            val isDone = i < currentRound - 1
            val isActive = i == currentRound - 1
            val size: Dp = if (isActive) 14.dp else 10.dp

            val infiniteTransition = rememberInfiniteTransition(label = "dot$i")
            val dotGlow by infiniteTransition.animateFloat(
                initialValue = 0.4f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
                label = "dot-glow-$i"
            )

            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(when {
                        isDone -> Purple
                        isActive -> Purple3
                        else -> Navy5
                    })
                    .then(if (isActive) Modifier.border(1.dp, Purple2.copy(alpha = dotGlow), CircleShape) else Modifier)
            )
        }
    }
}

// ─────────────────────────────────────────────
@Composable
fun TimerControls(
    isRunning: Boolean,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    onSkip: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isRunning) 1f else 0.95f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "btn-scale"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Reset
        IconButton(
            onClick = onReset,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Navy3)
                .border(1.dp, Navy4, CircleShape)
        ) {
            Icon(Icons.Default.SkipPrevious, contentDescription = "Reset", tint = TextSecondary, modifier = Modifier.size(24.dp))
        }

        // Main play/pause
        Box(
            modifier = Modifier
                .size(76.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Purple, Purple2)))
                .clickable { onToggle() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isRunning) "Pause" else "Start",
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
        }

        // Skip
        IconButton(
            onClick = onSkip,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Navy3)
                .border(1.dp, Navy4, CircleShape)
        ) {
            Icon(Icons.Default.SkipNext, contentDescription = "Skip", tint = TextSecondary, modifier = Modifier.size(24.dp))
        }
    }
}

// ─────────────────────────────────────────────
@Composable
fun SensorStatusRow(isPhoneStill: Boolean, noiseDb: Float, noiseStatus: NoiseStatus) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SensorChip(
            icon = if (isPhoneStill) "📳" else "📲",
            label = if (isPhoneStill) "Still · Focused" else "Moving",
            color = if (isPhoneStill) Green else Orange,
            modifier = Modifier.weight(1f)
        )
        SensorChip(
            icon = "🎙️",
            label = "${noiseDb.toInt()} dB",
            color = when (noiseStatus) {
                NoiseStatus.QUIET -> Green
                NoiseStatus.MODERATE -> Orange
                NoiseStatus.LOUD -> Red
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SensorChip(icon: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50.dp))
            .background(Navy3)
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(50.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(icon, fontSize = 12.sp)
        Text(label, fontSize = 11.sp, color = color, fontWeight = FontWeight.Medium)
    }
}

// ─────────────────────────────────────────────
@Composable
fun NoiseWarning(noiseDb: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Orange.copy(alpha = 0.1f))
            .border(1.dp, Orange.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("🔊", fontSize = 18.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text("Loud Environment (${noiseDb.toInt()} dB)", fontWeight = FontWeight.Bold, color = Orange, fontSize = 13.sp)
            Text("Move to a quieter place for better focus", fontSize = 11.sp, color = TextSecondary)
        }
    }
}

// ─────────────────────────────────────────────
@Composable
fun TodayStatsRow(sessions: Int, minutesFocused: Int, breaks: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatChip("Sessions", "$sessions", Purple3, Modifier.weight(1f))
        StatChip("Focused", if (minutesFocused>=60) "${minutesFocused/60}h ${minutesFocused%60}m" else "${minutesFocused}m", Green, Modifier.weight(1f))
        StatChip("Breaks", "$breaks", Orange, Modifier.weight(1f))
    }
}

@Composable
fun StatChip(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Navy3)
            .border(1.dp, Navy4, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = color)
        Text(label, fontSize = 10.sp, color = TextMuted, modifier = Modifier.padding(top = 2.dp))
    }
}
