package info.proteo.cupcake.ui.metadata

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
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
        setupTypeSpinner()
        setupSearchView()
        setupTermTypeFilter()
        setupRecyclerView()
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
        ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            metadataTypes
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.metadataTypeSpinner.adapter = adapter
        }

        binding.metadataTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                updateCurrentAdapter(position)
                binding.termTypeFilterLayout.visibility =
                    if (position == 3) View.VISIBLE else View.GONE

                performSearch(binding.searchView.query.toString())
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }
    }

    private fun setupTermTypeFilter() {
        binding.termTypeRadioGroup.setOnCheckedChangeListener { _, _ ->
            performSearch(binding.searchView.query.toString())
        }
    }


    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                performSearch(query)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                performSearch(newText)
                return true
            }
        })

        binding.searchModeRadioGroup.setOnCheckedChangeListener { _, _ ->
            performSearch(binding.searchView.query.toString())
        }
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
        val metadataType = when (binding.metadataTypeSpinner.selectedItemPosition) {
            0 -> MetadataType.HUMAN_DISEASE
            1 -> MetadataType.SUBCELLULAR_LOCATION
            2 -> MetadataType.TISSUE
            3 -> MetadataType.MS_UNIQUE_VOCABULARIES
            4 -> MetadataType.SPECIES
            5 -> MetadataType.UNIMOD
            else -> MetadataType.HUMAN_DISEASE
        }

        val searchMode = if (binding.startsWithRadioButton.isChecked) {
            SearchMode.STARTS_WITH
        } else {
            SearchMode.CONTAINS
        }

        var termType = ""
        if (metadataType == MetadataType.MS_UNIQUE_VOCABULARIES) {
            termType = when (binding.termTypeRadioGroup.checkedRadioButtonId) {
                R.id.labelRadioButton -> "sample attribute"
                R.id.cleavageAgentRadioButton -> "cleavage agent"
                R.id.instrumentRadioButton -> "instrument"
                R.id.dissociationMethodRadioButton -> "dissociation method"
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
                    binding.statusText.text = "Enter search terms"
                    binding.statusText.visibility = View.VISIBLE
                }
                is SearchState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.statusText.visibility = View.GONE
                }
                is SearchState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    if (state.count == 0) {
                        binding.statusText.text = "No results found"
                        binding.statusText.visibility = View.VISIBLE
                    } else {
                        binding.statusText.visibility = View.GONE
                    }
                }
                is SearchState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.statusText.text = "Error: ${state.message}"
                    binding.statusText.visibility = View.VISIBLE
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
        _binding = null
    }
}