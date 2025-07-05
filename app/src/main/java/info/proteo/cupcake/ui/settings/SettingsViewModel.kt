package info.proteo.cupcake.ui.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import info.proteo.cupcake.shared.data.model.user.User
import info.proteo.cupcake.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _userData = MutableLiveData<User>()
    val userData: LiveData<User> = _userData

    private val _authToken = MutableLiveData<String>()
    val authToken: LiveData<String> = _authToken

    private val _darkMode = MutableLiveData<Int>()
    val darkMode: LiveData<Int> = _darkMode

    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    companion object {
        const val DARK_MODE_FOLLOW_SYSTEM = 0
        const val DARK_MODE_LIGHT = 1
        const val DARK_MODE_DARK = 2
        private const val PREF_DARK_MODE = "dark_mode"
    }

    init {
        loadUserData()
        loadDarkModePreference()
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

    private fun loadDarkModePreference() {
        val savedMode = sharedPreferences.getInt(PREF_DARK_MODE, DARK_MODE_FOLLOW_SYSTEM)
        _darkMode.value = savedMode
    }

    fun setDarkMode(mode: Int) {
        _darkMode.value = mode
        sharedPreferences.edit().putInt(PREF_DARK_MODE, mode).apply()
        
        val nightMode = when (mode) {
            DARK_MODE_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            DARK_MODE_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }
}