package info.proteo.cupcake.ui.labgroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.proteo.cupcake.data.repository.LabGroupRepository
import info.proteo.cupcake.shared.data.model.user.LabGroup
import info.proteo.cupcake.shared.data.model.user.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LabGroupDetailUiState(
    val labGroup: LabGroup? = null,
    val allMembers: List<User> = emptyList(),
    val managers: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val isMembersLoading: Boolean = false,
    val error: String? = null,
    val membersError: String? = null
)

@HiltViewModel
class LabGroupDetailViewModel @Inject constructor(
    private val labGroupRepository: LabGroupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LabGroupDetailUiState())
    val uiState: StateFlow<LabGroupDetailUiState> = _uiState.asStateFlow()

    fun loadLabGroupDetail(labGroupId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val labGroupResult = labGroupRepository.getLabGroupById(labGroupId)
                
                if (labGroupResult.isSuccess) {
                    val labGroup = labGroupResult.getOrNull()
                    _uiState.value = _uiState.value.copy(
                        labGroup = labGroup,
                        isLoading = false
                    )
                    
                    // Load members after lab group is loaded
                    loadMembers(labGroupId)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load lab group details"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    private fun loadMembers(labGroupId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isMembersLoading = true, membersError = null)
            
            try {
                android.util.Log.d("LabGroupDetail", "Loading members for lab group: $labGroupId")
                
                // Load all members and managers concurrently
                val allMembersResult = labGroupRepository.getUsers(labGroupId)
                val managersResult = labGroupRepository.getManagers(labGroupId)
                
                android.util.Log.d("LabGroupDetail", "Members result: ${allMembersResult.isSuccess}, count: ${allMembersResult.getOrNull()?.size}")
                android.util.Log.d("LabGroupDetail", "Managers result: ${managersResult.isSuccess}, count: ${managersResult.getOrNull()?.size}")
                
                val allMembers = allMembersResult.getOrNull() ?: emptyList()
                val managers = managersResult.getOrNull() ?: emptyList()
                
                android.util.Log.d("LabGroupDetail", "Manager IDs: ${managers.map { it.id }}")
                android.util.Log.d("LabGroupDetail", "All member IDs: ${allMembers.map { it.id }}")
                
                // Check if API calls failed
                val membersError = if (allMembersResult.isFailure && managersResult.isFailure) {
                    "Failed to load members: ${allMembersResult.exceptionOrNull()?.message}"
                } else null
                
                val newState = _uiState.value.copy(
                    allMembers = allMembers,
                    managers = managers,
                    isMembersLoading = false,
                    membersError = membersError
                )
                
                android.util.Log.d("LabGroupDetail", "About to update UI State with ${allMembers.size} members and ${managers.size} managers")
                _uiState.value = newState
                android.util.Log.d("LabGroupDetail", "UI State update completed")
                
            } catch (e: Exception) {
                android.util.Log.e("LabGroupDetail", "Exception loading members", e)
                _uiState.value = _uiState.value.copy(
                    isMembersLoading = false,
                    membersError = e.message ?: "Failed to load members"
                )
            }
        }
    }

    fun refresh(labGroupId: Int) {
        android.util.Log.d("LabGroupDetailViewModel", "Refreshing lab group detail for ID: $labGroupId")
        loadLabGroupDetail(labGroupId)
    }
    
    fun refreshMembersOnly(labGroupId: Int) {
        android.util.Log.d("LabGroupDetailViewModel", "Refreshing members only for lab group ID: $labGroupId")
        viewModelScope.launch {
            kotlinx.coroutines.delay(200)
            loadMembers(labGroupId)
        }
    }
    
    fun updateLabGroupFromManagerOperation(updatedLabGroup: LabGroup) {
        android.util.Log.d("LabGroupDetailViewModel", "Updating lab group from manager operation")
        android.util.Log.d("LabGroupDetailViewModel", "Updated lab group managers: ${updatedLabGroup.managers?.map { it.id }}")
        android.util.Log.d("LabGroupDetailViewModel", "Current UI state before update: members=${_uiState.value.allMembers.size}, managers=${_uiState.value.managers.size}")
        
        // First update the lab group immediately
        _uiState.value = _uiState.value.copy(
            labGroup = updatedLabGroup
        )
        android.util.Log.d("LabGroupDetailViewModel", "Lab group updated in state")
        
        // Then refresh members to get full User objects
        viewModelScope.launch {
            android.util.Log.d("LabGroupDetailViewModel", "Starting loadMembers for immediate refresh")
            loadMembers(updatedLabGroup.id)
        }
    }
}