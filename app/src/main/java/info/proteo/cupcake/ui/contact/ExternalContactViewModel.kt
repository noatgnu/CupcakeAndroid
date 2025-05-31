package info.proteo.cupcake.ui.contact

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.proteo.cupcake.data.remote.model.instrument.ExternalContact
import info.proteo.cupcake.data.remote.model.instrument.SupportInformation
import info.proteo.cupcake.data.repository.SupportInformationRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExternalContactViewModel @Inject constructor(
    private val supportInformationRepository: SupportInformationRepository
) : ViewModel() {

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _supportInfo = MutableLiveData<Result<SupportInformation>>()
    val supportInfo: LiveData<Result<SupportInformation>> = _supportInfo

    private val _contactOperationStatus = MutableLiveData<Result<String>?>()
    val contactOperationStatus: LiveData<Result<String>?> = _contactOperationStatus

    private val TAG = "ExternalContactVM"

    fun loadSupportInformation(supportInfoId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            Log.d(TAG, "Loading support info for ID: $supportInfoId")
            try {
                val result = supportInformationRepository.getSupportInformationById(supportInfoId).first()
                _supportInfo.value = result
                Log.d(TAG, "Support info loaded: ${result.isSuccess}")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading support info", e)
                _supportInfo.value = Result.failure(e)
            }
            _isLoading.value = false
        }
    }

    fun removeContactLink(supportInfoId: Int, contactId: Int, contactRole: String) {
        Log.d(TAG, "Removing contact link: supportInfoId=$supportInfoId, contactId=$contactId, role=$contactRole")
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d(TAG, "Starting contact removal for ID: $contactId with role: $contactRole")

                val deleteResult = supportInformationRepository.removeContact(supportInfoId, contactId, contactRole.lowercase())

                Log.d(TAG, "Delete result: $deleteResult")
                deleteResult.fold(
                    onSuccess = {
                        Log.d(TAG, "Contact removed successfully: $contactId")
                        _isLoading.value = false
                        refreshSupportInformation(supportInfoId)
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to remove contact: ${error.message}", error)
                        _isLoading.value = false
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception during contact removal", e)
                _isLoading.value = false
            }
        }
    }

    private fun refreshSupportInformation(supportInfoId: Int) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Refreshing support info after contact change")
                val result = supportInformationRepository.getSupportInformationById(supportInfoId).first()
                _supportInfo.value = result
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing support info", e)
            }
        }
    }
    fun clearContactOperationStatus() {
        _contactOperationStatus.value = null
    }
}