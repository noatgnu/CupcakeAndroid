package info.proteo.cupcake.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.proteo.cupcake.data.repository.UserRepository
import info.proteo.cupcake.data.remote.service.ChangePasswordRequest
import info.proteo.cupcake.data.remote.service.ExportDataRequest
import info.proteo.cupcake.data.remote.service.UpdateProfileRequest
import info.proteo.cupcake.shared.data.model.user.LabGroup
import info.proteo.cupcake.shared.data.model.user.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserProfileUiState(
    val user: User? = null,
    val labGroups: List<LabGroup> = emptyList(),
    val managedLabGroups: List<LabGroup> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isStaff: Boolean = false
)

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    private val _profileUpdateResult = MutableStateFlow<Result<Unit>?>(null)
    val profileUpdateResult: StateFlow<Result<Unit>?> = _profileUpdateResult.asStateFlow()

    private val _passwordChangeResult = MutableStateFlow<Result<Unit>?>(null)
    val passwordChangeResult: StateFlow<Result<Unit>?> = _passwordChangeResult.asStateFlow()

    private val _exportDataResult = MutableStateFlow<Result<Unit>?>(null)
    val exportDataResult: StateFlow<Result<Unit>?> = _exportDataResult.asStateFlow()

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Load current user
                val userResult = userRepository.getCurrentUser()
                if (userResult.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load user profile: ${userResult.exceptionOrNull()?.message}"
                    )
                    return@launch
                }

                val user = userResult.getOrNull()
                if (user == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "User profile not found"
                    )
                    return@launch
                }

                // Check if user is staff
                val staffResult = userRepository.isStaff()
                val isStaff = staffResult.getOrNull()?.isStaff ?: false

                // Extract lab groups and managed lab groups
                val labGroups = user.labGroups
                val managedLabGroups = user.managedLabGroups

                _uiState.value = _uiState.value.copy(
                    user = user,
                    labGroups = labGroups,
                    managedLabGroups = managedLabGroups,
                    isStaff = isStaff,
                    isLoading = false,
                    error = null
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "An unexpected error occurred: ${e.message}"
                )
            }
        }
    }

    fun updateProfile(firstName: String?, lastName: String?, email: String?) {
        viewModelScope.launch {
            try {
                val request = UpdateProfileRequest(
                    firstName = firstName?.takeIf { it.isNotBlank() },
                    lastName = lastName?.takeIf { it.isNotBlank() },
                    email = email?.takeIf { it.isNotBlank() }
                )

                val result = userRepository.updateProfile(request)
                if (result.isSuccess) {
                    // Update local state with new user data
                    val updatedUser = result.getOrNull()
                    if (updatedUser != null) {
                        _uiState.value = _uiState.value.copy(user = updatedUser)
                    }
                    _profileUpdateResult.value = Result.success(Unit)
                } else {
                    _profileUpdateResult.value = Result.failure(
                        result.exceptionOrNull() ?: Exception("Profile update failed")
                    )
                }
            } catch (e: Exception) {
                _profileUpdateResult.value = Result.failure(e)
            }
        }
    }

    fun changePassword(oldPassword: String, newPassword: String) {
        viewModelScope.launch {
            try {
                val request = ChangePasswordRequest(
                    oldPassword = oldPassword,
                    password = newPassword
                )

                val result = userRepository.changePassword(request)
                _passwordChangeResult.value = result
            } catch (e: Exception) {
                _passwordChangeResult.value = Result.failure(e)
            }
        }
    }

    fun exportData(
        protocolIds: List<Int>? = null, 
        sessionIds: List<Int>? = null, 
        format: String = "zip"
    ) {
        viewModelScope.launch {
            try {
                val request = ExportDataRequest(
                    protocolIds = protocolIds,
                    sessionIds = sessionIds,
                    format = format
                )

                val result = userRepository.exportData(request)
                _exportDataResult.value = result
            } catch (e: Exception) {
                _exportDataResult.value = Result.failure(e)
            }
        }
    }

    fun clearProfileUpdateResult() {
        _profileUpdateResult.value = null
    }

    fun clearPasswordChangeResult() {
        _passwordChangeResult.value = null
    }

    fun clearExportDataResult() {
        _exportDataResult.value = null
    }

    fun retry() {
        loadUserProfile()
    }
}