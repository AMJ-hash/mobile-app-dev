package gradecalculator.sensor

import gradecalculator.model.SensorData
import gradecalculator.model.SensorType
import gradecalculator.model.ISensorAware
import kotlinx.coroutines.*
import kotlin.math.sin
import kotlin.random.Random

/**
 * SensorManager — Simulates device sensors for desktop
 *
 * On a real mobile/embedded device, these would read from actual hardware.
 * On desktop, we simulate sensor values and dispatch them to listeners.
 *
 * Sensors demonstrated:
 *  1. AMBIENT_LIGHT  → auto dark/light theme switching
 *  2. ACCELEROMETER  → shake-to-reset gesture
 *  3. PROXIMITY      → auto-pause (student walked away)
 *  4. GYROSCOPE      → tilt navigation
 *  5. MICROPHONE     → voice session tracking
 */
class SensorManager {

    private val listeners: MutableList<ISensorAware> = mutableListOf()
    private val sensorReadings: MutableMap<SensorType, SensorData> = mutableMapOf()
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    // Callbacks for UI updates
    var onLightChanged: ((Double) -> Unit)? = null
    var onShakeDetected: (() -> Unit)? = null
    var onProximityChanged: ((Boolean) -> Unit)? = null
    var onSensorLog: ((String) -> Unit)? = null

    fun registerListener(listener: ISensorAware) { listeners.add(listener) }
    fun unregisterListener(listener: ISensorAware) { listeners.remove(listener) }

    fun getLatestReading(type: SensorType): SensorData? = sensorReadings[type]

    /**
     * Start simulated sensor polling
     */
    fun startSimulation() {
        job = scope.launch {
            var tick = 0
            while (isActive) {
                tick++

                // 1. AMBIENT LIGHT: sinusoidal "daylight cycle"
                val lightValue = 50 + 50 * sin(tick * 0.05)
                dispatchSensor(SensorData(SensorType.AMBIENT_LIGHT, lightValue, "lux"))
                onLightChanged?.invoke(lightValue)

                // 2. ACCELEROMETER: random spike = "shake"
                val accelValue = if (Random.nextDouble() < 0.03) {
                    onShakeDetected?.invoke()
                    onSensorLog?.invoke("📳 Shake gesture detected!")
                    15.0
                } else {
                    Random.nextDouble(0.5, 1.5)  // normal movement
                }
                dispatchSensor(SensorData(SensorType.ACCELEROMETER, accelValue, "m/s²"))

                // 3. PROXIMITY: every 15 ticks simulate walking away
                if (tick % 15 == 0) {
                    val proximity = if (Random.nextBoolean()) 0.0 else 20.0
                    val nearby = proximity < 5.0
                    dispatchSensor(SensorData(SensorType.PROXIMITY, proximity, "cm"))
                    onProximityChanged?.invoke(nearby)
                    onSensorLog?.invoke("📡 Proximity: ${if (nearby) "near (timer paused)" else "far (timer running)"}")
                }

                // 4. GYROSCOPE: gentle tilt variation
                val tilt = Random.nextDouble(-5.0, 5.0)
                dispatchSensor(SensorData(SensorType.GYROSCOPE, tilt, "deg"))

                // 5. MICROPHONE: every 20 ticks, "voice activity"
                if (tick % 20 == 0) {
                    val voiceActivity = Random.nextDouble(1.0, 10.0)
                    dispatchSensor(SensorData(SensorType.MICROPHONE, voiceActivity, "min"))
                    onSensorLog?.invoke("🎙 Voice activity: +${voiceActivity.toInt()} min")
                }

                delay(1000L)
            }
        }
        onSensorLog?.invoke("✅ Sensor simulation started (5 sensors active)")
    }

    fun stopSimulation() {
        job?.cancel()
        onSensorLog?.invoke("⛔ Sensor simulation stopped")
    }

    fun isRunning(): Boolean = job?.isActive == true

    /**
     * Manually fire a specific sensor event (for testing / demo)
     */
    fun fireSensorEvent(type: SensorType, value: Double, unit: String = "") {
        val data = SensorData(type, value, unit)
        dispatchSensor(data)
    }

    private fun dispatchSensor(data: SensorData) {
        sensorReadings[data.type] = data
        listeners.forEach { it.onSensorDataReceived(data) }
    }

    /**
     * Human-readable sensor dashboard
     */
    fun getDashboard(): String = buildString {
        appendLine("╔═══════════════ SENSOR DASHBOARD ════════════════╗")
        for (type in SensorType.values()) {
            val reading = sensorReadings[type]
            if (reading != null) {
                appendLine("║  %-16s  %.2f %-8s  %-14s ║".format(
                    type.name, reading.value, reading.unit,
                    java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date(reading.timestamp))
                ))
            } else {
                appendLine("║  %-16s  [no data]                       ║".format(type.name))
            }
        }
        append("╚══════════════════════════════════════════════════╝")
    }
}
