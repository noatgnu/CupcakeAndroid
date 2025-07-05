package info.proteo.cupcake.ui.storage

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.proteo.cupcake.shared.data.model.storage.StorageObject
import info.proteo.cupcake.data.repository.StorageRepository
import info.proteo.cupcake.data.repository.StoredReagentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StorageViewModel @Inject constructor(
    private val storageRepository: StorageRepository,
    private val storedReagentService: StoredReagentRepository
) : ViewModel() {

    private val _storageState = MutableStateFlow<StorageUiState>(StorageUiState.Loading)
    val storageState: StateFlow<StorageUiState> = _storageState.asStateFlow()

    private val _currentPath = MutableStateFlow<List<StorageObject>>(emptyList())
    val currentPath: StateFlow<List<StorageObject>> = _currentPath.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore

    private var currentOffset = 0
    private val pageSize = 20
    private var hasMoreData = true
    private var currentParentId: Int? = null

    private val currentList = mutableListOf<StorageObject>()



    fun loadStorageObjects(parentId: Int? = null) {
        currentOffset = 0
        hasMoreData = true
        currentParentId = parentId
        currentList.clear()

        viewModelScope.launch {
            _storageState.value = StorageUiState.Loading
            updatePathForParent(parentId)
            fetchStorageObjects(parentId, 0)
        }
    }

    fun loadMoreStorageObjects() {
        if (!hasMoreData || _isLoadingMore.value) return

        viewModelScope.launch {
            _isLoadingMore.value = true
            fetchStorageObjects(currentParentId, currentOffset)
        }
    }

    private suspend fun fetchStorageObjects(parentId: Int?, offset: Int) {
        try {
            Log.d("StorageViewModel", "Fetching storage objects with parentId: $parentId, offset: $offset")

            val result = if (parentId == null) {
                storageRepository.getRootStorageObjects(offset = offset, limit = pageSize)
            } else {
                storageRepository.getChildStorageObjects(parentId, offset = offset, limit = pageSize)
            }

            result.onSuccess { response ->
                val newItems = response.results

                if (offset == 0) {
                    currentList.clear()
                }

                currentList.addAll(newItems)

                _storageState.value = if (currentList.isEmpty()) {
                    StorageUiState.Empty
                } else {
                    StorageUiState.Success(currentList.toList())
                }

                // Update pagination state
                currentOffset += newItems.size
                hasMoreData = newItems.size >= pageSize
                Log.d("StorageViewModel", "Loaded ${newItems.size} items, hasMoreData: $hasMoreData")
            }.onFailure { exception ->
                Log.e("StorageViewModel", "Error fetching storage objects: ${exception.message}")
                if (offset == 0) {
                    // Only show error state for initial load
                    _storageState.value = StorageUiState.Error(exception.message ?: "Unknown error")
                }
            }
        } catch (e: Exception) {
            Log.e("StorageViewModel", "Exception fetching storage objects: ${e.message}")
            if (offset == 0) {
                _storageState.value = StorageUiState.Error(e.message ?: "Unknown error")
            }
        } finally {
            _isLoadingMore.value = false
        }
    }

    private suspend fun updatePathForParent(parentId: Int?) {
        if (parentId == null) {
            _currentPath.value = emptyList()
            return
        }

        storageRepository.getStorageObjectById(parentId).onSuccess { parentObject ->
            val pathObjects = mutableListOf<StorageObject>()

            parentObject.pathToRoot?.forEach { pathItem ->
                pathObjects.add(StorageObject(
                    id = pathItem.id,
                    objectName = pathItem.name,
                    objectType = null,
                    objectDescription = null,
                    createdAt = null,
                    updatedAt = null,
                    canDelete = false,
                    storedAt = null,
                    storedReagents = null,
                    pngBase64 = null,
                    user = null,
                    accessLabGroups = null,
                    pathToRoot = null,
                    childCount = 0,
                    remoteId = null,
                    remoteHost = null
                ))
            }

            if (pathObjects.isEmpty() || pathObjects.last().id != parentObject.id) {
                pathObjects.add(parentObject)
            }

            _currentPath.value = pathObjects
        }
    }

    fun navigateUp() {
        // Get the parent of current location and navigate to it
        val currentLocation = _currentPath.value
        if (currentLocation.size > 1) {
            val parentLocation = currentLocation[currentLocation.size - 2]
            loadStorageObjects(parentLocation.id)
        } else {
            loadStorageObjects(null)  // Navigate to root
        }
    }

    fun refresh() {
        loadStorageObjects(currentParentId)
    }
    
    fun createStorageObject(name: String, type: String, parentId: Int?) {
        viewModelScope.launch {
            try {
                val newStorageObject = StorageObject(
                    id = 0, // Will be assigned by server
                    objectName = name,
                    objectType = type,
                    objectDescription = null,
                    createdAt = null,
                    updatedAt = null,
                    canDelete = true,
                    storedAt = parentId,
                    storedReagents = null,
                    pngBase64 = null,
                    user = null,
                    accessLabGroups = null,
                    pathToRoot = null,
                    childCount = 0,
                    remoteId = null,
                    remoteHost = null
                )
                
                storageRepository.createStorageObject(newStorageObject).onSuccess {
                    // Refresh the current view to show the new object
                    refresh()
                }.onFailure { exception ->
                    Log.e("StorageViewModel", "Error creating storage object: ${exception.message}")
                    // Could emit error state here if needed
                }
            } catch (e: Exception) {
                Log.e("StorageViewModel", "Exception creating storage object: ${e.message}")
            }
        }
    }
}

sealed class StorageUiState {
    data object Loading : StorageUiState()
    data class Success(val storageObjects: List<StorageObject>) : StorageUiState()
    data object Empty : StorageUiState()
    data class Error(val message: String) : StorageUiState()
}