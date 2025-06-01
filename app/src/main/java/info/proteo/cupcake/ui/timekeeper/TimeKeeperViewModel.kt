package info.proteo.cupcake.ui.timekeeper

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.proteo.cupcake.data.remote.model.protocol.TimeKeeper
import info.proteo.cupcake.data.repository.ProtocolStepRepository
import info.proteo.cupcake.data.repository.TimeKeeperRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimeKeeperViewModel @Inject constructor(
    private val timeKeeperRepository: TimeKeeperRepository,
    private val protocolStepRepository: ProtocolStepRepository
) : ViewModel() {

    private val _timeKeepers = MutableStateFlow<List<TimeKeeper>>(emptyList())
    val timeKeepers: StateFlow<List<TimeKeeper>> = _timeKeepers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _activeTimers = MutableStateFlow<Map<Int, TimerState>>(emptyMap())
    val activeTimers: StateFlow<Map<Int, TimerState>> = _activeTimers.asStateFlow()

    // Map to keep track of timer coroutine jobs
    private val timerJobs = mutableMapOf<Int, Job>()

    // Timer state data class
    data class TimerState(
        val id: Int,
        val currentDuration: Float,
        val started: Boolean
    )

    // Methods to load timeKeepers
    fun loadTimeKeepers() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = timeKeeperRepository.getTimeKeepers()
                if (result.isSuccess) {
                    _timeKeepers.value = result.getOrNull()?.results ?: emptyList()

                    // Initialize active timers
                    val initialTimers = _timeKeepers.value
                        .filter { it.started == true }
                        .associate {
                            it.id to TimerState(
                                id = it.id,
                                currentDuration = it.currentDuration ?: 0f,
                                started = true
                            )
                        }
                    _activeTimers.value = initialTimers

                    // Start jobs for active timers
                    initialTimers.forEach { (id, state) ->
                        startTimer(id, _timeKeepers.value.find { it.id == id }?.step, state.currentDuration)
                    }
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to load timeKeepers"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Methods to manage timeKeepers
    fun createTimeKeeper(sessionId: Int?, stepId: Int?, started: Boolean, initialDuration: Float) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // If a step ID is provided, try to get the duration from the step
                val duration = if (stepId != null) {
                    getStepDurationById(stepId) ?: initialDuration
                } else {
                    initialDuration
                }

                val timeKeeper = TimeKeeper(
                    id = 0,  // Will be assigned by the backend
                    session = sessionId,
                    step = stepId,
                    started = started,
                    currentDuration = duration,
                    startTime = null  // Will be assigned by the backend if started is true
                )

                val result = timeKeeperRepository.createTimeKeeper(timeKeeper)
                if (result.isSuccess) {
                    val createdTimeKeeper = result.getOrNull()
                    if (createdTimeKeeper != null && started) {
                        startTimer(createdTimeKeeper.id, stepId, duration)
                    }
                    loadTimeKeepers()
                } else {
                    _error.value = "Failed to create timeKeeper: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _error.value = "Error creating timeKeeper: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteTimeKeeper(id: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Cancel any running timer for this ID
                pauseTimer(id)

                val result = timeKeeperRepository.deleteTimeKeeper(id)
                if (result.isSuccess) {
                    loadTimeKeepers()
                } else {
                    _error.value = "Failed to delete timeKeeper: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _error.value = "Error deleting timeKeeper: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Timer management methods
    fun startTimer(id: Int, step: Int?, initialDuration: Float) {
        // Cancel any existing timer job for this ID
        pauseTimer(id)

        // Update timer state
        val mutableTimers = _activeTimers.value.toMutableMap()
        mutableTimers[id] = TimerState(id, initialDuration, true)
        _activeTimers.value = mutableTimers

        // Start a new timer job
        timerJobs[id] = viewModelScope.launch {
            val timer = mutableTimers[id] ?: return@launch
            var newDuration = timer.currentDuration

            while (true) {
                delay(1000) // Update every second

                if (timer.started) {
                    newDuration -= 1f

                    // Update the timer state
                    val updatedTimers = _activeTimers.value.toMutableMap()
                    updatedTimers[id] = timer.copy(currentDuration = newDuration)
                    _activeTimers.value = updatedTimers

                    if (newDuration <= 0) {
                        pauseTimer(id)
                        break
                    }
                }
            }
        }

        // Update timeKeeper on the server
        updateTimeKeeperOnServer(id, true, initialDuration)
    }

    fun pauseTimer(id: Int) {
        // Cancel the running job if it exists
        timerJobs[id]?.cancel()

        // Update the timer state to paused
        val mutableTimers = _activeTimers.value.toMutableMap()
        val currentTimer = mutableTimers[id]
        if (currentTimer != null) {
            mutableTimers[id] = currentTimer.copy(started = false)
            _activeTimers.value = mutableTimers

            // Update timeKeeper on the server
            updateTimeKeeperOnServer(id, false, currentTimer.currentDuration)
        }

        // Remove the job reference
        timerJobs.remove(id)
    }

    fun resetTimer(id: Int, newDuration: Float) {
        // Cancel the running job
        timerJobs[id]?.cancel()

        // Update the timer state
        val mutableTimers = _activeTimers.value.toMutableMap()
        mutableTimers[id] = TimerState(id, newDuration, false)
        _activeTimers.value = mutableTimers

        // Update timeKeeper on the server
        updateTimeKeeperOnServer(id, false, newDuration)

        // Remove the job reference
        timerJobs.remove(id)
    }

    private fun updateTimeKeeperOnServer(id: Int, started: Boolean, currentDuration: Float) {
        viewModelScope.launch {
            try {
                val existingTimeKeeper = _timeKeepers.value.find { it.id == id }
                if (existingTimeKeeper != null) {
                    val updatedTimeKeeper = existingTimeKeeper.copy(
                        started = started,
                        currentDuration = currentDuration
                    )
                    timeKeeperRepository.updateTimeKeeper(id, updatedTimeKeeper)
                }
            } catch (e: Exception) {
                _error.value = "Failed to update timeKeeper on server: ${e.message}"
            }
        }
    }

    // Get the duration from a protocol step using the repository
    private suspend fun getStepDurationById(stepId: Int): Float? {
        return try {
            val result = protocolStepRepository.getProtocolStepById(stepId)
            if (result.isSuccess) {
                val step = result.getOrNull()
                step?.stepDuration?.toFloatOrNull()?.times(60) // Convert minutes to seconds
            } else {
                null
            }
        } catch (e: Exception) {
            _error.value = "Failed to retrieve step duration: ${e.message}"
            null
        }
    }
}