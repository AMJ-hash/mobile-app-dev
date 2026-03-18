package gradecalculator

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
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
import java.io.File
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

fun main(args: Array<String>) {
    if (args.contains("--console") || args.contains("-c")) {
        ConsoleRunner.run()
    } else {
        application {
            Window(
                onCloseRequest = ::exitApplication,
                title = "GradeCalc Pro - Batch Student Grade Calculator",
                state = rememberWindowState(width = 1400.dp, height = 900.dp)
            ) {
                GradeApp()
            }
        }
    }
}

// ── Dark Black & Green Color Palette ────────────────────────────────────
object C {
    val BG       = Color(0xFF0F0F0F)      // Dark black background
    val Surface  = Color(0xFF1A1A1A)      // Slightly lighter black for cards
    val Surface2 = Color(0xFF2A2A2A)      // Even lighter black
    val Border   = Color(0xFF3D5C2B)      // Dark green border
    val Accent   = Color(0xFF4CAF50)      // Bright green
    val AccentLight = Color(0xFF1B5E20)   // Dark green
    val Blue     = Color(0xFF66BB6A)      // Light green variant
    val Green    = Color(0xFF43A047)      // Medium green
    val Yellow   = Color(0xFFAED581)      // Light yellow-green
    val Orange   = Color(0xFF7CB342)      // Yellow-green
    val Red      = Color(0xFFFF5252)      // Bright red
    val Text     = Color(0xFFE8F5E9)      // Light green text
    val Sub      = Color(0xFFA1D581)      // Light green secondary
    val Muted    = Color(0xFF558B2F)      // Muted green
    val Success  = Color(0xFF059669)
    val Warning  = Color(0xFFD97706)
}

// ── Data Models ────────────────────────────────────────────────────
data class StudentResult(
    val student: Student,
    val courses: List<CourseEntry>,
    val grades: List<Double?>,
    val cumulativeGPA: Double,
    val totalCredits: Int,
    val distinction: String = "Pass"
)

fun calculateDistinction(gpa: Double): String = when {
    gpa >= 3.7 -> "First Class Honours"
    gpa >= 3.3 -> "Upper Second Class"
    gpa >= 3.0 -> "Lower Second Class"
    gpa >= 2.0 -> "Third Class"
    else -> "Pass"
}

// ── ROOT APP ───────────────────────────────────────────────────────
@Composable
fun GradeApp() {
    var activeTab by remember { mutableStateOf(0) }
    var studentName by remember { mutableStateOf("") }
    var matricule by remember { mutableStateOf("") }
    var selectedMajor by remember { mutableStateOf(Major.COMPUTER_SCIENCE) }
    var majorOpen by remember { mutableStateOf(false) }
    val courses = remember { mutableStateListOf(CourseEntry()) }
    val allResults = remember { mutableStateListOf<StudentResult>() }
    var errorMsg by remember { mutableStateOf("") }
    var successMsg by remember { mutableStateOf("") }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Column(Modifier.fillMaxSize().background(C.BG)) {
            // HEADER
            Row(
                Modifier.fillMaxWidth().background(C.Surface)
                    .border(1.dp, C.Border)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(44.dp).background(C.Accent, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center) {
                        Text("G", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
                    }
                    Column {
                        Text("GradeCalc Pro", color = C.Text, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Text("Batch Student Grade Calculator", color = C.Sub, fontSize = 12.sp)
                    }
                }
                if (allResults.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { allResults.clear(); successMsg = "All results cleared" },
                            modifier = Modifier.height(40.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = C.Red),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(Modifier.width(6.dp))
                            Text("Clear", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        Button(
                            onClick = { doExportBatch(allResults, { successMsg = it }) },
                            modifier = Modifier.height(40.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = C.Success),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(Modifier.width(6.dp))
                            Text("Export All (${allResults.size})", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // TABS
            Row(
                Modifier.fillMaxWidth().background(C.Surface2)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                listOf("Add Student" to 0, "View Results" to 1).forEach { (label, idx) ->
                    Box(
                        Modifier.padding(vertical = 12.dp).clickable { activeTab = idx }
                            .border(if (activeTab == idx) 2.dp else 0.dp, if (activeTab == idx) C.Accent else Color.Transparent, RoundedCornerShape(0.dp))
                            .padding(bottom = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, color = if (activeTab == idx) C.Accent else C.Muted,
                            fontWeight = if (activeTab == idx) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
                Spacer(Modifier.weight(1f))
                if (allResults.isNotEmpty()) {
                    Text("${allResults.size} students", color = C.Muted, fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.CenterVertically).padding(horizontal = 16.dp))
                }
            }

            // CONTENT
            Box(Modifier.fillMaxSize().background(C.BG)) {
                if (activeTab == 0) {
                    InputStudentPanel(
                        studentName, { studentName = it },
                        matricule, { matricule = it },
                        selectedMajor, { selectedMajor = it },
                        majorOpen, { majorOpen = it },
                        courses,
                        errorMsg, { errorMsg = it },
                        successMsg, { successMsg = it },
                        { student, valid, tc, gpa ->
                            allResults.add(StudentResult(student, valid, valid.map { it.calculate() }, gpa, tc, calculateDistinction(gpa)))
                            studentName = ""
                            matricule = ""
                            courses.clear()
                            courses.add(CourseEntry())
                            selectedMajor = Major.COMPUTER_SCIENCE
                            errorMsg = ""
                            successMsg = "Student added successfully!"
                        }
                    )
                } else {
                    ResultsListPanel(allResults, { idx -> allResults.removeAt(idx) })
                }
            }
        }
    }
}

// ── INPUT PANEL ────────────────────────────────────────────────────
@Composable
fun InputStudentPanel(
    studentName: String, onNameChange: (String) -> Unit,
    matricule: String, onMatriculeChange: (String) -> Unit,
    selectedMajor: Major, onMajorChange: (Major) -> Unit,
    majorOpen: Boolean, onMajorOpenChange: (Boolean) -> Unit,
    courses: MutableList<CourseEntry>,
    errorMsg: String, onErrorChange: (String) -> Unit,
    successMsg: String, onSuccessChange: (String) -> Unit,
    onAddStudent: (Student, List<CourseEntry>, Int, Double) -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // FILE UPLOAD BUTTON
        Button(
            onClick = { uploadStudentFile { name, mat, major, loadedCourses ->
                onNameChange(name)
                onMatriculeChange(mat)
                onMajorChange(major)
                courses.clear()
                courses.addAll(loadedCourses)
                onSuccessChange("✓ Student data loaded from Excel!")
            }},
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = C.AccentLight),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(20.dp), tint = C.Accent)
            Spacer(Modifier.width(10.dp))
            Text("📁 Load from Excel", color = C.Accent, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
        
        // STUDENT INFO CARD
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = C.Surface),
            border = BorderStroke(1.dp, C.Border)
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Student Information", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = C.Text)
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Full Name", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = C.Muted)
                    SimpleTextField(studentName, { onNameChange(it); onErrorChange("") }, "e.g. John Doe")
                }
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Matricule", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = C.Muted)
                    SimpleTextField(matricule, { onMatriculeChange(it); onErrorChange("") }, "e.g. 21T2874")
                }
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Major", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = C.Muted)
                    Box {
                        Box(
                            Modifier.fillMaxWidth()
                                .background(C.Surface2, RoundedCornerShape(8.dp))
                                .border(1.dp, C.Border, RoundedCornerShape(8.dp))
                                .clickable { onMajorOpenChange(!majorOpen) }
                                .padding(12.dp)
                        ) {
                            Text(selectedMajor.displayName, color = C.Text, fontSize = 14.sp)
                        }
                        DropdownMenu(expanded = majorOpen, onDismissRequest = { onMajorOpenChange(false) }) {
                            Major.values().forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(m.displayName) },
                                    onClick = { onMajorChange(m); onMajorOpenChange(false) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // COURSES CARD
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = C.Surface),
            border = BorderStroke(1.dp, C.Border)
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Courses & Marks", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = C.Text)
                Text("Weights: Assignments 30%, Midterm 30%, Final Exam 40%", fontSize = 11.sp, color = C.Muted)
                
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    courses.forEachIndexed { i, course ->
                        CourseInputRow(course, i, 
                            { courses[i] = it },
                            if (courses.size > 1) {{ courses.removeAt(i) }} else null
                        )
                    }
                }
                
                Button(
                    onClick = { courses.add(CourseEntry()) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = C.AccentLight),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("+ Add Course", color = C.Accent, fontWeight = FontWeight.Medium)
                }
            }
        }

        // MESSAGES
        if (errorMsg.isNotEmpty()) {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                border = BorderStroke(1.dp, C.Red.copy(0.3f))
            ) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("⚠", fontSize = 16.sp)
                    Text(errorMsg, fontSize = 13.sp, color = C.Red)
                }
            }
        }

        if (successMsg.isNotEmpty()) {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                border = BorderStroke(1.dp, C.Success.copy(0.3f))
            ) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("✓", fontSize = 16.sp)
                    Text(successMsg, fontSize = 13.sp, color = C.Success)
                }
            }
        }

        // ADD BUTTON
        Button(
            onClick = {
                onErrorChange("")
                if (studentName.isBlank()) { onErrorChange("Enter student name"); return@Button }
                if (matricule.isBlank()) { onErrorChange("Enter matricule"); return@Button }
                val valid = courses.filter { it.courseName.isNotBlank() }
                if (valid.isEmpty()) { onErrorChange("Add at least one course"); return@Button }

                val student = Student(studentName.trim(), matricule.trim(), selectedMajor)
                val grades = valid.map { it.calculate() }
                val tc = valid.sumOf { it.creditHours }
                val gpa = if (tc == 0) 0.0 else {
                    valid.zip(grades).sumOf { (c, g) ->
                        (if (g != null) GradeScale.fromPercentage(g).gpa else 0.0) * c.creditHours
                    } / tc
                }
                onAddStudent(student, valid, tc, gpa)
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = C.Accent),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("ADD STUDENT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(Modifier.height(20.dp))
    }
}

// ── RESULTS PANEL ──────────────────────────────────────────────────
@Composable
fun ResultsListPanel(allResults: List<StudentResult>, onRemove: (Int) -> Unit) {
    if (allResults.isEmpty()) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("📚", fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text("No students yet", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = C.Text)
            Text("Add students from the 'Add Student' tab", fontSize = 13.sp, color = C.Muted)
        }
    } else {
        LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(allResults) { idx, result ->
                StudentResultCard(result, idx, { onRemove(idx) })
            }
        }
    }
}

@Composable
fun StudentResultCard(result: StudentResult, idx: Int, onRemove: () -> Unit) {
    var showExportMenu by remember { mutableStateOf(false) }
    var exportMsg by remember { mutableStateOf("") }

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = C.Surface),
        border = BorderStroke(1.dp, C.Border)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(result.student.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = C.Text)
                    Text("ID: ${result.student.matricule} | ${result.student.major.displayName}", fontSize = 11.sp, color = C.Sub)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box {
                        Button(
                            onClick = { showExportMenu = true },
                            modifier = Modifier.height(32.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = C.Accent),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                            Spacer(Modifier.width(4.dp))
                            Text("Export", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }

                        DropdownMenu(
                            expanded = showExportMenu,
                            onDismissRequest = { showExportMenu = false }
                        ) {
                            listOf("excel" to "Excel", "pdf" to "PDF", "html" to "HTML", "csv" to "CSV").forEach { (fmt, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        doExport(result, fmt, { exportMsg = it })
                                        showExportMenu = false
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = C.Red)
                    }
                }
            }

            if (exportMsg.isNotEmpty()) {
                Text(exportMsg, fontSize = 11.sp, color = if (exportMsg.startsWith("✓")) C.Green else C.Red)
            }

            // Stats
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) { StatBox("GPA", "%.2f".format(result.cumulativeGPA), whenGpaColor(result.cumulativeGPA)) }
                Box(Modifier.weight(1f)) { StatBox("Credits", "${result.totalCredits}", C.Blue) }
                Box(Modifier.weight(1f)) { StatBox("Distinction", result.distinction, whenDistinctionColor(result.distinction)) }
            }

            // Courses table
            Column(Modifier.fillMaxWidth().background(C.Surface2, RoundedCornerShape(8.dp)), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                result.courses.zip(result.grades).forEachIndexed { i, (course, grade) ->
                    val letter = if (grade != null) GradeScale.fromPercentage(grade).letter else "N/A"
                    Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(course.courseName, Modifier.weight(2f), fontSize = 12.sp, color = C.Text)
                        Text(if (grade != null) "%.1f%%".format(grade) else "—", Modifier.weight(1f), fontSize = 12.sp, color = C.Text, textAlign = TextAlign.Center)
                        Text(letter, Modifier.weight(0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = whenLetterColor(letter), textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String, color: Color) {
    Box(
        Modifier.background(color.copy(0.1f), RoundedCornerShape(8.dp))
            .border(1.dp, color.copy(0.3f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 10.sp, color = C.Muted)
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

// ── HELPER COMPONENTS ──────────────────────────────────────────────
@Composable
fun CourseInputRow(course: CourseEntry, index: Int, onUpdate: (CourseEntry) -> Unit, onDelete: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().background(C.Surface2, RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SimpleTextField(course.courseName, { onUpdate(course.copy(courseName = it)) }, "Course name", Modifier.weight(2f))
        SimpleTextField(
            if (course.creditHours == 0) "" else course.creditHours.toString(),
            { onUpdate(course.copy(creditHours = it.toIntOrNull() ?: 3)) },
            "CR", Modifier.width(40.dp)
        )
        SimpleTextField(course.assignments?.toString() ?: "", { onUpdate(course.copy(assignments = it.toDoubleOrNull())) }, "Assign", Modifier.weight(1f))
        SimpleTextField(course.midterm?.toString() ?: "", { onUpdate(course.copy(midterm = it.toDoubleOrNull())) }, "Mid", Modifier.weight(1f))
        SimpleTextField(course.finalExam?.toString() ?: "", { onUpdate(course.copy(finalExam = it.toDoubleOrNull())) }, "Final", Modifier.weight(1f))
        if (onDelete != null) {
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.Delete, contentDescription = "Remove course", modifier = Modifier.size(16.dp), tint = C.Red)
            }
        }
    }
}

@Composable
fun SimpleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.background(C.Surface, RoundedCornerShape(6.dp))
            .border(1.dp, C.Border, RoundedCornerShape(6.dp)).padding(8.dp),
        textStyle = androidx.compose.ui.text.TextStyle(color = C.Text, fontSize = 12.sp),
        singleLine = true,
        decorationBox = { inner ->
            if (value.isEmpty()) Text(placeholder, fontSize = 12.sp, color = C.Muted)
            inner()
        }
    )
}

// ── COLOR HELPERS ──────────────────────────────────────────────────
fun whenGpaColor(gpa: Double) = when {
    gpa >= 3.7 -> C.Green
    gpa >= 3.0 -> C.Blue
    gpa >= 2.0 -> C.Yellow
    else -> C.Red
}

fun whenDistinctionColor(d: String) = when {
    d.contains("First") -> C.Green
    d.contains("Upper") -> C.Blue
    d.contains("Lower") -> C.Yellow
    d.contains("Third") -> C.Orange
    else -> C.Muted
}

fun whenLetterColor(l: String) = when (l.firstOrNull()) {
    'A' -> C.Green
    'B' -> C.Blue
    'C' -> C.Yellow
    'D' -> C.Orange
    else -> C.Red
}

// ── EXPORT HELPER ──────────────────────────────────────────────────
fun doExportBatch(results: List<StudentResult>, onMsg: (String) -> Unit) {
    try {
        val chooser = javax.swing.JFileChooser()
        chooser.fileFilter = javax.swing.filechooser.FileNameExtensionFilter("Excel Files", "xlsx")
        chooser.selectedFile = java.io.File("grades_batch_${System.currentTimeMillis()}.xlsx")
        val result = chooser.showSaveDialog(null)

        if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
            val file = chooser.selectedFile
            val outputPath = if (file.extension == "xlsx") file.absolutePath else "${file.absolutePath}.xlsx"
            
            ExportEngine.toExcelBatch(results, outputPath)
            onMsg("✓ Exported ${results.size} students to ${java.io.File(outputPath).name}")
        }
    } catch (e: Exception) {
        onMsg("✗ Export failed: ${e.message?.take(30)}")
    }
}

fun doExport(result: StudentResult, fmt: String, onMsg: (String) -> Unit) {
    try {
        val chooser = javax.swing.JFileChooser()
        val extension = when (fmt) {
            "excel" -> "xlsx"
            "pdf" -> "pdf"
            "html" -> "html"
            else -> "csv"
        }
        chooser.fileFilter = javax.swing.filechooser.FileNameExtensionFilter("${fmt.uppercase()} Files", extension)
        chooser.selectedFile = java.io.File("grades_${result.student.name.replace(" ", "_")}_${System.currentTimeMillis()}.$extension")
        val dialogResult = chooser.showSaveDialog(null)

        if (dialogResult == javax.swing.JFileChooser.APPROVE_OPTION) {
            val file = chooser.selectedFile
            val outputPath = if (file.extension == extension) file.absolutePath else "${file.absolutePath}.$extension"

            val calcs = result.courses.mapIndexed { i, ce ->
                val calc = UniversityGradeCalculator(ce.courseName.ifBlank { "Course ${i+1}" }, ce.creditHours, department = result.student.major.displayName)
                val cat = GradeCategory(name = "Overall", weight = 100.0)
                val g = result.grades[i]
                if (g != null) cat.grades.add(Grade(name = "Final", score = g, total = 100.0))
                calc.addCategory(cat)
                calc
            }

            val exportedFile = when (fmt) {
                "excel" -> ExportEngine.toExcel(calcs, outputPath, result.student)
                "pdf" -> ExportEngine.toPdf(calcs, outputPath, result.student)
                "html" -> ExportEngine.toHtml(calcs, outputPath, result.student)
                else -> ExportEngine.toCsv(calcs, outputPath)
            }
            onMsg("✓ Exported ${result.student.name} to ${java.io.File(outputPath).name}")
        }
    } catch (e: Exception) {
        onMsg("✗ Export failed: ${e.message?.take(30)}")
    }
}

// ── FILE UPLOAD HANDLER ────────────────────────────────────────────
fun uploadStudentFile(onLoad: (String, String, Major, List<CourseEntry>) -> Unit) {
    try {
        val chooser = javax.swing.JFileChooser()
        chooser.fileFilter = javax.swing.filechooser.FileNameExtensionFilter("Excel Files", "xlsx", "xls")
        val result = chooser.showOpenDialog(null)
        
        if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
            val file = chooser.selectedFile
            if (file.extension == "xlsx" || file.extension == "xls") {
                val wb = org.apache.poi.xssf.usermodel.XSSFWorkbook(file.inputStream())
                val sheet = wb.getSheetAt(0)
                
                // Row 0: Student Name, Matricule, Major
                // Row 1+: Course Name, Credits, Assignment, Midterm, Final
                val name = sheet.getRow(0)?.getCell(0)?.stringCellValue ?: "Unknown"
                val mat = sheet.getRow(0)?.getCell(1)?.stringCellValue ?: ""
                val majorStr = sheet.getRow(0)?.getCell(2)?.stringCellValue ?: "COMPUTER_SCIENCE"
                val major = try { Major.valueOf(majorStr) } catch (e: Exception) { Major.COMPUTER_SCIENCE }
                
                val courses = mutableListOf<CourseEntry>()
                for (i in 1..sheet.lastRowNum) {
                    val row = sheet.getRow(i) ?: continue
                    val courseName = row.getCell(0)?.stringCellValue ?: ""
                    if (courseName.isNotBlank()) {
                        val credits = (row.getCell(1)?.numericCellValue ?: 3.0).toInt()
                        val assign = row.getCell(2)?.numericCellValue?.takeIf { it > 0 }
                        val midterm = row.getCell(3)?.numericCellValue?.takeIf { it > 0 }
                        val finalExam = row.getCell(4)?.numericCellValue?.takeIf { it > 0 }
                        courses.add(CourseEntry(courseName = courseName, creditHours = credits, assignments = assign, midterm = midterm, finalExam = finalExam))
                    }
                }
                wb.close()
                onLoad(name, mat, major, courses)
            }
        }
    } catch (e: Exception) {
        println("Error loading file: ${e.message}")
    }
}
