package gradecalculator.model

/**
 * ABSTRACT BASE CLASS: GradeCalculator
 *
 * Demonstrates:
 *  - Abstract class with abstract methods (polymorphism)
 *  - Implementing multiple interfaces
 *  - Encapsulation with private/protected fields
 *  - Open functions for subclass override
 */
abstract class GradeCalculator(
    val id: String = java.util.UUID.randomUUID().toString(),
    open val name: String,
    val credits: Int = 3
) : ICalculable, IExportable, ISensorAware {

    // Encapsulated grade storage
    protected val _categories: MutableList<GradeCategory> = mutableListOf()
    private var sensorEnabled: Boolean = false
    private var lastSensorData: SensorData? = null

    // ── Abstract methods (subclasses MUST implement) ─────────────────
    abstract fun getCalculatorType(): String
    abstract fun getDescription(): String

    // ── Concrete implementations ──────────────────────────────────────

    override fun calculateGrade(): Double {
        val validCats = _categories.filter { it.getEffectiveAverage() != null }
        if (validCats.isEmpty()) return 0.0
        val totalWeight = validCats.sumOf { it.weight }
        if (totalWeight == 0.0) return 0.0
        return validCats.sumOf { it.getEffectiveAverage()!! * it.weight } / totalWeight
    }

    override fun getLetterGrade(): String {
        val pct = calculateGrade()
        return if (pct == 0.0 && _categories.all { it.grades.isEmpty() }) "N/A"
        else GradeScale.fromPercentage(pct).letter
    }

    override fun getGPA(): Double {
        val pct = calculateGrade()
        return GradeScale.fromPercentage(pct).gpa
    }

    override fun getSummary(): String {
        val pct = calculateGrade()
        return buildString {
            appendLine("╔══════════════════════════════════════════╗")
            appendLine("║  ${getCalculatorType().padEnd(42)}║")
            appendLine("║  Course: ${name.padEnd(34)}║")
            appendLine("╠══════════════════════════════════════════╣")
            for (cat in _categories) {
                val avg = cat.getEffectiveAverage()
                val avgStr = if (avg != null) "%.1f%%".format(avg) else "N/A"
                appendLine("║  %-20s %6s  (wt:%.0f%%)   ║".format(cat.name, avgStr, cat.weight))
            }
            appendLine("╠══════════════════════════════════════════╣")
            appendLine("║  Overall:  %-8.2f%%  %-5s  GPA: %-4.1f  ║".format(pct, getLetterGrade(), getGPA()))
            appendLine("╚══════════════════════════════════════════╝")
        }
    }

    // ── IExportable ───────────────────────────────────────────────────

    override fun toExcelRow(): List<String> = listOf(
        name, getCalculatorType(), credits.toString(),
        "%.2f".format(calculateGrade()), getLetterGrade(), getGPA().toString()
    )

    override fun toHtmlRow(): String =
        "<tr><td>$name</td><td>${getCalculatorType()}</td><td>$credits</td>" +
        "<td>${"%.2f".format(calculateGrade())}%</td><td>${getLetterGrade()}</td><td>${getGPA()}</td></tr>"

    override fun toXmlElement(): String = buildString {
        appendLine("  <course id=\"$id\">")
        appendLine("    <name>$name</name>")
        appendLine("    <type>${getCalculatorType()}</type>")
        appendLine("    <credits>$credits</credits>")
        appendLine("    <grade>${"%.2f".format(calculateGrade())}</grade>")
        appendLine("    <letter>${getLetterGrade()}</letter>")
        appendLine("    <gpa>${getGPA()}</gpa>")
        appendLine("    <categories>")
        for (cat in _categories) {
            appendLine("      <category name=\"${cat.name}\" weight=\"${cat.weight}\">")
            for (g in cat.grades) {
                appendLine("        <assignment name=\"${g.name}\" score=\"${g.score}\" total=\"${g.total}\"/>")
            }
            appendLine("      </category>")
        }
        appendLine("    </categories>")
        append("  </course>")
    }

    override fun toCsvRow(): String =
        "\"$name\",\"${getCalculatorType()}\",$credits,${"%.2f".format(calculateGrade())},${getLetterGrade()},${getGPA()}"

    // ── ISensorAware ──────────────────────────────────────────────────

    override fun onSensorDataReceived(data: SensorData) {
        lastSensorData = data
        sensorEnabled = true
        handleSensorEvent(data)
    }

    override fun getSensorStatus(): String =
        if (sensorEnabled) "Sensor active: ${lastSensorData?.type}" else "No sensor data"

    // Open so subclasses can override sensor behavior
    open fun handleSensorEvent(data: SensorData) {
        println("[${getCalculatorType()}] Sensor event: ${data.type} = ${data.value} ${data.unit}")
    }

    // ── Category management ───────────────────────────────────────────

    fun addCategory(category: GradeCategory) { _categories.add(category) }
    fun removeCategory(id: String) { _categories.removeIf { it.id == id } }
    fun getCategories(): List<GradeCategory> = _categories.toList()
    fun getTotalWeight(): Double = _categories.sumOf { it.weight }

    fun getNeededScore(targetPct: Double): Double {
        val earnedWeight = _categories.filter { it.getEffectiveAverage() != null }.sumOf { it.weight }
        val pendingWeight = _categories.filter { it.getEffectiveAverage() == null }.sumOf { it.weight }
        if (pendingWeight == 0.0) return -1.0
        val earnedSum = _categories.filter { it.getEffectiveAverage() != null }
            .sumOf { it.getEffectiveAverage()!! * it.weight }
        val totalW = earnedWeight + pendingWeight
        return (targetPct * totalW - earnedSum) / pendingWeight
    }
}

/**
 * Grade Category — groups assignments together
 */
data class GradeCategory(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val weight: Double,
    val dropLowest: Boolean = false,
    val color: String = "#3b82f6"
) {
    val grades: MutableList<Grade> = mutableListOf()

    fun getEffectiveAverage(): Double? {
        val valid = grades.filter { it.score != null }
        if (valid.isEmpty()) return null
        var percentages = valid.map { it.score!! / it.total * 100.0 }
        if (dropLowest && percentages.size > 1) {
            percentages = percentages.toMutableList().apply { remove(minOrNull()!!) }
        }
        return percentages.average()
    }
}

/**
 * Individual grade entry
 */
data class Grade(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val score: Double?,
    val total: Double = 100.0
)
