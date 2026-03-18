package com.studypulse.app.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

// ─────────────────────────────────────────────
//  Data classes for sensor state
// ─────────────────────────────────────────────

data class SensorState(
    val isPhoneStill: Boolean = true,          // Accelerometer
    val noiseLevelDb: Float = 0f,              // Microphone
    val noiseStatus: NoiseStatus = NoiseStatus.QUIET,
    val isLowLight: Boolean = false,           // Light sensor
    val lightLux: Float = 0f
)

enum class NoiseStatus { QUIET, MODERATE, LOUD }

// ─────────────────────────────────────────────
//  Accelerometer Manager
//  - Detects if phone is still (focused) or moving
//  - Detects shake gesture to start/pause session
// ─────────────────────────────────────────────

class AccelerometerManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _isStill = MutableStateFlow(true)
    val isStill: StateFlow<Boolean> = _isStill

    private val _onShake = MutableStateFlow(false)
    val onShake: StateFlow<Boolean> = _onShake

    private var lastX = 0f; private var lastY = 0f; private var lastZ = 0f
    private var lastShakeTime = 0L
    private val SHAKE_THRESHOLD = 12f
    private val STILL_THRESHOLD = 1.5f
    private val SHAKE_COOLDOWN_MS = 1000L

    fun register() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun unregister() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val deltaX = abs(x - lastX)
        val deltaY = abs(y - lastY)
        val deltaZ = abs(z - lastZ)

        val movement = sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)

        // Detect if phone is still (good focus state)
        _isStill.value = movement < STILL_THRESHOLD

        // Detect shake gesture
        val now = System.currentTimeMillis()
        if (movement > SHAKE_THRESHOLD && now - lastShakeTime > SHAKE_COOLDOWN_MS) {
            lastShakeTime = now
            _onShake.value = true
            // Reset shake flag after a brief moment
            CoroutineScope(Dispatchers.Main).launch {
                delay(300)
                _onShake.value = false
            }
        }

        lastX = x; lastY = y; lastZ = z
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

// ─────────────────────────────────────────────
//  Microphone / Noise Level Manager
//  - Measures ambient dB level
//  - Warns when noise is too high for studying
// ─────────────────────────────────────────────

class MicrophoneManager {

    private val _noiseDb = MutableStateFlow(0f)
    val noiseDb: StateFlow<Float> = _noiseDb

    private val _noiseStatus = MutableStateFlow(NoiseStatus.QUIET)
    val noiseStatus: StateFlow<NoiseStatus> = _noiseStatus

    private var audioRecord: AudioRecord? = null
    private var monitoringJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        const val QUIET_THRESHOLD = 50f    // dB — good for studying
        const val MODERATE_THRESHOLD = 65f // dB — somewhat distracting
        // Above 65 dB = LOUD
    }

    fun startMonitoring() {
        val sampleRate = 44100
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
        audioRecord?.startRecording()

        monitoringJob = scope.launch {
            val buffer = ShortArray(bufferSize)
            while (true) {
                val read = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                if (read > 0) {
                    val db = calculateDecibels(buffer, read)
                    _noiseDb.value = db
                    _noiseStatus.value = when {
                        db < QUIET_THRESHOLD -> NoiseStatus.QUIET
                        db < MODERATE_THRESHOLD -> NoiseStatus.MODERATE
                        else -> NoiseStatus.LOUD
                    }
                }
                delay(500) // Sample every 500ms
            }
        }
    }

    fun stopMonitoring() {
        monitoringJob?.cancel()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    private fun calculateDecibels(buffer: ShortArray, readSize: Int): Float {
        var sum = 0.0
        for (i in 0 until readSize) {
            sum += (buffer[i] * buffer[i]).toDouble()
        }
        val rms = sqrt(sum / readSize)
        return if (rms > 0) (20 * log10(rms)).toFloat() else 0f
    }
}

// ─────────────────────────────────────────────
//  Light Sensor Manager
//  - Detects ambient light level
//  - Triggers dark/light mode accordingly
//  - Alerts if too dark (eye strain)
// ─────────────────────────────────────────────

class LightSensorManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    private val _lightLux = MutableStateFlow(0f)
    val lightLux: StateFlow<Float> = _lightLux

    private val _isDarkEnvironment = MutableStateFlow(false)
    val isDarkEnvironment: StateFlow<Boolean> = _isDarkEnvironment

    private val _lightStatus = MutableStateFlow(LightStatus.NORMAL)
    val lightStatus: StateFlow<LightStatus> = _lightStatus

    companion object {
        const val DARK_THRESHOLD = 50f    // lux — very dark room
        const val DIM_THRESHOLD = 150f    // lux — dim light
        const val BRIGHT_THRESHOLD = 500f // lux — bright/daylight
    }

    enum class LightStatus { DARK, DIM, NORMAL, BRIGHT }

    fun register() {
        sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun unregister() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_LIGHT) return
        val lux = event.values[0]
        _lightLux.value = lux
        _isDarkEnvironment.value = lux < DARK_THRESHOLD
        _lightStatus.value = when {
            lux < DARK_THRESHOLD -> LightStatus.DARK
            lux < DIM_THRESHOLD -> LightStatus.DIM
            lux < BRIGHT_THRESHOLD -> LightStatus.NORMAL
            else -> LightStatus.BRIGHT
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

// ─────────────────────────────────────────────
//  Combined Sensor Controller
//  - Single entry point for all sensors
// ─────────────────────────────────────────────

class StudyPulseSensorController(context: Context) {

    val accelerometer = AccelerometerManager(context)
    val microphone = MicrophoneManager()
    val lightSensor = LightSensorManager(context)

    fun startAll() {
        accelerometer.register()
        microphone.startMonitoring()
        lightSensor.register()
    }

    fun stopAll() {
        accelerometer.unregister()
        microphone.stopMonitoring()
        lightSensor.unregister()
    }
}
