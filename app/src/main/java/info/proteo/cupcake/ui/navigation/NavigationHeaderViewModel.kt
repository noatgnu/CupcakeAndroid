package info.proteo.cupcake.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.proteo.cupcake.data.repository.UserRepository
import info.proteo.cupcake.shared.data.model.user.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NavigationHeaderUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isStaff: Boolean = false,
    val labGroupsCount: Int = 0
)

@HiltViewModel
class NavigationHeaderViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NavigationHeaderUiState())
    val uiState: StateFlow<NavigationHeaderUiState> = _uiState.asStateFlow()

    init {
        loadUserData()
    }

    fun loadUserData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Try to get user from active preference first (for quick display)
                val cachedUser = userRepository.getUserFromActivePreference()
                if (cachedUser != null) {
                    _uiState.value = _uiState.value.copy(
                        user = cachedUser,
                        labGroupsCount = cachedUser.labGroups.size,
                        isLoading = false
                    )
                }

                // Then fetch fresh data from API
                val userResult = userRepository.getCurrentUser()
                if (userResult.isSuccess) {
                    val user = userResult.getOrNull()
                    if (user != null) {
                        // Check if user is staff
                        val staffResult = userRepository.isStaff()
                        val isStaff = staffResult.getOrNull()?.isStaff ?: false

                        _uiState.value = _uiState.value.copy(
                            user = user,
                            isStaff = isStaff,
                            labGroupsCount = user.labGroups.size,
                            isLoading = false,
                            error = null
                        )
                    }
                } else {
                    // Only show error if we don't have cached data
                    if (cachedUser == null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to load profile"
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error loading profile"
                )
            }
        }
    }

    fun refresh() {
        loadUserData()
    }
}