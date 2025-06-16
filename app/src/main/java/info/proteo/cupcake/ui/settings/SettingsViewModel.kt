package info.proteo.cupcake.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.proteo.cupcake.shared.data.model.user.User
import info.proteo.cupcake.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _userData = MutableLiveData<User>()
    val userData: LiveData<User> = _userData

    private val _authToken = MutableLiveData<String>()
    val authToken: LiveData<String> = _authToken

    init {
        loadUserData()
    }

    fun loadUserData() {
        viewModelScope.launch {
            userRepository.getUserFromActivePreference()?.let { user ->
                _userData.postValue(user)
            }

            userRepository.getActiveUserPreference()?.let { preference ->
                preference.authToken?.let { token ->
                    _authToken.postValue(token)
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            val activePreference = userRepository.getActiveUserPreference()
            if (activePreference != null) {
                userRepository.deleteAuthToken(activePreference.userId, activePreference.hostname)
            }
        }
    }
}