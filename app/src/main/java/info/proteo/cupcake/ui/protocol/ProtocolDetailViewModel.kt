package info.proteo.cupcake.ui.protocol

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.proteo.cupcake.data.remote.model.protocol.ProtocolModel
import info.proteo.cupcake.data.remote.model.protocol.Session
import info.proteo.cupcake.data.remote.service.SessionMinimal
import info.proteo.cupcake.data.repository.ProtocolRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProtocolDetailViewModel @Inject constructor(
    private val protocolRepository: ProtocolRepository
) : ViewModel() {

    private val _protocol = MutableStateFlow<ProtocolModel?>(null)
    val protocol: StateFlow<ProtocolModel?> = _protocol.asStateFlow()

    private val _sessions = MutableStateFlow<List<Session>>(emptyList())
    val sessions: StateFlow<List<Session>> = _sessions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadProtocolDetails(protocolId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Load protocol details
                protocolRepository.getProtocolById(protocolId).onSuccess { protocol ->
                    Log.d("ProtocolDetailViewModel", "Loaded protocol: ${protocol.reagents}")
                    _protocol.value = protocol
                }

                // Load associated sessions
                protocolRepository.getAssociatedSessions(protocolId).onSuccess { sessionsList ->
                    _sessions.value = sessionsList
                }
            } catch (e: Exception) {
                Log.e("ProtocolDetailViewModel", "Error loading protocol details: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}