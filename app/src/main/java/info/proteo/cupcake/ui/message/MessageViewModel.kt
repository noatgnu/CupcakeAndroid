package info.proteo.cupcake.ui.message

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.proteo.cupcake.data.remote.model.message.MessageThread
import info.proteo.cupcake.data.repository.MessageThreadRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessageViewModel @Inject constructor(
    private val messageThreadRepository: MessageThreadRepository
) : ViewModel() {

    private val _messageThreads = MutableStateFlow<List<MessageThread>>(emptyList())
    val messageThreads: StateFlow<List<MessageThread>> = _messageThreads.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _hasError = MutableStateFlow(false)
    val hasError: StateFlow<Boolean> = _hasError.asStateFlow()

    private var currentPage = 0
    private val pageSize = 20
    private var isLastPage = false

    init {
        loadFirstPage()
    }

    fun loadFirstPage() {
        currentPage = 0
        isLastPage = false
        _messageThreads.value = emptyList()
        loadMoreThreads()
    }

    fun loadMoreThreads() {
        if (_isLoading.value || isLastPage) return

        viewModelScope.launch {
            _isLoading.value = true
            _hasError.value = false

            messageThreadRepository.getMessageThreads(
                offset = currentPage * pageSize,
                limit = pageSize
            ).collect { result ->
                _isLoading.value = false

                result.onSuccess { response ->
                    val newThreads = response.results
                    if (newThreads.isEmpty()) {
                        isLastPage = true
                    } else {
                        val currentThreads = _messageThreads.value.toMutableList()
                        currentThreads.addAll(newThreads)
                        _messageThreads.value = currentThreads
                        currentPage++
                    }
                }.onFailure {
                    _hasError.value = true
                }
            }
        }
    }
}