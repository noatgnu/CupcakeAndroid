package info.proteo.cupcake.ui.metadata

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.proteo.cupcake.data.local.dao.metadatacolumn.HumanDiseaseDao
import info.proteo.cupcake.data.local.dao.metadatacolumn.MSUniqueVocabulariesDao
import info.proteo.cupcake.data.local.dao.metadatacolumn.SearchMode
import info.proteo.cupcake.data.local.dao.metadatacolumn.SpeciesDao
import info.proteo.cupcake.data.local.dao.metadatacolumn.SubcellularLocationDao
import info.proteo.cupcake.data.local.dao.metadatacolumn.TissueDao
import info.proteo.cupcake.data.local.dao.metadatacolumn.UnimodDao
import info.proteo.cupcake.data.local.entity.metadatacolumn.HumanDiseaseEntity
import info.proteo.cupcake.data.local.entity.metadatacolumn.MSUniqueVocabulariesEntity
import info.proteo.cupcake.data.local.entity.metadatacolumn.SpeciesEntity
import info.proteo.cupcake.data.local.entity.metadatacolumn.SubcellularLocationEntity
import info.proteo.cupcake.data.local.entity.metadatacolumn.TissueEntity
import info.proteo.cupcake.data.local.entity.metadatacolumn.UnimodEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class MetadataType {
    HUMAN_DISEASE,
    SUBCELLULAR_LOCATION,
    TISSUE,
    MS_UNIQUE_VOCABULARIES,
    SPECIES,
    UNIMOD
}

sealed class SearchState {
    object Idle : SearchState()
    object Loading : SearchState()
    data class Success(val count: Int) : SearchState()
    data class Error(val message: String) : SearchState()
}

@HiltViewModel
class MetadataViewModel @Inject constructor(
    private val humanDiseaseDao: HumanDiseaseDao,
    private val subcellularLocationDao: SubcellularLocationDao,
    private val tissueDao: TissueDao,
    private val msUniqueVocabulariesDao: MSUniqueVocabulariesDao,
    private val speciesDao: SpeciesDao,
    private val unimodDao: UnimodDao
) : ViewModel() {

    private val _searchState = MutableLiveData<SearchState>(SearchState.Idle)
    val searchState: LiveData<SearchState> = _searchState

    private val _humanDiseaseResults = MutableLiveData<List<HumanDiseaseEntity>>(emptyList())
    val humanDiseaseResults: LiveData<List<HumanDiseaseEntity>> = _humanDiseaseResults

    private val _subcellularLocationResults = MutableLiveData<List<SubcellularLocationEntity>>(emptyList())
    val subcellularLocationResults: LiveData<List<SubcellularLocationEntity>> = _subcellularLocationResults

    private val _tissueResults = MutableLiveData<List<TissueEntity>>(emptyList())
    val tissueResults: LiveData<List<TissueEntity>> = _tissueResults

    private val _msUniqueVocabulariesResults = MutableLiveData<List<MSUniqueVocabulariesEntity>>(emptyList())
    val msUniqueVocabulariesResults: LiveData<List<MSUniqueVocabulariesEntity>> = _msUniqueVocabulariesResults

    private val _speciesResults = MutableLiveData<List<SpeciesEntity>>(emptyList())
    val speciesResults: LiveData<List<SpeciesEntity>> = _speciesResults

    private val _unimodResults = MutableLiveData<List<UnimodEntity>>(emptyList())
    val unimodResults: LiveData<List<UnimodEntity>> = _unimodResults

    private var searchJob: Job? = null

    companion object {
        private const val MAX_RESULTS = 100 // Limit results to prevent UI overwhelm
    }

    fun search(type: MetadataType, query: String, searchMode: SearchMode = SearchMode.CONTAINS, termType: String = "") {
        // Cancel any ongoing search
        searchJob?.cancel()
        
        if (query.isBlank()) {
            _searchState.value = SearchState.Idle
            clearResults()
            return
        }

        // Require minimum 2 characters to prevent expensive searches
        if (query.trim().length < 2) {
            _searchState.value = SearchState.Idle
            clearResults()
            return
        }

        searchJob = viewModelScope.launch {
            _searchState.value = SearchState.Loading
            try {
                // All database operations run on IO dispatcher to avoid blocking UI thread
                val results = withContext(Dispatchers.IO) {
                    // Convert enum to int for database query
                    val modeValue = if (searchMode == SearchMode.CONTAINS) 0 else 1
                    val queryLower = query.lowercase().trim()

                    when (type) {
                        MetadataType.HUMAN_DISEASE -> {
                            humanDiseaseDao.searchDiseases(queryLower, modeValue).first()
                        }
                        MetadataType.SUBCELLULAR_LOCATION -> {
                            subcellularLocationDao.searchLocations(queryLower, modeValue).first()
                        }
                        MetadataType.TISSUE -> {
                            tissueDao.searchTissues(queryLower, modeValue).first()
                        }
                        MetadataType.MS_UNIQUE_VOCABULARIES -> {
                            msUniqueVocabulariesDao.searchVocabularies(
                                queryLower,
                                modeValue,
                                termType
                            ).first()
                        }
                        MetadataType.SPECIES -> {
                            speciesDao.searchSpecies(queryLower, modeValue).first()
                        }
                        MetadataType.UNIMOD -> {
                            unimodDao.searchUnimods(queryLower, modeValue).first()
                        }
                    }
                }

                // Limit results to prevent UI overwhelm and improve performance
                val limitedResults = if (results.size > MAX_RESULTS) {
                    results.take(MAX_RESULTS)
                } else {
                    results
                }

                // Update UI on main thread
                when (type) {
                    MetadataType.HUMAN_DISEASE -> {
                        _humanDiseaseResults.value = limitedResults as List<HumanDiseaseEntity>
                    }
                    MetadataType.SUBCELLULAR_LOCATION -> {
                        _subcellularLocationResults.value = limitedResults as List<SubcellularLocationEntity>
                    }
                    MetadataType.TISSUE -> {
                        _tissueResults.value = limitedResults as List<TissueEntity>
                    }
                    MetadataType.MS_UNIQUE_VOCABULARIES -> {
                        _msUniqueVocabulariesResults.value = limitedResults as List<MSUniqueVocabulariesEntity>
                    }
                    MetadataType.SPECIES -> {
                        _speciesResults.value = limitedResults as List<SpeciesEntity>
                    }
                    MetadataType.UNIMOD -> {
                        _unimodResults.value = limitedResults as List<UnimodEntity>
                    }
                }
                
                // Show the original count to inform user if results were truncated
                _searchState.value = SearchState.Success(results.size)
            } catch (e: Exception) {
                _searchState.value = SearchState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun clearResults() {
        _humanDiseaseResults.value = emptyList()
        _subcellularLocationResults.value = emptyList()
        _tissueResults.value = emptyList()
        _msUniqueVocabulariesResults.value = emptyList()
        _speciesResults.value = emptyList()
        _unimodResults.value = emptyList()
    }
}