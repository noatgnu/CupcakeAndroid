package info.proteo.cupcake.ui.reagent

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.proteo.cupcake.data.repository.ReagentRepository
import info.proteo.cupcake.data.repository.StoredReagentRepository
import info.proteo.cupcake.shared.data.model.reagent.Reagent
import info.proteo.cupcake.shared.data.model.reagent.StoredReagent
import info.proteo.cupcake.shared.data.model.reagent.StoredReagentCreateRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateStoredReagentViewModel @Inject constructor(
    private val reagentRepository: ReagentRepository,
    private val storedReagentRepository: StoredReagentRepository
) : ViewModel() {

    private val TAG = "CreateStoredReagentVM"

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedReagent = MutableStateFlow<Reagent?>(null)
    val selectedReagent = _selectedReagent.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Reagent>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _exportedMetadata = MutableStateFlow<Map<String, Any>?>(null)
    val exportedMetadata = _exportedMetadata.asStateFlow()

    private var searchJob: Job? = null
    private val searchDebounceTime = 300L // ms

    init {
        loadExportedMetadata()
    }

    fun setupSearchObserver() {
        // Search observer is now implemented in updateSearchQuery
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        Log.d(TAG, "Search query updated: $query")

        if (query.isEmpty()) {
            _searchResults.value = emptyList()
            Log.d(TAG, "Empty query, clearing results")
            return
        }

        // Cancel any ongoing search job
        searchJob?.cancel()

        // Start a new search job with debounce
        searchJob = viewModelScope.launch {
            delay(searchDebounceTime) // Debounce
            _isLoading.value = true
            Log.d(TAG, "Starting reagent search for: $query")

            try {
                val result = reagentRepository.getReagents(0, 10, query)
                Log.d(TAG, "API call completed for: $query")

                result.onSuccess { response ->
                    val reagents = response.results
                    Log.d(TAG, "Search successful, found ${reagents.size} reagents for query: $query")
                    _searchResults.value = reagents

                    // Log each found reagent for debugging
                    reagents.forEachIndexed { index, reagent ->
                        Log.d(TAG, "Result $index: ${reagent.name} (${reagent.unit})")
                    }
                }.onFailure { error ->
                    Log.e(TAG, "Search failed for query: $query", error)
                    _error.value = error.message ?: "An error occurred during search"
                    _searchResults.value = emptyList()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Exception during search for query: $query", e)
                _error.value = e.message ?: "An error occurred during search"
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectReagent(reagent: Reagent) {
        _selectedReagent.value = reagent
    }

    fun clearError() {
        _error.value = null
    }

    private fun loadExportedMetadata() {
        viewModelScope.launch {
            try {
            } catch (e: Exception) {
                _error.value = "Failed to load metadata: ${e.message}"
            }
        }
    }

    fun createStoredReagent(request: StoredReagentCreateRequest): LiveData<Result<StoredReagent>> {
        val result = MutableLiveData<Result<StoredReagent>>()
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val createResult = storedReagentRepository.createStoredReagent(request)
                result.postValue(createResult)
            } catch (e: Exception) {
                result.postValue(Result.failure(e))
            } finally {
                _isLoading.value = false
            }
        }

        return result
    }

    // Helper methods for metadata
    fun getMetadataProtocolId(): Int? {
        return exportedMetadata.value?.get("protocol") as? Int
    }

    fun getMetadataSessionId(): String? {
        return exportedMetadata.value?.get("session") as? String
    }

    fun getMetadataStepId(): Int? {
        return exportedMetadata.value?.get("step") as? Int
    }
}