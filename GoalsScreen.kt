package com.studypulse.app.ui.screens.goals

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.studypulse.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

// ═══════════════════════════════════════════════
//  DATA MODEL
// ═══════════════════════════════════════════════

enum class GoalType(val label: String, val icon: String) {
    DAILY("Daily", "📅"),
    WEEKLY("Weekly", "📆"),
    MONTHLY("Monthly", "🗓️"),
    CUSTOM("Custom", "✏️")
}

enum class GoalPriority(val label: String, val color: Color) {
    HIGH("🔥 High", Color(0xFFEF4444)),
    MEDIUM("⚡ Medium", Color(0xFFF59E0B)),
    LOW("💤 Low", Color(0xFF64748B))
}

enum class GoalUnit(val label: String) { HOURS("hours"), SESSIONS("sessions"), PAGES("pages"), TASKS("tasks") }

data class Goal(
    val id: String = "",
    val title: String = "",
    val type: GoalType = GoalType.WEEKLY,
    val courseName: String = "All Courses",
    val target: Float = 5f,
    val current: Float = 0f,
    val unit: GoalUnit = GoalUnit.HOURS,
    val priority: GoalPriority = GoalPriority.MEDIUM,
    val colorHex: String = "#7c3aed",
    val icon: String = "🎯",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val userId: String = ""
) {
    val progress: Float get() = if (target > 0) (current / target).coerceIn(0f, 1f) else 0f
    val progressPct: Int get() = (progress * 100).toInt()
    val isOnTrack: Boolean get() = progressPct >= 50
    val statusLabel: String get() = when {
        isCompleted || current >= target -> "✓ Done"
        progressPct < 40 -> "Behind"
        else -> "On Track"
    }
}

fun defaultGoals(userId: String) = listOf(
    Goal(id="g1", title="Study 8h of FYP", type=GoalType.WEEKLY, courseName="Final Year Project", target=8f, current=6.5f, unit=GoalUnit.HOURS, priority=GoalPriority.HIGH, colorHex="#7c3aed", icon="🎓", userId=userId),
    Goal(id="g2", title="Complete 3 AI sessions", type=GoalType.WEEKLY, courseName="Artificial Intelligence", target=3f, current=3f, unit=GoalUnit.SESSIONS, priority=GoalPriority.HIGH, colorHex="#3b82f6", icon="🤖", userId=userId),
    Goal(id="g3", title="Study 2h daily", type=GoalType.DAILY, courseName="All Courses", target=2f, current=1.5f, unit=GoalUnit.HOURS, priority=GoalPriority.MEDIUM, colorHex="#10b981", icon="⏱️", userId=userId),
    Goal(id="g4", title="Finish ML assignment", type=GoalType.CUSTOM, courseName="Machine Learning", target=1f, current=0f, unit=GoalUnit.TASKS, priority=GoalPriority.HIGH, colorHex="#8b5cf6", icon="🧠", userId=userId),
    Goal(id="g5", title="Read 30 pages research", type=GoalType.WEEKLY, courseName="Research Methods", target=30f, current=22f, unit=GoalUnit.PAGES, priority=GoalPriority.MEDIUM, colorHex="#06b6d4", icon="📝", userId=userId),
    Goal(id="g6", title="Practice Networks lab", type=GoalType.WEEKLY, courseName="Networks & Security", target=4f, current=1f, unit=GoalUnit.HOURS, priority=GoalPriority.LOW, colorHex="#ef4444", icon="🔒", userId=userId),
)

// ═══════════════════════════════════════════════
//  REPOSITORY
// ═══════════════════════════════════════════════

@Singleton
class GoalRepository @Inject constructor(private val firestore: FirebaseFirestore) {

    private val _goals = MutableStateFlow<List<Goal>>(emptyList())
    val goals: StateFlow<List<Goal>> = _goals

    suspend fun loadGoals(userId: String) {
        try {
            val snap = firestore.collection("users").document(userId).collection("goals").get().await()
            val loaded = snap.documents.mapNotNull { it.toObject(Goal::class.java) }
            _goals.value = if (loaded.isEmpty()) { seedGoals(userId); defaultGoals(userId) } else loaded
        } catch (e: Exception) { _goals.value = defaultGoals(userId) }
    }

    private suspend fun seedGoals(userId: String) {
        val batch = firestore.batch()
        val col = firestore.collection("users").document(userId).collection("goals")
        defaultGoals(userId).forEach { batch.set(col.document(it.id), it) }
        batch.commit().await()
    }

    suspend fun addGoal(userId: String, goal: Goal): Boolean {
        return try {
            val ref = firestore.collection("users").document(userId).collection("goals").document()
            val g = goal.copy(id = ref.id, userId = userId)
            ref.set(g).await()
            _goals.value = _goals.value + g
            true
        } catch (e: Exception) { false }
    }

    suspend fun updateGoal(userId: String, goal: Goal): Boolean {
        return try {
            firestore.collection("users").document(userId).collection("goals").document(goal.id).set(goal).await()
            _goals.value = _goals.value.map { if (it.id == goal.id) goal else it }
            true
        } catch (e: Exception) { false }
    }

    suspend fun deleteGoal(userId: String, goalId: String): Boolean {
        return try {
            firestore.collection("users").document(userId).collection("goals").document(goalId).delete().await()
            _goals.value = _goals.value.filter { it.id != goalId }
            true
        } catch (e: Exception) { false }
    }

    suspend fun logProgress(userId: String, goalId: String, increment: Float) {
        val goal = _goals.value.find { it.id == goalId } ?: return
        val updated = goal.copy(current = (goal.current + increment).coerceAtMost(goal.target), isCompleted = (goal.current + increment) >= goal.target)
        updateGoal(userId, updated)
    }
}

// ═══════════════════════════════════════════════
//  VIEWMODEL
// ═══════════════════════════════════════════════

enum class GoalsTab { GOALS, PROGRESS, STREAK, BADGES }

data class GoalsUiState(
    val activeTab: GoalsTab = GoalsTab.GOALS,
    val showAddDialog: Boolean = false,
    val streakDays: Int = 12,
    val studiedDays: Set<Int> = (1..12).toSet()
)

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val repository: GoalRepository
) : ViewModel() {

    private val _state = MutableStateFlow(GoalsUiState())
    val state: StateFlow<GoalsUiState> = _state.asStateFlow()
    val goals = repository.goals

    init { viewModelScope.launch { repository.loadGoals("current_user") } }

    fun setTab(tab: GoalsTab) = _state.update { it.copy(activeTab = tab) }
    fun openAddDialog() = _state.update { it.copy(showAddDialog = true) }
    fun closeDialog() = _state.update { it.copy(showAddDialog = false) }

    fun addGoal(title: String, type: GoalType, course: String, target: Float, unit: GoalUnit, priority: GoalPriority) {
        viewModelScope.launch {
            repository.addGoal("current_user", Goal(title=title, type=type, courseName=course, target=target, unit=unit, priority=priority))
            closeDialog()
        }
    }

    fun logProgress(goalId: String, increment: Float) {
        viewModelScope.launch { repository.logProgress("current_user", goalId, increment) }
    }

    fun completeGoal(goalId: String) {
        val goal = goals.value.find { it.id == goalId } ?: return
        viewModelScope.launch { repository.updateGoal("current_user", goal.copy(current = goal.target, isCompleted = true)) }
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch { repository.deleteGoal("current_user", goalId) }
    }
}

// ═══════════════════════════════════════════════
//  SCREEN
// ═══════════════════════════════════════════════

@Composable
fun GoalsScreen(viewModel: GoalsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val goals by viewModel.goals.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(Navy)) {

        // Header
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("🎯 Goals & Progress", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = TextPrimary)
                Text("Track your targets and achievements", fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(top = 2.dp))
            }
            Button(onClick = { viewModel.openAddDialog() }, colors = ButtonDefaults.buttonColors(containerColor = Purple), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("New Goal", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }

        // Tabs
        GoalsTabRow(selected = state.activeTab, onSelect = { viewModel.setTab(it) })

        // Overview cards
        OverviewRow(goals = goals, streakDays = state.streakDays, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(16.dp))

        // Tab content
        when (state.activeTab) {
            GoalsTab.GOALS -> GoalsTabContent(goals = goals, onLog = { viewModel.logProgress(it, 0.5f) }, onComplete = { viewModel.completeGoal(it) }, onDelete = { viewModel.deleteGoal(it) })
            GoalsTab.STREAK -> StreakTabContent(streakDays = state.streakDays, studiedDays = state.studiedDays)
            GoalsTab.BADGES -> BadgesTabContent()
            GoalsTab.PROGRESS -> ProgressTabContent(goals = goals)
        }
    }

    if (state.showAddDialog) {
        AddGoalDialog(onSave = { title, type, course, target, unit, priority ->
            viewModel.addGoal(title, type, course, target, unit, priority)
        }, onDismiss = { viewModel.closeDialog() })
    }
}

// ─────────────────────────────────────────────
@Composable
fun GoalsTabRow(selected: GoalsTab, onSelect: (GoalsTab) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clip(RoundedCornerShape(14.dp)).background(Navy2).border(1.dp, Navy4, RoundedCornerShape(14.dp)).padding(4.dp)) {
        GoalsTab.values().forEach { tab ->
            val isSelected = tab == selected
            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) Brush.linearGradient(listOf(Purple, Purple2)) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
                .clickable { onSelect(tab) }.padding(vertical = 9.dp), contentAlignment = Alignment.Center
            ) {
                Text(tab.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (isSelected) Color.White else TextMuted)
            }
        }
    }
    Spacer(Modifier.height(12.dp))
}

// ─────────────────────────────────────────────
@Composable
fun OverviewRow(goals: List<Goal>, streakDays: Int, modifier: Modifier = Modifier) {
    val completed = goals.count { it.isCompleted }
    val avgProgress = if (goals.isNotEmpty()) goals.map { it.progressPct }.average().toInt() else 0
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OverviewCard("${goals.size}", "Active Goals", Purple3, "🎯", Modifier.weight(1f))
        OverviewCard("$avgProgress%", "Avg Progress", Green, "📈", Modifier.weight(1f))
        OverviewCard("$streakDays🔥", "Streak", Orange, "🔥", Modifier.weight(1f))
        OverviewCard("$completed", "Completed", Accent, "✓", Modifier.weight(1f))
    }
}

@Composable
fun OverviewCard(value: String, label: String, color: Color, icon: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(14.dp)).background(Navy2).border(1.dp, Navy4, RoundedCornerShape(14.dp)).padding(14.dp)) {
        Column {
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = color)
            Text(label, fontSize = 10.sp, color = TextSecondary, modifier = Modifier.padding(top = 3.dp))
        }
        Text(icon, fontSize = 22.sp, modifier = Modifier.align(Alignment.TopEnd), color = color.copy(alpha = 0.12f))
    }
}

// ─────────────────────────────────────────────
@Composable
fun GoalsTabContent(goals: List<Goal>, onLog: (String) -> Unit, onComplete: (String) -> Unit, onDelete: (String) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(goals) { goal ->
            GoalCard(goal = goal, onLog = { onLog(goal.id) }, onComplete = { onComplete(goal.id) }, onDelete = { onDelete(goal.id) })
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun GoalCard(goal: Goal, onLog: () -> Unit, onComplete: () -> Unit, onDelete: () -> Unit) {
    val cardColor = try { Color(android.graphics.Color.parseColor(goal.colorHex)) } catch (e: Exception) { Purple }
    val animProg by animateFloatAsState(targetValue = goal.progress, animationSpec = tween(1000, easing = EaseOutCubic), label = "goal-progress")

    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Navy2).border(1.dp, Navy4, RoundedCornerShape(14.dp))) {
        // Left color bar
        Box(modifier = Modifier.width(3.dp).fillMaxHeight().background(cardColor))
        Column(modifier = Modifier.weight(1f).padding(14.dp)) {
            // Header row
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(cardColor.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) { Text(goal.icon, fontSize = 18.sp) }
                Column(modifier = Modifier.weight(1f)) {
                    Text(goal.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${goal.type.label} · ${goal.courseName}", fontSize = 11.sp, color = TextMuted)
                }
                // Status badge
                val (badgeColor, badgeBg) = when {
                    goal.isCompleted || goal.current >= goal.target -> Pair(Purple3, Purple.copy(alpha = 0.2f))
                    goal.progressPct < 40 -> Pair(Red, Red.copy(alpha = 0.15f))
                    else -> Pair(Green, Green.copy(alpha = 0.15f))
                }
                Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(badgeBg).padding(horizontal = 9.dp, vertical = 4.dp)) {
                    Text(goal.statusLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = badgeColor)
                }
            }
            Spacer(Modifier.height(10.dp))
            // Progress
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${goal.current} / ${goal.target} ${goal.unit.label}", fontSize = 11.sp, color = TextSecondary)
                Text("${goal.progressPct}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = cardColor)
            }
            Spacer(Modifier.height(5.dp))
            LinearProgressIndicator(progress = { animProg }, modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(10.dp)), color = cardColor, trackColor = Navy4)
            Spacer(Modifier.height(10.dp))
            // Actions
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!goal.isCompleted) {
                    OutlinedButton(onClick = onLog, modifier = Modifier.height(32.dp), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Navy5), contentPadding = PaddingValues(horizontal = 12.dp)) {
                        Text("+ Log", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    }
                    Button(onClick = onComplete, modifier = Modifier.height(32.dp), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Green), contentPadding = PaddingValues(horizontal = 12.dp)) {
                        Text("✓ Done", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
@Composable
fun StreakTabContent(streakDays: Int, studiedDays: Set<Int>) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Navy2), border = BorderStroke(1.dp, Navy4)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("🔥 March 2026 Streak", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("$streakDays", fontSize = 52.sp, fontWeight = FontWeight.ExtraBold, color = Orange, fontFamily = FontFamily.Default)
                        Column {
                            Text("day streak 🔥", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("9 more to beat your record of 21", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    // Day headers
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("M","T","W","T","F","S","S").forEach { day ->
                            Text(day, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    // Calendar grid
                    val days = listOf("M","T","W","T","F","S","S")
                    repeat(5) { week ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(7) { dayIdx ->
                                val dayNum = week * 7 + dayIdx + 1
                                if (dayNum <= 31) {
                                    val isDone = dayNum in studiedDays
                                    val isToday = dayNum == 11
                                    Box(modifier = Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(6.dp))
                                        .background(if (isDone) Brush.linearGradient(listOf(Purple, Purple2)) else Brush.linearGradient(listOf(Navy4, Navy4)))
                                        .then(if (isToday) Modifier.border(2.dp, Purple3, RoundedCornerShape(6.dp)) else Modifier),
                                        contentAlignment = Alignment.Center
                                    ) { Text("$dayNum", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isDone) Color.White else TextMuted) }
                                } else { Box(modifier = Modifier.weight(1f).aspectRatio(1f)) }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
        item {
            // Streak stats
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(Triple("12", "Current Streak", Orange), Triple("21", "Best Streak", Purple3), Triple("18", "Sessions / Month", Green), Triple("89%", "Consistency", Accent)).forEach { (v,l,c) ->
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Navy2).border(1.dp, Navy4, RoundedCornerShape(12.dp)).padding(12.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(v, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = c)
                            Text(l, fontSize = 10.sp, color = TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

// ─────────────────────────────────────────────
@Composable
fun ProgressTabContent(goals: List<Goal>) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Navy2), border = BorderStroke(1.dp, Navy4)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("📚 Course Goal Progress", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                    Spacer(Modifier.height(16.dp))
                    goals.forEach { goal ->
                        val cardColor = try { Color(android.graphics.Color.parseColor(goal.colorHex)) } catch (e: Exception) { Purple }
                        val anim by animateFloatAsState(targetValue = goal.progress, animationSpec = tween(1000), label = "prog-${goal.id}")
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(goal.icon, fontSize = 16.sp)
                            Column(modifier = Modifier.weight(1f)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(goal.title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                    Text("${goal.progressPct}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = cardColor)
                                }
                                Spacer(Modifier.height(4.dp))
                                LinearProgressIndicator(progress = { anim }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(10.dp)), color = cardColor, trackColor = Navy4)
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

// ─────────────────────────────────────────────
@Composable
fun BadgesTabContent() {
    val badges = listOf(
        Triple("🔥", "7-Day Streak", true), Triple("⚡", "Speed Learner", true),
        Triple("🎯", "Goal Crusher", true), Triple("🌙", "Night Owl", true),
        Triple("📚", "Bookworm", true), Triple("🏅", "First Session", true),
        Triple("⭐", "5-Star Week", true), Triple("🤝", "Consistent", true),
        Triple("💎", "30-Day Streak", false), Triple("🚀", "50h Month", false),
        Triple("👑", "Top Scholar", false), Triple("🧠", "Mind Master", false),
    )
    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Navy2), border = BorderStroke(1.dp, Navy4)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("🏆 All Achievements", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                    Spacer(Modifier.height(16.dp))
                    val rows = (badges.size + 3) / 4
                    repeat(rows) { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            repeat(4) { col ->
                                val idx = row * 4 + col
                                if (idx < badges.size) {
                                    val (icon, name, earned) = badges[idx]
                                    Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                        .background(if (earned) Purple.copy(alpha = 0.1f) else Navy3)
                                        .border(1.dp, if (earned) Purple.copy(alpha = 0.4f) else Navy4, RoundedCornerShape(12.dp))
                                        .padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(icon, fontSize = 26.sp, color = if (earned) Color.Unspecified else Color.Gray)
                                        Text(name, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, color = if (earned) Purple3 else TextMuted)
                                        if (!earned) Text("🔒", fontSize = 11.sp)
                                    }
                                } else Box(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

// ─────────────────────────────────────────────
@Composable
fun AddGoalDialog(onSave: (String, GoalType, String, Float, GoalUnit, GoalPriority) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(GoalType.WEEKLY) }
    var course by remember { mutableStateOf("All Courses") }
    var target by remember { mutableStateOf("5") }
    var selectedUnit by remember { mutableStateOf(GoalUnit.HOURS) }
    var selectedPriority by remember { mutableStateOf(GoalPriority.MEDIUM) }
    val courses = listOf("All Courses","Final Year Project","Artificial Intelligence","Machine Learning","Software Engineering","Networks & Security","Database Administration","Mobile Development","Research Methods")

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Navy2), border = BorderStroke(1.dp, Navy4)) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Text("🎯 Create New Goal", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = TextPrimary)
                Spacer(Modifier.height(20.dp))
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Goal Title") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Purple2, unfocusedBorderColor = Navy5, focusedContainerColor = Navy3, unfocusedContainerColor = Navy3, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary))
                Spacer(Modifier.height(14.dp))
                Text("Type", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GoalType.values().forEach { type ->
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                            .background(if (selectedType == type) Purple.copy(alpha = 0.2f) else Navy3)
                            .border(1.dp, if (selectedType == type) Purple.copy(alpha = 0.4f) else Navy4, RoundedCornerShape(8.dp))
                            .clickable { selectedType = type }.padding(vertical = 8.dp), contentAlignment = Alignment.Center
                        ) { Text(type.icon, fontSize = 12.sp) }
                    }
                }
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(value = target, onValueChange = { target = it }, label = { Text("Target") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Purple2, unfocusedBorderColor = Navy5, focusedContainerColor = Navy3, unfocusedContainerColor = Navy3, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary))
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Navy5)) { Text("Cancel", color = TextSecondary) }
                    Button(onClick = { if (title.isNotBlank()) onSave(title, selectedType, course, target.toFloatOrNull() ?: 5f, selectedUnit, selectedPriority) }, modifier = Modifier.weight(2f), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = Purple)) { Text("Create Goal", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
