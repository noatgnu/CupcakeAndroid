package info.proteo.cupcake.ui.session

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.proteo.cupcake.data.remote.model.protocol.ProtocolModel
import info.proteo.cupcake.data.remote.model.protocol.ProtocolStep
import info.proteo.cupcake.data.remote.model.protocol.Session
import info.proteo.cupcake.data.remote.model.protocol.StepReagent
import info.proteo.cupcake.data.remote.model.reagent.ReagentAction
import info.proteo.cupcake.data.remote.model.annotation.Annotation
import info.proteo.cupcake.data.remote.model.reagent.StoredReagent
import info.proteo.cupcake.data.remote.service.SessionService
import info.proteo.cupcake.data.repository.AnnotationRepository
import info.proteo.cupcake.data.repository.ProtocolRepository
import info.proteo.cupcake.data.repository.ProtocolStepRepository
import info.proteo.cupcake.data.repository.ReagentActionRepository
import info.proteo.cupcake.data.repository.StoredReagentRepository
import info.proteo.cupcake.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


data class DisplayableStepReagent(
     val stepReagent: StepReagent,
     val existingBookings: List<ReagentAction>,
)

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionService: SessionService,
    private val protocolRepository: ProtocolRepository,
    private val protocolStepRepository: ProtocolStepRepository,
    private val storedReagentRepository: StoredReagentRepository,
    private val reagentActionRepository: ReagentActionRepository,
    private val annotationRepository: AnnotationRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _session = MutableStateFlow<Session?>(null)
    val session: StateFlow<Session?> = _session.asStateFlow()

    private val _protocol = MutableStateFlow<ProtocolModel?>(null)
    val protocol: StateFlow<ProtocolModel?> = _protocol.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentStepReagentInfo = MutableStateFlow<List<DisplayableStepReagent>>(emptyList())
    val currentStepReagentInfo: StateFlow<List<DisplayableStepReagent>> = _currentStepReagentInfo.asStateFlow()

    private val _stepAnnotations = MutableStateFlow<List<Annotation>>(emptyList())
    val stepAnnotations: StateFlow<List<Annotation>> = _stepAnnotations.asStateFlow()

    private val _hasEditPermission = MutableStateFlow(false)
    val hasEditPermission: StateFlow<Boolean> = _hasEditPermission.asStateFlow()

    private val _hasMoreAnnotations = MutableStateFlow(false)
    val hasMoreAnnotations: StateFlow<Boolean> = _hasMoreAnnotations.asStateFlow()


    fun loadAnnotationsForStep(stepId: Int, sessionId: String, offset: Int =0, limit: Int = 10) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d("SessionViewModel", "Requesting annotations with offset=$offset, limit=$limit")

                annotationRepository.getAnnotations(
                    stepId = stepId,
                    sessionUniqueId = sessionId,
                    limit = limit,
                    offset = offset,
                    ordering = "-created_at"
                ).collect { result ->
                    result.onSuccess { response ->
                        _hasMoreAnnotations.value = response.next != null
                        _stepAnnotations.value = response.results

                        Log.d("SessionViewModel", "Annotations loaded successfully: ${response.results.size} annotations")
                    }
                    _isLoading.value = false
                    result.onFailure { error ->
                        Log.e("SessionViewModel", "Error loading annotations: ${error.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("SessionViewModel", "Exception loading annotations", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadReagentInfoForStep(step: ProtocolStep, sessionId: String) {
       viewModelScope.launch {
           val stepReagents = step.reagents ?: emptyList()
           if (stepReagents.isEmpty()) {
               _currentStepReagentInfo.value = emptyList()
               return@launch
           }

           val allBookingsResult = protocolStepRepository.getAssociatedReagentActions(step.id, sessionId)
           val allBookings = allBookingsResult.getOrNull() ?: emptyList()

           val displayableReagents = stepReagents.map { sr ->
               val bookingsForThisReagent = allBookings.filter { action ->
                   action.stepReagent == sr.id || (action.stepReagent == null && action.reagent == sr.reagent.id)
               }
               DisplayableStepReagent(sr, bookingsForThisReagent)
           }
           _currentStepReagentInfo.value = displayableReagents
       }
    }

    fun loadSessionDetails(sessionId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d("SessionViewModel", "Loading session with ID: $sessionId")
                sessionService.getSessionByUniqueId(sessionId)
                    .onSuccess { loadedSession ->
                        _session.value = loadedSession
                        checkSessionPermission(sessionId)
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

    fun bookReagent(storedReagent: StoredReagent, stepReagent: StepReagent, quantity: Float, session: String, notes: String?) {
        viewModelScope.launch {
            val result = reagentActionRepository.createReagentAction(
                reagentId = storedReagent.id,
                actionType = "reserve",
                quantity = quantity,
                notes = notes,
                stepReagent = stepReagent.id,
                session = session
            )
            if (result.isSuccess) {
                val action = result.getOrNull()
                if (action != null) {
                    val currentDisplayedReagents = _currentStepReagentInfo.value
                    val updatedReagents = currentDisplayedReagents.map { displayable ->
                        if (displayable.stepReagent.id == stepReagent.id) {
                            DisplayableStepReagent(
                                stepReagent = displayable.stepReagent,
                                existingBookings = displayable.existingBookings + action
                            )
                        } else {
                            displayable
                        }
                    }
                    _currentStepReagentInfo.value = updatedReagents

                    Log.d("SessionViewModel", "Reagent booked successfully: $action")
                } else {

                    Log.e("SessionViewModel", "Failed to book reagent: Action is null")
                }
            } else {
                Log.e("SessionViewModel", "Error booking reagent: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    suspend fun getAvailableStoredReagents(
        reagentName: String,
        offset: Int = 0,
        limit: Int = 20
    ): Pair<List<StoredReagent>, Boolean> {
        try {
            var resultList = emptyList<StoredReagent>()
            var hasMoreItems = false

            storedReagentRepository.getStoredReagents(
                offset = offset,
                limit = limit,
                search = reagentName
            ).collect { result ->
                if (result.isSuccess) {
                    val response = result.getOrNull()
                    if (response != null) {
                        resultList = response.results
                        hasMoreItems = response.next != null
                    }
                }
            }
            return Pair(resultList, hasMoreItems)
        } catch (e: Exception) {
            Log.e("SessionViewModel", "Error fetching stored reagents: ${e.message}")
            return Pair(emptyList(), false)
        }
    }

    fun checkSessionPermission(sessionId: String) {
        viewModelScope.launch {
            try {
                userRepository.checkSessionPermission(sessionId)
                    .onSuccess { response ->
                        _hasEditPermission.value = response.edit
                        Log.d("SessionViewModel", "Edit permission: ${_hasEditPermission.value}")
                    }
                    .onFailure { error ->
                        _hasEditPermission.value = false
                        Log.e("SessionViewModel", "Failed to check permissions: ${error.message}")
                    }
            } catch (e: Exception) {
                _hasEditPermission.value = false
                Log.e("SessionViewModel", "Exception checking permissions", e)
            }
        }
    }
}