package info.proteo.cupcake.ui.timekeeper

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.proteo.cupcake.data.remote.model.protocol.TimeKeeper
import info.proteo.cupcake.data.remote.service.WebSocketService
import info.proteo.cupcake.data.repository.ProtocolStepRepository
import info.proteo.cupcake.data.repository.TimeKeeperRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class TimeKeeperViewModel @Inject constructor(
    private val timeKeeperRepository: TimeKeeperRepository,
    private val protocolStepRepository: ProtocolStepRepository,
    private val webSocketService: WebSocketService
) : ViewModel() {

    private val _timeKeepers = MutableStateFlow<List<TimeKeeper>>(emptyList())
    val timeKeepers: StateFlow<List<TimeKeeper>> = _timeKeepers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _activeTimers = MutableStateFlow<Map<Int, TimerState>>(emptyMap())
    val activeTimers: StateFlow<Map<Int, TimerState>> = _activeTimers.asStateFlow()

    private val timerJobs = mutableMapOf<Int, Job>()

    private data class TimerNotification(
        val type: String,
        val action: String,
        val timer_id: Int,
        val started: Boolean,
        val current_duration: Double,
        val timestamp: String
    )

    data class TimerState(
        val id: Int,
        val currentDuration: Int,
        val started: Boolean
    )

    init {
        observeWebSocketNotifications()
    }

    private fun observeWebSocketNotifications() {
        viewModelScope.launch {
            webSocketService.notificationMessages
                .catch { e ->
                    Log.e("TimeKeeperViewModel", "Error collecting WebSocket messages", e)
                    _error.value = "WebSocket connection error: ${e.message}"
                }
                .collect { jsonString ->
                    try {
                        Log.d("TimeKeeperViewModel", "Received WebSocket message: $jsonString")
                        val jsonObj = JSONObject(jsonString)
                        val type = jsonObj.optString("type")
                        val action = jsonObj.optString("action")

                        if (type == "timer_notification" && action == "updated") {
                            val timerId = jsonObj.getInt("timer_id")
                            val started = jsonObj.getBoolean("started")
                            val currentDurationDouble = jsonObj.getDouble("current_duration")
                            val timestamp = jsonObj.getString("timestamp")

                            val notification = TimerNotification(
                                type, action, timerId, started, currentDurationDouble, timestamp
                            )
                            handleTimerNotification(notification)
                        }
                    } catch (e: Exception) {
                        Log.e("TimeKeeperViewModel", "Error parsing WebSocket message", e)
                        _error.value = "Error processing WebSocket update: ${e.message}"
                    }
                }
        }
    }

    private fun handleTimerNotification(notification: TimerNotification) {
        val timerId = notification.timer_id
        val wsStarted = notification.started
        val wsDuration = notification.current_duration.toInt().coerceAtLeast(0)

        Log.d("TimeKeeperViewModel", "Handling Timer Notification for ID $timerId: Started=$wsStarted, Duration=$wsDuration")

        _timeKeepers.value = _timeKeepers.value.map { tk ->
            if (tk.id == timerId) {
                tk.copy(
                    started = wsStarted,
                    currentDuration = wsDuration
                )
            } else {
                tk
            }
        }

        _activeTimers.value = _activeTimers.value.toMutableMap().apply {
            this[timerId] = TimerState(timerId, wsDuration, wsStarted)
        }

        timerJobs[timerId]?.cancel()
        timerJobs.remove(timerId)

        if (wsStarted) {
            timerJobs[timerId] = viewModelScope.launch {
                var currentLocalDuration = wsDuration
                try {
                    Log.d("TimeKeeperViewModel", "WS: Starting local timer job for ID $timerId, duration $currentLocalDuration")
                    while (this.isActive && currentLocalDuration > 0) {
                        delay(1000)
                        currentLocalDuration -= 1
                        _activeTimers.value = _activeTimers.value.toMutableMap().apply {
                            this[timerId]?.takeIf { it.started }?.let {
                                this[timerId] = it.copy(currentDuration = currentLocalDuration)
                            }
                        }
                    }
                    if (this.isActive && currentLocalDuration <= 0) { // Timer reached zero
                        Log.d("TimeKeeperViewModel", "WS: Local timer job for ID $timerId reached zero.")
                        _activeTimers.value = _activeTimers.value.toMutableMap().apply {
                            this[timerId]?.let { this[timerId] = it.copy(started = false, currentDuration = 0) }
                        }
                        _timeKeepers.value = _timeKeepers.value.map { tk ->
                            if (tk.id == timerId) tk.copy(started = false, currentDuration = 0) else tk
                        }
                    }
                } finally {
                    if (timerJobs[timerId] == this.coroutineContext[Job]) {
                        timerJobs.remove(timerId)
                        Log.d("TimeKeeperViewModel", "WS: Cleaned up local timer job for ID $timerId")
                    }
                }
            }
        } else {
            Log.d("TimeKeeperViewModel", "WS: Timer ID $timerId is not started. No local job created/job removed.")
        }
    }

    fun loadTimeKeepers() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = timeKeeperRepository.getTimeKeepers()
                if (result.isSuccess) {
                    val fetchedTimeKeepers = result.getOrNull()?.results ?: emptyList()
                    _timeKeepers.value = fetchedTimeKeepers

                    val initialActiveTimers = fetchedTimeKeepers
                        .filter { it.started == true }
                        .associate {
                            it.id to TimerState(
                                id = it.id,
                                currentDuration = it.currentDuration ?: 0,
                                started = true
                            )
                        }
                    _activeTimers.value = initialActiveTimers

                    initialActiveTimers.forEach { (id, state) ->
                        timerJobs[id]?.cancel() // Cancel any old job
                        timerJobs[id] = viewModelScope.launch {
                            var currentLocalDuration = state.currentDuration
                            try {
                                Log.d("TimeKeeperViewModel", "Load: Starting local timer job for ID $id, duration $currentLocalDuration")
                                while (this.isActive && currentLocalDuration > 0) {
                                    delay(1000)
                                    currentLocalDuration -= 1
                                    _activeTimers.value = _activeTimers.value.toMutableMap().apply {
                                        this[id]?.takeIf { it.started }?.let {
                                            this[id] = it.copy(currentDuration = currentLocalDuration)
                                        }
                                    }
                                }
                                if (this.isActive && currentLocalDuration <= 0) {
                                    Log.d("TimeKeeperViewModel", "Load: Local timer job for ID $id reached zero.")
                                    _activeTimers.value = _activeTimers.value.toMutableMap().apply {
                                        this[id]?.let { this[id] = it.copy(started = false, currentDuration = 0) }
                                    }
                                    _timeKeepers.value = _timeKeepers.value.map { tk ->
                                        if (tk.id == id) tk.copy(started = false, currentDuration = 0) else tk
                                    }
                                }
                            } finally {
                                if (timerJobs[id] == this.coroutineContext[Job]) {
                                    timerJobs.remove(id)
                                    Log.d("TimeKeeperViewModel", "Load: Cleaned up local timer job for ID $id")
                                }
                            }
                        }
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

    fun createTimeKeeper(sessionId: Int?, stepId: Int?, started: Boolean, initialDuration: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val duration = if (stepId != null) {
                    getStepDurationById(stepId) ?: initialDuration
                } else {
                    initialDuration
                }

                val timeKeeper = TimeKeeper(
                    id = 0,
                    session = sessionId,
                    step = stepId,
                    started = started,
                    currentDuration = duration,
                    startTime = null
                )

                val result = timeKeeperRepository.createTimeKeeper(timeKeeper)
                if (result.isSuccess) {
                    val createdTimeKeeper = result.getOrNull()
                    loadTimeKeepers()
                    if (createdTimeKeeper != null && started) {
                        _activeTimers.value = _activeTimers.value.toMutableMap().apply {
                            this[createdTimeKeeper.id] = TimerState(createdTimeKeeper.id, createdTimeKeeper.currentDuration ?:0, true)
                        }
                    }
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

                timerJobs[id]?.cancel()
                timerJobs.remove(id)
                _activeTimers.value = _activeTimers.value.toMutableMap().apply { remove(id) }


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

    fun startTimer(id: Int, step: Int?, initialDuration: Int) {
        Log.d("TimeKeeperViewModel", "User action: Start timer ID $id, duration $initialDuration")
        timerJobs[id]?.cancel()

        _activeTimers.value = _activeTimers.value.toMutableMap().apply {
            this[id] = TimerState(id, initialDuration, true)
        }
        _timeKeepers.value = _timeKeepers.value.map { tk ->
            if (tk.id == id) tk.copy(started = true, currentDuration = initialDuration) else tk
        }

        timerJobs[id] = viewModelScope.launch {
            var currentLocalDuration = initialDuration
            try {
                while (this.isActive && currentLocalDuration > 0) {
                    delay(1000)
                    currentLocalDuration -= 1
                    _activeTimers.value = _activeTimers.value.toMutableMap().apply {
                        this[id]?.takeIf { it.started }?.let {
                            this[id] = it.copy(currentDuration = currentLocalDuration)
                        }
                    }
                }
                if (this.isActive && currentLocalDuration <= 0) {
                    Log.d("TimeKeeperViewModel", "User action: Timer ID $id reached zero.")
                    _activeTimers.value = _activeTimers.value.toMutableMap().apply {
                        this[id]?.let { this[id] = it.copy(started = false, currentDuration = 0) }
                    }
                    _timeKeepers.value = _timeKeepers.value.map { tk ->
                        if (tk.id == id) tk.copy(started = false, currentDuration = 0) else tk
                    }
                    updateTimeKeeperOnServer(id, false, 0)
                }
            } finally {
                if (timerJobs[id] == this.coroutineContext[Job]) {
                    timerJobs.remove(id)
                }
            }
        }
        updateTimeKeeperOnServer(id, true, initialDuration)
    }

    fun pauseTimer(id: Int) {
        Log.d("TimeKeeperViewModel", "User action: Pause timer ID $id")
        timerJobs[id]?.cancel()

        val currentTimerState = _activeTimers.value[id]
        val durationToReport = currentTimerState?.currentDuration
            ?: _timeKeepers.value.find { it.id == id }?.currentDuration
            ?: 0

        _activeTimers.value = _activeTimers.value.toMutableMap().apply {
            this[id] = TimerState(id, durationToReport, false)
        }
        _timeKeepers.value = _timeKeepers.value.map { tk ->
            if (tk.id == id) tk.copy(started = false, currentDuration = durationToReport) else tk
        }

        updateTimeKeeperOnServer(id, false, durationToReport)
    }

    fun resetTimer(id: Int, newDuration: Int) {
        Log.d("TimeKeeperViewModel", "User action: Reset timer ID $id to duration $newDuration")
        timerJobs[id]?.cancel()

        _activeTimers.value = _activeTimers.value.toMutableMap().apply {
            this[id] = TimerState(id, newDuration, false)
        }
        _timeKeepers.value = _timeKeepers.value.map { tk ->
            if (tk.id == id) tk.copy(started = false, currentDuration = newDuration) else tk
        }
        updateTimeKeeperOnServer(id, false, newDuration)
    }

    private fun updateTimeKeeperOnServer(id: Int, started: Boolean, currentDuration: Int) {
        viewModelScope.launch {
            try {
                val existingTimeKeeper = _timeKeepers.value.find { it.id == id } ?: run {
                    Log.e("TimeKeeperViewModel", "TimeKeeper with id $id not found for server update.")
                    _error.value = "Cannot update: TimeKeeper $id not found."
                    return@launch
                }

                val timeKeeperToUpdate = existingTimeKeeper.copy(
                    started = started,
                    currentDuration = currentDuration
                )
                Log.d("TimeKeeperViewModel", "Updating server for ID $id: Started=$started, Duration=$currentDuration")
                val result = timeKeeperRepository.updateTimeKeeper(id, timeKeeperToUpdate)

                if (result.isSuccess) {
                    val updatedFromServer = result.getOrNull()
                    if (updatedFromServer != null) {
                        Log.d("TimeKeeperViewModel", "Server update success for ID $id. New state: Started=${updatedFromServer.started}, Duration=${updatedFromServer.currentDuration}")
                        _timeKeepers.value = _timeKeepers.value.map {
                            if (it.id == updatedFromServer.id) updatedFromServer else it
                        }
                        _activeTimers.value = _activeTimers.value.toMutableMap().apply {
                            this[id] = TimerState(
                                updatedFromServer.id,
                                updatedFromServer.currentDuration ?: 0,
                                updatedFromServer.started ?: false
                            )
                        }
                        if (updatedFromServer.started != timerJobs[id]?.isActive) {
                            handleTimerNotification(TimerNotification(
                                type = "timer_notification",
                                action = "updated",
                                timer_id = updatedFromServer.id,
                                started = updatedFromServer.started ?: false,
                                current_duration = (updatedFromServer.currentDuration ?: 0).toDouble(),
                                timestamp = ""
                            ))
                        }

                    } else {
                        Log.w("TimeKeeperViewModel", "Server update for ID $id successful but no data returned.")
                    }
                } else {
                    Log.e("TimeKeeperViewModel", "Failed to update timeKeeper on server for ID $id: ${result.exceptionOrNull()?.message}")
                    _error.value = "Failed to update timeKeeper $id on server: ${result.exceptionOrNull()?.message}"

                }
            } catch (e: Exception) {
                Log.e("TimeKeeperViewModel", "Exception during updateTimeKeeperOnServer for ID $id", e)
                _error.value = "Exception updating timeKeeper $id: ${e.message}"
            }
        }
    }

    private suspend fun getStepDurationById(stepId: Int): Int? {
        return try {
            val result = protocolStepRepository.getProtocolStepById(stepId)
            if (result.isSuccess) {
                result.getOrNull()?.stepDuration ?: 0
            } else {
                _error.value = "Failed to retrieve step duration for step $stepId: ${result.exceptionOrNull()?.message}"
                null
            }
        } catch (e: Exception) {
            _error.value = "Exception retrieving step duration for step $stepId: ${e.message}"
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJobs.values.forEach { it.cancel() }
        timerJobs.clear()
        Log.d("TimeKeeperViewModel", "ViewModel cleared, all timer jobs cancelled.")
    }
}