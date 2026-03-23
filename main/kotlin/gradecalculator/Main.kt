package gradecalculator

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import gradecalculator.model.*
import gradecalculator.export.ExportEngine
import gradecalculator.console.ConsoleRunner
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import javax.swing.JFileChooser
import javax.swing.UIManager
import javax.swing.filechooser.FileNameExtensionFilter

fun main(args: Array<String>) {
    if (args.contains("--console") || args.contains("-c")) { ConsoleRunner.run(); return }
    try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()) } catch (_: Exception) {}
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "GradeOS — Multi-Student Grade Calculator",
            state = rememberWindowState(width = 1400.dp, height = 860.dp)
        ) { GradeApp() }
    }
}

// ── Colours ───────────────────────────────────────────────────────
object C {
    val BG       = Color(0xFF0D1117)
    val Surface  = Color(0xFF161B22)
    val Surface2 = Color(0xFF21262D)
    val Surface3 = Color(0xFF2D333B)
    val Border   = Color(0xFF30363D)
    val Accent   = Color(0xFFC8F542)
    val Blue     = Color(0xFF58A6FF)
    val Green    = Color(0xFF3FB950)
    val Yellow   = Color(0xFFD29922)
    val Orange   = Color(0xFFF97316)
    val Red      = Color(0xFFF85149)
    val Purple   = Color(0xFF9B59B6)
    val Gold     = Color(0xFFF5C542)
    val Text     = Color(0xFFE6EDF3)
    val Sub      = Color(0xFF8B949E)
    val Muted    = Color(0xFF484F58)
}

fun gradeColor(p: Double) = when {
    p >= 90 -> C.Green; p >= 80 -> C.Blue
    p >= 70 -> C.Yellow; p >= 60 -> C.Orange; else -> C.Red
}
fun letterColor(l: String) = when (l.firstOrNull()) {
    'A' -> C.Green; 'B' -> C.Blue; 'C' -> C.Yellow; 'D' -> C.Orange; else -> C.Red
}
fun majorColor(m: Major) = when (m) {
    Major.CYBERSECURITY        -> C.Red
    Major.SOFTWARE_ENGINEERING -> C.Blue
    Major.ISN                  -> C.Yellow
    Major.COMPUTER_SCIENCE     -> C.Green
}
fun distinctionColor(d: Distinction) = when (d) {
    Distinction.SUMMA_CUM_LAUDE -> C.Gold
    Distinction.MAGNA_CUM_LAUDE -> C.Accent
    Distinction.CUM_LAUDE       -> C.Blue
    Distinction.HONOURS         -> C.Purple
    Distinction.PASS            -> C.Green
    Distinction.FAIL            -> C.Red
}

// ── App screens ───────────────────────────────────────────────────
enum class Screen { DASHBOARD, ADD_STUDENT, STUDENT_DETAIL, IMPORT }

// ── ROOT ──────────────────────────────────────────────────────────
@Composable
fun GradeApp() {
    val students = remember { mutableStateListOf<Student>() }
    var screen   by remember { mutableStateOf(Screen.DASHBOARD) }
    var editing  by remember { mutableStateOf<Student?>(null) }
    var selected by remember { mutableStateOf<Student?>(null) }
    var toast    by remember { mutableStateOf("") }
    var toastOk  by remember { mutableStateOf(true) }

    fun showToast(msg: String, ok: Boolean = true) { toast = msg; toastOk = ok }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(Modifier.fillMaxSize().background(C.BG)) {

            Row(Modifier.fillMaxSize()) {
                // ── SIDEBAR ───────────────────────────────────────
                Sidebar(
                    students    = students,
                    screen      = screen,
                    selected    = selected,
                    onDashboard = { screen = Screen.DASHBOARD; selected = null },
                    onAdd       = { editing = Student(); screen = Screen.ADD_STUDENT },
                    onImport    = { screen = Screen.IMPORT },
                    onSelect    = { s -> selected = s; screen = Screen.STUDENT_DETAIL },
                    onDelete    = { s ->
                        students.remove(s)
                        if (selected?.id == s.id) { selected = null; screen = Screen.DASHBOARD }
                        showToast("Student removed")
                    },
                    onExportAll = { fmt -> exportAll(students, fmt, ::showToast) }
                )

                // ── MAIN PANEL ────────────────────────────────────
                Box(Modifier.fillMaxSize()) {
                    when (screen) {
                        Screen.DASHBOARD -> DashboardScreen(
                            students = students,
                            onSelect = { s -> selected = s; screen = Screen.STUDENT_DETAIL },
                            onAdd    = { editing = Student(); screen = Screen.ADD_STUDENT }
                        )
                        Screen.ADD_STUDENT -> AddEditScreen(
                            initial  = editing ?: Student(),
                            onSave   = { s ->
                                val idx = students.indexOfFirst { it.id == s.id }
                                if (idx >= 0) students[idx] = s else students.add(s)
                                selected = s; screen = Screen.STUDENT_DETAIL
                                showToast("${s.name} saved!")
                            },
                            onCancel = { screen = if (selected != null) Screen.STUDENT_DETAIL else Screen.DASHBOARD }
                        )
                        Screen.STUDENT_DETAIL -> selected?.let { s ->
                            // refresh from list in case edited
                            val fresh = students.firstOrNull { it.id == s.id } ?: s
                            StudentDetailScreen(
                                student  = fresh,
                                onEdit   = { editing = fresh; screen = Screen.ADD_STUDENT },
                                onExport = { fmt -> exportOne(fresh, fmt, ::showToast) }
                            )
                        } ?: DashboardScreen(students, { s -> selected = s; screen = Screen.STUDENT_DETAIL }, { editing = Student(); screen = Screen.ADD_STUDENT })
                        Screen.IMPORT -> ImportScreen(
                            onImported = { list ->
                                students.addAll(list)
                                screen = Screen.DASHBOARD
                                showToast("Imported ${list.size} student(s)!", true)
                            },
                            onCancel = { screen = Screen.DASHBOARD }
                        )
                    }
                }
            }

            // Toast
            if (toast.isNotEmpty()) {
                Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp)) {
                    Box(
                        Modifier.background(if (toastOk) C.Green.copy(.15f) else C.Red.copy(.15f), RoundedCornerShape(10.dp))
                            .border(1.dp, if (toastOk) C.Green.copy(.4f) else C.Red.copy(.4f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text(toast, color = if (toastOk) C.Green else C.Red, fontSize = 13.sp)
                    }
                    LaunchedEffect(toast) {
                        kotlinx.coroutines.delay(2500)
                        toast = ""
                    }
                }
            }
        }
    }
}

// ── SIDEBAR ───────────────────────────────────────────────────────
@Composable
fun Sidebar(
    students: List<Student>, screen: Screen, selected: Student?,
    onDashboard: () -> Unit, onAdd: () -> Unit, onImport: () -> Unit,
    onSelect: (Student) -> Unit, onDelete: (Student) -> Unit,
    onExportAll: (String) -> Unit
) {
    var exportMenu by remember { mutableStateOf(false) }

    Column(
        Modifier.width(260.dp).fillMaxHeight()
            .background(C.Surface)
            .border(BorderStroke(1.dp, C.Border))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Logo
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(bottom = 8.dp)) {
            Box(Modifier.size(36.dp).background(C.Accent, RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center) {
                Text("G", color = C.BG, fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
            Column {
                Text("GradeOS", color = C.Text, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text("Grade Calculator", color = C.Muted, fontSize = 10.sp)
            }
        }

        Divider(color = C.Border)

        // Nav buttons
        NavBtn("Dashboard", screen == Screen.DASHBOARD, onClick = onDashboard)
        NavBtn("Add Student", screen == Screen.ADD_STUDENT, onClick = onAdd)
        NavBtn("Import File", screen == Screen.IMPORT, onClick = onImport)

        Divider(color = C.Border)

        // Stats
        if (students.isNotEmpty()) {
            val avg = students.map { it.cumulativeGPA() }.average()
            Box(Modifier.fillMaxWidth().background(C.Surface2, RoundedCornerShape(8.dp)).padding(12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("CLASS OVERVIEW", color = C.Muted, fontSize = 9.sp, letterSpacing = 1.5.sp)
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Students", color = C.Sub, fontSize = 11.sp)
                        Text("${students.size}", color = C.Text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Avg GPA", color = C.Sub, fontSize = 11.sp)
                        Text("%.2f".format(avg), color = C.Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Pass rate", color = C.Sub, fontSize = 11.sp)
                        val passing = students.count { it.cumulativeGPA() >= 2.0 }
                        Text("$passing/${students.size}", color = C.Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Student list
        Text("STUDENTS", color = C.Muted, fontSize = 9.sp, letterSpacing = 1.5.sp, modifier = Modifier.padding(top = 4.dp))

        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            items(students) { s ->
                val isSelected = selected?.id == s.id
                val dist = s.distinction()
                Row(
                    Modifier.fillMaxWidth()
                        .background(if (isSelected) C.Surface3 else Color.Transparent, RoundedCornerShape(7.dp))
                        .clickable { onSelect(s) }
                        .padding(horizontal = 8.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isSelected) Box(Modifier.width(3.dp).height(18.dp).background(C.Accent, RoundedCornerShape(2.dp)))
                    Box(Modifier.size(28.dp).background(majorColor(s.major), CircleShape),
                        contentAlignment = Alignment.Center) {
                        Text(s.name.firstOrNull()?.toString() ?: "?", color = Color.White,
                            fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(s.name.ifBlank { "Unnamed" }, color = C.Text, fontSize = 12.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(s.matricule, color = C.Muted, fontSize = 10.sp)
                    }
                    Text("%.1f".format(s.cumulativeGPA()), color = distinctionColor(dist),
                        fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (students.isEmpty()) {
                item { Text("No students yet", color = C.Muted, fontSize = 11.sp, modifier = Modifier.padding(8.dp)) }
            }
        }

        Divider(color = C.Border)

        // Export all
        if (students.isNotEmpty()) {
            Box {
                Button(
                    onClick = { exportMenu = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = C.Surface2),
                    border = BorderStroke(1.dp, C.Border), shape = RoundedCornerShape(8.dp)
                ) { Text("Download All Results ▾", color = C.Accent, fontSize = 12.sp) }
                DropdownMenu(expanded = exportMenu, onDismissRequest = { exportMenu = false },
                    modifier = Modifier.background(C.Surface2)) {
                    for ((lbl, fmt) in listOf("Excel (.xlsx)" to "excel", "PDF (.pdf)" to "pdf",
                        "HTML (.html)" to "html", "XML (.xml)" to "xml", "CSV (.csv)" to "csv")) {
                        DropdownMenuItem(
                            text = { Text(lbl, color = C.Text, fontSize = 13.sp) },
                            onClick = { onExportAll(fmt); exportMenu = false }
                        )
                    }
                }
            }
        }
    }
}

// ── NAV BUTTON ────────────────────────────────────────────────────
@Composable
fun NavBtn(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth()
            .background(if (active) C.Surface3 else Color.Transparent, RoundedCornerShape(7.dp))
            .border(if (active) BorderStroke(1.dp, C.Border) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(7.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Text(label, color = if (active) C.Text else C.Sub, fontSize = 13.sp,
            fontWeight = if (active) FontWeight.Medium else FontWeight.Normal)
    }
}

// ── DASHBOARD ─────────────────────────────────────────────────────
@Composable
fun DashboardScreen(students: List<Student>, onSelect: (Student) -> Unit, onAdd: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {

        // Header
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Dashboard", color = C.Text, fontWeight = FontWeight.Bold, fontSize = 26.sp)
                Text("Overview of all students and their grades", color = C.Sub, fontSize = 13.sp)
            }
            Button(onClick = onAdd,
                colors = ButtonDefaults.buttonColors(containerColor = C.Accent),
                shape = RoundedCornerShape(9.dp)) {
                Text("+ Add Student", color = C.BG, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        if (students.isEmpty()) {
            // Empty state
            Box(Modifier.fillMaxWidth().height(300.dp)
                .background(C.Surface, RoundedCornerShape(12.dp))
                .border(1.dp, C.Border, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("🎓", fontSize = 52.sp)
                    Text("No students yet", color = C.Sub, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text("Click  + Add Student  or use  Import File  to get started",
                        color = C.Muted, fontSize = 12.sp)
                }
            }
            return@Column
        }

        // Summary stat cards
        val avgGPA     = students.map { it.cumulativeGPA() }.average()
        val avgPct     = students.map { it.overallPercentage() }.average()
        val passing    = students.count { it.cumulativeGPA() >= 2.0 }
        val topStudent = students.maxByOrNull { it.cumulativeGPA() }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Total Students", "${students.size}", "enrolled", C.Blue, Modifier.weight(1f))
            StatCard("Average GPA",    "%.2f".format(avgGPA), "class average", C.Accent, Modifier.weight(1f))
            StatCard("Average Score",  "%.1f%%".format(avgPct), "class average", C.Green, Modifier.weight(1f))
            StatCard("Pass Rate",      "$passing/${students.size}", "students passing", C.Yellow, Modifier.weight(1f))
            topStudent?.let {
                StatCard("Top Student", it.name.split(" ").first(), "%.2f GPA".format(it.cumulativeGPA()), C.Gold, Modifier.weight(1f))
            }
        }

        // Distinction breakdown
        Box(Modifier.fillMaxWidth()
            .background(C.Surface, RoundedCornerShape(12.dp))
            .border(1.dp, C.Border, RoundedCornerShape(12.dp))
            .padding(20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("DISTINCTION BREAKDOWN", color = C.Muted, fontSize = 10.sp, letterSpacing = 2.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Distinction.values().forEach { d ->
                        val count = students.count { it.distinction() == d }
                        val dc = distinctionColor(d)
                        Column(Modifier.weight(1f)
                            .background(dc.copy(.08f), RoundedCornerShape(9.dp))
                            .border(1.dp, dc.copy(.25f), RoundedCornerShape(9.dp))
                            .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("$count", color = dc, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                            Text(d.label, color = dc, fontSize = 9.sp, textAlign = TextAlign.Center,
                                letterSpacing = 0.5.sp, lineHeight = 13.sp)
                            Text("≥ ${d.minGPA} GPA", color = C.Muted, fontSize = 9.sp)
                        }
                    }
                }
            }
        }

        // All students table
        Text("ALL STUDENTS", color = C.Muted, fontSize = 10.sp, letterSpacing = 2.sp)

        Box(Modifier.fillMaxWidth()
            .background(C.Surface, RoundedCornerShape(12.dp))
            .border(1.dp, C.Border, RoundedCornerShape(12.dp))) {
            Column {
                // Table header
                Row(Modifier.fillMaxWidth()
                    .background(C.Surface2, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .padding(horizontal = 16.dp, vertical = 11.dp)) {
                    for ((t, w) in listOf("#" to .3f, "Name" to 1.8f, "Matricule" to 1f,
                        "Major" to 1.2f, "Avg %" to .7f, "GPA" to .6f, "Letter" to .6f, "Distinction" to 1.4f)) {
                        Text(t, color = C.Muted, fontSize = 10.sp, modifier = Modifier.weight(w),
                            letterSpacing = 1.sp,
                            textAlign = if (t == "Name" || t == "Distinction") TextAlign.Start else TextAlign.Center)
                    }
                }
                students.forEachIndexed { i, s ->
                    val gpa    = s.cumulativeGPA()
                    val pct    = s.overallPercentage()
                    val letter = if (pct > 0) GradeScale.fromPercentage(pct).letter else "N/A"
                    val dist   = s.distinction()
                    val dc     = distinctionColor(dist)
                    val lc     = if (letter != "N/A") letterColor(letter) else C.Muted

                    Row(Modifier.fillMaxWidth().clickable { onSelect(s) }
                        .background(if (i % 2 == 0) Color.Transparent else C.Surface2.copy(.3f))
                        .padding(horizontal = 16.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("${i+1}", color = C.Muted, fontSize = 12.sp, modifier = Modifier.weight(.3f), textAlign = TextAlign.Center)
                        Text(s.name, color = C.Text, fontSize = 13.sp, modifier = Modifier.weight(1.8f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(s.matricule, color = C.Sub, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Row(Modifier.weight(1.2f), horizontalArrangement = Arrangement.Center) {
                            Box(Modifier.background(majorColor(s.major).copy(.12f), RoundedCornerShape(5.dp))
                                .padding(horizontal = 7.dp, vertical = 3.dp)) {
                                Text(s.major.code, color = majorColor(s.major), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(if (pct > 0) "%.1f%%".format(pct) else "—", color = gradeColor(pct),
                            fontSize = 12.sp, modifier = Modifier.weight(.7f), textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold)
                        Text("%.2f".format(gpa), color = lc, fontSize = 12.sp,
                            modifier = Modifier.weight(.6f), textAlign = TextAlign.Center)
                        Text(letter, color = lc, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(.6f), textAlign = TextAlign.Center)
                        Row(Modifier.weight(1.4f)) {
                            Box(Modifier.background(dc.copy(.12f), RoundedCornerShape(5.dp))
                                .border(1.dp, dc.copy(.3f), RoundedCornerShape(5.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)) {
                                Text(dist.label, color = dc, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                    if (i < students.lastIndex) Divider(color = C.Border, thickness = 0.5.dp)
                }
            }
        }
    }
}

// ── ADD / EDIT STUDENT ────────────────────────────────────────────
@Composable
fun AddEditScreen(initial: Student, onSave: (Student) -> Unit, onCancel: () -> Unit) {
    var name     by remember { mutableStateOf(initial.name) }
    var mat      by remember { mutableStateOf(initial.matricule) }
    var major    by remember { mutableStateOf(initial.major) }
    var majOpen  by remember { mutableStateOf(false) }
    val courses  = remember { mutableStateListOf<CourseEntry>().also {
        it.addAll(if (initial.courses.isEmpty()) listOf(CourseEntry()) else initial.courses)
    }}
    var error    by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {

        // Header
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text(if (initial.name.isBlank()) "Add New Student" else "Edit — ${initial.name}",
                color = C.Text, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onCancel, border = BorderStroke(1.dp, C.Border),
                    shape = RoundedCornerShape(8.dp)) {
                    Text("Cancel", color = C.Sub, fontSize = 13.sp)
                }
                Button(onClick = {
                    error = ""
                    if (name.isBlank()) { error = "Please enter the student's name."; return@Button }
                    if (mat.isBlank())  { error = "Please enter the matricule."; return@Button }
                    val valid = courses.filter { it.courseName.isNotBlank() }
                    if (valid.isEmpty()) { error = "Add at least one course with a name."; return@Button }
                    onSave(Student(id = initial.id, name = name.trim(), matricule = mat.trim(),
                        major = major, courses = valid))
                }, colors = ButtonDefaults.buttonColors(containerColor = C.Accent),
                    shape = RoundedCornerShape(8.dp)) {
                    Text("Save & Calculate", color = C.BG, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Student info
            SCard("Student Information", Modifier.weight(1f)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FField("Full Name") {
                        ATF(name, { name = it }, "e.g. Jean-Paul Mbarga")
                    }
                    FField("Matricule") {
                        ATF(mat, { mat = it }, "e.g. 21T2874")
                    }
                    FField("Major") {
                        Box {
                            Row(Modifier.fillMaxWidth()
                                .background(C.Surface2, RoundedCornerShape(8.dp))
                                .border(1.dp, C.Border, RoundedCornerShape(8.dp))
                                .clickable { majOpen = true }
                                .padding(horizontal = 12.dp, vertical = 11.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(9.dp).background(majorColor(major), CircleShape))
                                    Text(major.displayName, color = C.Text, fontSize = 14.sp)
                                    Text("(${major.code})", color = C.Sub, fontSize = 11.sp)
                                }
                                Text("▾", color = C.Sub)
                            }
                            DropdownMenu(expanded = majOpen, onDismissRequest = { majOpen = false },
                                modifier = Modifier.background(C.Surface2)) {
                                Major.values().forEach { m ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalAlignment = Alignment.CenterVertically) {
                                                Box(Modifier.size(8.dp).background(majorColor(m), CircleShape))
                                                Text(m.displayName, color = C.Text, fontSize = 14.sp)
                                                Text("(${m.code})", color = C.Sub, fontSize = 11.sp)
                                            }
                                        },
                                        onClick = { major = m; majOpen = false }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Preview live GPA
            val previewStudent = Student(name = name, matricule = mat, major = major,
                courses = courses.filter { it.courseName.isNotBlank() })
            val gpa = previewStudent.cumulativeGPA()
            val dist = previewStudent.distinction()
            val dc = distinctionColor(dist)

            SCard("Live Preview", Modifier.weight(1f)) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(60.dp).background(majorColor(major), CircleShape),
                        contentAlignment = Alignment.Center) {
                        Text(name.firstOrNull()?.toString() ?: "?", color = Color.White,
                            fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    }
                    Text(name.ifBlank { "Student Name" }, color = C.Text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(mat.ifBlank { "Matricule" }, color = C.Sub, fontSize = 12.sp)
                    Divider(color = C.Border)
                    Text("%.2f".format(gpa), color = dc, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 38.sp)
                    Text("GPA", color = C.Muted, fontSize = 11.sp)
                    Box(Modifier.background(dc.copy(.12f), RoundedCornerShape(8.dp))
                        .border(1.dp, dc.copy(.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)) {
                        Text(dist.label, color = dc, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // Courses table
        SCard("Courses & Marks") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Headers
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Course Name",    color = C.Muted, fontSize = 10.sp, modifier = Modifier.weight(2.5f), letterSpacing = 1.sp)
                    Text("Credits",       color = C.Muted, fontSize = 10.sp, modifier = Modifier.width(52.dp), textAlign = TextAlign.Center, letterSpacing = 1.sp)
                    Text("Assignments\n/100", color = C.Muted, fontSize = 9.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, lineHeight = 13.sp)
                    Text("Midterm\n/100", color = C.Muted, fontSize = 9.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, lineHeight = 13.sp)
                    Text("Final Exam\n/100", color = C.Muted, fontSize = 9.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, lineHeight = 13.sp)
                    Text("Grade",         color = C.Muted, fontSize = 10.sp, modifier = Modifier.width(52.dp), textAlign = TextAlign.Center, letterSpacing = 1.sp)
                    Spacer(Modifier.width(28.dp))
                }
                courses.forEachIndexed { i, c ->
                    val grade  = c.calculate()
                    val letter = if (grade != null) GradeScale.fromPercentage(grade).letter else "—"
                    val lc     = if (grade != null) letterColor(letter) else C.Muted
                    Row(Modifier.fillMaxWidth()
                        .background(C.Surface2, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ATF(c.courseName, { courses[i] = c.copy(courseName = it) }, "Course ${i+1}", Modifier.weight(2.5f))
                        ATF(if (c.creditHours == 0) "" else c.creditHours.toString(),
                            { courses[i] = c.copy(creditHours = it.toIntOrNull() ?: 3) },
                            "3", Modifier.width(52.dp), centered = true)
                        MF(c.assignments, { courses[i] = c.copy(assignments = it) }, Modifier.weight(1f))
                        MF(c.midterm,     { courses[i] = c.copy(midterm = it) },     Modifier.weight(1f))
                        MF(c.finalExam,   { courses[i] = c.copy(finalExam = it) },   Modifier.weight(1f))
                        // Live letter grade
                        Box(Modifier.width(52.dp), contentAlignment = Alignment.Center) {
                            Text(letter, color = lc, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Box(Modifier.width(28.dp), contentAlignment = Alignment.Center) {
                            if (courses.size > 1)
                                Text("✕", color = C.Muted, fontSize = 13.sp,
                                    modifier = Modifier.clickable { courses.removeAt(i) })
                        }
                    }
                }
                // Add course
                Box(Modifier.fillMaxWidth().border(1.dp, C.Border, RoundedCornerShape(8.dp))
                    .clickable { courses.add(CourseEntry()) }.padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center) {
                    Text("＋  Add Course", color = C.Accent, fontSize = 13.sp)
                }
            }
        }

        if (error.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().background(C.Red.copy(.1f), RoundedCornerShape(8.dp))
                .border(1.dp, C.Red.copy(.35f), RoundedCornerShape(8.dp)).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("⚠  $error", color = C.Red, fontSize = 13.sp)
            }
        }
    }
}

// ── STUDENT DETAIL ────────────────────────────────────────────────
@Composable
fun StudentDetailScreen(student: Student, onEdit: () -> Unit, onExport: (String) -> Unit) {
    val gpa    = student.cumulativeGPA()
    val pct    = student.overallPercentage()
    val dist   = student.distinction()
    val dc     = distinctionColor(dist)
    val letter = if (pct > 0) GradeScale.fromPercentage(pct).letter else "N/A"
    val lc     = if (letter != "N/A") letterColor(letter) else C.Muted
    var expMenu by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {

        // Header row
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text("Student Report", color = C.Text, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onEdit, border = BorderStroke(1.dp, C.Border),
                    shape = RoundedCornerShape(8.dp)) {
                    Text("Edit", color = C.Sub, fontSize = 13.sp)
                }
                Box {
                    Button(onClick = { expMenu = true },
                        colors = ButtonDefaults.buttonColors(containerColor = C.Accent),
                        shape = RoundedCornerShape(8.dp)) {
                        Text("Download Report ▾", color = C.BG, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    DropdownMenu(expanded = expMenu, onDismissRequest = { expMenu = false },
                        modifier = Modifier.background(C.Surface2)) {
                        for ((lbl, fmt) in listOf("Excel (.xlsx)" to "excel", "PDF (.pdf)" to "pdf",
                            "HTML (.html)" to "html", "XML (.xml)" to "xml", "CSV (.csv)" to "csv")) {
                            DropdownMenuItem(
                                text = { Text(lbl, color = C.Text, fontSize = 13.sp) },
                                onClick = { onExport(fmt); expMenu = false }
                            )
                        }
                    }
                }
            }
        }

        // Profile + GPA card
        Box(Modifier.fillMaxWidth().background(C.Surface, RoundedCornerShape(14.dp))
            .border(1.dp, C.Border, RoundedCornerShape(14.dp)).padding(24.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(72.dp).background(majorColor(student.major), CircleShape),
                    contentAlignment = Alignment.Center) {
                    Text(student.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""),
                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(student.name, color = C.Text, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Chip2("🆔 ${student.matricule}", C.Blue)
                        Chip2("📚 ${student.major.displayName}", majorColor(student.major))
                        Chip2("${student.totalCredits()} credits", C.Sub)
                    }
                }
                // Distinction badge
                Box(Modifier.background(dc.copy(.1f), RoundedCornerShape(12.dp))
                    .border(1.dp, dc.copy(.35f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("DISTINCTION", color = C.Muted, fontSize = 9.sp, letterSpacing = 1.5.sp)
                        Text(dist.label, color = dc, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
                // GPA
                Column(horizontalAlignment = Alignment.End) {
                    Text("GPA", color = C.Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
                    Text("%.2f".format(gpa), color = dc, fontWeight = FontWeight.Bold,
                        fontSize = 40.sp, lineHeight = 42.sp)
                    Text("/ 4.00", color = C.Muted, fontSize = 11.sp)
                }
            }
        }

        // 3 stat cards
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Overall Average", "%.1f%%".format(pct), "across all courses", gradeColor(pct), Modifier.weight(1f))
            StatCard("Letter Grade", letter, "overall standing", lc, Modifier.weight(1f))
            StatCard("Courses Taken", "${student.courses.size}", "${student.totalCredits()} credit hours", C.Blue, Modifier.weight(1f))
        }

        // Per-course cards
        Text("COURSE BREAKDOWN", color = C.Muted, fontSize = 10.sp, letterSpacing = 2.sp)
        student.courses.forEach { c ->
            val cGrade  = c.calculate()
            val cLetter = if (cGrade != null) GradeScale.fromPercentage(cGrade).letter else "N/A"
            val cLc     = if (cLetter != "N/A") letterColor(cLetter) else C.Muted
            val cGc     = if (cGrade != null) gradeColor(cGrade) else C.Muted

            Box(Modifier.fillMaxWidth().background(C.Surface, RoundedCornerShape(10.dp))
                .border(1.dp, cLc.copy(.2f), RoundedCornerShape(10.dp)).padding(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(c.courseName, color = C.Text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text("${c.creditHours} credit hours  ·  Assign 30% / Midterm 30% / Final 40%",
                                color = C.Muted, fontSize = 10.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (cGrade != null) "%.1f%%".format(cGrade) else "—",
                                color = cGc, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                            Box(Modifier.size(42.dp).background(cLc.copy(.12f), RoundedCornerShape(9.dp))
                                .border(1.dp, cLc.copy(.35f), RoundedCornerShape(9.dp)),
                                contentAlignment = Alignment.Center) {
                                Text(cLetter, color = cLc, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                    if (cGrade != null) {
                        Box(Modifier.fillMaxWidth().height(5.dp).background(C.Surface2, RoundedCornerShape(3.dp))) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth((cGrade/100.0).toFloat().coerceIn(0f,1f))
                                .background(cGc, RoundedCornerShape(3.dp)))
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for ((lbl, v) in listOf("Assignments (30%)" to c.assignments,
                            "Midterm (30%)" to c.midterm, "Final Exam (40%)" to c.finalExam)) {
                            val vc = if (v != null) gradeColor(v) else C.Muted
                            Column(Modifier.weight(1f).background(C.Surface2, RoundedCornerShape(8.dp)).padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(lbl, color = C.Muted, fontSize = 9.sp, textAlign = TextAlign.Center)
                                Text(if (v != null) "%.1f / 100".format(v) else "—",
                                    color = vc, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                if (v != null) {
                                    Box(Modifier.fillMaxWidth().height(3.dp).background(C.Surface3, RoundedCornerShape(2.dp))) {
                                        Box(Modifier.fillMaxHeight().fillMaxWidth((v/100.0).toFloat().coerceIn(0f,1f))
                                            .background(vc, RoundedCornerShape(2.dp)))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Summary table
        Box(Modifier.fillMaxWidth().background(C.Surface, RoundedCornerShape(12.dp))
            .border(1.dp, C.Border, RoundedCornerShape(12.dp))) {
            Column {
                Row(Modifier.fillMaxWidth().background(C.Surface2, RoundedCornerShape(topStart=12.dp, topEnd=12.dp))
                    .padding(12.dp)) {
                    for ((t, w) in listOf("Course" to 2.5f, "Grade %" to 1f, "Letter" to .8f,
                        "GPA Pts" to .8f, "Credits" to .8f, "Distinction" to 1.3f)) {
                        Text(t, color = C.Muted, fontSize = 10.sp, modifier = Modifier.weight(w),
                            textAlign = if (t == "Course") TextAlign.Start else TextAlign.Center, letterSpacing = 1.sp)
                    }
                }
                student.courses.forEach { c ->
                    val g = c.calculate()
                    val l = if (g != null) GradeScale.fromPercentage(g).letter else "N/A"
                    val gp= if (g != null) GradeScale.fromPercentage(g).gpa    else 0.0
                    val d = Distinction.from(gp)
                    val lc2 = if (l != "N/A") letterColor(l) else C.Muted
                    val dc2 = distinctionColor(d)
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(c.courseName, color = C.Text, fontSize = 13.sp, modifier = Modifier.weight(2.5f))
                        Text(if (g != null) "%.1f%%".format(g) else "—", color = lc2, fontSize = 13.sp,
                            modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Text(l, color = lc2, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                            modifier = Modifier.weight(.8f), textAlign = TextAlign.Center)
                        Text("%.1f".format(gp), color = C.Sub, fontSize = 13.sp,
                            modifier = Modifier.weight(.8f), textAlign = TextAlign.Center)
                        Text("${c.creditHours}", color = C.Sub, fontSize = 13.sp,
                            modifier = Modifier.weight(.8f), textAlign = TextAlign.Center)
                        Row(Modifier.weight(1.3f)) {
                            Box(Modifier.background(dc2.copy(.1f), RoundedCornerShape(5.dp))
                                .border(1.dp, dc2.copy(.3f), RoundedCornerShape(5.dp))
                                .padding(horizontal = 7.dp, vertical = 3.dp)) {
                                Text(d.label, color = dc2, fontSize = 9.sp)
                            }
                        }
                    }
                    Divider(color = C.Border, thickness = 0.5.dp)
                }
                // Footer
                Row(Modifier.fillMaxWidth().background(C.Surface2,
                    RoundedCornerShape(bottomStart=12.dp, bottomEnd=12.dp)).padding(12.dp)) {
                    Text("CUMULATIVE", color = C.Sub, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                        modifier = Modifier.weight(2.5f), letterSpacing = 1.sp)
                    Text("%.1f%%".format(pct), color = dc, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Spacer(Modifier.weight(.8f))
                    Text("%.2f".format(gpa), color = dc, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                        modifier = Modifier.weight(.8f), textAlign = TextAlign.Center)
                    Text("${student.totalCredits()} cr", color = C.Sub, fontSize = 13.sp,
                        modifier = Modifier.weight(.8f), textAlign = TextAlign.Center)
                    Box(Modifier.weight(1.3f)) {
                        Box(Modifier.background(dc.copy(.1f), RoundedCornerShape(5.dp))
                            .border(1.dp, dc.copy(.3f), RoundedCornerShape(5.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)) {
                            Text(dist.label, color = dc, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ── IMPORT SCREEN ─────────────────────────────────────────────────
@Composable
fun ImportScreen(onImported: (List<Student>) -> Unit, onCancel: () -> Unit) {
    var status by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<List<Student>>(emptyList()) }

    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Import File", color = C.Text, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                Text("Upload an Excel, CSV, or text file with student data", color = C.Sub, fontSize = 13.sp)
            }
            OutlinedButton(onClick = onCancel, border = BorderStroke(1.dp, C.Border), shape = RoundedCornerShape(8.dp)) {
                Text("Cancel", color = C.Sub)
            }
        }

        // Format guide
        SCard("Expected File Format") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Your Excel/CSV file should have these columns (row 1 = headers, row 2+ = data):",
                    color = C.Sub, fontSize = 13.sp)
                Box(Modifier.fillMaxWidth().background(C.BG, RoundedCornerShape(8.dp))
                    .border(1.dp, C.Border, RoundedCornerShape(8.dp)).padding(14.dp)) {
                    Text("Name  |  Matricule  |  Major  |  Course1  |  Credits1  |  Assign1  |  Midterm1  |  Final1  |  Course2  |  ...",
                        color = C.Accent, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
                Text("Major options: Cybersecurity, Software Engineering, ISN, Computer Science",
                    color = C.Muted, fontSize = 11.sp)
                Text("Marks are out of 100. Leave blank if not yet graded.", color = C.Muted, fontSize = 11.sp)
            }
        }

        // Drop zone / browse button
        Box(Modifier.fillMaxWidth().height(160.dp)
            .background(C.Surface, RoundedCornerShape(12.dp))
            .border(2.dp, C.Border, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("📂", fontSize = 40.sp)
                Text("Click to browse for your file", color = C.Sub, fontSize = 14.sp)
                Text("Supported: .xlsx, .xls, .csv, .txt", color = C.Muted, fontSize = 11.sp)
                Button(
                    onClick = {
                        val fc = JFileChooser().apply {
                            dialogTitle = "Select student data file"
                            fileFilter = FileNameExtensionFilter(
                                "Spreadsheet / CSV files", "xlsx", "xls", "csv", "txt")
                        }
                        if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                            val file = fc.selectedFile
                            try {
                                val students = parseFile(file)
                                preview = students
                                status = "✓ Parsed ${students.size} student(s) from ${file.name}"
                                isError = false
                            } catch (e: Exception) {
                                status = "Error: ${e.message}"
                                isError = true
                                preview = emptyList()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = C.Surface2),
                    border = BorderStroke(1.dp, C.Border), shape = RoundedCornerShape(8.dp)
                ) { Text("Browse File", color = C.Accent, fontSize = 13.sp) }
            }
        }

        if (status.isNotEmpty()) {
            Row(Modifier.fillMaxWidth()
                .background((if (isError) C.Red else C.Green).copy(.1f), RoundedCornerShape(8.dp))
                .border(1.dp, (if (isError) C.Red else C.Green).copy(.3f), RoundedCornerShape(8.dp))
                .padding(12.dp)) {
                Text(status, color = if (isError) C.Red else C.Green, fontSize = 13.sp)
            }
        }

        // Preview table
        if (preview.isNotEmpty()) {
            Text("PREVIEW (${preview.size} students)", color = C.Muted, fontSize = 10.sp, letterSpacing = 2.sp)
            Box(Modifier.fillMaxWidth().background(C.Surface, RoundedCornerShape(10.dp))
                .border(1.dp, C.Border, RoundedCornerShape(10.dp))) {
                Column {
                    Row(Modifier.fillMaxWidth().background(C.Surface2,
                        RoundedCornerShape(topStart=10.dp, topEnd=10.dp)).padding(10.dp)) {
                        for (h in listOf("Name", "Matricule", "Major", "Courses", "GPA", "Distinction")) {
                            Text(h, color = C.Muted, fontSize = 10.sp,
                                modifier = Modifier.weight(1f), textAlign = TextAlign.Center, letterSpacing = 1.sp)
                        }
                    }
                    preview.take(8).forEach { s ->
                        val dist = s.distinction()
                        val dc = distinctionColor(dist)
                        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(s.name, color = C.Text, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            Text(s.matricule, color = C.Sub, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text(s.major.code, color = majorColor(s.major), fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text("${s.courses.size}", color = C.Sub, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text("%.2f".format(s.cumulativeGPA()), color = dc, fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                Box(Modifier.background(dc.copy(.1f), RoundedCornerShape(5.dp))
                                    .border(1.dp, dc.copy(.3f), RoundedCornerShape(5.dp))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)) {
                                    Text(dist.label, color = dc, fontSize = 9.sp)
                                }
                            }
                        }
                        Divider(color = C.Border, thickness = 0.5.dp)
                    }
                    if (preview.size > 8) {
                        Box(Modifier.fillMaxWidth().padding(10.dp), contentAlignment = Alignment.Center) {
                            Text("... and ${preview.size - 8} more", color = C.Muted, fontSize = 11.sp)
                        }
                    }
                }
            }

            Button(onClick = { onImported(preview) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = C.Accent),
                shape = RoundedCornerShape(9.dp)) {
                Text("Import ${preview.size} Student(s)", color = C.BG, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

// ── FILE PARSER ───────────────────────────────────────────────────
fun parseFile(file: File): List<Student> {
    val ext = file.extension.lowercase()
    return when {
        ext == "csv" || ext == "txt" -> parseCsv(file.readLines())
        ext == "xlsx" || ext == "xls" -> parseExcel(file)
        else -> throw Exception("Unsupported format: .$ext")
    }
}

fun parseCsv(lines: List<String>): List<Student> {
    if (lines.size < 2) throw Exception("File must have a header row and at least one data row.")
    val students = mutableListOf<Student>()
    for (line in lines.drop(1)) {
        if (line.isBlank()) continue
        val cols = line.split(",").map { it.trim().trim('"') }
        if (cols.size < 2) continue
        students.add(buildStudentFromCols(cols))
    }
    return students
}

fun parseExcel(file: File): List<Student> {
    val wb = WorkbookFactory.create(file)
    val sheet = wb.getSheetAt(0)
    val students = mutableListOf<Student>()
    for (r in 1..sheet.lastRowNum) {
        val row = sheet.getRow(r) ?: continue
        val cols = (0 until row.lastCellNum).map { i ->
            val cell = row.getCell(i)
            when (cell?.cellType) {
                org.apache.poi.ss.usermodel.CellType.NUMERIC -> {
                    val n = cell.numericCellValue
                    if (n == kotlin.math.floor(n)) n.toInt().toString() else "%.1f".format(n)
                }
                org.apache.poi.ss.usermodel.CellType.STRING -> cell.stringCellValue.trim()
                else -> ""
            }
        }
        if (cols.isEmpty() || cols[0].isBlank()) continue
        students.add(buildStudentFromCols(cols))
    }
    wb.close()
    return students
}

fun buildStudentFromCols(cols: List<String>): Student {
    val name = cols.getOrElse(0) { "" }
    val mat  = cols.getOrElse(1) { "" }
    val majorStr = cols.getOrElse(2) { "" }.lowercase()
    val major = when {
        "cyber"    in majorStr -> Major.CYBERSECURITY
        "software" in majorStr -> Major.SOFTWARE_ENGINEERING
        "isn"      in majorStr -> Major.ISN
        else                   -> Major.COMPUTER_SCIENCE
    }
    val courses = mutableListOf<CourseEntry>()
    var i = 3
    while (i < cols.size) {
        val cName = cols.getOrElse(i) { "" }
        if (cName.isBlank()) { i += 5; continue }
        val cr    = cols.getOrElse(i+1) { "3" }.toIntOrNull() ?: 3
        val asgn  = cols.getOrElse(i+2) { "" }.toDoubleOrNull()
        val mid   = cols.getOrElse(i+3) { "" }.toDoubleOrNull()
        val fin   = cols.getOrElse(i+4) { "" }.toDoubleOrNull()
        courses.add(CourseEntry(courseName = cName, creditHours = cr,
            assignments = asgn, midterm = mid, finalExam = fin))
        i += 5
    }
    return Student(name = name, matricule = mat, major = major, courses = courses)
}

// ── EXPORT ────────────────────────────────────────────────────────
fun exportAll(students: List<Student>, fmt: String, toast: (String) -> Unit) {
    val calcs = students.map { it.toGradeCalculator() }
    doExport(calcs, fmt, toast)
}

fun exportOne(student: Student, fmt: String, toast: (String) -> Unit) {
    doExport(listOf(student.toGradeCalculator()), fmt, toast)
}

fun gradecalculator.model.Student.toGradeCalculator(): gradecalculator.model.UniversityGradeCalculator {
    val calc = gradecalculator.model.UniversityGradeCalculator(
        name = this.name, credits = this.totalCredits(), department = this.major.displayName)
    for (c in this.courses) {
        val cat = gradecalculator.model.GradeCategory(name = c.courseName, weight = 100.0 / this.courses.size)
        val g = c.calculate()
        if (g != null) cat.grades.add(gradecalculator.model.Grade(name = c.courseName, score = g, total = 100.0))
        calc.addCategory(cat)
    }
    return calc
}

fun doExport(calcs: List<gradecalculator.model.GradeCalculator>, fmt: String, toast: (String) -> Unit) {
    try {
        val fc = JFileChooser().apply {
            dialogTitle = "Save file"
            val (ext, desc) = when (fmt) {
                "excel" -> "xlsx" to "Excel Files"
                "pdf"   -> "pdf"  to "PDF Files"
                "html"  -> "html" to "HTML Files"
                "xml"   -> "xml"  to "XML Files"
                else    -> "csv"  to "CSV Files"
            }
            fileFilter = FileNameExtensionFilter(desc, ext)
            selectedFile = File("grades_report.$ext")
        }
        if (fc.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return
        var path = fc.selectedFile.absolutePath
        if (!path.contains(".")) path += when(fmt) {
            "excel"->"xlsx"; "pdf"->"pdf"; "html"->"html"; "xml"->"xml"; else->"csv"
        }.let { ".$it" }

        val file = when (fmt) {
            "excel" -> ExportEngine.toExcel(calcs, path)
            "pdf"   -> ExportEngine.toPdf(calcs, path)
            "html"  -> ExportEngine.toHtml(calcs, path)
            "xml"   -> ExportEngine.toXml(calcs, path)
            else    -> ExportEngine.toCsv(calcs, path)
        }
        toast("✓ Saved: ${file.name}")
    } catch (e: Exception) {
        toast("⚠ Export failed: ${e.message?.take(60)}")
    }
}

// ── REUSABLE COMPONENTS ───────────────────────────────────────────
@Composable
fun StatCard(label: String, value: String, sub: String, color: Color, modifier: Modifier = Modifier) {
    Box(modifier.background(C.Surface, RoundedCornerShape(10.dp))
        .border(1.dp, C.Border, RoundedCornerShape(10.dp)).padding(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label.uppercase(), color = C.Muted, fontSize = 9.sp, letterSpacing = 1.5.sp)
            Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 26.sp)
            Text(sub, color = C.Muted, fontSize = 10.sp)
        }
    }
}

@Composable
fun SCard(title: String, modifier: Modifier = Modifier.fillMaxWidth(), content: @Composable () -> Unit) {
    Column(modifier.background(C.Surface, RoundedCornerShape(12.dp))
        .border(1.dp, C.Border, RoundedCornerShape(12.dp))) {
        Box(Modifier.fillMaxWidth().background(C.Surface2, RoundedCornerShape(topStart=12.dp, topEnd=12.dp))
            .padding(horizontal = 16.dp, vertical = 9.dp)) {
            Text(title.uppercase(), color = C.Sub, fontSize = 10.sp, letterSpacing = 2.sp)
        }
        Box(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
fun FField(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label.uppercase(), color = C.Muted, fontSize = 9.sp, letterSpacing = 1.5.sp)
        content()
    }
}

@Composable
fun ATF(value: String, onChange: (String) -> Unit, placeholder: String = "",
        modifier: Modifier = Modifier.fillMaxWidth(), centered: Boolean = false) {
    BasicTextField(value = value, onValueChange = onChange,
        modifier = modifier.background(C.Surface2, RoundedCornerShape(7.dp))
            .border(1.dp, C.Border, RoundedCornerShape(7.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        textStyle = androidx.compose.ui.text.TextStyle(color = C.Text, fontSize = 13.sp,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start),
        singleLine = true,
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(placeholder, color = C.Muted, fontSize = 13.sp,
                    textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                    modifier = if (centered) Modifier.fillMaxWidth() else Modifier)
                inner()
            }
        })
}

@Composable
fun MF(value: Double?, onChange: (Double?) -> Unit, modifier: Modifier) {
    var text by remember(value) {
        mutableStateOf(value?.let {
            if (it == kotlin.math.floor(it)) it.toInt().toString() else "%.1f".format(it)
        } ?: "")
    }
    val bc = when {
        text.isEmpty() -> C.Border
        text.toDoubleOrNull()?.let { it in 0.0..100.0 } == true -> {
            val v = text.toDouble()
            when { v >= 90 -> C.Green; v >= 70 -> C.Blue; v >= 50 -> C.Yellow; else -> C.Red }
        }
        else -> C.Red
    }
    BasicTextField(value = text,
        onValueChange = { s -> text = s; onChange(s.toDoubleOrNull()?.coerceIn(0.0, 100.0)) },
        modifier = modifier.background(C.BG, RoundedCornerShape(6.dp))
            .border(1.dp, bc, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 9.dp),
        textStyle = androidx.compose.ui.text.TextStyle(color = C.Text, fontSize = 12.sp,
            textAlign = TextAlign.Center, fontFamily = FontFamily.Monospace),
        singleLine = true,
        decorationBox = { inner ->
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (text.isEmpty()) Text("—", color = C.Muted, fontSize = 12.sp)
                inner()
            }
        })
}

@Composable
fun Chip2(text: String, color: Color) {
    Box(Modifier.background(color.copy(.12f), RoundedCornerShape(6.dp))
        .border(1.dp, color.copy(.3f), RoundedCornerShape(6.dp))
        .padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

