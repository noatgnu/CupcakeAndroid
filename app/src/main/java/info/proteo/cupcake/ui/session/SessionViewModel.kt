package info.proteo.cupcake.ui.session

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.proteo.cupcake.SessionManager
import info.proteo.cupcake.data.local.dao.protocol.RecentSessionDao
import info.proteo.cupcake.data.local.entity.protocol.RecentSessionEntity
import info.proteo.cupcake.data.remote.model.protocol.ProtocolModel
import info.proteo.cupcake.data.remote.model.protocol.ProtocolStep
import info.proteo.cupcake.data.remote.model.protocol.Session
import info.proteo.cupcake.data.remote.model.protocol.StepReagent
import info.proteo.cupcake.data.remote.model.reagent.ReagentAction
import info.proteo.cupcake.data.remote.model.annotation.Annotation
import info.proteo.cupcake.data.remote.model.annotation.AnnotationWithPermissions
import info.proteo.cupcake.data.remote.model.instrument.Instrument
import info.proteo.cupcake.data.remote.model.reagent.StoredReagent
import info.proteo.cupcake.data.remote.model.user.User
import info.proteo.cupcake.data.remote.service.CreateAnnotationRequest
import info.proteo.cupcake.data.remote.service.SessionService
import info.proteo.cupcake.data.repository.AnnotationRepository
import info.proteo.cupcake.data.repository.InstrumentRepository
import info.proteo.cupcake.data.repository.ProtocolRepository
import info.proteo.cupcake.data.repository.ProtocolStepRepository
import info.proteo.cupcake.data.repository.ReagentActionRepository
import info.proteo.cupcake.data.repository.StoredReagentRepository
import info.proteo.cupcake.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.MultipartBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import kotlin.text.format
import kotlin.text.get


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
    private val userRepository: UserRepository,
    private val recentSessionDao: RecentSessionDao,
    private val sessionManager: SessionManager,
    private val instrumentRepository: InstrumentRepository
) : ViewModel() {

    private val _session = MutableStateFlow<Session?>(null)
    val session: StateFlow<Session?> = _session.asStateFlow()

    private val _protocol = MutableStateFlow<ProtocolModel?>(null)
    val protocol: StateFlow<ProtocolModel?> = _protocol.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentStepReagentInfo = MutableStateFlow<List<DisplayableStepReagent>>(emptyList())
    val currentStepReagentInfo: StateFlow<List<DisplayableStepReagent>> = _currentStepReagentInfo.asStateFlow()

    private val _stepAnnotations = MutableStateFlow<List<AnnotationWithPermissions>>(emptyList())
    val stepAnnotations: StateFlow<List<AnnotationWithPermissions>> = _stepAnnotations.asStateFlow()

    private val _hasEditPermission = MutableStateFlow(false)
    val hasEditPermission: StateFlow<Boolean> = _hasEditPermission.asStateFlow()

    private val _hasMoreAnnotations = MutableStateFlow(false)
    val hasMoreAnnotations: StateFlow<Boolean> = _hasMoreAnnotations.asStateFlow()

    private val _instruments = MutableStateFlow<List<Instrument>>(emptyList())
    val instruments: StateFlow<List<Instrument>> = _instruments.asStateFlow()



    fun updateRecentSession(session: Session, protocolId: Int, protocolName: String?, stepId: Int?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val activeUser = userRepository.getUserFromActivePreference()
                if (activeUser != null) {
                    val currentTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }.format(Date())

                    val recentS = recentSessionDao.getRecentSessionsByUser(activeUser.id, 100).first()
                    val existingSession = recentS.find { it.sessionUniqueId == session.uniqueId }
                    if (existingSession != null) {
                        val updatedSession = existingSession.copy(
                            lastAccessed = currentTime,
                            sessionName = existingSession.sessionName,
                            protocolName = protocolName,
                            stepId = stepId
                        )
                        recentSessionDao.update(updatedSession)
                    } else {
                        val recentSession = RecentSessionEntity(
                            id = 0,
                            sessionId = session.id,
                            protocolId = protocolId,
                            sessionUniqueId = session.uniqueId,
                            userId = activeUser.id,
                            protocolName = protocolName,
                            lastAccessed = currentTime,
                            sessionName = session.name,
                            stepId = stepId
                        )
                        recentSessionDao.insert(recentSession)
                    }


                    val recentSessions = recentSessionDao.getRecentSessionsByUser(activeUser.id, 100).first()
                    if (recentSessions.size > RecentSessionEntity.MAX_RECENT_SESSIONS) {
                        recentSessions
                            .sortedBy { it.lastAccessed }
                            .take(recentSessions.size - RecentSessionEntity.MAX_RECENT_SESSIONS)
                            .forEach { recentSessionDao.delete(it) }
                    }
                }
            } catch (e: Exception) {
                Log.e("SessionViewModel", "Error updating recent sessions: ${e.message}")
            }
        }
    }

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
                        val annotations = response.results
                        if (annotations.isNotEmpty()) {
                            val annotationIds = annotations.map { it.id }
                            val permissionsResult = userRepository.checkAnnotationsPermission(annotationIds)

                            if (permissionsResult.isSuccess) {
                                val permissions = permissionsResult.getOrDefault(emptyList())
                                Log.d("SessionViewModel", "Permissions loaded successfully: ${permissions.size} permissions")
                                val permissionsMap = permissions.associate {
                                    it.annotation to it.permission
                                }
                                Log.d("SessionViewModel", "Permissions map created with ${permissionsMap.size} entries")

                                val annotationsWithPermissions = annotations.map { annotation ->
                                    val permission = permissionsMap[annotation.id]
                                    AnnotationWithPermissions(
                                        annotation = annotation,
                                        canEdit = permission?.edit == true,
                                        canDelete = permission?.delete == true
                                    )
                                }

                                _stepAnnotations.value = annotationsWithPermissions
                            } else {
                                // If permission check fails, assume no permissions
                                _stepAnnotations.value = annotations.map {
                                    AnnotationWithPermissions(it, false, false)
                                }
                            }
                        }  else {
                            _stepAnnotations.value = emptyList()
                        }
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

    fun getInstruments(offset: Int, limit: Int) {
        viewModelScope.launch {
            try {
                instrumentRepository.getInstruments(limit = limit, offset = offset).collect {
                    result ->
                    result.onSuccess { response ->
                        Log.d("SessionViewModel", "Instruments loaded successfully: ${response.results.size} instruments")

                        _instruments.value = response.results
                    }
                    result.onFailure { error ->
                        Log.e("SessionViewModel", "Error loading instruments: ${error.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("SessionViewModel", "Exception loading instruments", e)
            }
        }
    }

    fun createAnnotation(request: CreateAnnotationRequest, filePart: MultipartBody.Part?) {
        viewModelScope.launch {
            val response = annotationRepository.createAnnotationInRepository(request, filePart).getOrNull()
            if (response != null) {
                Log.d("SessionViewModel", "Annotation created successfully: ${response.id}")
                // Optionally, you can refresh annotations for the current step
                if (response.step != null && session.value != null) {
                    loadAnnotationsForStep(response.step, session.value!!.uniqueId, 0, 10)
                }
            } else {
                Log.e("SessionViewModel", "Failed to create annotation")
            }
        }
    }

    fun deleteAnnotation(annotationId: Int) {
        viewModelScope.launch {
            try {
                val result = annotationRepository.deleteAnnotation(annotationId)
                if (result.isSuccess) {
                    // Remove the deleted annotation from the list
                    _stepAnnotations.value = _stepAnnotations.value.filter {
                        it.annotation.id != annotationId
                    }
                }
            } catch (e: Exception) {
                Log.e("SessionViewModel", "Error deleting annotation: ${e.message}")
            }
        }
    }

    fun renameAnnotation(annotationId: Int, annotationName: String?) {
        viewModelScope.launch {
            try {
                if (annotationName.isNullOrBlank()) {
                    Log.e("SessionViewModel", "Annotation name cannot be null or blank")
                    return@launch
                }
                val result = annotationRepository.renameAnnotation(annotationId, annotationName)
                if (result.isSuccess) {
                    // Update the annotation in the list
                    _stepAnnotations.value = _stepAnnotations.value.map { annotationWithPermissions ->
                        if (annotationWithPermissions.annotation.id == annotationId) {
                            AnnotationWithPermissions(
                                annotation = annotationWithPermissions.annotation.copy(annotationName = annotationName),
                                canEdit = annotationWithPermissions.canEdit,
                                canDelete = annotationWithPermissions.canDelete
                            )
                        } else {
                            annotationWithPermissions
                        }
                    }
                } else {
                    Log.e("SessionViewModel", "Failed to rename annotation: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {

            }
        }
    }

    fun getRecentSession(userId: Int, sessionId: String, protocolId: Int): RecentSessionEntity? {
        return runBlocking {
            try {
                // Get the most recent session for this user
                val recentSession = recentSessionDao.getMostRecentSession(userId)

                // Check if it matches our current session and protocol
                if (recentSession?.sessionId == sessionId.toInt() && recentSession.protocolId == protocolId) {
                    return@runBlocking recentSession
                }
                null
            } catch (e: Exception) {
                Log.e("SessionViewModel", "Error getting recent session: ${e.message}")
                null
            }
        }
    }
}