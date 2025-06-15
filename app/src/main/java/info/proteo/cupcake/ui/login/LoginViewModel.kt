package info.proteo.cupcake.ui.login

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.proteo.cupcake.data.local.dao.user.UserPreferencesDao
import info.proteo.cupcake.data.local.entity.user.UserPreferencesEntity
import info.proteo.cupcake.data.remote.model.user.User
import info.proteo.cupcake.data.repository.AuthRepository
import info.proteo.cupcake.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val userPreferencesDao: UserPreferencesDao
) : ViewModel() {

    private val _loginState = MutableLiveData<LoginState>()
    val loginState: LiveData<LoginState> = _loginState

    fun login(username: String, password: String, hostname: String) {
        _loginState.value = LoginState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = authRepository.login(username, password, hostname)

                if (result.isSuccess) {
                    Log.d("LoginViewModel", "Login successful: ${result.getOrNull()}")
                    val authToken = result.getOrNull()?.token

                    if (authToken != null) {
                        // Save user preferences
                        val userPrefs = UserPreferencesEntity(
                            userId = username,
                            hostname = hostname,
                            authToken = authToken,
                            lastLoginTimestamp = System.currentTimeMillis(),
                            rememberCredentials = true,
                            sessionToken = "",
                            theme = "system",
                            notificationsEnabled = true,
                            syncFrequency = 15,
                            syncOnWifiOnly = true,
                            lastSyncTimestamp = System.currentTimeMillis(),
                            isActive = true,
                            allowOverlapBookings = false,
                            useCoturn = false,
                            useLlm = false,
                            useOcr = false,
                            useWhisper = false,
                            defaultServiceLabGroup = "MS Facilty",
                            canSendEmail = false,
                        )
                        Log.d("LoginViewModel", "UserPrefs to save: $userPrefs")

                        userPreferencesDao.insertOrUpdate(userPrefs)
                        userPreferencesDao.switchActivePreference(username, hostname)
                        val activePrefs = userPreferencesDao.getCurrentlyActivePreference()
                        Log.d("LoginViewModel", "Active UserPrefs after save: $activePrefs")

                        _loginState.postValue(LoginState.VerifyingToken)
                        verifyToken()
                    } else {
                        _loginState.postValue(LoginState.Error("Invalid login response"))
                    }
                } else {
                    _loginState.postValue(LoginState.Error(result.exceptionOrNull()?.message ?: "Login failed"))
                }
            } catch (e: Exception) {
                _loginState.postValue(LoginState.Error(e.message ?: "Unknown error"))
            }
        }
    }

    private fun verifyToken() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Add a timeout to prevent hanging
                withTimeout(10000) { // 10 seconds timeout
                    val userResult = userRepository.getCurrentUser()
                    if (userResult.isSuccess) {
                        _loginState.postValue(LoginState.Success(userResult.getOrNull()!!))
                        val activePref = userPreferencesDao.getCurrentlyActivePreference()
                        val serverSettings = userRepository.getServerSettings()
                        activePref?.let { prefs ->
                            val pref = prefs.copy(
                                isActive = true,
                                allowOverlapBookings = serverSettings.allowOverlapBookings,
                                useCoturn = serverSettings.useCoturn,
                                useLlm = serverSettings.useLlm,
                                useOcr = serverSettings.useOcr,
                                useWhisper = serverSettings.useWhisper,
                                defaultServiceLabGroup = serverSettings.defaultServiceLabGroup,
                                canSendEmail = serverSettings.canSendEmail,
                            )
                            userPreferencesDao.insertOrUpdate(pref)
                        }
                    } else {
                        val error = userResult.exceptionOrNull()
                        Log.e("LoginViewModel", "Token verification failed", error)
                        _loginState.postValue(LoginState.Error("Token verification failed: ${error?.message ?: "Unknown error"}"))
                    }
                }
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Exception during verification", e)
                _loginState.postValue(LoginState.Error("Verification failed: ${e.message ?: "Unknown error"}"))
            }
        }
    }

    suspend fun checkExistingLogin() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Get all hostnames
                val hostnames = userPreferencesDao.getAllHostnames()

                if (hostnames.isNotEmpty()) {
                    // Get first hostname with valid login
                    for (hostname in hostnames) {
                        val preference = userPreferencesDao.getCurrentlyActivePreferences(hostname)
                        if (preference != null && preference.isActive) {
                            val serverSettings = userRepository.getServerSettings()
                            val pref = preference.copy(
                                isActive = true,
                                allowOverlapBookings = serverSettings.allowOverlapBookings,
                                useCoturn = serverSettings.useCoturn,
                                useLlm = serverSettings.useLlm,
                                useOcr = serverSettings.useOcr,
                                useWhisper = serverSettings.useWhisper,
                                defaultServiceLabGroup = serverSettings.defaultServiceLabGroup,
                                canSendEmail = serverSettings.canSendEmail,
                            )
                            userPreferencesDao.insertOrUpdate(pref)
                            _loginState.postValue(LoginState.ExistingLoginFound)
                            return@launch
                        }
                    }
                }

                _loginState.postValue(LoginState.NeedLogin)
            } catch (e: Exception) {
                _loginState.postValue(LoginState.Error(e.message ?: "Error checking login status"))
            }
        }
    }

    sealed class LoginState {
        object Loading : LoginState()
        object VerifyingToken : LoginState()
        data class Success(val user: User) : LoginState()
        object NeedLogin : LoginState()
        object ExistingLoginFound : LoginState()
        data class Error(val message: String) : LoginState()
    }
}