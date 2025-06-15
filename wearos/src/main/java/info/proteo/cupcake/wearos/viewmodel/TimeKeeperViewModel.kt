package info.proteo.cupcake.wearos.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.proteo.cupcake.shared.model.TimeKeeperData
import info.proteo.cupcake.wearos.repository.TimeKeeperRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimeKeeperViewModel @Inject constructor(
    private val repository: TimeKeeperRepository
) : ViewModel() {

    private val _timeKeeper = MutableLiveData<TimeKeeperData?>()
    val timeKeeper: LiveData<TimeKeeperData?> = _timeKeeper

    init {
        viewModelScope.launch {
            repository.observeTimeKeeper().collect { timeKeeperData ->
                _timeKeeper.postValue(timeKeeperData)
            }
        }
    }

    /**
     * Send an action to control the TimeKeeper on the phone
     */
    fun sendAction(action: String) {
        viewModelScope.launch {
            repository.sendAction(action)
        }
    }
}
