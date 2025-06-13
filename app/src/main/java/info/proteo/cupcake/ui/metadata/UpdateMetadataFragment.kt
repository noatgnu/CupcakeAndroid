package info.proteo.cupcake.ui.metadata

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.R
import info.proteo.cupcake.databinding.FragmentUpdateMetadataBinding

@AndroidEntryPoint
class UpdateMetadataFragment : Fragment() {

    private var _binding: FragmentUpdateMetadataBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: UpdateMetadataViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUpdateMetadataBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[UpdateMetadataViewModel::class.java]

        setupButtons()
        observeViewModelState()
    }

    private fun setupButtons() {
        binding.updateHumanDiseaseButton.setOnClickListener {
            viewModel.updateHumanDiseaseData()
        }

        binding.updateSubcellularLocationButton.setOnClickListener {
            viewModel.updateSubcellularLocationData()
        }

        binding.updateUnimodButton.setOnClickListener {
            viewModel.updateUnimodData()
        }
        binding.updatemsUniqueVocabulariesButton.setOnClickListener {
            viewModel.updateMSUniqueVocabulariesData()
        }

        binding.updateSpeciesButton.setOnClickListener {
            viewModel.updateSpeciesData()
        }
        binding.updateTissueButton.setOnClickListener {
            viewModel.updateTissueData()
        }
    }

    private fun observeViewModelState() {
        viewModel.humanDiseaseUpdateState.observe(viewLifecycleOwner) { state ->
            updateUiState(
                state,
                binding.humanDiseaseProgressBar,
                binding.humanDiseaseStatus,
                binding.updateHumanDiseaseButton
            )
        }

        viewModel.subcellularLocationUpdateState.observe(viewLifecycleOwner) { state ->
            updateUiState(
                state,
                binding.subcellularProgressBar,
                binding.subcellularStatus,
                binding.updateSubcellularLocationButton
            )
        }

        viewModel.unimodUpdateState.observe(viewLifecycleOwner) { state ->
            updateUiState(
                state,
                binding.unimodProgressBar,
                binding.unimodStatus,
                binding.updateUnimodButton
            )
        }
        viewModel.msUniqueVocabulariesUpdateState.observe (viewLifecycleOwner) { state ->
            updateUiState(
                state,
                binding.msUniqueVocabulariesProgressBar,
                binding.msUniqueVocabulariesStatus,
                binding.updatemsUniqueVocabulariesButton
            )
        }
        viewModel.speciesUpdateState.observe(viewLifecycleOwner) { state ->
            updateUiState(
                state,
                binding.speciesProgressBar,
                binding.speciesStatus,
                binding.updateSpeciesButton
            )
        }
        viewModel.tissueUpdateState.observe(viewLifecycleOwner) { state ->
            updateUiState(
                state,
                binding.tissueProgressBar,
                binding.tissueStatus,
                binding.updateTissueButton
            )
        }
    }

    private fun updateUiState(
        state: UpdateMetadataViewModel.UpdateState,
        progressBar: ProgressBar,
        statusTextView: TextView,
        button: Button
    ) {
        when (state) {
            is UpdateMetadataViewModel.UpdateState.Idle -> {
                progressBar.visibility = View.GONE
                statusTextView.text = "Ready to update"
                button.isEnabled = true
            }
            is UpdateMetadataViewModel.UpdateState.Loading -> {
                progressBar.visibility = View.VISIBLE
                statusTextView.text = "Downloading and processing data..."
                button.isEnabled = false
            }
            is UpdateMetadataViewModel.UpdateState.Success -> {
                progressBar.visibility = View.GONE
                statusTextView.text = "Successfully updated (${state.count} entries)"
                button.isEnabled = true
            }
            is UpdateMetadataViewModel.UpdateState.Error -> {
                progressBar.visibility = View.GONE
                statusTextView.text = "Error: ${state.message}"
                button.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}