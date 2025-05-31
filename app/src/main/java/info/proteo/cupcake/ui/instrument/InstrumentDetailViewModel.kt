package info.proteo.cupcake.ui.instrument

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.proteo.cupcake.data.remote.model.LimitOffsetResponse
import info.proteo.cupcake.data.remote.model.annotation.Annotation
import info.proteo.cupcake.data.remote.model.instrument.Instrument
import info.proteo.cupcake.data.remote.model.user.UserBasic
import info.proteo.cupcake.data.remote.service.InstrumentPermission
import info.proteo.cupcake.data.remote.service.SignedTokenResponse
import info.proteo.cupcake.data.repository.AnnotationRepository
import info.proteo.cupcake.data.repository.InstrumentRepository
import info.proteo.cupcake.data.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InstrumentDetailViewModel @Inject constructor(
    private val instrumentRepository: InstrumentRepository,
    private val annotationRepository: AnnotationRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _instrument = MutableLiveData<Result<Instrument>>()
    val instrument: LiveData<Result<Instrument>> = _instrument

    private val _isLoadingInstrument = MutableLiveData<Boolean>()
    val isLoadingInstrument: LiveData<Boolean> = _isLoadingInstrument

    private val _folderAnnotations = MutableLiveData<Result<LimitOffsetResponse<Annotation>>>()
    val folderAnnotations: LiveData<Result<LimitOffsetResponse<Annotation>>> = _folderAnnotations

    private val _isLoadingFolderAnnotations = MutableLiveData<Boolean>()
    val isLoadingFolderAnnotations: LiveData<Boolean> = _isLoadingFolderAnnotations

    private val _canManageInstrument = MutableLiveData<Boolean>()
    val canManageInstrument: LiveData<Boolean> = _canManageInstrument

    private val _updateResult = MutableLiveData<Result<Instrument>>()
    val updateResult: LiveData<Result<Instrument>> = _updateResult

    private val _isUpdatingInstrument = MutableLiveData<Boolean>()
    val isUpdatingInstrument: LiveData<Boolean> = _isUpdatingInstrument

    private val _imageUpdateResult = MutableLiveData<Result<Instrument>>()
    val imageUpdateResult: LiveData<Result<Instrument>> = _imageUpdateResult

    private val _deletionResult = MutableLiveData<Result<Unit>>()
    val deletionResult: LiveData<Result<Unit>> = _deletionResult

    private val _isDeleting = MutableLiveData<Boolean>()
    val isDeleting: LiveData<Boolean> = _isDeleting

    fun deleteInstrument(instrumentId: Int) {
        viewModelScope.launch {
            _isDeleting.value = true
            val result = instrumentRepository.deleteInstrument(instrumentId)
            _deletionResult.value = result
            _isDeleting.value = false
        }
    }

    fun loadInstrumentDetailsAndPermissions(instrumentId: Int) {
        viewModelScope.launch {
            _isLoadingInstrument.value = true
            _canManageInstrument.value = false

            var isStaffUser = false
            try {
                val currentUser = userRepository.getCurrentUser().getOrNull()
                if (currentUser != null) {
                    isStaffUser = currentUser.isStaff
                    _canManageInstrument.value = isStaffUser
                } else {
                    Log.w("InstrumentDetailVM", "Current user is null, assuming not staff for permissions")
                }
            } catch (e: Exception) {
                Log.e("InstrumentDetailVM", "Failed to get current user, assuming not staff for permissions", e)
            }

            instrumentRepository.getInstrument(instrumentId)
                .catch { e_instrument ->
                    Log.e("InstrumentDetailVM", "Error in getInstrument flow", e_instrument)
                    _instrument.value = Result.failure(e_instrument)
                    _isLoadingInstrument.value = false
                }
                .collect { instrumentResult ->
                    _instrument.value = instrumentResult

                    if (instrumentResult.isSuccess) {
                        if (!isStaffUser) {
                            instrumentRepository.getInstrumentPermission(instrumentId)
                                .catch { e_perm_flow ->
                                    Log.e("InstrumentDetailVM", "Error in getInstrumentPermission flow", e_perm_flow)
                                    _canManageInstrument.value = false
                                    _isLoadingInstrument.value = false
                                }
                                .collect { permissionResult ->
                                    permissionResult.fold(
                                        onSuccess = { permission ->
                                            _canManageInstrument.value = permission.canManage
                                        },
                                        onFailure = { permError ->
                                            Log.e("InstrumentDetailVM", "Failed to get instrument permission", permError)
                                            _canManageInstrument.value = false
                                        }
                                    )
                                    _isLoadingInstrument.value = false
                                }
                        } else {
                            _isLoadingInstrument.value = false
                        }
                    } else {
                        Log.e("InstrumentDetailVM", "Failed to load instrument", instrumentResult.exceptionOrNull())
                        _isLoadingInstrument.value = false
                    }
                }
        }
    }

    fun loadAnnotationsForFolder(
        folderId: Int,
        search: String? = null,
        limit: Int = 20,
        offset: Int = 0
    ) {
        viewModelScope.launch {
            annotationRepository.getAnnotationsInFolder(folderId, search, limit, offset)
                .onStart {
                    _isLoadingFolderAnnotations.value = true
                }
                .catch { e ->
                    _folderAnnotations.value = Result.failure(e)
                    _isLoadingFolderAnnotations.value = false
                }
                .collect { result ->
                    _folderAnnotations.value = result
                    _isLoadingFolderAnnotations.value = false
                }
        }
    }

    suspend fun getAnnotationDownloadToken(annotationId: Int): Flow<Result<SignedTokenResponse>> {
        return flow {
            emit(annotationRepository.getSignedUrl(annotationId))
        }
    }

    fun updateInstrumentDetails(instrumentToUpdate: Instrument) {
        viewModelScope.launch {
            _isUpdatingInstrument.value = true
            val result = instrumentRepository.updateInstrument(instrumentToUpdate.id, instrumentToUpdate)
            _updateResult.value = result
            if (result.isSuccess) {
                loadInstrumentDetailsAndPermissions(instrumentToUpdate.id)
            } else {
                Log.e("InstrumentDetailVM", "Failed to update instrument", result.exceptionOrNull())
            }
            _isUpdatingInstrument.value = false
        }
    }

    suspend fun searchUsers(query: String): Result<LimitOffsetResponse<UserBasic>> {
        return userRepository.searchUsers(query)
    }

    fun getInstrumentPermissionFor(instrumentId: Int, username: String): Flow<Result<InstrumentPermission>> {
        return instrumentRepository.getInstrumentPermissionFor(instrumentId, username)
    }

    suspend fun assignInstrumentPermission(
        instrumentId: Int,
        username: String,
        canManage: Boolean,
        canBook: Boolean,
        canView: Boolean
    ): Result<Unit> {
        return instrumentRepository.assignInstrumentPermission(
            instrumentId, username, canManage, canBook, canView
        )
    }



}