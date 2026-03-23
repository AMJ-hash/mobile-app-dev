package gradecalculator.model

enum class Major(val displayName: String, val code: String) {
    CYBERSECURITY("Cybersecurity", "CYB"),
    SOFTWARE_ENGINEERING("Software Engineering", "SWE"),
    ISN("ISN", "ISN"),
    COMPUTER_SCIENCE("Computer Science", "CSC");
    override fun toString() = displayName
}

enum class Distinction(val label: String, val minGPA: Double, val color: String) {
    SUMMA_CUM_LAUDE("Summa Cum Laude",  3.9, "#F5C542"),
    MAGNA_CUM_LAUDE("Magna Cum Laude",  3.7, "#C8F542"),
    CUM_LAUDE(      "Cum Laude",         3.5, "#42C8F5"),
    HONOURS(        "Honours",           3.0, "#9B59B6"),
    PASS(           "Pass",              2.0, "#3FB950"),
    FAIL(           "Fail",              0.0, "#F85149");

    companion object {
        fun from(gpa: Double) = values().first { gpa >= it.minGPA }
    }
}

data class Student(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val matricule: String = "",
    val major: Major = Major.COMPUTER_SCIENCE,
    val courses: List<CourseEntry> = emptyList()
) {
    fun grades(): List<Double?> = courses.map { it.calculate() }

    fun cumulativeGPA(): Double {
        val tc = courses.sumOf { it.creditHours }
        if (tc == 0) return 0.0
        return courses.zip(grades()).sumOf { (c, g) ->
            (if (g != null) GradeScale.fromPercentage(g).gpa else 0.0) * c.creditHours
        } / tc
    }

    fun overallPercentage(): Double {
        val valid = grades().filterNotNull()
        return if (valid.isEmpty()) 0.0 else valid.average()
    }

    fun distinction(): Distinction = Distinction.from(cumulativeGPA())

    fun totalCredits(): Int = courses.sumOf { it.creditHours }
}

data class CourseEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val courseName: String = "",
    val creditHours: Int = 3,
    val assignments: Double? = null,   // /100  weight 30%
    val midterm: Double? = null,       // /100  weight 30%
    val finalExam: Double? = null      // /100  weight 40%
) {
    fun calculate(): Double? {
        val parts = mutableListOf<Pair<Double, Double>>()
        if (assignments != null) parts.add(assignments to 0.30)
        if (midterm     != null) parts.add(midterm     to 0.30)
        if (finalExam   != null) parts.add(finalExam   to 0.40)
        if (parts.isEmpty()) return null
        val tw = parts.sumOf { it.second }
        return parts.sumOf { it.first * it.second } / tw
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
