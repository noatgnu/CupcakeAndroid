package info.proteo.cupcake.ui.instrument

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.proteo.cupcake.data.local.dao.user.UserPreferencesDao
import info.proteo.cupcake.data.local.entity.user.UserPreferencesEntity
import info.proteo.cupcake.shared.data.model.instrument.CreateInstrumentUsageRequest
import info.proteo.cupcake.shared.data.model.LimitOffsetResponse
import info.proteo.cupcake.shared.data.model.annotation.Annotation
import info.proteo.cupcake.shared.data.model.instrument.Instrument
import info.proteo.cupcake.shared.data.model.instrument.InstrumentUsage
import info.proteo.cupcake.shared.data.model.user.UserBasic
import info.proteo.cupcake.data.remote.service.InstrumentPermission
import info.proteo.cupcake.data.remote.service.SignedTokenResponse
import info.proteo.cupcake.data.repository.AnnotationRepository
import info.proteo.cupcake.data.repository.InstrumentRepository
import info.proteo.cupcake.data.repository.InstrumentUsageRepository
import info.proteo.cupcake.data.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class InstrumentDetailViewModel @Inject constructor(
    private val instrumentRepository: InstrumentRepository,
    private val annotationRepository: AnnotationRepository,
    private val userRepository: UserRepository,
    private val instrumentUsageRepository: InstrumentUsageRepository,
    private val userPreferencesDao: UserPreferencesDao
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

    private val _canBookInstrument = MutableLiveData<Boolean>()
    val canBookInstrument: LiveData<Boolean> = _canBookInstrument

    private val _canViewInstrument = MutableLiveData<Boolean>()
    val canViewInstrument: LiveData<Boolean> = _canViewInstrument

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

    private val _startDate = MutableLiveData<Calendar>()
    val startDate: LiveData<Calendar> = _startDate

    private val _endDate = MutableLiveData<Calendar>()
    val endDate: LiveData<Calendar> = _endDate

    private val _bookings = MutableLiveData<Result<LimitOffsetResponse<InstrumentUsage>>>()
    val bookings: LiveData<Result<LimitOffsetResponse<InstrumentUsage>>> = _bookings

    private val _isLoadingBookings = MutableLiveData<Boolean>()
    val isLoadingBookings: LiveData<Boolean> = _isLoadingBookings

    private val _userPreferences = MutableLiveData<UserPreferencesEntity>()
    val userPreferences: LiveData<UserPreferencesEntity> = _userPreferences

    init {
        // Load user preferences when ViewModel is initialized
        viewModelScope.launch {
            try {
                val preferences = userPreferencesDao.getCurrentlyActivePreference()
                if (preferences != null) {
                    _userPreferences.value = preferences
                } else {
                    Log.w("InstrumentDetailVM", "User preferences are null")
                }
            } catch (e: Exception) {
                Log.e("InstrumentDetailVM", "Failed to load user preferences", e)
            }
        }
    }

    fun initializeBookingDates() {
        val now = Calendar.getInstance()
        _startDate.value = now

        val twoWeeksLater = Calendar.getInstance()
        twoWeeksLater.add(Calendar.DAY_OF_MONTH, 14)
        _endDate.value = twoWeeksLater

        loadBookings()
    }

    fun loadBookings() {
        val instrumentId = _instrument.value?.getOrNull()?.id ?: return
        var start = startDate.value
        var end = endDate.value


        if (start == null || end == null) return

        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val startString = formatter.format(start.time)
        val endString = formatter.format(end.time)
        Log.d("InstrumentDetailVM", "Loading bookings for instrument $instrumentId from $startString to $endString")

        viewModelScope.launch {
            _isLoadingBookings.value = true
            instrumentUsageRepository.getInstrumentUsages(
                instrument = instrumentId.toString(),
                timeStarted = startString,
                timeEnded = endString,
                limit = 100,
                searchType = "usage"
            ).collect { result ->
                _bookings.value = result
                _isLoadingBookings.value = false
            }
        }
    }

    fun navigateToPreviousPeriod() {
        val start = startDate.value ?: return
        val end = endDate.value ?: return

        val periodLength = ((end.timeInMillis - start.timeInMillis) / 86400000).toInt() // days

        start.add(Calendar.DAY_OF_MONTH, -periodLength)
        end.add(Calendar.DAY_OF_MONTH, -periodLength)

        _startDate.value = start
        _endDate.value = end

        loadBookings()
    }

    fun navigateToNextPeriod() {
        val start = startDate.value ?: return
        val end = endDate.value ?: return

        val periodLength = ((end.timeInMillis - start.timeInMillis) / 86400000).toInt() // days

        start.add(Calendar.DAY_OF_MONTH, periodLength)
        end.add(Calendar.DAY_OF_MONTH, periodLength)

        _startDate.value = start
        _endDate.value = end

        loadBookings()
    }


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
            _canBookInstrument.value = false

            var isStaffUser = false
            try {
                val currentUser = userRepository.getCurrentUser().getOrNull()
                if (currentUser != null) {
                    isStaffUser = currentUser.isStaff
                    _canManageInstrument.value = isStaffUser
                    _canBookInstrument.value = isStaffUser
                    _canViewInstrument.value = isStaffUser
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
                        initializeBookingDates()
                        if (!isStaffUser) {
                            instrumentRepository.getInstrumentPermission(instrumentId)
                                .catch { e_perm_flow ->
                                    Log.e("InstrumentDetailVM", "Error in getInstrumentPermission flow", e_perm_flow)
                                    _canManageInstrument.value = false
                                    _canBookInstrument.value = false
                                    _canViewInstrument.value = false
                                    _isLoadingInstrument.value = false
                                }
                                .collect { permissionResult ->
                                    permissionResult.fold(
                                        onSuccess = { permission ->
                                            _canManageInstrument.value = permission.canManage
                                            _canBookInstrument.value = permission.canBook
                                            _canViewInstrument.value = permission.canView
                                        },
                                        onFailure = { permError ->
                                            Log.e("InstrumentDetailVM", "Failed to get instrument permission", permError)
                                            _canManageInstrument.value = false
                                            _canBookInstrument.value = false
                                            _canViewInstrument.value = false
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

    suspend fun createInstrumentUsage(usageRequest: CreateInstrumentUsageRequest): Result<InstrumentUsage> {
        return instrumentUsageRepository.createInstrumentUsage(usageRequest)
    }


    suspend fun toggleBookingApproval(usageId: Int): Result<InstrumentUsage> {
        return instrumentUsageRepository.approveUsageToggle(usageId)
    }
}