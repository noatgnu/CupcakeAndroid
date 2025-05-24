package info.proteo.cupcake.ui.message

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.proteo.cupcake.data.remote.model.message.Message
import info.proteo.cupcake.data.remote.model.message.MessageThreadDetail
import info.proteo.cupcake.data.remote.model.message.ThreadMessage
import info.proteo.cupcake.data.repository.MessageRepository
import info.proteo.cupcake.data.repository.MessageThreadRepository
import info.proteo.cupcake.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThreadDetailViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val messageThreadRepository: MessageThreadRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _threadDetails = MutableStateFlow<MessageThreadDetail?>(null)
    val threadDetails: StateFlow<MessageThreadDetail?> = _threadDetails.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _sendingMessage = MutableStateFlow(false)
    val sendingMessage: StateFlow<Boolean> = _sendingMessage.asStateFlow()

    private val _isStaffUser = MutableStateFlow(false)
    val isStaffUser: StateFlow<Boolean> = _isStaffUser.asStateFlow()

    private val _messageType = MutableStateFlow("user_message")
    val messageType: StateFlow<String> = _messageType.asStateFlow()

    private val _messagePriority = MutableStateFlow("normal")
    val messagePriority: StateFlow<String> = _messagePriority.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.getUserFromActivePreference()?.let {
                _isStaffUser.value = it.isStaff
            }
        }
    }

    fun setMessageType(type: String) {
        if (_isStaffUser.value) {
            _messageType.value = type
        }
    }

    fun setMessagePriority(priority: String) {
        if (_isStaffUser.value) {
            _messagePriority.value = priority
        }
    }

    fun loadThreadDetails(threadId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                messageThreadRepository.getMessageThread(threadId).collect { result ->
                    _isLoading.value = false

                    result.onSuccess { threadDetail ->
                        _threadDetails.value = threadDetail
                    }.onFailure { exception ->
                        _error.value = exception.message ?: "Failed to load thread details"
                    }
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _error.value = e.message ?: "An unexpected error occurred"
            }
        }
    }

    fun sendMessage(htmlContent: String) {
        val threadId = _threadDetails.value?.id ?: return

        viewModelScope.launch {
            _sendingMessage.value = true

            try {
                val result = messageRepository.createMessage(threadId, htmlContent, _messageType.value,
                    _messagePriority.value)
                _sendingMessage.value = false

                result.fold(
                    onSuccess = { message ->
                        _threadDetails.value?.let { currentThread ->
                            val updatedMessages = currentThread.messages.toMutableList().apply {
                                add(message.toThreadMessage())
                            }
                            _threadDetails.value = currentThread.copy(messages = updatedMessages)
                        }
                    },
                    onFailure = { exception ->
                        _sendingMessage.value = false
                        _error.value = exception.message ?: "An unexpected error occurred while sending message"

                    }
                )
            } catch (e: Exception) {
                _sendingMessage.value = false
                _error.value = e.message ?: "An unexpected error occurred while sending message"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun refreshThread() {
        _threadDetails.value?.id?.let { loadThreadDetails(it) }
    }

    private fun Message.toThreadMessage(): ThreadMessage {
        return ThreadMessage(
            id = this.id,
            sender = this.sender,
            content = this.content,
            createdAt = this.createdAt,
            messageType = this.messageType,
            priority = this.priority,
            attachmentCount = this.attachments?.size ?: 0,
            isRead = this.isRead
        )
    }
}