package info.proteo.cupcake.ui.labgroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.proteo.cupcake.data.repository.LabGroupRepository
import info.proteo.cupcake.data.repository.UserRepository
import info.proteo.cupcake.shared.data.model.user.LabGroup
import info.proteo.cupcake.shared.data.model.user.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LabGroupManagementUiState(
    val labGroups: List<LabGroup> = emptyList(),
    val filteredLabGroups: List<LabGroup> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val filterType: FilterType = FilterType.ALL,
    val isStaff: Boolean = false,
    val currentUser: User? = null
)

enum class FilterType {
    ALL, PROFESSIONAL_ONLY, REGULAR_ONLY
}

@HiltViewModel
class LabGroupManagementViewModel @Inject constructor(
    private val labGroupRepository: LabGroupRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LabGroupManagementUiState())
    val uiState: StateFlow<LabGroupManagementUiState> = _uiState.asStateFlow()

    private val _createLabGroupResult = MutableStateFlow<Result<LabGroup>?>(null)
    val createLabGroupResult: StateFlow<Result<LabGroup>?> = _createLabGroupResult.asStateFlow()

    private val _updateLabGroupResult = MutableStateFlow<Result<LabGroup>?>(null)
    val updateLabGroupResult: StateFlow<Result<LabGroup>?> = _updateLabGroupResult.asStateFlow()

    private val _deleteLabGroupResult = MutableStateFlow<Result<Unit>?>(null)
    val deleteLabGroupResult: StateFlow<Result<Unit>?> = _deleteLabGroupResult.asStateFlow()

    private val _removeUserResult = MutableStateFlow<Result<Unit>?>(null)
    val removeUserResult: StateFlow<Result<Unit>?> = _removeUserResult.asStateFlow()

    init {
        checkUserPermissions()
        loadLabGroups()
    }

    private fun checkUserPermissions() {
        viewModelScope.launch {
            try {
                val userResult = userRepository.getCurrentUser()
                if (userResult.isSuccess) {
                    val user = userResult.getOrNull()
                    val staffResult = userRepository.isStaff()
                    val isStaff = staffResult.getOrNull()?.isStaff ?: false

                    _uiState.value = _uiState.value.copy(
                        currentUser = user,
                        isStaff = isStaff
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to check user permissions: ${e.message}"
                )
            }
        }
    }

    fun loadLabGroups() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = labGroupRepository.getLabGroups(
                    offset = 0,
                    limit = 100, // Load all lab groups for management
                    search = null,
                    ordering = "name",
                    storedReagentId = null,
                    storageObjectId = null,
                    isProfessional = null
                )

                if (result.isSuccess) {
                    val response = result.getOrNull()
                    val labGroups = response?.results ?: emptyList()
                    
                    _uiState.value = _uiState.value.copy(
                        labGroups = labGroups,
                        isLoading = false,
                        error = null
                    )
                    
                    // Apply current filters
                    applyFilters()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load lab groups: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "An unexpected error occurred: ${e.message}"
                )
            }
        }
    }

    fun searchLabGroups(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilters()
    }

    fun setFilter(filterType: FilterType) {
        _uiState.value = _uiState.value.copy(filterType = filterType)
        applyFilters()
    }

    private fun applyFilters() {
        val currentState = _uiState.value
        var filtered = currentState.labGroups

        // Apply search filter
        if (currentState.searchQuery.isNotBlank()) {
            filtered = filtered.filter { labGroup ->
                labGroup.name.contains(currentState.searchQuery, ignoreCase = true) ||
                labGroup.description?.contains(currentState.searchQuery, ignoreCase = true) == true
            }
        }

        // Apply type filter
        filtered = when (currentState.filterType) {
            FilterType.ALL -> filtered
            FilterType.PROFESSIONAL_ONLY -> filtered.filter { it.isProfessional }
            FilterType.REGULAR_ONLY -> filtered.filter { !it.isProfessional }
        }

        _uiState.value = currentState.copy(filteredLabGroups = filtered)
    }

    fun createLabGroup(
        name: String,
        description: String,
        isProfessional: Boolean,
        serviceStorageId: Int? = null
    ) {
        if (!_uiState.value.isStaff) {
            _createLabGroupResult.value = Result.failure(Exception("Only staff users can create lab groups"))
            return
        }

        viewModelScope.launch {
            try {
                val result = labGroupRepository.createLabGroup(name, description, isProfessional)
                
                if (result.isSuccess && isProfessional && serviceStorageId != null) {
                    // Update with service storage if professional
                    val labGroup = result.getOrNull()
                    if (labGroup != null) {
                        val updateResult = labGroupRepository.updateLabGroup(
                            labGroup.id,
                            mapOf("service_storage" to serviceStorageId)
                        )
                        _createLabGroupResult.value = updateResult
                    } else {
                        _createLabGroupResult.value = result
                    }
                } else {
                    _createLabGroupResult.value = result
                }

                // Reload lab groups if successful
                if (result.isSuccess) {
                    loadLabGroups()
                }
            } catch (e: Exception) {
                _createLabGroupResult.value = Result.failure(e)
            }
        }
    }

    fun updateLabGroup(
        labGroupId: Int,
        name: String? = null,
        description: String? = null,
        isProfessional: Boolean? = null,
        serviceStorageId: Int? = null
    ) {
        if (!_uiState.value.isStaff) {
            _updateLabGroupResult.value = Result.failure(Exception("Only staff users can update lab groups"))
            return
        }

        viewModelScope.launch {
            try {
                val updates = mutableMapOf<String, Any>()
                name?.let { updates["name"] = it }
                description?.let { updates["description"] = it }
                isProfessional?.let { updates["is_professional"] = it }
                serviceStorageId?.let { updates["service_storage"] = it }

                val result = labGroupRepository.updateLabGroup(labGroupId, updates)
                _updateLabGroupResult.value = result

                // Reload lab groups if successful
                if (result.isSuccess) {
                    loadLabGroups()
                }
            } catch (e: Exception) {
                _updateLabGroupResult.value = Result.failure(e)
            }
        }
    }

    fun deleteLabGroup(labGroupId: Int) {
        if (!_uiState.value.isStaff) {
            _deleteLabGroupResult.value = Result.failure(Exception("Only staff users can delete lab groups"))
            return
        }

        viewModelScope.launch {
            try {
                val result = labGroupRepository.deleteLabGroup(labGroupId)
                _deleteLabGroupResult.value = result

                // Reload lab groups if successful
                if (result.isSuccess) {
                    loadLabGroups()
                }
            } catch (e: Exception) {
                _deleteLabGroupResult.value = Result.failure(e)
            }
        }
    }

    fun addUserToLabGroup(labGroupId: Int, userId: Int) {
        val currentUser = _uiState.value.currentUser
        val isStaff = _uiState.value.isStaff
        val isManager = currentUser?.managedLabGroups?.any { it.id == labGroupId } ?: false
        
        if (!isStaff && !isManager) {
            return
        }

        viewModelScope.launch {
            try {
                val result = labGroupRepository.addUser(labGroupId, userId)
                if (result.isSuccess) {
                    // Reload to get updated member counts
                    loadLabGroups()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to add user: ${e.message}"
                )
            }
        }
    }

    fun removeUserFromLabGroup(labGroupId: Int, userId: Int) {
        val currentUser = _uiState.value.currentUser
        val isStaff = _uiState.value.isStaff
        val isManager = currentUser?.managedLabGroups?.any { it.id == labGroupId } ?: false
        
        if (!isStaff && !isManager) {
            return
        }

        viewModelScope.launch {
            try {
                val result = labGroupRepository.removeUser(labGroupId, userId)
                if (result.isSuccess) {
                    _removeUserResult.value = Result.success(Unit)
                    // Reload to get updated member counts
                    loadLabGroups()
                } else {
                    _removeUserResult.value = Result.failure(Exception("Failed to remove user"))
                }
            } catch (e: Exception) {
                _removeUserResult.value = Result.failure(e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to remove user: ${e.message}"
                )
            }
        }
    }

    fun clearCreateResult() {
        _createLabGroupResult.value = null
    }

    fun clearUpdateResult() {
        _updateLabGroupResult.value = null
    }

    fun clearDeleteResult() {
        _deleteLabGroupResult.value = null
    }

    fun clearRemoveUserResult() {
        _removeUserResult.value = null
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun retry() {
        loadLabGroups()
    }
}