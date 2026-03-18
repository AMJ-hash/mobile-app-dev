package com.studypulse.app.ui.screens.subjects

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studypulse.app.data.subjects.Subject
import com.studypulse.app.data.subjects.SubjectRepository
import com.studypulse.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────
//  ViewModel
// ─────────────────────────────────────────────

enum class SubjectFilter { ALL, HIGH_PRIORITY, NEEDS_ATTENTION, HAS_EXAM }
enum class SubjectView { GRID, LIST }

data class SubjectsUiState(
    val subjects: List<Subject> = emptyList(),
    val filter: SubjectFilter = SubjectFilter.ALL,
    val view: SubjectView = SubjectView.GRID,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val showAddDialog: Boolean = false,
    val editingSubject: Subject? = null,
    val selectedSubject: Subject? = null
)

@HiltViewModel
class SubjectsViewModel @Inject constructor(
    private val repository: SubjectRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SubjectsUiState())
    val state: StateFlow<SubjectsUiState> = _state.asStateFlow()

    val filteredSubjects: StateFlow<List<Subject>> = combine(
        repository.subjects,
        _state.map { it.filter },
        _state.map { it.searchQuery }
    ) { subjects, filter, query ->
        subjects
            .filter { s ->
                val matchQ = s.name.contains(query, ignoreCase = true) || s.code.contains(query, ignoreCase = true)
                val matchF = when (filter) {
                    SubjectFilter.ALL -> true
                    SubjectFilter.HIGH_PRIORITY -> s.tags.any { it.contains("High Priority", ignoreCase = true) }
                    SubjectFilter.NEEDS_ATTENTION -> s.tags.any { it.contains("Needs Attention", ignoreCase = true) }
                    SubjectFilter.HAS_EXAM -> s.hasExam
                }
                matchQ && matchF
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val totalHours get() = repository.subjects.value.sumOf { it.hoursThisWeek.toDouble() }.toFloat()
    val mostStudied get() = repository.subjects.value.maxByOrNull { it.hoursThisWeek }

    fun setFilter(f: SubjectFilter) = _state.update { it.copy(filter = f) }
    fun setView(v: SubjectView) = _state.update { it.copy(view = v) }
    fun setSearch(q: String) = _state.update { it.copy(searchQuery = q) }
    fun openAddDialog() = _state.update { it.copy(showAddDialog = true, editingSubject = null) }
    fun openEditDialog(s: Subject) = _state.update { it.copy(showAddDialog = true, editingSubject = s) }
    fun closeDialog() = _state.update { it.copy(showAddDialog = false, editingSubject = null) }
    fun selectSubject(s: Subject?) = _state.update { it.copy(selectedSubject = s) }

    fun saveSubject(name: String, code: String, icon: String, colorHex: String, credits: Int, goalHours: Float) {
        viewModelScope.launch {
            val userId = "current_user" // Replace with actual auth UID
            val editing = _state.value.editingSubject
            if (editing != null) {
                repository.updateSubject(userId, editing.copy(name = name, code = code, icon = icon, colorHex = colorHex, credits = credits, weeklyGoalHours = goalHours))
            } else {
                repository.addSubject(userId, Subject(name = name, code = code, icon = icon, colorHex = colorHex, credits = credits, weeklyGoalHours = goalHours))
            }
            closeDialog()
        }
    }

    fun deleteSubject(id: String) {
        viewModelScope.launch {
            repository.deleteSubject("current_user", id)
        }
    }
}

// ─────────────────────────────────────────────
//  Screen
// ─────────────────────────────────────────────

@Composable
fun SubjectsScreen(
    viewModel: SubjectsViewModel = hiltViewModel(),
    onStartStudy: (Subject) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val subjects by viewModel.filteredSubjects.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(Navy)) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                SubjectsHeader(onAddClick = { viewModel.openAddDialog() })
            }

            // Summary
            item {
                SummaryBar(subjects = subjects)
            }

            // Search
            item {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.setSearch(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search courses...", color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = TextMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Purple2, unfocusedBorderColor = Navy5,
                        focusedContainerColor = Navy2, unfocusedContainerColor = Navy2,
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                    )
                )
            }

            // Filter chips
            item {
                FilterRow(
                    selected = state.filter,
                    view = state.view,
                    onFilter = { viewModel.setFilter(it) },
                    onView = { viewModel.setView(it) }
                )
            }

            // Grid or List
            if (state.view == SubjectView.GRID) {
                item {
                    SubjectsGrid(
                        subjects = subjects,
                        onTap = { viewModel.selectSubject(it) },
                        onEdit = { viewModel.openEditDialog(it) },
                        onStudy = onStartStudy,
                        onAdd = { viewModel.openAddDialog() }
                    )
                }
            } else {
                items(subjects) { subject ->
                    SubjectListRow(
                        subject = subject,
                        onTap = { viewModel.selectSubject(it) },
                        onStudy = onStartStudy,
                        onEdit = { viewModel.openEditDialog(it) }
                    )
                }
                item {
                    // Add button at bottom of list
                    OutlinedButton(
                        onClick = { viewModel.openAddDialog() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Navy5)
                    ) {
                        Icon(Icons.Default.Add, null, tint = TextSecondary)
                        Spacer(Modifier.width(8.dp))
                        Text("Add New Course", color = TextSecondary)
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }

        // Add/Edit Dialog
        if (state.showAddDialog) {
            AddSubjectDialog(
                editing = state.editingSubject,
                onSave = { name, code, icon, color, credits, goal ->
                    viewModel.saveSubject(name, code, icon, color, credits, goal)
                },
                onDismiss = { viewModel.closeDialog() }
            )
        }

        // Detail bottom sheet
        state.selectedSubject?.let { subject ->
            SubjectDetailDialog(
                subject = subject,
                onDismiss = { viewModel.selectSubject(null) },
                onStudy = { onStartStudy(subject); viewModel.selectSubject(null) }
            )
        }
    }
}

// ─────────────────────────────────────────────
@Composable
fun SubjectsHeader(onAddClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text("📚 My Courses", fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = TextPrimary)
            Text("Year 4 · Computer Science", fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(top = 2.dp))
        }
        Button(
            onClick = onAddClick,
            colors = ButtonDefaults.buttonColors(containerColor = Purple),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Add Course", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
}

// ─────────────────────────────────────────────
@Composable
fun SummaryBar(subjects: List<Subject>) {
    val totalHours = subjects.sumOf { it.hoursThisWeek.toDouble() }.toFloat()
    val avgHours = if (subjects.isNotEmpty()) totalHours / subjects.size else 0f
    val topSubject = subjects.maxByOrNull { it.hoursThisWeek }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SummaryCard("Courses", "${subjects.size}", Purple3, "📚", Modifier.weight(1f))
        SummaryCard("This Week", "${String.format("%.1f", totalHours)}h", Green, "⏱️", Modifier.weight(1f))
        SummaryCard("Average", "${String.format("%.1f", avgHours)}h", Orange, "📊", Modifier.weight(1f))
        SummaryCard("Top", topSubject?.code?.takeLast(3) ?: "–", Accent, "🏆", Modifier.weight(1f))
    }
}

@Composable
fun SummaryCard(label: String, value: String, color: Color, icon: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier
        .clip(RoundedCornerShape(14.dp))
        .background(Navy2)
        .border(1.dp, Navy4, RoundedCornerShape(14.dp))
        .padding(14.dp)
    ) {
        Column {
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = color)
            Text(label, fontSize = 10.sp, color = TextSecondary, modifier = Modifier.padding(top = 3.dp),
                letterSpacing = 0.3.sp)
        }
        Text(icon, fontSize = 22.sp, modifier = Modifier.align(Alignment.TopEnd), color = color.copy(alpha = 0.15f))
    }
}

// ─────────────────────────────────────────────
@Composable
fun FilterRow(selected: SubjectFilter, view: SubjectView, onFilter: (SubjectFilter) -> Unit, onView: (SubjectView) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
            listOf(
                SubjectFilter.ALL to "All",
                SubjectFilter.HIGH_PRIORITY to "🔥 Priority",
                SubjectFilter.NEEDS_ATTENTION to "⚠️ Low",
                SubjectFilter.HAS_EXAM to "📝 Exam"
            ).forEach { (filter, label) ->
                FilterChip(label = label, isSelected = selected == filter, onClick = { onFilter(filter) })
            }
        }
        Row(modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Navy3)
            .border(1.dp, Navy4, RoundedCornerShape(8.dp))
            .padding(3.dp)
        ) {
            listOf(SubjectView.GRID to "⊞", SubjectView.LIST to "☰").forEach { (v, icon) ->
                Box(modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (view == v) Navy4 else Color.Transparent)
                    .clickable { onView(v) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                ) { Text(icon, fontSize = 14.sp, color = if (view == v) TextPrimary else TextMuted) }
            }
        }
    }
}

@Composable
fun FilterChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier
        .clip(RoundedCornerShape(50.dp))
        .background(if (isSelected) Purple.copy(alpha = 0.2f) else Navy3)
        .border(1.dp, if (isSelected) Purple.copy(alpha = 0.4f) else Navy4, RoundedCornerShape(50.dp))
        .clickable(onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            color = if (isSelected) Accent else TextSecondary)
    }
}

// ─────────────────────────────────────────────
@Composable
fun SubjectsGrid(subjects: List<Subject>, onTap: (Subject) -> Unit, onEdit: (Subject) -> Unit, onStudy: (Subject) -> Unit, onAdd: () -> Unit) {
    val cols = 2
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        val rows = (subjects.size + cols - 1) / cols
        repeat(rows) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                repeat(cols) { col ->
                    val idx = row * cols + col
                    if (idx < subjects.size) {
                        SubjectGridCard(subject = subjects[idx], onTap = onTap, onEdit = onEdit, onStudy = onStudy, modifier = Modifier.weight(1f))
                    } else {
                        // Add card
                        Box(modifier = Modifier
                            .weight(1f)
                            .height(200.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Navy2)
                            .border(1.dp, Navy4, RoundedCornerShape(18.dp))
                            .clickable { onAdd() },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Add, null, tint = TextMuted, modifier = Modifier.size(32.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("Add Course", fontSize = 12.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubjectGridCard(subject: Subject, onTap: (Subject) -> Unit, onEdit: (Subject) -> Unit, onStudy: (Subject) -> Unit, modifier: Modifier = Modifier) {
    val cardColor = try { Color(android.graphics.Color.parseColor(subject.colorHex)) } catch (e: Exception) { Purple }
    Card(
        modifier = modifier.clickable { onTap(subject) },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Navy2),
        border = BorderStroke(1.dp, cardColor.copy(alpha = 0.2f))
    ) {
        Column {
            // Color top bar
            Box(modifier = Modifier.fillMaxWidth().height(5.dp).background(cardColor))
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(cardColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center) { Text(subject.icon, fontSize = 20.sp) }
                    IconButton(onClick = { onEdit(subject) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.MoreVert, null, tint = TextMuted, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(subject.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(subject.code, fontSize = 11.sp, color = TextMuted, modifier = Modifier.padding(top = 2.dp))
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${String.format("%.0f", subject.progress * 100)}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = cardColor)
                    Text("${subject.hoursThisWeek}h / ${subject.weeklyGoalHours}h", fontSize = 11.sp, color = TextSecondary)
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { subject.progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(10.dp)),
                    color = cardColor, trackColor = Navy4
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = { onStudy(subject) }, modifier = Modifier.fillMaxWidth().height(34.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = cardColor),
                    contentPadding = PaddingValues(0.dp)
                ) { Text("▶ Study", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

// ─────────────────────────────────────────────
@Composable
fun SubjectListRow(subject: Subject, onTap: (Subject) -> Unit, onStudy: (Subject) -> Unit, onEdit: (Subject) -> Unit) {
    val cardColor = try { Color(android.graphics.Color.parseColor(subject.colorHex)) } catch (e: Exception) { Purple }
    Row(modifier = Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .background(Navy2)
        .border(1.dp, Navy4, RoundedCornerShape(14.dp))
        .clickable { onTap(subject) }
        .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.width(3.dp).height(44.dp).clip(RoundedCornerShape(4.dp)).background(cardColor))
        Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(cardColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center) { Text(subject.icon, fontSize = 20.sp) }
        Column(modifier = Modifier.weight(1f)) {
            Text(subject.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${subject.code} · ${subject.credits} credits", fontSize = 11.sp, color = TextMuted)
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { subject.progress },
                modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(10.dp)),
                color = cardColor, trackColor = Navy4
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${subject.hoursThisWeek}h", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = cardColor)
            Text("of ${subject.weeklyGoalHours}h", fontSize = 10.sp, color = TextMuted)
        }
        Button(onClick = { onStudy(subject) }, shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = cardColor),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.height(32.dp)
        ) { Text("▶", fontSize = 12.sp) }
    }
}

// ─────────────────────────────────────────────
@Composable
fun AddSubjectDialog(editing: Subject?, onSave: (String, String, String, String, Int, Float) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var code by remember { mutableStateOf(editing?.code ?: "") }
    var credits by remember { mutableStateOf(editing?.credits?.toString() ?: "3") }
    var goal by remember { mutableStateOf(editing?.weeklyGoalHours?.toString() ?: "5") }
    val icons = listOf("🎓","🤖","🧠","⚙️","🔒","🗄️","📱","📝","📊","🔬","💡","🌐")
    var selectedIcon by remember { mutableStateOf(editing?.icon ?: "📚") }
    val colors = listOf("#7c3aed","#3b82f6","#10b981","#ef4444","#f59e0b","#ec4899","#06b6d4","#8b5cf6")
    var selectedColor by remember { mutableStateOf(editing?.colorHex ?: "#7c3aed") }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Navy2),
            border = BorderStroke(1.dp, Navy4)) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Text(if (editing != null) "Edit Course" else "Add New Course",
                    fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = TextPrimary)
                Spacer(Modifier.height(20.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Course Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Purple2, unfocusedBorderColor = Navy5, focusedContainerColor = Navy3, unfocusedContainerColor = Navy3, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary))
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Code") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Purple2, unfocusedBorderColor = Navy5, focusedContainerColor = Navy3, unfocusedContainerColor = Navy3, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary))
                    OutlinedTextField(value = credits, onValueChange = { credits = it }, label = { Text("Credits") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Purple2, unfocusedBorderColor = Navy5, focusedContainerColor = Navy3, unfocusedContainerColor = Navy3, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary))
                    OutlinedTextField(value = goal, onValueChange = { goal = it }, label = { Text("Goal (h)") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Purple2, unfocusedBorderColor = Navy5, focusedContainerColor = Navy3, unfocusedContainerColor = Navy3, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary))
                }
                Spacer(Modifier.height(16.dp))
                Text("Icon", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    icons.take(8).forEach { icon ->
                        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                            .background(if (selectedIcon == icon) Purple.copy(alpha = 0.3f) else Navy3)
                            .border(1.dp, if (selectedIcon == icon) Purple2 else Navy4, RoundedCornerShape(8.dp))
                            .clickable { selectedIcon = icon },
                            contentAlignment = Alignment.Center
                        ) { Text(icon, fontSize = 18.sp) }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Color", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colors.forEach { hex ->
                        val c = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Purple }
                        Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(c)
                            .border(2.dp, if (selectedColor == hex) Color.White else Color.Transparent, CircleShape)
                            .clickable { selectedColor = hex })
                    }
                }
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Navy5)) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Button(onClick = {
                        if (name.isNotBlank()) onSave(name, code, selectedIcon, selectedColor, credits.toIntOrNull() ?: 3, goal.toFloatOrNull() ?: 5f)
                    }, modifier = Modifier.weight(2f), shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Purple)
                    ) { Text("Save Course", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
@Composable
fun SubjectDetailDialog(subject: Subject, onDismiss: () -> Unit, onStudy: () -> Unit) {
    val cardColor = try { Color(android.graphics.Color.parseColor(subject.colorHex)) } catch (e: Exception) { Purple }
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Navy2), border = BorderStroke(1.dp, Navy4)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(4.dp)).background(cardColor))
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(cardColor.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) { Text(subject.icon, fontSize = 26.sp) }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(subject.name, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = TextPrimary)
                        Text("${subject.code} · ${subject.credits} credits", fontSize = 12.sp, color = TextMuted)
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = TextMuted) }
                }
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(
                        Triple("${subject.hoursThisWeek}h", "This Week", cardColor),
                        Triple("${String.format("%.0f", subject.progress * 100)}%", "Goal Progress", Green),
                        Triple("${subject.weeklyGoalHours}h", "Weekly Goal", Accent)
                    ).forEach { (val_, label, color) ->
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Navy3).padding(12.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(val_, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = color)
                                Text(label, fontSize = 10.sp, color = TextSecondary, modifier = Modifier.padding(top = 3.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Navy5)) { Text("Close", color = TextSecondary) }
                    Button(onClick = onStudy, modifier = Modifier.weight(2f), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = cardColor)) { Text("▶ Start Studying", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
