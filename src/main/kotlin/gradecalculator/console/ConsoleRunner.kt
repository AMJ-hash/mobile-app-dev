package gradecalculator.console

import gradecalculator.model.*
import gradecalculator.sensor.SensorManager
import gradecalculator.export.ExportEngine
import java.io.File

/**
 * ConsoleRunner
 * Full interactive console mode for the grade calculator.
 * Works independently — no GUI needed.
 */
object ConsoleRunner {

    private val courses: MutableList<GradeCalculator> = mutableListOf()
    private val sensorManager = SensorManager()
    private var student: Student? = null

    fun run() {
        printBanner()

        // Collect student profile upfront
        println("═══════════════════════════════════════════")
        println("  Student Profile Setup")
        println("═══════════════════════════════════════════")
        print("Full Name    : "); val name = readLine()?.trim() ?: ""
        print("Matricule    : "); val mat  = readLine()?.trim() ?: ""
        println("Major options:")
        Major.values().forEachIndexed { i, m -> println("  [${i+1}] ${m.displayName} (${m.code})") }
        print("Choose [1-4] : ")
        val majorIdx = (readLine()?.toIntOrNull() ?: 4) - 1
        val major = Major.values().getOrElse(majorIdx) { Major.COMPUTER_SCIENCE }
        student = Student(name, mat, major)
        println("\n✅ Welcome, ${student!!.name} | ${student!!.matricule} | ${student!!.major.displayName}\n")

        // Set up sensor logging to console
        sensorManager.onSensorLog = { log -> println("  [SENSOR] $log") }
        sensorManager.onShakeDetected = { println("  [SENSOR] 📳 Shake detected! Use 'reset' to clear grades.") }

        var running = true
        while (running) {
            print("\nGradeOS> ")
            val input = readLine()?.trim() ?: continue
            val parts = input.split(" ")
            val cmd = parts[0].lowercase()

            when (cmd) {
                "help"    -> printHelp()
                "add"     -> addCourseInteractive()
                "list"    -> listCourses()
                "select"  -> selectAndEditCourse(parts.getOrNull(1)?.toIntOrNull())
                "summary" -> printSummary()
                "sensor"  -> sensorMenu(parts.getOrNull(1))
                "export"  -> exportMenu(parts.getOrNull(1))
                "demo"    -> loadDemoData()
                "gpa"     -> printGPA()
                "quit", "exit" -> { running = false; println("Goodbye! 🎓") }
                "" -> {}
                else -> println("Unknown command: '$cmd'. Type 'help' for commands.")
            }
        }
    }

    private fun printBanner() {
        println("""
╔══════════════════════════════════════════════════════════╗
║                                                          ║
║   ██████╗ ██████╗  █████╗ ██████╗ ███████╗ ██████╗ ███████╗║
║  ██╔════╝ ██╔══██╗██╔══██╗██╔══██╗██╔════╝██╔═══██╗██╔════╝║
║  ██║  ███╗██████╔╝███████║██║  ██║█████╗  ██║   ██║███████╗║
║  ██║   ██║██╔══██╗██╔══██║██║  ██║██╔══╝  ██║   ██║╚════██║║
║  ╚██████╔╝██║  ██║██║  ██║██████╔╝███████╗╚██████╔╝███████║║
║   ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═╝╚═════╝ ╚══════╝ ╚═════╝ ╚══════╝║
║                                                          ║
║   Grade Calculator — Kotlin Desktop App v1.0             ║
║   Console Mode | OOP | Sensors | Multi-Export            ║
╚══════════════════════════════════════════════════════════╝
        """.trimIndent())
        println("Type 'help' to see commands or 'demo' to load sample data.\n")
    }

    private fun printHelp() {
        println("""
┌─────────────────────────────────────────────────────┐
│  COMMANDS                                           │
├─────────────────────────────────────────────────────┤
│  add            — Add a new course                  │
│  list           — List all courses                  │
│  select [n]     — Select and edit course by number  │
│  summary        — Show all courses summary          │
│  gpa            — Show cumulative GPA               │
│  sensor [start|stop|status|fire] — Sensor controls  │
│  export [excel|pdf|html|xml|csv] — Export grades    │
│  demo           — Load demonstration data           │
│  help           — Show this menu                    │
│  quit           — Exit console mode                 │
└─────────────────────────────────────────────────────┘""")
    }

    private fun addCourseInteractive() {
        println("\n── Add New Course ──")
        print("Course name: ")
        val name = readLine()?.trim() ?: return
        println("Calculator type:")
        println("  1. University Course")
        println("  2. High School Course")
        println("  3. Online Course")
        print("Choice [1-3]: ")

        val calc: GradeCalculator = when (readLine()?.trim()) {
            "1" -> {
                print("Credits [3]: ")
                val cr = readLine()?.toIntOrNull() ?: 3
                print("Department [General]: ")
                val dept = readLine()?.ifBlank { "General" } ?: "General"
                print("Semester [Semester 1]: ")
                val sem = readLine()?.ifBlank { "Semester 1" } ?: "Semester 1"
                UniversityGradeCalculator(name, cr, sem, dept)
            }
            "2" -> {
                print("Is AP? [y/N]: ")
                val ap = readLine()?.lowercase() == "y"
                print("Grade level [11]: ")
                val level = readLine()?.toIntOrNull() ?: 11
                HighSchoolGradeCalculator(name, 1, ap, false, level)
            }
            "3" -> {
                print("Platform [Coursera]: ")
                val plat = readLine()?.ifBlank { "Coursera" } ?: "Coursera"
                OnlineCourseCalculator(name, 3, plat)
            }
            else -> UniversityGradeCalculator(name)
        }

        // Add categories
        println("\nAdd grade categories (press Enter with empty name to finish):")
        while (true) {
            print("Category name (or Enter to finish): ")
            val catName = readLine()?.trim() ?: break
            if (catName.isEmpty()) break
            print("Weight % for '$catName': ")
            val weight = readLine()?.toDoubleOrNull() ?: 0.0
            print("Drop lowest? [y/N]: ")
            val drop = readLine()?.lowercase() == "y"
            calc.addCategory(GradeCategory(name = catName, weight = weight, dropLowest = drop))
            println("  ✓ Category '$catName' added (${weight}%)")
        }

        sensorManager.registerListener(calc)
        courses.add(calc)
        println("\n✅ Course '${calc.name}' added (${calc.getCalculatorType()})!")
    }

    private fun listCourses() {
        if (courses.isEmpty()) { println("No courses added yet. Use 'add' to add one."); return }
        println("\n── Courses ──────────────────────────────────────────")
        courses.forEachIndexed { i, c ->
            val pct = c.calculateGrade()
            val letter = c.getLetterGrade()
            println("  [${i+1}] %-30s  %6.1f%%  %-3s  GPA: %.1f  (%d cr)".format(
                c.name, pct, letter, c.getGPA(), c.credits))
        }
    }

    private fun selectAndEditCourse(idx: Int?) {
        if (courses.isEmpty()) { println("No courses. Use 'add' first."); return }
        val i = (idx ?: run {
            listCourses()
            print("\nSelect course number: ")
            readLine()?.toIntOrNull() ?: return
        }) - 1

        val calc = courses.getOrNull(i) ?: run { println("Invalid selection."); return }
        println("\n${calc.getSummary()}")

        var editing = true
        while (editing) {
            println("\n  [1] Add grade  [2] View categories  [3] Predictor  [4] Back")
            print("  > ")
            when (readLine()?.trim()) {
                "1" -> addGradeToCategory(calc)
                "2" -> showCategories(calc)
                "3" -> showPredictor(calc)
                "4" -> editing = false
            }
        }
    }

    private fun addGradeToCategory(calc: GradeCalculator) {
        val cats = calc.getCategories()
        if (cats.isEmpty()) { println("No categories in this course."); return }
        cats.forEachIndexed { i, c -> println("  [${i+1}] ${c.name} (${c.weight}%)") }
        print("Select category: ")
        val idx = (readLine()?.toIntOrNull() ?: return) - 1
        val cat = cats.getOrNull(idx) ?: return

        print("Assignment name: ")
        val aName = readLine()?.trim() ?: return
        print("Score: ")
        val score = readLine()?.toDoubleOrNull()
        print("Total points [100]: ")
        val total = readLine()?.toDoubleOrNull() ?: 100.0

        cat.grades.add(Grade(name = aName, score = score, total = total))
        val pct = score?.div(total)?.times(100)
        println("✓ Added: $aName — ${score ?: "N/A"} / $total ${if (pct != null) "(%.1f%%)".format(pct) else ""}")
        println(calc.getSummary())
    }

    private fun showCategories(calc: GradeCalculator) {
        for (cat in calc.getCategories()) {
            println("\n  ${cat.name} (${cat.weight}%)${if (cat.dropLowest) " [drop lowest]" else ""}")
            println("  Average: ${cat.getEffectiveAverage()?.let { "%.1f%%".format(it) } ?: "N/A"}")
            for (g in cat.grades) {
                val pct = g.score?.div(g.total)?.times(100)
                println("    • ${g.name}: ${g.score ?: "—"} / ${g.total}${if (pct != null) " (%.1f%%)".format(pct) else ""}")
            }
        }
    }

    private fun showPredictor(calc: GradeCalculator) {
        println("\n── Grade Predictor ──────────────────────────────────")
        for ((letter, target) in listOf("A" to 93.0, "B" to 83.0, "C" to 73.0, "D" to 63.0)) {
            val needed = calc.getNeededScore(target)
            val status = when {
                needed < 0 -> "Already achieved! ✓"
                needed > 100 -> "Not possible ✗"
                else -> "Need %.1f%% on remaining work".format(needed)
            }
            println("  To get $letter (≥${target.toInt()}%): $status")
        }
    }

    private fun printSummary() {
        if (courses.isEmpty()) { println("No courses."); return }
        println()
        for (c in courses) println(c.getSummary())
        printGPA()
    }

    private fun printGPA() {
        if (courses.isEmpty()) { println("No courses."); return }
        val cumulative = CumulativeGPACalculator()
        courses.forEach { cumulative.addCourse(it) }
        println(cumulative.getSummary())
    }

    private fun sensorMenu(arg: String?) {
        when (arg?.lowercase()) {
            "start" -> {
                sensorManager.startSimulation()
                println("✅ Sensors started! Reading every second.")
            }
            "stop" -> {
                sensorManager.stopSimulation()
                println("⛔ Sensors stopped.")
            }
            "status" -> println(sensorManager.getDashboard())
            "fire" -> {
                println("Fire sensor event:")
                println("  Types: AMBIENT_LIGHT, ACCELEROMETER, PROXIMITY, GYROSCOPE, MICROPHONE")
                print("  Type: ")
                val typeStr = readLine()?.trim()?.uppercase() ?: return
                val type = try { SensorType.valueOf(typeStr) } catch (e: Exception) { println("Invalid sensor type"); return }
                print("  Value: ")
                val value = readLine()?.toDoubleOrNull() ?: return
                sensorManager.fireSensorEvent(type, value)
                println("✓ Fired: $type = $value")
            }
            else -> println("Usage: sensor [start|stop|status|fire]")
        }
    }

    private fun exportMenu(format: String?) {
        if (courses.isEmpty()) { println("No courses to export."); return }
        val dir = File("exports").apply { mkdirs() }
        val ts = System.currentTimeMillis()

        val fmt = format ?: run {
            println("Export format: [1] Excel  [2] PDF  [3] HTML  [4] XML  [5] CSV  [6] All")
            print("> ")
            when (readLine()?.trim()) {
                "1" -> "excel"; "2" -> "pdf"; "3" -> "html"
                "4" -> "xml";   "5" -> "csv";  "6" -> "all"
                else -> "all"
            }
        }

        val formats = if (fmt == "all") listOf("excel","pdf","html","xml","csv") else listOf(fmt)
        for (f in formats) {
            try {
                val file = when (f) {
                    "excel" -> ExportEngine.toExcel(courses, "${dir.path}/grades_$ts.xlsx")
                    "pdf"   -> ExportEngine.toPdf(courses, "${dir.path}/grades_$ts.pdf")
                    "html"  -> ExportEngine.toHtml(courses, "${dir.path}/grades_$ts.html")
                    "xml"   -> ExportEngine.toXml(courses, "${dir.path}/grades_$ts.xml")
                    "csv"   -> ExportEngine.toCsv(courses, "${dir.path}/grades_$ts.csv")
                    else    -> null
                }
                if (file != null) println("✅ Exported: ${file.absolutePath}")
            } catch (e: Exception) {
                println("⚠ Export failed for $f: ${e.message}")
            }
        }
    }

    private fun loadDemoData() {
        println("Loading demo data...")
        val bio = UniversityGradeCalculator("Introduction to Biology", 3, "Semester 1", "Science")
        val bioHw = GradeCategory(name = "Homework", weight = 30.0, dropLowest = true, color = "#22c55e")
        bioHw.grades.addAll(listOf(
            Grade(name = "HW 1", score = 92.0), Grade(name = "HW 2", score = 78.0),
            Grade(name = "HW 3", score = 88.0), Grade(name = "HW 4", score = 95.0)
        ))
        val bioMid = GradeCategory(name = "Midterm", weight = 30.0)
        bioMid.grades.add(Grade(name = "Midterm Exam", score = 84.0))
        val bioFinal = GradeCategory(name = "Final", weight = 40.0)
        bioFinal.grades.add(Grade(name = "Final Exam", score = null))
        bio.addCategory(bioHw); bio.addCategory(bioMid); bio.addCategory(bioFinal)

        val math = HighSchoolGradeCalculator("AP Calculus BC", 1, isAP = true, gradeLevel = 12)
        val mathHw = GradeCategory(name = "Homework", weight = 20.0)
        mathHw.grades.addAll(listOf(
            Grade(name = "Problem Set 1", score = 95.0),
            Grade(name = "Problem Set 2", score = 88.0)
        ))
        val mathTests = GradeCategory(name = "Tests", weight = 50.0)
        mathTests.grades.add(Grade(name = "Unit 1 Test", score = 91.0))
        val mathFinal = GradeCategory(name = "AP Exam", weight = 30.0)
        math.addCategory(mathHw); math.addCategory(mathTests); math.addCategory(mathFinal)

        val python = OnlineCourseCalculator("Python for Data Science", 3, "Coursera", "Dec 31")
        python.logSession(45)
        val pyQuiz = GradeCategory(name = "Quizzes", weight = 40.0)
        pyQuiz.grades.addAll(listOf(
            Grade(name = "Quiz 1", score = 90.0), Grade(name = "Quiz 2", score = 85.0)
        ))
        val pyProject = GradeCategory(name = "Projects", weight = 60.0)
        pyProject.grades.add(Grade(name = "Final Project", score = 96.0))
        python.addCategory(pyQuiz); python.addCategory(pyProject)

        courses.addAll(listOf(bio, math, python))
        courses.forEach { sensorManager.registerListener(it) }

        println("✅ Demo data loaded: ${courses.size} courses!")
        listCourses()
    }
}
