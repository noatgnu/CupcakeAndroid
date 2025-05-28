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
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val messageThreadRepository: MessageThreadRepository
) : ViewModel() {

    private val _userData = MutableLiveData<User?>()
    val userData: LiveData<User?> = _userData

    private val _messageThreads = MutableStateFlow<List<MessageThread>>(emptyList())
    val messageThreads: StateFlow<List<MessageThread>> = _messageThreads.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

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
}