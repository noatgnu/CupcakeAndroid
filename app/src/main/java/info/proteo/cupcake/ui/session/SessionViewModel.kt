package info.proteo.cupcake.ui.session

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.proteo.cupcake.data.remote.model.protocol.ProtocolModel
import info.proteo.cupcake.data.remote.model.protocol.Session
import info.proteo.cupcake.data.remote.service.SessionService
import info.proteo.cupcake.data.repository.ProtocolRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionService: SessionService,
    private val protocolRepository: ProtocolRepository
) : ViewModel() {

    private val _session = MutableStateFlow<Session?>(null)
    val session: StateFlow<Session?> = _session.asStateFlow()

    private val _protocol = MutableStateFlow<ProtocolModel?>(null)
    val protocol: StateFlow<ProtocolModel?> = _protocol.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadSessionDetails(sessionId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d("SessionViewModel", "Loading session with ID: $sessionId")
                sessionService.getSessionByUniqueId(sessionId)
                    .onSuccess { loadedSession ->
                        _session.value = loadedSession
                    }
                    .onFailure { error ->
                        Log.e("SessionViewModel", "Error loading session: ${error.message}")
                    }
            } catch (e: Exception) {
                Log.e("SessionViewModel", "Exception loading session", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadProtocolDetails(protocolId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                protocolRepository.getProtocolById(protocolId)
                    .onSuccess { loadedProtocol ->
                        _protocol.value = loadedProtocol
                    }
                    .onFailure { error ->
                        Log.e("SessionViewModel", "Error loading protocol: ${error.message}")
                    }
            } catch (e: Exception) {
                Log.e("SessionViewModel", "Exception loading protocol", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}