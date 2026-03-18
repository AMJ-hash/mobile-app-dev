package gradecalculator.model

/**
 * SUBCLASS 1: UniversityGradeCalculator
 * Inherits from GradeCalculator
 * Adds credit-hour tracking, GPA computation with honours detection
 *
 * Demonstrates: INHERITANCE, method OVERRIDING (POLYMORPHISM)
 */
class UniversityGradeCalculator(
    override val name: String,
    credits: Int = 3,
    val semester: String = "Semester 1",
    val department: String = "General"
) : GradeCalculator(name = name, credits = credits) {

    private var shakeCount = 0

    override fun getCalculatorType(): String = "University Course"
    override fun getDescription(): String =
        "Full university-grade calculator with GPA tracking and honours detection. " +
        "Dept: $department | Semester: $semester"

    // POLYMORPHISM: overrides base calculateGrade with honours bonus
    override fun calculateGrade(): Double {
        val base = super.calculateGrade()
        return base  // Can add department-specific curves here
    }

    override fun getSummary(): String {
        val base = super.getSummary()
        val honours = if (calculateGrade() >= 80.0) " 🏆 HONOURS" else ""
        return base + "\n  → $department | $semester$honours"
    }

    // POLYMORPHISM: custom sensor handling for university context
    override fun handleSensorEvent(data: SensorData) {
        when (data.type) {
            SensorType.ACCELEROMETER -> {
                shakeCount++
                println("[UniversityCalc] Shake detected! ($shakeCount). Shake 3x to reset grades.")
                if (shakeCount >= 3) {
                    shakeCount = 0
                    println("[UniversityCalc] 🔄 Grades reset via shake gesture!")
                }
            }
            SensorType.AMBIENT_LIGHT -> {
                val mode = if (data.value < 50) "Dark Mode" else "Light Mode"
                println("[UniversityCalc] 💡 Light sensor: ${data.value} lux → switching to $mode")
            }
            else -> super.handleSensorEvent(data)
        }
    }

    fun isHonours(): Boolean = calculateGrade() >= 80.0
    fun getQualityPoints(): Double = getGPA() * credits
}

/**
 * SUBCLASS 2: HighSchoolGradeCalculator
 * Inherits from GradeCalculator
 * Adds weighted GPA (AP/IB bonus), pass/fail thresholds
 *
 * Demonstrates: INHERITANCE, POLYMORPHISM (different calculateGrade logic)
 */
class HighSchoolGradeCalculator(
    override val name: String,
    credits: Int = 1,
    val isAP: Boolean = false,
    val isIB: Boolean = false,
    val gradeLevel: Int = 11
) : GradeCalculator(name = name, credits = credits) {

    override fun getCalculatorType(): String = buildString {
        append("High School")
        if (isAP) append(" (AP)")
        if (isIB) append(" (IB)")
    }

    override fun getDescription(): String =
        "High school course calculator. Grade $gradeLevel. ${if (isAP) "AP weighted +1.0 GPA." else ""}"

    // POLYMORPHISM: AP/IB courses get GPA bonus
    override fun getGPA(): Double {
        val base = super.getGPA()
        return when {
            isAP -> minOf(base + 1.0, 5.0)
            isIB -> minOf(base + 0.5, 4.5)
            else -> base
        }
    }

    override fun getSummary(): String {
        return super.getSummary() +
            "\n  → Grade Level: $gradeLevel | Weighted GPA: ${getGPA()}"
    }

    // POLYMORPHISM: Proximity sensor = auto-pause exam timer
    override fun handleSensorEvent(data: SensorData) {
        when (data.type) {
            SensorType.PROXIMITY -> {
                val status = if (data.value < 5) "PAUSED (student stepped away)" else "RUNNING"
                println("[HighSchoolCalc] ⏱ Exam timer: $status")
            }
            SensorType.GYROSCOPE -> {
                println("[HighSchoolCalc] 📐 Tilt: ${data.value}° → Adjusting UI layout")
            }
            else -> super.handleSensorEvent(data)
        }
    }

    fun isPassing(): Boolean = calculateGrade() >= 60.0
    fun needsIntervention(): Boolean = calculateGrade() < 70.0
}

/**
 * SUBCLASS 3: OnlineCourseCalculator
 * Inherits from GradeCalculator
 * Tracks completion rate, participation, time-on-task sensor
 *
 * Demonstrates: INHERITANCE, POLYMORPHISM (extra fields + unique sensors)
 */
class OnlineCourseCalculator(
    override val name: String,
    credits: Int = 3,
    val platform: String = "Coursera",
    val completionDeadline: String = "TBD"
) : GradeCalculator(name = name, credits = credits) {

    private var sessionMinutes: Int = 0

    override fun getCalculatorType(): String = "Online Course ($platform)"
    override fun getDescription(): String =
        "Online course on $platform. Deadline: $completionDeadline."

    // POLYMORPHISM: participation penalty if low engagement
    override fun calculateGrade(): Double {
        val base = super.calculateGrade()
        val penalty = if (sessionMinutes < 30) 2.0 else 0.0  // simulate engagement penalty
        return maxOf(0.0, base - penalty)
    }

    override fun getSummary(): String {
        return super.getSummary() +
            "\n  → Platform: $platform | Session: ${sessionMinutes}min | Deadline: $completionDeadline"
    }

    // POLYMORPHISM: microphone for voice commands
    override fun handleSensorEvent(data: SensorData) {
        when (data.type) {
            SensorType.MICROPHONE -> {
                sessionMinutes += data.value.toInt()
                println("[OnlineCourseCalc] 🎙 Voice activity: +${data.value.toInt()}min session detected. Total: ${sessionMinutes}min")
            }
            SensorType.AMBIENT_LIGHT -> {
                val focus = if (data.value < 30) "Night study mode" else "Daytime mode"
                println("[OnlineCourseCalc] 💡 $focus activated (${data.value} lux)")
            }
            else -> super.handleSensorEvent(data)
        }
    }

    fun logSession(minutes: Int) { sessionMinutes += minutes }
    fun getCompletionPct(): Double {
        val completed = _categories.count { it.getEffectiveAverage() != null }
        return if (_categories.isEmpty()) 0.0 else completed.toDouble() / _categories.size * 100
    }
}

/**
 * SUBCLASS 4: CumulativeGPACalculator
 * Inherits from GradeCalculator
 * Aggregates multiple courses, calculates semester/cumulative GPA
 *
 * Demonstrates: INHERITANCE + POLYMORPHISM (completely different calculation)
 */
class CumulativeGPACalculator(
    override val name: String = "Cumulative GPA"
) : GradeCalculator(name = name, credits = 0) {

    private val courses: MutableList<GradeCalculator> = mutableListOf()

    override fun getCalculatorType(): String = "Cumulative GPA Tracker"
    override fun getDescription(): String =
        "Aggregates all courses and computes weighted GPA by credit hours."

    // POLYMORPHISM: completely different calculation — credit-weighted GPA
    override fun calculateGrade(): Double {
        val totalCredits = courses.sumOf { it.credits }
        if (totalCredits == 0) return 0.0
        return courses.sumOf { it.getGPA() * it.credits } / totalCredits
    }

    // Letter grade from GPA (4.0 scale) for cumulative
    override fun getLetterGrade(): String = when {
        calculateGrade() >= 3.7 -> "A"
        calculateGrade() >= 3.3 -> "B+"
        calculateGrade() >= 3.0 -> "B"
        calculateGrade() >= 2.7 -> "B-"
        calculateGrade() >= 2.3 -> "C+"
        calculateGrade() >= 2.0 -> "C"
        calculateGrade() >= 1.0 -> "D"
        else -> "F"
    }

    override fun getGPA(): Double = calculateGrade()

    override fun getSummary(): String = buildString {
        appendLine("╔══════════════════════════════════════════╗")
        appendLine("║  CUMULATIVE GPA REPORT                   ║")
        appendLine("╠══════════════════════════════════════════╣")
        for (c in courses) {
            appendLine("║  %-22s  GPA: %-5.1f (%-2dcr) ║".format(
                c.name.take(22), c.getGPA(), c.credits))
        }
        appendLine("╠══════════════════════════════════════════╣")
        appendLine("║  Cumulative GPA: %-5.2f  Total Cr: %-3d  ║".format(
            calculateGrade(), courses.sumOf { it.credits }))
        append("╚══════════════════════════════════════════╝")
    }

    override fun handleSensorEvent(data: SensorData) {
        println("[CumulativeGPA] Sensor: ${data.type} = ${data.value}")
    }

    fun addCourse(course: GradeCalculator) { courses.add(course) }
    fun removeCourse(id: String) { courses.removeIf { it.id == id } }
    fun getCourses(): List<GradeCalculator> = courses.toList()
    fun getTotalCredits(): Int = courses.sumOf { it.credits }
}
