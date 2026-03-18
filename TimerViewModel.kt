package com.studypulse.app.ui.screens.timer

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studypulse.app.sensors.NoiseStatus
import com.studypulse.app.sensors.StudyPulseSensorController
import com.studypulse.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────
//  Timer Mode
// ─────────────────────────────────────────────

enum class TimerMode(val label: String, val color: Color, val defaultMinutes: Int) {
    FOCUS("🎯 Focus", Purple3, 25),
    SHORT_BREAK("☕ Short Break", Green, 5),
    LONG_BREAK("🌿 Long Break", Orange, 15)
}

// ─────────────────────────────────────────────
//  UI State
// ─────────────────────────────────────────────

data class TimerState(
    val mode: TimerMode = TimerMode.FOCUS,
    val totalMinutes: Int = 25,
    val remainingSeconds: Int = 25 * 60,
    val isRunning: Boolean = false,
    val currentRound: Int = 1,
    val selectedCourse: String = "Final Year Project",
    val sessionsToday: Int = 0,
    val minutesToday: Int = 0,
    val breaksToday: Int = 0
) {
    val totalSeconds: Int get() = totalMinutes * 60
    val progress: Float get() = remainingSeconds.toFloat() / totalSeconds.toFloat()
    val timeDisplay: String get() {
        val m = remainingSeconds / 60
        val s = remainingSeconds % 60
        return "%02d:%02d".format(m, s)
    }
}

// ─────────────────────────────────────────────
//  ViewModel
// ─────────────────────────────────────────────

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val sensorController: StudyPulseSensorController
) : ViewModel() {

    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> = _state.asStateFlow()

    // Sensor flows
    val isPhoneStill = sensorController.accelerometer.isStill
    val onShake = sensorController.accelerometer.onShake
    val noiseDb = sensorController.microphone.noiseDb
    val noiseStatus = sensorController.microphone.noiseStatus

    private var timerJob: Job? = null

    init {
        sensorController.startAll()
    }

    // ── Toggle play/pause (also called on shake)
    fun toggleTimer() {
        if (_state.value.isRunning) pauseTimer() else startTimer()
    }

    fun startTimer() {
        _state.update { it.copy(isRunning = true) }
        timerJob = viewModelScope.launch {
            while (_state.value.remainingSeconds > 0) {
                delay(1000)
                _state.update { it.copy(remainingSeconds = it.remainingSeconds - 1) }
            }
            onTimerComplete()
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _state.update { it.copy(isRunning = false) }
    }

    fun resetTimer() {
        timerJob?.cancel()
        _state.update { it.copy(isRunning = false, remainingSeconds = it.totalSeconds) }
    }

    fun skipToNext() {
        timerJob?.cancel()
        onTimerComplete()
    }

    private fun onTimerComplete() {
        val s = _state.value
        if (s.mode == TimerMode.FOCUS) {
            val newRound = if (s.currentRound >= 4) 1 else s.currentRound + 1
            _state.update {
                it.copy(
                    isRunning = false,
                    sessionsToday = it.sessionsToday + 1,
                    minutesToday = it.minutesToday + it.totalMinutes,
                    currentRound = newRound,
                    remainingSeconds = it.totalSeconds
                )
            }
        } else {
            _state.update {
                it.copy(
                    isRunning = false,
                    breaksToday = it.breaksToday + 1,
                    remainingSeconds = it.totalSeconds
                )
            }
        }
    }

    fun setMode(mode: TimerMode) {
        timerJob?.cancel()
        _state.update {
            it.copy(
                mode = mode,
                isRunning = false,
                totalMinutes = mode.defaultMinutes,
                remainingSeconds = mode.defaultMinutes * 60
            )
        }
    }

    fun setDuration(minutes: Int) {
        if (_state.value.isRunning) return
        _state.update { it.copy(totalMinutes = minutes, remainingSeconds = minutes * 60) }
    }

    fun selectCourse(course: String) {
        _state.update { it.copy(selectedCourse = course) }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        sensorController.stopAll()
    }
}
