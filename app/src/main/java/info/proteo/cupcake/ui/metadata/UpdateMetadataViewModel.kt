package info.proteo.cupcake.ui.metadata

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.proteo.cupcake.data.local.dao.metadatacolumn.HumanDiseaseDao
import info.proteo.cupcake.data.local.dao.metadatacolumn.MSUniqueVocabulariesDao
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import javax.inject.Inject

@HiltViewModel
class UpdateMetadataViewModel @Inject constructor(
    private val humanDiseaseDao: HumanDiseaseDao,
    private val subcellularLocationDao: SubcellularLocationDao,
    private val unimodDao: UnimodDao,
    private val msUniqueVocabulariesDao: MSUniqueVocabulariesDao,
    private val speciesDao: SpeciesDao,
    private val tissueDao: TissueDao
) : ViewModel() {

    sealed class UpdateState {
        object Idle : UpdateState()
        object Loading : UpdateState()
        data class Success(val count: Int) : UpdateState()
        data class Error(val message: String) : UpdateState()
    }

    private val _humanDiseaseUpdateState = MutableLiveData<UpdateState>(UpdateState.Idle)
    val humanDiseaseUpdateState: LiveData<UpdateState> = _humanDiseaseUpdateState

    private val _subcellularLocationUpdateState = MutableLiveData<UpdateState>(UpdateState.Idle)
    val subcellularLocationUpdateState: LiveData<UpdateState> = _subcellularLocationUpdateState

    private val _unimodUpdateState = MutableLiveData<UpdateState>(UpdateState.Idle)
    val unimodUpdateState: LiveData<UpdateState> = _unimodUpdateState

    private val _msUniqueVocabulariesUpdateState = MutableLiveData<UpdateState>(UpdateState.Idle)
    val msUniqueVocabulariesUpdateState: LiveData<UpdateState> = _msUniqueVocabulariesUpdateState

    private val _speciesUpdateState = MutableLiveData<UpdateState>(UpdateState.Idle)
    val speciesUpdateState: LiveData<UpdateState> = _speciesUpdateState

    private val _tissueUpdateState = MutableLiveData<UpdateState>(UpdateState.Idle)
    val tissueUpdateState: LiveData<UpdateState> = _tissueUpdateState

    fun updateTissueData() {
        viewModelScope.launch(Dispatchers.IO) {
            _tissueUpdateState.postValue(UpdateState.Loading)
            try {
                val entries = downloadAndParseTissuesData()
                tissueDao.deleteAll()
                tissueDao.insertAll(entries)
                _tissueUpdateState.postValue(UpdateState.Success(entries.size))
            } catch (e: Exception) {
                _tissueUpdateState.postValue(UpdateState.Error(e.message ?: "Unknown error"))
            }
        }
    }

    fun updateSpeciesData() {
        viewModelScope.launch(Dispatchers.IO) {
            _speciesUpdateState.postValue(UpdateState.Loading)
            try {
                val entries = downloadAndParseSpeciesData()
                speciesDao.deleteAll()
                speciesDao.insertAll(entries)
                _speciesUpdateState.postValue(UpdateState.Success(entries.size))
            } catch (e: Exception) {
                _speciesUpdateState.postValue(UpdateState.Error(e.message ?: "Unknown error"))
            }
        }
    }

    fun updateMSUniqueVocabulariesData() {
        viewModelScope.launch(Dispatchers.IO) {
            _msUniqueVocabulariesUpdateState.postValue(UpdateState.Loading)
            try {
                val entries = downloadAndParseMSUniqueVocabulariesData()
                msUniqueVocabulariesDao.deleteAll()
                msUniqueVocabulariesDao.insertAll(entries)
                _msUniqueVocabulariesUpdateState.postValue(UpdateState.Success(entries.size))
            } catch (e: Exception) {
                _msUniqueVocabulariesUpdateState.postValue(UpdateState.Error(e.message ?: "Unknown error"))
            }
        }
    }

    fun updateUnimodData() {
        viewModelScope.launch(Dispatchers.IO) {
            _unimodUpdateState.postValue(UpdateState.Loading)
            try {
                val entries = downloadAndParseUnimodData()
                unimodDao.deleteAll()
                unimodDao.insertAll(entries)
                _unimodUpdateState.postValue(UpdateState.Success(entries.size))
            } catch (e: Exception) {
                _unimodUpdateState.postValue(UpdateState.Error(e.message ?: "Unknown error"))
            }
        }
    }

    fun updateHumanDiseaseData() {
        viewModelScope.launch(Dispatchers.IO) {
            _humanDiseaseUpdateState.postValue(UpdateState.Loading)
            try {
                val entries = downloadAndParseHumanDiseaseData()
                humanDiseaseDao.deleteAll()
                humanDiseaseDao.insertAll(entries)
                _humanDiseaseUpdateState.postValue(UpdateState.Success(entries.size))
            } catch (e: Exception) {
                _humanDiseaseUpdateState.postValue(UpdateState.Error(e.message ?: "Unknown error"))
            }
        }
    }

    fun updateSubcellularLocationData() {
        viewModelScope.launch(Dispatchers.IO) {
            _subcellularLocationUpdateState.postValue(UpdateState.Loading)
            try {
                val entries = downloadAndParseSubcellularLocationData()
                subcellularLocationDao.deleteAll()
                subcellularLocationDao.insertAll(entries)
                _subcellularLocationUpdateState.postValue(UpdateState.Success(entries.size))
            } catch (e: Exception) {
                _subcellularLocationUpdateState.postValue(UpdateState.Error(e.message ?: "Unknown error"))
            }
        }
    }

    private suspend fun downloadAndParseHumanDiseaseData(): List<HumanDiseaseEntity> {
        return withContext(Dispatchers.IO) {
            val entries = mutableListOf<HumanDiseaseEntity>()
            val url = "https://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/complete/docs/humdisease.txt"
            val response = URL(url).readText()

            var entry: HumanDiseaseEntity? = null

            response.lineSequence().forEach { line ->
                val trimmedLine = line.trim()

                when {
                    trimmedLine.startsWith("//") -> {
                        entry?.let {
                            entries.add(it)
                            entry = null
                        }
                    }
                    trimmedLine.startsWith("ID") -> {
                        entry = HumanDiseaseEntity(
                            id = 0,
                            identifier = trimmedLine.substring(5).trim().removeSuffix("."),
                            acronym = null,
                            accession = null,
                            synonyms = null,
                            crossReferences = null,
                            definition = null,
                            keywords = null
                        )
                    }
                    trimmedLine.startsWith("AC") && entry != null -> {
                        entry = entry!!.copy(accession = trimmedLine.substring(5).trim())
                    }
                    trimmedLine.startsWith("AR") && entry != null -> {
                        entry = entry!!.copy(acronym = trimmedLine.substring(5).trim())
                    }
                    trimmedLine.startsWith("DE") && entry != null -> {
                        val currentDefinition = entry!!.definition ?: ""
                        entry = entry!!.copy(definition = currentDefinition + trimmedLine.substring(5).trim() + " ")
                    }
                    trimmedLine.startsWith("SY") && entry != null -> {
                        val currentSynonyms = entry!!.synonyms ?: ""
                        entry = entry!!.copy(synonyms = currentSynonyms + trimmedLine.substring(5).trim() + "; ")
                    }
                    trimmedLine.startsWith("DR") && entry != null -> {
                        entry = entry!!.copy(crossReferences = trimmedLine.substring(5).trim())
                    }
                    trimmedLine.startsWith("KW") && entry != null -> {
                        val currentKeywords = entry!!.keywords ?: ""
                        entry = entry!!.copy(keywords = currentKeywords + trimmedLine.substring(5).trim() + "; ")
                    }
                }
            }

            entries
        }
    }

    private suspend fun downloadAndParseSubcellularLocationData(): List<SubcellularLocationEntity> {
        return withContext(Dispatchers.IO) {
            val entries = mutableListOf<SubcellularLocationEntity>()
            val url = "https://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/complete/docs/subcell.txt"
            val response = URL(url).readText()

            var entry: SubcellularLocationEntity? = null
            var started = false

            response.lineSequence().forEach { line ->
                val trimmedLine = line.trim()

                // Start parsing after first AN tag as in Python implementation
                if (trimmedLine.startsWith("AN")) {
                    started = true
                }

                if (started) {
                    when {
                        trimmedLine.startsWith("//") -> {
                            entry?.let {
                                entries.add(it)
                                entry = null
                            }
                        }
                        trimmedLine.startsWith("ID") -> {
                            entry = SubcellularLocationEntity(
                                id = 0,
                                locationIdentifier = trimmedLine.substring(5).trim().removeSuffix("."),
                                topologyIdentifier = null,
                                orientationIdentifier = null,
                                accession = null,
                                definition = null,
                                synonyms = null,
                                content = null,
                                isA = null,
                                partOf = null,
                                keyword = null,
                                geneOntology = null,
                                annotation = null,
                                references = null,
                                links = null
                            )
                        }
                        trimmedLine.startsWith("IT") && entry != null -> {
                            entry = entry!!.copy(topologyIdentifier = trimmedLine.substring(5).trim())
                        }
                        trimmedLine.startsWith("IO") && entry != null -> {
                            entry = entry!!.copy(orientationIdentifier = trimmedLine.substring(5).trim())
                        }
                        trimmedLine.startsWith("AC") && entry != null -> {
                            entry = entry!!.copy(accession = trimmedLine.substring(5).trim())
                        }
                        trimmedLine.startsWith("DE") && entry != null -> {
                            val currentDefinition = entry!!.definition ?: ""
                            entry = entry!!.copy(definition = currentDefinition + trimmedLine.substring(5).trim() + " ")
                        }
                        trimmedLine.startsWith("SY") && entry != null -> {
                            val currentSynonyms = entry!!.synonyms ?: ""
                            entry = entry!!.copy(synonyms = currentSynonyms + trimmedLine.substring(5).trim() + "; ")
                        }
                        trimmedLine.startsWith("SL") && entry != null -> {
                            entry = entry!!.copy(content = trimmedLine.substring(5).trim())
                        }
                        trimmedLine.startsWith("HI") && entry != null -> {
                            val currentIsA = entry!!.isA ?: ""
                            entry = entry!!.copy(isA = currentIsA + trimmedLine.substring(5).trim() + "; ")
                        }
                        trimmedLine.startsWith("HP") && entry != null -> {
                            val currentPartOf = entry!!.partOf ?: ""
                            entry = entry!!.copy(partOf = currentPartOf + trimmedLine.substring(5).trim() + "; ")
                        }
                        trimmedLine.startsWith("KW") && entry != null -> {
                            entry = entry!!.copy(keyword = trimmedLine.substring(5).trim())
                        }
                        trimmedLine.startsWith("GO") && entry != null -> {
                            val currentGeneOntology = entry!!.geneOntology ?: ""
                            entry = entry!!.copy(geneOntology = currentGeneOntology + trimmedLine.substring(5).trim() + "; ")
                        }
                        trimmedLine.startsWith("AN") && entry != null -> {
                            val currentAnnotation = entry!!.annotation ?: ""
                            entry = entry!!.copy(annotation = currentAnnotation + trimmedLine.substring(5).trim() + " ")
                        }
                        trimmedLine.startsWith("RX") && entry != null -> {
                            val currentReferences = entry!!.references ?: ""
                            entry = entry!!.copy(references = currentReferences + trimmedLine.substring(5).trim() + "; ")
                        }
                        trimmedLine.startsWith("WW") && entry != null -> {
                            val currentLinks = entry!!.links ?: ""
                            entry = entry!!.copy(links = currentLinks + trimmedLine.substring(5).trim() + "; ")
                        }
                    }
                }
            }

            entries
        }
    }

    private suspend fun downloadAndParseUnimodData(): List<UnimodEntity> {
        return withContext(Dispatchers.IO) {
            val entries = mutableListOf<UnimodEntity>()
            val url = "https://www.unimod.org/obo/unimod.obo"
            val response = URL(url).readText()

            var currentTermId: String? = null
            var currentTermName: String? = null
            var currentTermDefinition: String? = null
            val currentXrefs = mutableListOf<Map<String, String>>()
            var inTerm = false

            response.lineSequence().forEach { line ->
                val trimmedLine = line.trim()

                when {
                    trimmedLine == "[Term]" -> {
                        // Save previous term if exists
                        if (currentTermId != null && currentTermId.startsWith("UNIMOD:") && currentTermName != null) {
                            // Convert xrefs to JSON
                            val additionalData = if (currentXrefs.isNotEmpty()) {
                                JSONArray(currentXrefs).toString()
                            } else {
                                null
                            }

                            entries.add(
                                UnimodEntity(
                                    accession = currentTermId,
                                    name = currentTermName,
                                    definition = currentTermDefinition,
                                    additionalData = additionalData
                                )
                            )
                        }

                        // Reset for new term
                        currentTermId = null
                        currentTermName = null
                        currentTermDefinition = null
                        currentXrefs.clear()
                        inTerm = true
                    }
                    inTerm && trimmedLine.startsWith("id: ") -> {
                        currentTermId = trimmedLine.substring(4).trim()
                    }
                    inTerm && trimmedLine.startsWith("name: ") -> {
                        currentTermName = trimmedLine.substring(6).trim()
                    }
                    inTerm && trimmedLine.startsWith("def: ") -> {
                        currentTermDefinition = trimmedLine.substring(5).trim()
                            .removeSurrounding("\"")
                    }
                    inTerm && trimmedLine.startsWith("xref: ") -> {
                        val xrefContent = trimmedLine.substring(6).trim()

                        // Parse the xref which has format like "xref_id xref_description"
                        val spaceIndex = xrefContent.indexOf(" ")
                        if (spaceIndex > 0) {
                            val xrefId = xrefContent.substring(0, spaceIndex)
                            val xrefDesc = xrefContent.substring(spaceIndex + 1)
                                .replace("\"", "")

                            // Check if this xref_id already exists
                            val existingXref = currentXrefs.find { it["id"] == xrefId }
                            if (existingXref != null) {
                                // Append to existing description
                                val updatedXref = existingXref.toMutableMap()
                                updatedXref["description"] = existingXref["description"] + "," + xrefDesc

                                // Replace the existing entry
                                currentXrefs.remove(existingXref)
                                currentXrefs.add(updatedXref)
                            } else {
                                // Add new xref
                                currentXrefs.add(mapOf(
                                    "id" to xrefId,
                                    "description" to xrefDesc
                                ))
                            }
                        }
                    }
                    trimmedLine == "" && inTerm -> {
                        inTerm = false
                    }
                }
            }

            // Add the last term if exists
            if (currentTermId != null && currentTermId.startsWith("UNIMOD:") && currentTermName != null) {
                val additionalData = if (currentXrefs.isNotEmpty()) {
                    JSONArray(currentXrefs).toString()
                } else {
                    null
                }

                entries.add(
                    UnimodEntity(
                        accession = currentTermId,
                        name = currentTermName,
                        definition = currentTermDefinition,
                        additionalData = additionalData
                    )
                )
            }

            entries
        }
    }

    private suspend fun downloadAndParseMSUniqueVocabulariesData(): List<MSUniqueVocabulariesEntity> {
        return withContext(Dispatchers.IO) {
            val entries = mutableListOf<MSUniqueVocabulariesEntity>()

            // Process MS OBO file for instruments (MS:1000031) and cleavage agents (MS:1001045)
            val msOboUrl = "https://raw.githubusercontent.com/HUPO-PSI/psi-ms-CV/master/psi-ms.obo"
            val msOboResponse = URL(msOboUrl).readText()

            // Maps to hold term hierarchies and their data
            val terms = mutableMapOf<String, MSTermData>()
            val parents = mutableMapOf<String, MutableSet<String>>()

            // First pass: parse all terms
            var currentId: String? = null
            var currentName: String? = null
            var currentDef: String? = null
            var isA: MutableList<String> = mutableListOf()
            var inTerm = false

            msOboResponse.lineSequence().forEach { line ->
                val trimmedLine = line.trim()

                when {
                    trimmedLine == "[Term]" -> {
                        // Save previous term
                        if (currentId != null && currentName != null) {
                            terms[currentId] = MSTermData(currentId, currentName, currentDef)
                            isA.forEach { parentId ->
                                if (parentId !in parents) {
                                    parents[parentId] = mutableSetOf()
                                }
                                parents[parentId]?.add(currentId!!)
                            }
                        }

                        // Reset for new term
                        currentId = null
                        currentName = null
                        currentDef = null
                        isA = mutableListOf()
                        inTerm = true
                    }
                    inTerm && trimmedLine.startsWith("id:") -> {
                        currentId = trimmedLine.substring(3).trim()
                    }
                    inTerm && trimmedLine.startsWith("name:") -> {
                        currentName = trimmedLine.substring(5).trim()
                    }
                    inTerm && trimmedLine.startsWith("def:") -> {
                        currentDef = trimmedLine.substring(4).trim().removeSurrounding("\"")
                    }
                    inTerm && trimmedLine.startsWith("is_a:") -> {
                        val parentId = trimmedLine.substring(5).trim().split(" ")[0]
                        isA.add(parentId)
                    }
                    trimmedLine == "" && inTerm -> {
                        inTerm = false
                    }
                }
            }

            // Add leaf nodes under MS:1000031 (instrument)
            val instrumentLeaves = findLeafNodes("MS:1000031", terms, parents)
            entries.addAll(instrumentLeaves.map { termId ->
                val term = terms[termId]!!
                MSUniqueVocabulariesEntity(
                    accession = termId,
                    name = term.name,
                    definition = term.definition,
                    termType = "instrument"
                )
            })

            // Add leaf nodes under MS:1001045 (cleavage agent)
            val cleavageLeaves = findLeafNodes("MS:1001045", terms, parents)
            entries.addAll(cleavageLeaves.map { termId ->
                val term = terms[termId]!!
                MSUniqueVocabulariesEntity(
                    accession = termId,
                    name = term.name,
                    definition = term.definition,
                    termType = "cleavage agent"
                )
            })

            // Add leaf nodes under MS:1000133 (dissociation method)
            val dissociationLeaves = findLeafNodes("MS:1000133", terms, parents)
            entries.addAll(dissociationLeaves.map { termId ->
                val term = terms[termId]!!
                MSUniqueVocabulariesEntity(
                    accession = termId,
                    name = term.name,
                    definition = term.definition,
                    termType = "dissociation method"
                )
            })

            // Add leaf nodes under MS:1000443 (mass analyzer type)
            val massAnalyzerLeaves = findLeafNodes("MS:1000443", terms, parents)
            entries.addAll(massAnalyzerLeaves.map { termId ->
                val term = terms[termId]!!
                MSUniqueVocabulariesEntity(
                    accession = termId,
                    name = term.name,
                    definition = term.definition,
                    termType = "mass analyzer type"
                )
            })

            // Load EBI resources
            loadEbiResource(entries, "https://www.ebi.ac.uk/ols4/api/ontologies/pride/terms/http%253A%252F%252Fpurl.obolibrary.org%252Fobo%252FPRIDE_0000514/hierarchicalDescendants", "sample attribute")
            loadEbiResource(entries, "https://www.ebi.ac.uk/ols4/api/ontologies/efo/terms/http%253A%252F%252Fwww.ebi.ac.uk%252Fefo%252FEFO_0009090/hierarchicalDescendants", "enrichment process")
            loadEbiResource(entries, "https://www.ebi.ac.uk/ols4/api/ontologies/pride/terms/http%253A%252F%252Fpurl.obolibrary.org%252Fobo%252FPRIDE_0000550/hierarchicalDescendants", "fractionation method")
            loadEbiResource(entries, "https://www.ebi.ac.uk/ols4/api/ontologies/pride/terms/http%253A%252F%252Fpurl.obolibrary.org%252Fobo%252FPRIDE_0000659/hierarchicalDescendants", "proteomics data acquisition method")
            loadEbiResource(entries, "https://www.ebi.ac.uk/ols4/api/ontologies/pride/terms/http%253A%252F%252Fpurl.obolibrary.org%252Fobo%252FPRIDE_0000607/hierarchicalDescendants", "reduction reagent")
            loadEbiResource(entries, "https://www.ebi.ac.uk/ols4/api/ontologies/pride/terms/http%253A%252F%252Fpurl.obolibrary.org%252Fobo%252FPRIDE_0000598/hierarchicalDescendants", "alkylation reagent")

            entries
        }
    }

    // Helper class to store term data
    private data class MSTermData(
        val id: String,
        val name: String,
        val definition: String?
    )

    // Find leaf nodes in the ontology
    private fun findLeafNodes(
        rootId: String,
        terms: Map<String, MSTermData>,
        parents: Map<String, Set<String>>
    ): Set<String> {
        val result = mutableSetOf<String>()
        val children = parents[rootId] ?: return result

        for (childId in children) {
            if (childId !in parents) {
                // This is a leaf node
                result.add(childId)
            } else {
                // Recursively find leaves
                result.addAll(findLeafNodes(childId, terms, parents))
            }
        }

        return result
    }

    // Load data from EBI REST API
    private suspend fun loadEbiResource(
        entries: MutableList<MSUniqueVocabulariesEntity>,
        baseUrl: String,
        termType: String,
        size: Int = 1000
    ) {
        try {
            val url = "$baseUrl?page=0&size=$size"
            val response = URL(url).readText()
            val jsonObject = JSONObject(response)

            val embeddedObj = jsonObject.getJSONObject("_embedded")
            val termsArray = embeddedObj.getJSONArray("terms")

            for (i in 0 until termsArray.length()) {
                val term = termsArray.getJSONObject(i)
                val oboId = term.getString("obo_id")
                val label = term.getString("label")
                val description = if (term.has("description")) term.getString("description") else null

                entries.add(
                    MSUniqueVocabulariesEntity(
                        accession = oboId,
                        name = label,
                        definition = description,
                        termType = termType
                    )
                )
            }

            // Handle pagination
            val pageObj = jsonObject.getJSONObject("page")
            val totalPages = pageObj.getInt("totalPages")

            if (totalPages > 1) {
                for (page in 1 until totalPages) {
                    val nextPageUrl = "$baseUrl?page=$page&size=$size"
                    val nextPageResponse = URL(nextPageUrl).readText()
                    val nextPageJson = JSONObject(nextPageResponse)

                    if (nextPageJson.has("_embedded")) {
                        val nextEmbedded = nextPageJson.getJSONObject("_embedded")
                        val nextTerms = nextEmbedded.getJSONArray("terms")

                        for (j in 0 until nextTerms.length()) {
                            val term = nextTerms.getJSONObject(j)
                            val oboId = term.getString("obo_id")
                            val label = term.getString("label")
                            val description = if (term.has("description")) term.getString("description") else null

                            entries.add(
                                MSUniqueVocabulariesEntity(
                                    accession = oboId,
                                    name = label,
                                    definition = description,
                                    termType = termType
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Log error but continue with other resources
            Log.e("UpdateMetadataVM", "Error loading EBI resource: $baseUrl", e)
        }
    }

    private suspend fun downloadAndParseSpeciesData(): List<SpeciesEntity> {
        return withContext(Dispatchers.IO) {
            val entries = mutableListOf<SpeciesEntity>()
            val url = "https://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/complete/docs/speclist.txt"
            val response = URL(url).readText()

            var currentSpecies: MutableMap<String, String?> = mutableMapOf()

            response.lineSequence().forEach { line ->
                // Match the pattern for a new species entry
                val mainMatch = Regex("""^(\w+)\s+[VABEO]\s+(\d+):\s+N=(.*)$""").find(line)
                if (mainMatch != null) {
                    // Save the previous species if it exists
                    if (currentSpecies.isNotEmpty()) {
                        entries.add(
                            SpeciesEntity(
                                id = 0, // Auto-generated
                                code = currentSpecies["code"],
                                taxon = currentSpecies["taxon"],
                                commonName = currentSpecies["common_name"],
                                officialName = currentSpecies["official_name"],
                                synonym = currentSpecies["synonym"]
                            )
                        )
                    }

                    // Start a new species entry
                    currentSpecies = mutableMapOf(
                        "code" to mainMatch.groupValues[1],
                        "taxon" to mainMatch.groupValues[2],
                        "official_name" to mainMatch.groupValues[3],
                        "common_name" to null,
                        "synonym" to null
                    )
                } else {
                    // Match the continuation line for common name
                    val commonNameMatch = Regex("""^\s+C=(.*)$""").find(line)
                    if (commonNameMatch != null && currentSpecies.isNotEmpty()) {
                        currentSpecies["common_name"] = commonNameMatch.groupValues[1]
                    }

                    // Match the continuation line for synonym
                    val synonymMatch = Regex("""^\s+S=(.*)$""").find(line)
                    if (synonymMatch != null && currentSpecies.isNotEmpty()) {
                        currentSpecies["synonym"] = synonymMatch.groupValues[1]
                    }
                }
            }

            // Add the last species
            if (currentSpecies.isNotEmpty()) {
                entries.add(
                    SpeciesEntity(
                        id = 0, // Auto-generated
                        code = currentSpecies["code"],
                        taxon = currentSpecies["taxon"],
                        commonName = currentSpecies["common_name"],
                        officialName = currentSpecies["official_name"],
                        synonym = currentSpecies["synonym"]
                    )
                )
            }

            entries
        }
    }

    private suspend fun downloadAndParseTissuesData(): List<TissueEntity> {
        return withContext(Dispatchers.IO) {
            val entries = mutableListOf<TissueEntity>()
            val url = "https://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/complete/docs/tisslist.txt"
            val response = URL(url).readText()

            var entry: TissueEntity? = null

            response.lineSequence().forEach { line ->
                when {
                    line.startsWith("//") -> {
                        // End of entry, save if exists
                        entry?.let {
                            entries.add(it)
                        }
                        entry = null
                    }
                    line.startsWith("ID") -> {
                        // New entry
                        entry = TissueEntity(
                            id = 0,
                            identifier = line.substring(5).trim().removeSuffix("."),
                            accession = null,
                            synonyms = null,
                            crossReferences = null
                        )
                    }
                    line.startsWith("AC") && entry != null -> {
                        entry = entry!!.copy(
                            accession = line.substring(5).trim()
                        )
                    }
                    line.startsWith("SY") && entry != null -> {
                        val currentSynonyms = entry!!.synonyms
                        val newSynonyms = line.substring(5).trim()
                        entry = entry!!.copy(
                            synonyms = if (currentSynonyms.isNullOrEmpty())
                                newSynonyms
                            else
                                "$currentSynonyms; $newSynonyms"
                        )
                    }
                    line.startsWith("DR") && entry != null -> {
                        val currentRefs = entry!!.crossReferences
                        val newRefs = line.substring(5).trim()
                        entry = entry!!.copy(
                            crossReferences = if (currentRefs.isNullOrEmpty())
                                newRefs
                            else
                                "$currentRefs; $newRefs"
                        )
                    }
                }
            }

            // Add the last entry if it exists
            entry?.let {
                entries.add(it)
            }

            entries
        }
    }
}