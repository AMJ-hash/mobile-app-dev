package gradecalculator.model

enum class Major(val displayName: String, val code: String) {
    CYBERSECURITY("Cybersecurity", "CYB"),
    SOFTWARE_ENGINEERING("Software Engineering", "SWE"),
    ISN("ISN", "ISN"),
    COMPUTER_SCIENCE("Computer Science", "CSC");

    override fun toString() = displayName
}

data class Student(
    val name: String = "",
    val matricule: String = "",
    val major: Major = Major.COMPUTER_SCIENCE
)

data class CourseEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val courseName: String = "",
    val creditHours: Int = 3,
    val assignments: Double? = null,      // out of 100
    val midterm: Double? = null,           // out of 100
    val finalExam: Double? = null          // out of 100
) {
    // weights: assignments 30%, midterm 30%, final 40%
    fun calculate(): Double? {
        val parts = mutableListOf<Pair<Double, Double>>() // value, weight
        if (assignments != null) parts.add(assignments to 0.30)
        if (midterm     != null) parts.add(midterm     to 0.30)
        if (finalExam   != null) parts.add(finalExam   to 0.40)
        if (parts.isEmpty()) return null
        val totalWeight = parts.sumOf { it.second }
        return parts.sumOf { it.first * it.second } / totalWeight
    }

    fun getLetterGrade(): String {
        val g = calculate() ?: return "N/A"
        return GradeScale.fromPercentage(g).letter
    }

    fun getGPA(): Double {
        val g = calculate() ?: return 0.0
        return GradeScale.fromPercentage(g).gpa
    }
}
