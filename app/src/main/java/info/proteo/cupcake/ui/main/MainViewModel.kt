package info.proteo.cupcake.ui.main

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
import info.proteo.cupcake.data.remote.model.protocol.TimeKeeper
import info.proteo.cupcake.data.repository.TimeKeeperRepository
import info.proteo.cupcake.ui.timekeeper.TimeKeeperViewModel.TimerState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import kotlin.collections.component1
import kotlin.collections.component2

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val messageThreadRepository: MessageThreadRepository,
    private val timeKeeperRepository: TimeKeeperRepository
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

    private var timerJob: Job? = null


    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private fun startTimerUpdates() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                updateTimerStates()
                delay(1000)
            }
        }
    }

    private fun updateTimerStates() {
        val currentStates = _activeTimerStates.value.toMutableMap()
        val currentTime = System.currentTimeMillis() / 1000

        _activeTimekeepers.value.forEach { timeKeeper ->
            if (timeKeeper.started == true) {
                val totalDuration = timeKeeper.currentDuration ?: 0f
                val startTimeSeconds = parseTimeToSeconds(timeKeeper.startTime)
                val elapsedSeconds = if (startTimeSeconds > 0) {
                    (currentTime - startTimeSeconds).toFloat()
                } else {
                    0f
                }
                val remainingSeconds = maxOf(0f, totalDuration - elapsedSeconds)

                currentStates[timeKeeper.id] = TimerState(
                    timeKeeper.id,
                    started = true,
                    currentDuration = remainingSeconds
                )
            } else {
                currentStates[timeKeeper.id] = TimerState(
                    timeKeeper.id,
                    started = false,
                    currentDuration = timeKeeper.currentDuration ?: 0f
                )
            }
        }

        _activeTimerStates.value = currentStates
    }

    private fun parseTimeToSeconds(timeStr: String?): Long {
        return try {
            if (timeStr == null) return 0
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            format.timeZone = TimeZone.getTimeZone("UTC")
            val date = format.parse(timeStr) ?: return 0
            date.time / 1000
        } catch (e: Exception) {
            0
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
                    val timekeepers = result.getOrNull()?.results ?: emptyList()
                    _activeTimekeepers.value = timekeepers
                    _activeTimekeepersCount.value = result.getOrNull()?.count ?: 0
                    updateTimerStates()
                    startTimerUpdates()
                    Log.d("MainViewModel", "Active timekeepers fetched successfully: ${timekeepers}")

                    Log.d("MainViewModel", "Active timekeepers fetched: ${_activeTimekeepers.value.size}")
                } else {
                    Log.e("MainViewModel", "Error fetching active timekeepers: ${result.exceptionOrNull()}")
                    _activeTimekeepers.value = emptyList()
                    _activeTimekeepersCount.value = 0
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Exception fetching timekeepers", e)
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
                } else {
                    Log.d("MainViewModel", "No active user found")
                    _messageThreads.value = emptyList()
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading user data", e)
                _userData.postValue(null)
                _messageThreads.value = emptyList()
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
                    Log.d("MainViewModel", "Fetched message threads: ${response.results}")
                    _messageThreads.value = response.results
                }.onFailure {
                    Log.e("MainViewModel", "Error fetching message threads", it)
                    _messageThreads.value = emptyList()
                }
                _isLoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}