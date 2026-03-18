package gradecalculator.model

/**
 * Interface: ICalculable
 * Any class that can calculate a grade must implement this.
 * Demonstrates INTERFACE usage.
 */
interface ICalculable {
    fun calculateGrade(): Double
    fun getLetterGrade(): String
    fun getGPA(): Double
    fun getSummary(): String
}

/**
 * Interface: IExportable
 * Any class that can export data implements this.
 */
interface IExportable {
    fun toExcelRow(): List<String>
    fun toHtmlRow(): String
    fun toXmlElement(): String
    fun toCsvRow(): String
}

/**
 * Interface: ISensorAware
 * Sensor-capable components implement this.
 */
interface ISensorAware {
    fun onSensorDataReceived(data: SensorData)
    fun getSensorStatus(): String
}

/**
 * Data class for sensor readings
 */
data class SensorData(
    val type: SensorType,
    val value: Double,
    val unit: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class SensorType {
    AMBIENT_LIGHT,      // Auto dark/light mode
    ACCELEROMETER,      // Shake to reset
    PROXIMITY,          // Auto-pause timer when away
    GYROSCOPE,          // Tilt-based navigation
    MICROPHONE          // Voice input trigger
}

/**
 * Grade scale constants and helpers
 */
object GradeScale {
    data class GradeBand(val letter: String, val minPct: Double, val gpa: Double, val color: String)

    val bands = listOf(
        GradeBand("A+", 97.0, 4.0, "#22c55e"),
        GradeBand("A",  93.0, 4.0, "#22c55e"),
        GradeBand("A-", 90.0, 3.7, "#84cc16"),
        GradeBand("B+", 87.0, 3.3, "#3b82f6"),
        GradeBand("B",  83.0, 3.0, "#3b82f6"),
        GradeBand("B-", 80.0, 2.7, "#60a5fa"),
        GradeBand("C+", 77.0, 2.3, "#f59e0b"),
        GradeBand("C",  73.0, 2.0, "#f59e0b"),
        GradeBand("C-", 70.0, 1.7, "#fbbf24"),
        GradeBand("D+", 67.0, 1.3, "#f97316"),
        GradeBand("D",  63.0, 1.0, "#f97316"),
        GradeBand("D-", 60.0, 0.7, "#ef4444"),
        GradeBand("F",   0.0, 0.0, "#dc2626")
    )

    fun fromPercentage(pct: Double): GradeBand {
        return bands.first { pct >= it.minPct }
    }
}
