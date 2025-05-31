package info.proteo.cupcake.ui.instrument

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.ExternalContactActivity
import info.proteo.cupcake.data.repository.StorageRepository
import info.proteo.cupcake.data.repository.SupportInformationRepository
import info.proteo.cupcake.databinding.FragmentSupportInformationBinding
import javax.inject.Inject

@AndroidEntryPoint
class SupportInformationFragment : Fragment() {
    private var _binding: FragmentSupportInformationBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SupportInformationViewModel by viewModels()
    private lateinit var adapter: SupportInformationAdapter
    private var instrumentId: Int = -1

    @Inject
    lateinit var supportInformationRepository: SupportInformationRepository

    @Inject
    lateinit var storageObjectRepository: StorageRepository


    companion object {
        fun newInstance(instrumentId: Int): SupportInformationFragment {
            val fragment = SupportInformationFragment()
            val args = Bundle().apply {
                putInt("INSTRUMENT_ID", instrumentId)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instrumentId = arguments?.getInt("INSTRUMENT_ID", -1) ?: -1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSupportInformationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()

        if (instrumentId != -1) {
            viewModel.loadSupportInformation(instrumentId)
        } else {
            Snackbar.make(binding.root, "Invalid instrument ID", Snackbar.LENGTH_LONG).show()
            requireActivity().finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = SupportInformationAdapter(
            onItemClick = { supportInfo ->
                val intent = Intent(requireContext(), ExternalContactActivity::class.java).apply {
                    putExtra("SUPPORT_INFO_ID", supportInfo.id)
                }
                Log.d("SupportInfoFragment", "Navigating to ExternalContactActivity with ID: ${supportInfo.id}")
                Log.d("SupportInfoFragment", "Intent: $intent")
                startActivity(intent)
            },
            supportInformationRepository = supportInformationRepository,
            storageObjectRepository = storageObjectRepository
        )

        binding.recyclerViewSupportInfo.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@SupportInformationFragment.adapter
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
        }

        viewModel.supportInformation.observe(viewLifecycleOwner) { result ->
            result.fold(
                onSuccess = { supportInfoList ->
                    adapter.submitList(supportInfoList)
                    binding.emptyView.isVisible = supportInfoList.isEmpty()
                    binding.recyclerViewSupportInfo.isVisible = supportInfoList.isNotEmpty()
                },
                onFailure = { error ->
                    Snackbar.make(
                        binding.root,
                        "Error loading support information: ${error.message}",
                        Snackbar.LENGTH_LONG
                    ).show()
                    binding.emptyView.isVisible = true
                    binding.recyclerViewSupportInfo.isVisible = false
                }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}