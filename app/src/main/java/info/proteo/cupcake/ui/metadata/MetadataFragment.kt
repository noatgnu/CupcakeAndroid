package info.proteo.cupcake.ui.metadata

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.text.Editable
import android.text.TextWatcher
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import info.proteo.cupcake.R
import info.proteo.cupcake.data.local.dao.metadatacolumn.SearchMode
import info.proteo.cupcake.databinding.FragmentMetadataBinding
import kotlin.toString

@AndroidEntryPoint
class MetadataFragment : Fragment() {

    private var _binding: FragmentMetadataBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MetadataViewModel by viewModels()
    private lateinit var humanDiseaseAdapter: HumanDiseaseAdapter
    private lateinit var subcellularLocationAdapter: SubcellularLocationAdapter
    private lateinit var tissueAdapter: TissueAdapter
    private lateinit var msUniqueVocabulariesAdapter: MSUniqueVocabulariesAdapter
    private lateinit var speciesAdapter: SpeciesAdapter
    private lateinit var unimodAdapter: UnimodAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMetadataBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupTypeSpinner()
        setupSearchModeSpinner()
        setupTermTypeFilter()
        setupSearchView()
        observeViewModel()
    }

    private fun setupTypeSpinner() {
        val metadataTypes = arrayOf(
            "Human Disease",
            "Subcellular Location",
            "Organism part",    // Changed from "Tissue"
            "MS Unique Vocabularies",
            "Organisms",        // Changed from "Species"
            "Modifications"     // Changed from "Unimod"
        )
        
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            metadataTypes
        )
        binding.metadataTypeSpinner.setAdapter(adapter)
        binding.metadataTypeSpinner.threshold = 0
        
        // Set default selection
        binding.metadataTypeSpinner.setText(metadataTypes[0], false)
        updateCurrentAdapter(0)

        // Handle selection changes
        fun handleMetadataTypeSelection(selectedType: String) {
            val position = metadataTypes.indexOf(selectedType)
            if (position >= 0) {
                updateCurrentAdapter(position)
                binding.termTypeFilterLayout.visibility =
                    if (position == 3) View.VISIBLE else View.GONE
                performSearch(binding.searchView.text.toString())
            }
        }

        binding.metadataTypeSpinner.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            val selectedType = metadataTypes[position]
            handleMetadataTypeSelection(selectedType)
        }

        // Show dropdown when clicked
        binding.metadataTypeSpinner.setOnClickListener { 
            binding.metadataTypeSpinner.showDropDown() 
        }
    }

    private fun setupSearchModeSpinner() {
        val searchModes = arrayOf("Contains", "Starts With")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, searchModes)
        binding.searchModeSpinner.setAdapter(adapter)
        binding.searchModeSpinner.threshold = 0
        
        // Set default selection
        binding.searchModeSpinner.setText(searchModes[0], false)

        binding.searchModeSpinner.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            performSearch(binding.searchView.text.toString())
        }

        // Show dropdown when clicked
        binding.searchModeSpinner.setOnClickListener { 
            binding.searchModeSpinner.showDropDown() 
        }
    }

    private fun setupTermTypeFilter() {
        val termTypes = arrayOf(
            "All Types",
            "Label",
            "Cleavage agent details",
            "Instrument",
            "Dissociation method",
            "MS2 analyzer type",
            "Enrichment process",
            "Fractionation method",
            "Proteomics data acquisition method",
            "Reduction reagent",
            "Alkylation reagent"
        )

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, termTypes)
        binding.termTypeSpinner.setAdapter(adapter)
        binding.termTypeSpinner.threshold = 0
        
        // Set default selection
        binding.termTypeSpinner.setText(termTypes[0], false)

        binding.termTypeSpinner.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            performSearch(binding.searchView.text.toString())
        }

        // Show dropdown when clicked
        binding.termTypeSpinner.setOnClickListener { 
            binding.termTypeSpinner.showDropDown() 
        }
    }


    private var searchJob: kotlinx.coroutines.Job? = null
    
    private fun setupSearchView() {
        binding.searchView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                // Cancel previous search
                searchJob?.cancel()
                
                val query = s?.toString() ?: ""
                
                // Debounce search by 500ms to reduce database calls
                searchJob = viewLifecycleOwner.lifecycleScope.launch {
                    kotlinx.coroutines.delay(500)
                    performSearch(query)
                }
            }
        })
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        humanDiseaseAdapter = HumanDiseaseAdapter()
        subcellularLocationAdapter = SubcellularLocationAdapter()
        tissueAdapter = TissueAdapter()
        msUniqueVocabulariesAdapter = MSUniqueVocabulariesAdapter()
        speciesAdapter = SpeciesAdapter()
        unimodAdapter = UnimodAdapter()

        updateCurrentAdapter(0)
    }

    private fun updateCurrentAdapter(position: Int) {
        binding.recyclerView.adapter = when (position) {
            0 -> humanDiseaseAdapter
            1 -> subcellularLocationAdapter
            2 -> tissueAdapter
            3 -> msUniqueVocabulariesAdapter
            4 -> speciesAdapter
            5 -> unimodAdapter
            else -> humanDiseaseAdapter
        }
    }

    private fun performSearch(query: String) {
        val selectedMetadataType = binding.metadataTypeSpinner.text.toString()
        val metadataType = when (selectedMetadataType) {
            "Human Disease" -> MetadataType.HUMAN_DISEASE
            "Subcellular Location" -> MetadataType.SUBCELLULAR_LOCATION
            "Organism part" -> MetadataType.TISSUE
            "MS Unique Vocabularies" -> MetadataType.MS_UNIQUE_VOCABULARIES
            "Organisms" -> MetadataType.SPECIES
            "Modifications" -> MetadataType.UNIMOD
            else -> MetadataType.HUMAN_DISEASE
        }

        val selectedSearchMode = binding.searchModeSpinner.text.toString()
        val searchMode = if (selectedSearchMode == "Starts With") {
            SearchMode.STARTS_WITH
        } else {
            SearchMode.CONTAINS
        }

        var termType = ""
        if (metadataType == MetadataType.MS_UNIQUE_VOCABULARIES) {
            val selectedTermType = binding.termTypeSpinner.text.toString()
            termType = when (selectedTermType) {
                "All Types" -> "" 
                "Label" -> "sample attribute" 
                "Cleavage agent details" -> "cleavage agent" 
                "Instrument" -> "instrument" 
                "Dissociation method" -> "dissociation method" 
                "MS2 analyzer type" -> "mass analyzer type" 
                "Enrichment process" -> "enrichment process" 
                "Fractionation method" -> "fractionation method" 
                "Proteomics data acquisition method" -> "proteomics data acquisition method" 
                "Reduction reagent" -> "reduction reagent" 
                "Alkylation reagent" -> "alkylation reagent" 
                else -> ""
            }
        }

        viewModel.search(metadataType, query, searchMode, termType)

    }

    private fun observeViewModel() {
        viewModel.searchState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is SearchState.Idle -> {
                    binding.progressBar.visibility = View.GONE
                    binding.statusIcon.visibility = View.VISIBLE
                    binding.statusText.text = "Enter search terms to find metadata"
                    binding.statusSubtext.text = "Select a metadata type and start typing to search"
                    binding.statusText.visibility = View.VISIBLE
                    binding.statusSubtext.visibility = View.VISIBLE
                    binding.statusContainer.visibility = View.VISIBLE
                    binding.statusIcon.setImageResource(R.drawable.ic_search)
                }
                is SearchState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.statusIcon.visibility = View.GONE
                    binding.statusText.text = "Searching metadata..."
                    binding.statusSubtext.visibility = View.GONE
                    binding.statusText.visibility = View.VISIBLE
                    binding.statusContainer.visibility = View.VISIBLE
                }
                is SearchState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    if (state.count == 0) {
                        binding.statusIcon.visibility = View.VISIBLE
                        binding.statusIcon.setImageResource(R.drawable.ic_error_outline)
                        binding.statusText.text = "No results found"
                        binding.statusSubtext.text = "Try adjusting your search terms or metadata type"
                        binding.statusText.visibility = View.VISIBLE
                        binding.statusSubtext.visibility = View.VISIBLE
                        binding.statusContainer.visibility = View.VISIBLE
                    } else {
                        binding.statusContainer.visibility = View.GONE
                    }
                }
                is SearchState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.statusIcon.visibility = View.VISIBLE
                    binding.statusIcon.setImageResource(R.drawable.ic_error_outline)
                    binding.statusText.text = "Search Error"
                    binding.statusSubtext.text = state.message
                    binding.statusText.visibility = View.VISIBLE
                    binding.statusSubtext.visibility = View.VISIBLE
                    binding.statusContainer.visibility = View.VISIBLE
                }
            }
        }

        viewModel.humanDiseaseResults.observe(viewLifecycleOwner) {
            humanDiseaseAdapter.submitList(it)
        }

        viewModel.subcellularLocationResults.observe(viewLifecycleOwner) {
            subcellularLocationAdapter.submitList(it)
        }

        viewModel.tissueResults.observe(viewLifecycleOwner) {
            tissueAdapter.submitList(it)
        }

        viewModel.msUniqueVocabulariesResults.observe(viewLifecycleOwner) {
            msUniqueVocabulariesAdapter.submitList(it)
        }

        viewModel.speciesResults.observe(viewLifecycleOwner) {
            speciesAdapter.submitList(it)
        }

        viewModel.unimodResults.observe(viewLifecycleOwner) {
            unimodAdapter.submitList(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel()
        _binding = null
    }
}