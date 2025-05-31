package info.proteo.cupcake.ui.instrument

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.proteo.cupcake.data.remote.model.instrument.SupportInformation
import info.proteo.cupcake.data.repository.InstrumentRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SupportInformationViewModel @Inject constructor(
    private val instrumentRepository: InstrumentRepository
) : ViewModel() {

    private val _supportInformation = MutableLiveData<Result<List<SupportInformation>>>()
    val supportInformation: LiveData<Result<List<SupportInformation>>> = _supportInformation

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadSupportInformation(instrumentId: Int) {
        _isLoading.value = true
        instrumentRepository.listSupportInformation(instrumentId)
            .onEach { result ->
                _supportInformation.value = result
                _isLoading.value = false
            }
            .catch { e ->
                _supportInformation.value = Result.failure(e)
                _isLoading.value = false
            }
            .launchIn(viewModelScope)
    }

    fun removeSupportInformation(instrumentId: Int, supportInfoId: Int) {
        viewModelScope.launch {
            instrumentRepository.removeSupportInformation(instrumentId, supportInfoId)
                .onSuccess {
                    loadSupportInformation(instrumentId)
                }
        }
    }
}