package info.proteo.cupcake.ui.message

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.proteo.cupcake.data.remote.model.user.UserBasic
import info.proteo.cupcake.data.repository.MessageRepository
import info.proteo.cupcake.data.repository.MessageThreadRepository
import info.proteo.cupcake.data.repository.UserRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class NewThreadViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val messageRepository: MessageRepository,
    private val messageThreadRepository: MessageThreadRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _searchResults = MutableStateFlow<List<UserBasic>>(emptyList())
    val searchResults: StateFlow<List<UserBasic>> = _searchResults

    private val _selectedUsers = MutableStateFlow<List<UserBasic>>(emptyList())
    val selectedUsers: StateFlow<List<UserBasic>> = _selectedUsers

    private val _threadCreated = MutableStateFlow(false)
    val threadCreated: StateFlow<Boolean> = _threadCreated

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _isStaffUser = MutableStateFlow(false)
    val isStaffUser: StateFlow<Boolean> = _isStaffUser

    private val _shouldNavigateBack = MutableStateFlow(false)
    val shouldNavigateBack: StateFlow<Boolean> = _shouldNavigateBack

    private var selectedMessageType = "user_message"
    private var selectedPriority = "normal"

    init {
        viewModelScope.launch {
            searchQuery
                .debounce(300)
                .collect { query ->
                    if (query.length >= 2) {
                        searchUsers(query)
                    } else {
                        _searchResults.value = emptyList()
                    }
                }
        }

        viewModelScope.launch {
            checkStaffStatus()
        }
    }

    fun setMessageType(type: String) {
        selectedMessageType = type
    }

    fun setMessagePriority(priority: String) {
        selectedPriority = priority
    }

    private suspend fun checkStaffStatus() {
        userRepository.getUserFromActivePreference()?.let { user ->
            _isStaffUser.value = user.isStaff
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private fun searchUsers(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = userRepository.searchUsers(query)
                Log.d("NewThreadViewModel", "Search result: $result")
                result.fold(
                    onSuccess = { response ->
                        _searchResults.value = response.results
                    },
                    onFailure = { exception ->
                        _errorMessage.value = exception.message ?: "Error searching users"
                        _searchResults.value = emptyList()
                    }
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addUser(user: UserBasic) {
        if (!_selectedUsers.value.contains(user)) {
            _selectedUsers.value = _selectedUsers.value + user
        }
    }

    fun removeUser(user: UserBasic) {
        _selectedUsers.value = _selectedUsers.value.filter { it.id != user.id }
    }

    fun createThreadAndSendMessage(title: String, content: String, messageType: String? = null, priority: String? = null) {
        if (_selectedUsers.value.isEmpty()) {
            _errorMessage.value = "Please select at least one recipient"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val participantIds = _selectedUsers.value.map { it.id }
                val isSystemThread = messageType != "user_message"
                val threadResult = messageThreadRepository.createMessageThread(
                    title = title,
                    participants = participantIds,
                    isSystemThread = isSystemThread
                )

                threadResult.fold(
                    onSuccess = { thread ->
                        // Then create the message in the new thread
                        val messageResult = messageRepository.createMessage(
                            threadId = thread.id,
                            content = content,
                            messageType = selectedMessageType,
                            priority = selectedPriority
                        )

                        messageResult.fold(
                            onSuccess = {
                                _threadCreated.value = true
                                _shouldNavigateBack.value = true
                            },
                            onFailure = { exception ->
                                Log.e("NewThreadViewModel", "Failed to send message", exception)
                                _errorMessage.value = exception.message ?: "Failed to send message"
                            }
                        )
                    },
                    onFailure = { exception ->
                        Log.e("NewThreadViewModel", "Failed to create thread", exception)
                        _errorMessage.value = exception.message ?: "Failed to create thread"
                    }
                )
            } catch (e: Exception) {
                Log.e("NewThreadViewModel", "Error creating thread or sending message", e)
                _errorMessage.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun onNavigatedBack() {
        _shouldNavigateBack.value = false
    }
}