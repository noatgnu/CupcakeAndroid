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
                // Load all members and managers concurrently
                val allMembersResult = labGroupRepository.getUsers(labGroupId)
                val managersResult = labGroupRepository.getManagers(labGroupId)
                
                android.util.Log.d("LabGroupDetail", "Members result: ${allMembersResult.isSuccess}, count: ${allMembersResult.getOrNull()?.size}")
                android.util.Log.d("LabGroupDetail", "Managers result: ${managersResult.isSuccess}, count: ${managersResult.getOrNull()?.size}")
                
                val allMembers = allMembersResult.getOrNull() ?: emptyList()
                val managers = managersResult.getOrNull() ?: emptyList()
                
                // Check if API calls failed
                val membersError = if (allMembersResult.isFailure && managersResult.isFailure) {
                    "Failed to load members: ${allMembersResult.exceptionOrNull()?.message}"
                } else null
                
                _uiState.value = _uiState.value.copy(
                    allMembers = allMembers,
                    managers = managers,
                    isMembersLoading = false,
                    membersError = membersError
                )
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
        loadLabGroupDetail(labGroupId)
    }
}