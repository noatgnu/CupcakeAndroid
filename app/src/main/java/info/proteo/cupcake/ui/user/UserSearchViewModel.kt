package info.proteo.cupcake.ui.user

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

data class UserSearchUiState(
    val users: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = ""
)

@HiltViewModel
class UserSearchViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserSearchUiState())
    val uiState: StateFlow<UserSearchUiState> = _uiState.asStateFlow()

    fun searchUsers(query: String) {
        // Prevent duplicate searches for the same query
        if (_uiState.value.searchQuery == query && !_uiState.value.isLoading) {
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                searchQuery = query
            )

            try {
                // For now, just get the current user - you can implement proper user search later
                val currentUserResult = userRepository.getCurrentUser()
                val users = if (currentUserResult.isSuccess) {
                    listOfNotNull(currentUserResult.getOrNull())
                } else {
                    emptyList()
                }

                // Only update if the query is still the same (prevent race conditions)
                if (_uiState.value.searchQuery == query) {
                    _uiState.value = _uiState.value.copy(
                        users = users,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                // Only update if the query is still the same
                if (_uiState.value.searchQuery == query) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to search users"
                    )
                }
            }
        }
    }
}