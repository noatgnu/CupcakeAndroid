package info.proteo.cupcake.ui.maintenance

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.proteo.cupcake.data.repository.MaintenanceLogRepository
import info.proteo.cupcake.shared.data.model.LimitOffsetResponse
import info.proteo.cupcake.shared.data.model.maintenance.MaintenanceLog
import info.proteo.cupcake.shared.data.model.maintenance.MaintenanceLogRequest
import info.proteo.cupcake.shared.data.model.maintenance.MaintenanceStatus
import info.proteo.cupcake.shared.data.model.maintenance.MaintenanceStatusesResponse
import info.proteo.cupcake.shared.data.model.maintenance.MaintenanceType
import info.proteo.cupcake.shared.data.model.maintenance.MaintenanceTypesResponse
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class MaintenanceLogViewModel @Inject constructor(
    private val repository: MaintenanceLogRepository
) : ViewModel() {

    private val _maintenanceLogs = MutableLiveData<Result<LimitOffsetResponse<MaintenanceLog>>>()
    val maintenanceLogs: LiveData<Result<LimitOffsetResponse<MaintenanceLog>>> = _maintenanceLogs

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _maintenanceTypes = MutableLiveData<Result<List<MaintenanceType>>>()
    val maintenanceTypes: LiveData<Result<List<MaintenanceType>>> = _maintenanceTypes

    private val _maintenanceStatuses = MutableLiveData<Result<List<MaintenanceStatus>>>()
    val maintenanceStatuses: LiveData<Result<List<MaintenanceStatus>>> = _maintenanceStatuses

    private val _createResult = MutableLiveData<Result<MaintenanceLog>>()
    val createResult: LiveData<Result<MaintenanceLog>> = _createResult

    private val _updateResult = MutableLiveData<Result<MaintenanceLog>>()
    val updateResult: LiveData<Result<MaintenanceLog>> = _updateResult

    private val _deleteResult = MutableLiveData<Result<Unit>>()
    val deleteResult: LiveData<Result<Unit>> = _deleteResult

    private var instrumentId: Long? = null
    private var currentOffset = 0
    private val pageSize = 20
    private var _hasMoreData = true
    val hasMoreData: Boolean get() = _hasMoreData

    // Filter states
    private var currentMaintenanceType: String? = null
    private var currentStatus: String? = null
    private var currentSearchQuery: String? = null
    private var currentDateFrom: String? = null
    private var currentDateTo: String? = null
    private var showTemplatesOnly = false
    private var includeCompleted = true

    fun setInstrumentId(instrumentId: Long) {
        this.instrumentId = instrumentId
        loadMaintenanceLogs(refresh = true)
        loadMaintenanceTypes()
        loadMaintenanceStatuses()
    }

    fun loadMaintenanceLogs(refresh: Boolean = false) {
        if (refresh) {
            currentOffset = 0
            _hasMoreData = true
        }

        _isLoading.value = true

        repository.getMaintenanceLogsForInstrument(
            instrumentId = instrumentId ?: return,
            limit = pageSize,
            offset = currentOffset
        ).onEach { result ->
            _isLoading.value = false
            result.onSuccess { response ->
                if (!refresh && _maintenanceLogs.value?.isSuccess == true) {
                    // Append to existing list
                    val currentList = _maintenanceLogs.value?.getOrNull()?.results ?: emptyList()
                    val combinedList = currentList + response.results
                    _maintenanceLogs.value = Result.success(
                        response.copy(results = combinedList)
                    )
                } else {
                    // Replace list
                    _maintenanceLogs.value = result
                }

                _hasMoreData = response.next != null
                if (_hasMoreData) {
                    currentOffset += pageSize
                }
            }.onFailure {
                _maintenanceLogs.value = result
            }
        }.catch { exception ->
            _isLoading.value = false
            _maintenanceLogs.value = Result.failure(exception)
        }.launchIn(viewModelScope)
    }

    fun loadMoreMaintenanceLogs() {
        if (_hasMoreData && _isLoading.value != true) {
            loadMaintenanceLogs(refresh = false)
        }
    }

    fun filterByType(maintenanceType: String?) {
        currentMaintenanceType = maintenanceType
        applyFilters()
    }

    fun filterByStatus(status: String?) {
        currentStatus = status
        applyFilters()
    }

    fun searchMaintenanceLogs(query: String?) {
        currentSearchQuery = query
        applyFilters()
    }

    fun toggleTemplatesOnly() {
        showTemplatesOnly = !showTemplatesOnly
        applyFilters()
    }
    
    fun applyFilters(
        searchQuery: String? = null,
        maintenanceType: String? = null,
        status: String? = null,
        dateFrom: String? = null,
        dateTo: String? = null,
        templatesOnly: Boolean = false,
        includeCompleted: Boolean = true
    ) {
        currentSearchQuery = searchQuery
        currentMaintenanceType = maintenanceType
        currentStatus = status
        currentDateFrom = dateFrom
        currentDateTo = dateTo
        showTemplatesOnly = templatesOnly
        this.includeCompleted = includeCompleted
        applyFilters()
    }
    
    fun clearAllFilters() {
        currentSearchQuery = null
        currentMaintenanceType = null
        currentStatus = null
        currentDateFrom = null
        currentDateTo = null
        showTemplatesOnly = false
        includeCompleted = true
        applyFilters()
    }
    
    fun getCurrentSearchQuery(): String? = currentSearchQuery
    fun getShowTemplatesOnly(): Boolean = showTemplatesOnly

    private fun applyFilters() {
        currentOffset = 0
        _hasMoreData = true
        _isLoading.value = true

        val repositoryCall = if (currentSearchQuery?.isNotBlank() == true) {
            repository.searchMaintenanceLogs(
                query = currentSearchQuery!!,
                instrumentId = instrumentId,
                limit = pageSize,
                offset = currentOffset
            )
        } else {
            repository.getMaintenanceLogs(
                instrumentId = instrumentId,
                maintenanceType = currentMaintenanceType,
                status = if (!includeCompleted && currentStatus == null) {
                    // Exclude completed if includeCompleted is false and no specific status is selected
                    null // Let the repository handle this logic
                } else currentStatus,
                startDate = currentDateFrom,
                endDate = currentDateTo,
                isTemplate = if (showTemplatesOnly) true else null,
                ordering = "-maintenance_date",
                limit = pageSize,
                offset = currentOffset
            )
        }

        repositoryCall.onEach { result ->
            _isLoading.value = false
            _maintenanceLogs.value = result
            result.onSuccess { response ->
                _hasMoreData = response.next != null
                if (_hasMoreData) {
                    currentOffset += pageSize
                }
            }
        }.catch { exception ->
            _isLoading.value = false
            _maintenanceLogs.value = Result.failure(exception)
        }.launchIn(viewModelScope)
    }

    fun createMaintenanceLog(request: MaintenanceLogRequest) {
        repository.createMaintenanceLog(request)
            .onEach { result ->
                _createResult.value = result
                if (result.isSuccess) {
                    // Refresh the list to show the new log
                    loadMaintenanceLogs(refresh = true)
                }
            }
            .catch { exception ->
                _createResult.value = Result.failure(exception)
            }
            .launchIn(viewModelScope)
    }

    fun updateMaintenanceLogStatus(id: Long, status: String) {
        repository.updateMaintenanceLogStatus(id, status)
            .onEach { result ->
                _updateResult.value = result
                if (result.isSuccess) {
                    // Refresh the list to show the updated log
                    loadMaintenanceLogs(refresh = true)
                }
            }
            .catch { exception ->
                _updateResult.value = Result.failure(exception)
            }
            .launchIn(viewModelScope)
    }

    fun deleteMaintenanceLog(id: Long) {
        repository.deleteMaintenanceLog(id)
            .onEach { result ->
                _deleteResult.value = result
                if (result.isSuccess) {
                    // Refresh the list to remove the deleted log
                    loadMaintenanceLogs(refresh = true)
                }
            }
            .catch { exception ->
                _deleteResult.value = Result.failure(exception)
            }
            .launchIn(viewModelScope)
    }

    private fun loadMaintenanceTypes() {
        repository.getMaintenanceTypes()
            .onEach { result ->
                _maintenanceTypes.value = result
            }
            .catch { exception ->
                _maintenanceTypes.value = Result.failure(exception)
            }
            .launchIn(viewModelScope)
    }

    private fun loadMaintenanceStatuses() {
        repository.getMaintenanceStatuses()
            .onEach { result ->
                _maintenanceStatuses.value = result
            }
            .catch { exception ->
                _maintenanceStatuses.value = Result.failure(exception)
            }
            .launchIn(viewModelScope)
    }

    fun clearResults() {
        _createResult.value = null
        _updateResult.value = null
        _deleteResult.value = null
    }
}