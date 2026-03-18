package com.studypulse.app.ui.screens.dashboard

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studypulse.app.sensors.LightSensorManager
import com.studypulse.app.sensors.MicrophoneManager
import com.studypulse.app.sensors.NoiseStatus
import com.studypulse.app.sensors.StudyPulseSensorController
import com.studypulse.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val userName: String = "Student",
    val isSessionActive: Boolean = false,
    val activeSubject: String = "Mathematics",
    val elapsedSeconds: Int = 0,
    val todayHours: Float = 4.5f,
    val streakDays: Int = 12,
    val weeklyProgress: Float = 0.73f,
    val focusScore: Int = 88,
    val currentQuote: String = "The secret of getting ahead is getting started.",
    val quoteAuthor: String = "Mark Twain",
    val subjects: List<SubjectProgress> = defaultSubjects(),
    val recentSessions: List<StudySession> = defaultSessions(),
    val studiedDaysThisMonth: Set<Int> = (1..12).toSet()
)

fun defaultSubjects() = listOf(
    SubjectProgress("Mathematics", 0.82f, "6.2h", Purple),
    SubjectProgress("Computer Science", 0.68f, "5.1h", Green),
    SubjectProgress("Physics", 0.50f, "3.8h", Orange),
    SubjectProgress("English", 0.32f, "2.4h", Red),
    SubjectProgress("History", 0.25f, "1.9h", Color(0xFF06B6D4))
)

fun defaultSessions() = listOf(
    StudySession("Mathematics", "Today · 2:30 PM · Pomodoro", "1h 45m", Purple),
    StudySession("Computer Science", "Today · 10:00 AM · Deep Work", "2h 10m", Green),
    StudySession("Physics", "Yesterday · 7:00 PM", "55m", Orange)
)

private val quotes = listOf(
    Pair("The secret of getting ahead is getting started.", "Mark Twain"),
    Pair("It does not matter how slowly you go as long as you do not stop.", "Confucius"),
    Pair("Success is the sum of small efforts, repeated day in and day out.", "Robert Collier"),
    Pair("The expert in anything was once a beginner.", "Helen Hayes"),
    Pair("An investment in knowledge pays the best interest.", "Benjamin Franklin"),
    Pair("Education is the passport to the future.", "Malcolm X"),
    Pair("The beautiful thing about learning is nobody can take it away from you.", "B.B. King")
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val sensorController: StudyPulseSensorController
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    // ── Sensor flows exposed to UI
    val isPhoneStill: StateFlow<Boolean> = sensorController.accelerometer.isStill
    val onShake: StateFlow<Boolean> = sensorController.accelerometer.onShake
    val noiseDb: StateFlow<Float> = sensorController.microphone.noiseDb
    val noiseStatus: StateFlow<NoiseStatus> = sensorController.microphone.noiseStatus
    val lightStatus: StateFlow<LightSensorManager.LightStatus> = sensorController.lightSensor.lightStatus

    private var quoteIndex = 0
    private var sessionTimerJob: kotlinx.coroutines.Job? = null

    init {
        sensorController.startAll()
        observeLightSensor()
    }

    // Auto-switch dark/light UI hint based on light sensor
    private fun observeLightSensor() {
        viewModelScope.launch {
            sensorController.lightSensor.lightStatus.collect { status ->
                // You can emit a UI event here to change theme or notify user
                // For now we expose it via StateFlow for the UI to react
            }
        }
    }

    fun toggleSessionOnShake() {
        if (_uiState.value.isSessionActive) {
            pauseSession()
        } else {
            startSession()
        }
    }

    fun startSession() {
        _uiState.update { it.copy(isSessionActive = true, elapsedSeconds = 0) }
        sessionTimerJob?.cancel()
        sessionTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _uiState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
            }
        }
    }

    fun pauseSession() {
        sessionTimerJob?.cancel()
        _uiState.update { it.copy(isSessionActive = false) }
    }

    fun stopSession() {
        sessionTimerJob?.cancel()
        val minutes = _uiState.value.elapsedSeconds / 60
        val hours = minutes / 60f
        _uiState.update {
            it.copy(
                isSessionActive = false,
                elapsedSeconds = 0,
                todayHours = it.todayHours + hours
            )
        }
    }

    fun refreshQuote() {
        quoteIndex = (quoteIndex + 1) % quotes.size
        val (quote, author) = quotes[quoteIndex]
        _uiState.update { it.copy(currentQuote = quote, quoteAuthor = author) }
    }

    override fun onCleared() {
        super.onCleared()
        sensorController.stopAll()
        sessionTimerJob?.cancel()
    }
}
