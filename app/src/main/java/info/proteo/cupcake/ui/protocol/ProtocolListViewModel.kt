package info.proteo.cupcake.ui.protocol

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.proteo.cupcake.data.remote.model.protocol.ProtocolModel
import info.proteo.cupcake.data.remote.service.SessionMinimal
import info.proteo.cupcake.data.repository.ProtocolRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProtocolListViewModel @Inject constructor(
    private val protocolRepository: ProtocolRepository
) : ViewModel() {

    private val _protocols = MutableStateFlow<List<ProtocolWithSessions>>(emptyList())
    val protocols: StateFlow<List<ProtocolWithSessions>> = _protocols.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLastPage = MutableStateFlow(false)
    val isLastPage: StateFlow<Boolean> = _isLastPage.asStateFlow()

    private var currentSearch: String? = null
    private var currentOffset = 0
    private val pageSize = 20

    init {
        loadProtocols(refresh = true)
    }

    fun searchProtocols(query: String?) {
        currentSearch = query
        loadProtocols(refresh = true)
    }

    fun loadMoreProtocols() {
        if (!_isLoading.value && !_isLastPage.value) {
            loadProtocols(refresh = false)
        }
    }

    private fun loadProtocols(refresh: Boolean) {
        if (refresh) {
            currentOffset = 0
            _isLastPage.value = false
        }

        if (_isLoading.value) return

        _isLoading.value = true

        viewModelScope.launch {
            Log.d("ProtocolListViewModel", "Loading protocols with offset: $currentOffset, search: $currentSearch")
            protocolRepository.getUserProtocols(
                offset = currentOffset,
                limit = pageSize,
                search = currentSearch,
            ).onSuccess { response ->
                val protocolsWithSessions = mutableListOf<ProtocolWithSessions>()
                Log.d("ProtocolListViewModel", "Loaded ${response.results.size} protocols")

                response.results.forEach { protocol ->
                    val sessionsResult = protocolRepository.getAssociatedSessions(protocol.id)
                    val sessions = sessionsResult.getOrNull() ?: emptyList()
                    protocolsWithSessions.add(ProtocolWithSessions(protocol, sessions))
                }

                _protocols.value = if (refresh) {
                    protocolsWithSessions
                } else {
                    _protocols.value + protocolsWithSessions
                }

                currentOffset += response.results.size
                _isLastPage.value = response.next.isNullOrBlank()
            }.onFailure { error ->
                Log.e("ProtocolListViewModel", "Error loading protocols ${error.message}")
            }

            _isLoading.value = false
        }
    }

    data class ProtocolWithSessions(
        val protocol: ProtocolModel,
        val sessions: List<SessionMinimal>
    )
}