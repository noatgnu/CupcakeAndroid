package info.proteo.cupcake.ui.reagent

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.proteo.cupcake.shared.data.model.reagent.StoredReagent
import info.proteo.cupcake.shared.data.model.storage.StorageObject
import info.proteo.cupcake.data.remote.service.StorageObjectService
import info.proteo.cupcake.data.remote.service.StoredReagentService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class StoredReagentUiState {
    object Loading : StoredReagentUiState()
    data class Success(val reagents: List<StoredReagent>) : StoredReagentUiState()
    object Empty : StoredReagentUiState()
    data class NoSearchResults(val searchTerm: String) : StoredReagentUiState() // New state for search
    data class Error(val message: String) : StoredReagentUiState()
}

@HiltViewModel
class StoredReagentViewModel @Inject constructor(
    private val storedReagentService: StoredReagentService,
    private val storageService: StorageObjectService,
) : ViewModel() {

    private val _reagentsState = MutableStateFlow<StoredReagentUiState>(StoredReagentUiState.Loading)
    val reagentsState: StateFlow<StoredReagentUiState> = _reagentsState.asStateFlow()

    private val _storageObject = MutableStateFlow<StorageObject?>(null)
    val storageObject: StateFlow<StorageObject?> = _storageObject.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _isRootLocation = MutableStateFlow(false)
    val isRootLocation: StateFlow<Boolean> = _isRootLocation.asStateFlow()

    private val _isSearchingByBarcode = MutableStateFlow(false)
    val isSearchingByBarcode: StateFlow<Boolean> = _isSearchingByBarcode.asStateFlow()

    private val _currentBarcodeState = MutableStateFlow<String?>(null)
    val currentBarcodeState: StateFlow<String?> = _currentBarcodeState.asStateFlow()

    private val _locationPaths = MutableStateFlow<Map<Int, String>>(emptyMap())
    val locationPaths: StateFlow<Map<Int, String>> = _locationPaths.asStateFlow()


    private var currentBarcode: String? = null

    private var currentOffset = 0
    private val pageSize = 20
    private var currentStorageObjectId: Int? = null
    private var hasMoreData = true

    fun loadStoredReagents(storageObjectId: Int) {
        currentStorageObjectId = storageObjectId
        currentOffset = 0
        hasMoreData = true
        currentBarcode = null
        _isSearchingByBarcode.value = false
        _currentBarcodeState.value = null
        _reagentsState.value = StoredReagentUiState.Loading

        fetchStoredReagents(storageObjectId, currentOffset, pageSize)
    }

    fun loadStorageObjectInfo(storageObjectId: Int) {
        if (storageObjectId <= 0) {
            _isRootLocation.value = true
            _storageObject.value = null
            return
        }

        _isRootLocation.value = false
        viewModelScope.launch {
            try {
                val result = storageService.getStorageObjectById(storageObjectId)
                result.onSuccess { storageObject ->
                    _storageObject.value = storageObject
                }
            } catch (e: Exception) {
            }
        }
    }

    fun fetchLocationPath(locationId: Int) {
        viewModelScope.launch {
            storageService.getPathToRoot(locationId).onSuccess { pathItems ->
                val currentPaths = _locationPaths.value.toMutableMap()
                val pathString = pathItems.joinToString(" / ") { it.name }
                currentPaths[locationId] = pathString
                _locationPaths.value = currentPaths
            }
        }
    }

    fun searchByBarcode(barcode: String, storageObjectId: Int? = null) {
        viewModelScope.launch {
            _reagentsState.value = StoredReagentUiState.Loading
            currentOffset = 0
            hasMoreData = true
            currentBarcode = barcode
            currentStorageObjectId = storageObjectId
            _isSearchingByBarcode.value = true
            _currentBarcodeState.value = barcode
            Log.d("StoredReagentViewModel", "Starting search for barcode: $barcode")

            try {
                val result = storedReagentService.getStoredReagents(
                    offset = 0,
                    limit = pageSize,
                    storageObjectId = storageObjectId,
                    barcode = barcode
                )

                result.onSuccess { response ->
                    Log.d("StoredReagentViewModel", "Search completed with ${response.results.size} results")
                    if (response.results.isNotEmpty()) {
                        response.results.forEach { reagent ->
                            reagent.storageObject.id.let { locationId ->
                                fetchLocationPath(locationId)
                            }
                        }
                        _reagentsState.value = StoredReagentUiState.Success(response.results)
                        Log.d("StoredReagentViewModel", "Setting state to Success with ${response.results.size} items")
                    } else {
                        _reagentsState.value = StoredReagentUiState.NoSearchResults(barcode)
                        Log.d("StoredReagentViewModel", "Setting state to NoSearchResults for barcode: $barcode")
                    }
                    hasMoreData = response.next != null
                }

                result.onFailure { throwable ->
                    Log.e("StoredReagentViewModel", "Search failed", throwable)
                    _reagentsState.value = StoredReagentUiState.Error(
                        throwable.message ?: "Failed to search for barcode: $barcode"
                    )
                }
            } catch (e: Exception) {
                Log.e("StoredReagentViewModel", "Search exception", e)
                _reagentsState.value = StoredReagentUiState.Error(
                    e.message ?: "An error occurred while searching"
                )
            } finally {
                _isSearchingByBarcode.value = false
            }
        }
    }

    fun loadMoreStoredReagents() {
        if (!hasMoreData || _isLoadingMore.value || currentStorageObjectId == null) {
            return
        }

        _isLoadingMore.value = true
        currentOffset += pageSize

        fetchStoredReagents(currentStorageObjectId!!, currentOffset, pageSize, currentBarcode)
    }

    fun refresh(storageObjectId: Int) {
        currentOffset = 0
        hasMoreData = true
        fetchStoredReagents(storageObjectId, currentOffset, pageSize, currentBarcode)
    }

    private fun fetchStoredReagents(storageObjectId: Int, offset: Int, limit: Int, barcode: String? = null) {
        viewModelScope.launch {
            try {
                val result = storedReagentService.getStoredReagents(
                    offset = offset,
                    limit = limit,
                    storageObjectId = storageObjectId,
                    barcode = barcode
                )

                result.onSuccess { response ->
                    val reagents = response.results
                    if (reagents.isNotEmpty()) {
                        reagents.forEach { reagent ->
                            reagent.storageObject.id.let { locationId ->
                                fetchLocationPath(locationId)
                            }
                        }
                    }
                    if (offset == 0) {
                        if (reagents.isEmpty()) {
                            _reagentsState.value = StoredReagentUiState.Empty
                        } else {
                            _reagentsState.value = StoredReagentUiState.Success(reagents)
                        }
                    } else {
                        val currentList = (_reagentsState.value as? StoredReagentUiState.Success)?.reagents ?: emptyList()
                        _reagentsState.value = StoredReagentUiState.Success(currentList + reagents)
                    }

                    hasMoreData = response.next != null
                }

                result.onFailure { throwable ->
                    if (offset == 0) {
                        _reagentsState.value = StoredReagentUiState.Error(throwable.message ?: "Failed to load reagents")
                    }
                }
            } catch (e: Exception) {
                if (offset == 0) {
                    _reagentsState.value = StoredReagentUiState.Error(e.message ?: "Failed to load reagents")
                }
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun searchByTerm(searchTerm: String, storageObjectId: Int? = null) {
        viewModelScope.launch {
            _reagentsState.value = StoredReagentUiState.Loading
            currentOffset = 0
            hasMoreData = true
            currentBarcode = null
            currentStorageObjectId = storageObjectId
            _isSearchingByBarcode.value = true
            _currentBarcodeState.value = searchTerm

            try {
                Log.d("StoredReagentViewModel", "Starting search for term: $searchTerm in storageObjectId: $storageObjectId")
                val result = storedReagentService.getStoredReagents(
                    offset = 0,
                    limit = pageSize,
                    storageObjectId = storageObjectId,
                    search = searchTerm
                )

                result.onSuccess { response ->
                    if (response.results.isNotEmpty()) {
                        response.results.forEach { reagent ->
                            reagent.storageObject.id.let { locationId ->
                                fetchLocationPath(locationId)
                            }
                        }
                        _reagentsState.value = StoredReagentUiState.Success(response.results)
                    } else {
                        _reagentsState.value = StoredReagentUiState.NoSearchResults(searchTerm)
                    }
                    hasMoreData = response.next != null
                }

                result.onFailure { throwable ->
                    _reagentsState.value = StoredReagentUiState.Error(
                        throwable.message ?: "Failed to search for: $searchTerm"
                    )
                }
            } catch (e: Exception) {
                _reagentsState.value = StoredReagentUiState.Error(
                    e.message ?: "An error occurred while searching"
                )
            } finally {
                _isSearchingByBarcode.value = false
            }
        }
    }
}