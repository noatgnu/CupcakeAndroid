package info.proteo.cupcake.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.proteo.cupcake.SessionManager
import info.proteo.cupcake.data.local.dao.user.UserPreferencesDao
import info.proteo.cupcake.data.local.entity.user.UserPreferencesEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileManagementViewModel @Inject constructor(
    private val userPreferencesDao: UserPreferencesDao,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _userProfiles = MutableLiveData<List<UserPreferencesEntity>>()
    val userProfiles: LiveData<List<UserPreferencesEntity>> = _userProfiles

    init {
        loadUserProfiles()
    }

    private fun loadUserProfiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val profiles = userPreferencesDao.getAllUserPreferences()
            _userProfiles.postValue(profiles)
        }
    }

    fun setActiveProfile(userId: String, hostname: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferencesDao.switchActivePreference(userId, hostname)
            val activeProfile = userPreferencesDao.getCurrentlyActivePreference()
            if (activeProfile != null) {
                sessionManager.saveBaseUrl(hostname)
                if (activeProfile.authToken != null) {
                    sessionManager.saveToken(activeProfile.authToken)
                }
                sessionManager.saveBaseUrl(activeProfile.hostname)
            }
            loadUserProfiles()
        }
    }

}