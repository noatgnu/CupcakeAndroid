package info.proteo.cupcake.ui.main

import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.proteo.cupcake.data.remote.model.message.MessageThread
import info.proteo.cupcake.data.remote.model.user.User
import info.proteo.cupcake.data.repository.MessageThreadRepository
import info.proteo.cupcake.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import info.proteo.cupcake.data.local.dao.protocol.RecentSessionDao
import info.proteo.cupcake.data.local.entity.protocol.RecentSessionEntity
import info.proteo.cupcake.data.remote.model.protocol.TimeKeeper
import info.proteo.cupcake.data.remote.service.WebSocketService
import info.proteo.cupcake.data.repository.TimeKeeperRepository
import info.proteo.cupcake.ui.timekeeper.TimeKeeperViewModel.TimerState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import kotlin.math.max

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val messageThreadRepository: MessageThreadRepository,
    private val timeKeeperRepository: TimeKeeperRepository,
    private val webSocketService: WebSocketService,
    private val recentSessionDao: RecentSessionDao
) : ViewModel() {

    private val _userData = MutableLiveData<User?>()
    val userData: LiveData<User?> = _userData

    private val _messageThreads = MutableStateFlow<List<MessageThread>>(emptyList())
    val messageThreads: StateFlow<List<MessageThread>> = _messageThreads.asStateFlow()

    private val _activeTimekeepers = MutableStateFlow<List<TimeKeeper>>(emptyList())
    val activeTimekeepers: StateFlow<List<TimeKeeper>> = _activeTimekeepers.asStateFlow()

    private val _activeTimekeepersCount = MutableStateFlow(0)
    val activeTimekeepersCount: StateFlow<Int> = _activeTimekeepersCount.asStateFlow()

    private val _activeTimerStates = MutableStateFlow<Map<Int, TimerState>>(emptyMap())
    val activeTimerStates: StateFlow<Map<Int, TimerState>> = _activeTimerStates.asStateFlow()

    private val _recentSessions = MutableStateFlow<List<RecentSessionEntity>>(emptyList())
    val recentSessions: StateFlow<List<RecentSessionEntity>> = _recentSessions.asStateFlow()

    private var countDownTimer: CountDownTimer? = null
    private val timerUpdateIntervalMs = 1000L

    private var timerJob: Job? = null
    private var webSocketListenerJob: Job? = null


    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private data class TimerNotification(
        val type: String,
        val action: String,
        val timer_id: Int,
        val started: Boolean,
        val current_duration: Double,
        val timestamp: String
    )

    init {
        observeWebSocketNotifications()
    }

    private fun fetchRecentSessions() {
        viewModelScope.launch {
            try {
                val user = userRepository.getUserFromActivePreference()
                if (user != null) {
                    recentSessionDao.getRecentSessionsByUser(user.id, 5).collect { sessions ->
                        _recentSessions.value = sessions
                        Log.d("MainViewModel", "Fetched recent sessions: ${sessions.size}")
                    }
                } else {
                    _recentSessions.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error fetching recent sessions", e)
                _recentSessions.value = emptyList()
            }
        }
    }

    private fun observeWebSocketNotifications() {
        webSocketListenerJob?.cancel()
        webSocketListenerJob = viewModelScope.launch {
            webSocketService.notificationMessages
                .catch { e ->
                    Log.e("MainViewModel", "Error collecting WebSocket messages", e)
                }
                .collect { jsonString ->
                    try {
                        Log.d("MainViewModel", "Received WebSocket message: $jsonString")
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
                        Log.e("MainViewModel", "Error parsing WebSocket message", e)
                    }
                }
        }
    }

    private fun handleTimerNotification(notification: TimerNotification) {
        val timerId = notification.timer_id
        val wsStarted = notification.started
        val wsDuration = notification.current_duration.toInt().coerceAtLeast(0)

        Log.d("MainViewModel", "Handling WS Notification for Timer ID $timerId: Started=$wsStarted, Duration=$wsDuration")

        _activeTimerStates.value = _activeTimerStates.value.toMutableMap().apply {
            this[timerId] = TimerState(timerId, wsDuration, wsStarted)
        }

        val currentDisplayedTks = _activeTimekeepers.value.toMutableList()
        val existingTkIndex = currentDisplayedTks.indexOfFirst { it.id == timerId }

        var needsRefresh = false

        if (existingTkIndex != -1) {
            val oldTk = currentDisplayedTks[existingTkIndex]
            if (wsStarted) {
                currentDisplayedTks[existingTkIndex] = oldTk.copy(started = true, currentDuration = wsDuration)
                _activeTimekeepers.value = currentDisplayedTks
            } else {

                if (oldTk.started == true) {
                    needsRefresh = true
                }
            }
        } else {
            if (wsStarted) {
                needsRefresh = true
            }
        }

        if (needsRefresh) {
            Log.d("MainViewModel", "WS: Needs refresh for timer $timerId, wsStarted: $wsStarted")
            fetchActiveTimekeepers()
        } else {
            if (existingTkIndex != -1 && wsStarted) {
                _activeTimekeepers.value = _activeTimekeepers.value.map {
                    if (it.id == timerId) {
                        it.copy(started = true, currentDuration = wsDuration)
                    } else {
                        it
                    }
                }
            }
        }
    }


    private fun startTimerUpdates() {
        stopTimerUpdates() // Ensure any existing timer is stopped

        countDownTimer = object : CountDownTimer(Long.MAX_VALUE, timerUpdateIntervalMs) {
            override fun onTick(millisUntilFinished: Long) {
                updateTimerStates()
            }

            override fun onFinish() {
                // This won't be called unless we approach Long.MAX_VALUE
            }
        }.start()
    }

    private fun stopTimerUpdates() {
        countDownTimer?.cancel()
        countDownTimer = null
    }



    private fun updateTimerStates() {
        val currentTimekeepers = _activeTimekeepers.value
        if (currentTimekeepers.isEmpty()) return

        val updatedTimerStates = mutableMapOf<Int, TimerState>()
        val currentTime = System.currentTimeMillis()

        currentTimekeepers.forEach { timekeeper ->
            if (timekeeper.started == true && timekeeper.startTime != null) {
                try {
                    val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
                    formatter.timeZone = TimeZone.getTimeZone("UTC")
                    val startTime = formatter.parse(timekeeper.startTime)?.time ?: 0L

                    val elapsedSeconds = ((currentTime - startTime) / 1000).toInt()
                    val initialDuration = timekeeper.currentDuration ?: 0
                    val remainingDuration = (initialDuration - elapsedSeconds).coerceAtLeast(0)

                    updatedTimerStates[timekeeper.id] = TimerState(
                        started = true,
                        currentDuration = remainingDuration,
                        id = timekeeper.id
                    )
                } catch (e: Exception) {
                    // Fallback if date parsing fails
                    updatedTimerStates[timekeeper.id] = TimerState(
                        started = true,
                        currentDuration = timekeeper.currentDuration ?: 0,
                        id = timekeeper.id
                    )
                }
            } else {
                updatedTimerStates[timekeeper.id] = TimerState(
                    started = true,
                    currentDuration = timekeeper.currentDuration ?: 0,
                    id = timekeeper.id
                )
            }
        }

        _activeTimerStates.value = updatedTimerStates
    }


    private fun parseTimeToSeconds(timeStr: String?): Long {
        return try {
            if (timeStr == null) return 0
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            format.timeZone = TimeZone.getTimeZone("UTC")
            val date = format.parse(timeStr) ?: return 0
            date.time / 1000
        } catch (e: Exception) {
            try {
                val formatNoMillis = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                formatNoMillis.timeZone = TimeZone.getTimeZone("UTC")
                val date = formatNoMillis.parse(timeStr) ?: return 0
                date.time / 1000
            } catch (e2: Exception) {
                Log.e("MainViewModel", "Error parsing time string: $timeStr", e)
                0
            }
        }
    }


    private fun fetchActiveTimekeepers() {
        viewModelScope.launch {
            try {
                val result = timeKeeperRepository.getTimeKeepers(
                    limit = 5,
                    started = true
                )

                if (result.isSuccess) {
                    val fetchedTimekeepers = result.getOrNull()?.results ?: emptyList()
                    val totalCount = result.getOrNull()?.count ?: 0

                    _activeTimekeepers.value = fetchedTimekeepers
                    _activeTimekeepersCount.value = totalCount

                    val newTimerStates = _activeTimerStates.value.toMutableMap()
                    fetchedTimekeepers.forEach { tk ->
                        if (!newTimerStates.containsKey(tk.id) || newTimerStates[tk.id]?.started != tk.started || newTimerStates[tk.id]?.currentDuration != tk.currentDuration) {
                            newTimerStates[tk.id] = TimerState(
                                id = tk.id,
                                currentDuration = tk.currentDuration ?: 0,
                                started = tk.started ?: false
                            )
                        }
                    }
                    _activeTimerStates.value = newTimerStates
                    Log.d("MainViewModel", "Active timekeepers fetched: ${fetchedTimekeepers.size}, Total active: $totalCount")
                    if (fetchedTimekeepers.isNotEmpty()) {
                        startTimerUpdates()
                    } else {
                        timerJob?.cancel()
                    }
                } else {
                    Log.e("MainViewModel", "Error fetching active timekeepers: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Exception fetching timekeepers", e)
            } finally {
            }
        }
    }


    fun loadUserData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d("MainViewModel", "Loading user data")
                val user = userRepository.getUserFromActivePreference()
                Log.d("MainViewModel", "getUserFromActivePreference returned: $user")
                _userData.postValue(user)

                if (user != null) {
                    Log.d("MainViewModel", "User data found: ${user.username}")
                    fetchLatestMessageThreads()
                    fetchActiveTimekeepers()
                    fetchRecentSessions()
                    startTimerUpdates()
                } else {
                    Log.d("MainViewModel", "No active user found")
                    _messageThreads.value = emptyList()
                    _activeTimekeepers.value = emptyList()
                    _activeTimekeepersCount.value = 0
                    _activeTimerStates.value = emptyMap()
                    stopTimerUpdates()
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading user data", e)
                _userData.postValue(null)
                _messageThreads.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }



    fun fetchLatestMessageThreads() {
        Log.d("MainViewModel", "Fetching latest message threads")
        viewModelScope.launch {
            messageThreadRepository.getMessageThreads(
                offset = 0,
                limit = 5
            ).collect { result ->
                result.onSuccess { response ->
                    Log.d("MainViewModel", "Fetched message threads: ${response.results.size}")
                    _messageThreads.value = response.results
                }.onFailure {
                    Log.e("MainViewModel", "Error fetching message threads", it)
                    _messageThreads.value = emptyList()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopTimerUpdates()
        webSocketListenerJob?.cancel() // Clean up WebSocket listener
        Log.d("MainViewModel", "ViewModel cleared, jobs cancelled.")
    }
}