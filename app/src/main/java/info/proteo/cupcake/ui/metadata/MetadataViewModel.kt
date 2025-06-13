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

    fun search(type: MetadataType, query: String, searchMode: SearchMode = SearchMode.CONTAINS, termType: String = "") {
        if (query.isBlank()) {
            _searchState.value = SearchState.Idle
            return
        }

        viewModelScope.launch {
            _searchState.value = SearchState.Loading
            try {
                // Convert enum to int for database query
                val modeValue = if (searchMode == SearchMode.CONTAINS) 0 else 1

                when (type) {
                    MetadataType.HUMAN_DISEASE -> {
                        val results = humanDiseaseDao.searchDiseases(query.lowercase(), modeValue).first()
                        _humanDiseaseResults.value = results
                        _searchState.value = SearchState.Success(results.size)
                    }
                    MetadataType.SUBCELLULAR_LOCATION -> {
                        val results = subcellularLocationDao.searchLocations(query.lowercase(), modeValue).first()
                        _subcellularLocationResults.value = results
                        _searchState.value = SearchState.Success(results.size)
                    }
                    MetadataType.TISSUE -> {
                        val results = tissueDao.searchTissues(query.lowercase(), modeValue).first()
                        _tissueResults.value = results
                        _searchState.value = SearchState.Success(results.size)
                    }
                    MetadataType.MS_UNIQUE_VOCABULARIES -> {
                        val results = msUniqueVocabulariesDao.searchVocabularies(
                            query.lowercase(),
                            modeValue,
                            termType
                        ).first()
                        _msUniqueVocabulariesResults.value = results
                        _searchState.value = SearchState.Success(results.size)
                    }
                    MetadataType.SPECIES -> {
                        val results = speciesDao.searchSpecies(query.lowercase(), modeValue).first()
                        _speciesResults.value = results
                        _searchState.value = SearchState.Success(results.size)
                    }
                    MetadataType.UNIMOD -> {
                        val results = unimodDao.searchUnimods(query.lowercase(), modeValue).first()
                        _unimodResults.value = results
                        _searchState.value = SearchState.Success(results.size)
                    }
                }
            } catch (e: Exception) {
                _searchState.value = SearchState.Error(e.message ?: "Unknown error")
            }
        }
    }
}