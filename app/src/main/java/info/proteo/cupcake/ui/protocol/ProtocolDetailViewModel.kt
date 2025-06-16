package info.proteo.cupcake.ui.protocol

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.proteo.cupcake.shared.data.model.LimitOffsetResponse
import info.proteo.cupcake.shared.data.model.protocol.ProtocolModel
import info.proteo.cupcake.shared.data.model.protocol.ProtocolStep
import info.proteo.cupcake.shared.data.model.protocol.ProtocolTag
import info.proteo.cupcake.shared.data.model.protocol.Session
import info.proteo.cupcake.data.repository.ProtocolRepository
import info.proteo.cupcake.data.repository.TagRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProtocolDetailViewModel @Inject constructor(
    private val protocolRepository: ProtocolRepository,
    private val tagRepository: TagRepository
) : ViewModel() {

    private val _protocol = MutableStateFlow<ProtocolModel?>(null)
    val protocol: StateFlow<ProtocolModel?> = _protocol.asStateFlow()

    private val _sessions = MutableStateFlow<List<Session>>(emptyList())
    val sessions: StateFlow<List<Session>> = _sessions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _sectionStepsMap = MutableStateFlow<Map<Int, List<ProtocolStep>>>(emptyMap())
    val sectionStepsMap = _sectionStepsMap.asStateFlow()

    fun loadProtocolDetails(protocolId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                protocolRepository.getProtocolById(protocolId).onSuccess { protocol ->
                    _protocol.value = protocol

                    val stepsMap = mutableMapOf<Int, MutableList<ProtocolStep>>()
                    protocol.steps?.forEach { step ->
                        step.stepSection?.let { sectionId ->
                            val sectionSteps = stepsMap.getOrPut(sectionId) { mutableListOf() }
                            sectionSteps.add(step)
                        }
                    }

                    stepsMap.forEach { (_, steps) ->
                        steps.sortBy { it.stepId }
                    }

                    _sectionStepsMap.value = stepsMap
                }

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

    suspend fun searchTags(query: String): Result<LimitOffsetResponse<ProtocolTag>> {
        return try {
            tagRepository.getProtocolTags(search = query)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTagById(id: Int): Result<ProtocolTag> {
        return try {
            tagRepository.getProtocolTagById(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}