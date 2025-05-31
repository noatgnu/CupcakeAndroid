package info.proteo.cupcake.ui.reagent

import android.content.Intent
import androidx.fragment.app.viewModels
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.R
import info.proteo.cupcake.StoredReagentActivity
import info.proteo.cupcake.databinding.FragmentStoredReagentBinding
import info.proteo.cupcake.getThemeColor
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.io.use

@AndroidEntryPoint
class StoredReagentFragment : Fragment() {

    private var _binding: FragmentStoredReagentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StoredReagentViewModel by viewModels()
    private lateinit var storedReagentAdapter: StoredReagentAdapter
    private var isLoading = false
    private val PAGE_SIZE = 20

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStoredReagentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().supportFragmentManager.setFragmentResultListener(
            "barcode_result", viewLifecycleOwner
        ) { _, bundle ->
            val barcode = bundle.getString("barcode")
            val storageObjectId = bundle.getInt("storage_object_id", -1)
            barcode?.let {
                viewModel.searchByBarcode(it, storageObjectId.takeIf { id -> id != -1 })
            }
        }

        // Add observer for storage object to update Activity title
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.storageObject.collect { storageObject ->
                    storageObject?.let {
                        activity?.title = "Reagents in ${it.objectName}"
                        (activity as? StoredReagentActivity)?.updateLocation(it.objectName)
                    }
                }
            }
        }
        setupRecyclerView()
        setupObservers()
        setupSwipeRefresh()

        arguments?.let { args ->
            val storageObjectId = args.getInt("STORAGE_OBJECT_ID", -1)
            Log.d("StoredReagentFragment", "Got storageObjectId: $storageObjectId")

            val searchTerm = activity?.intent?.getStringExtra(StoredReagentActivity.EXTRA_SEARCH_TERM)
            Log.d("StoredReagentFragment", "Search term from intent: $searchTerm")

            if (!searchTerm.isNullOrEmpty()) {
                val searchId = if (storageObjectId != -1) storageObjectId else null
                Log.d("StoredReagentFragment", "Searching for term: $searchTerm in location: $searchId")
                viewModel.searchByTerm(searchTerm, searchId)
            } else if (storageObjectId != -1) {
                viewModel.loadStoredReagents(storageObjectId)
                viewModel.loadStorageObjectInfo(storageObjectId)
            } else {
                viewModel.loadStoredReagents(-1)
            }
        } ?: run {
            Log.e("StoredReagentFragment", "No arguments provided")
            showError("No location specified")
        }
    }


    private fun navigateToStoredReagentDetail(reagentId: Int) {
        val fragment = StoredReagentDetailFragment().apply {
            arguments = Bundle().apply {
                putInt("REAGENT_ID", reagentId)
            }
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun setupRecyclerView() {
        storedReagentAdapter = StoredReagentAdapter { storedReagent ->
            navigateToStoredReagentDetail(storedReagent.id)
        }
        binding.recyclerViewReagents.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = storedReagentAdapter
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                    if (!isLoading && (visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5
                        && firstVisibleItemPosition >= 0
                        && totalItemCount >= PAGE_SIZE
                    ) {
                        arguments?.getInt("STORAGE_OBJECT_ID", -1)?.let { storageObjectId ->
                            if (storageObjectId != -1) {
                                viewModel.loadMoreStoredReagents()
                            }
                        }
                    }
                }
            })
        }
    }

    private fun setupObservers() {
        // Use repeatOnLifecycle for proper lifecycle management
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                Log.d("StoredReagentFragment", "Starting reagentsState collection")
                viewModel.reagentsState.collect { state ->
                    Log.d("StoredReagentFragment", "State received: $state (${state.javaClass.simpleName})")
                    try {
                        when (state) {
                            is StoredReagentUiState.Loading -> {
                                Log.d("StoredReagentFragment", "Processing Loading state")
                                binding.progressBar.visibility = View.VISIBLE
                                binding.recyclerViewReagents.visibility = View.GONE
                                binding.textViewEmpty.visibility = View.GONE
                                binding.textViewError.visibility = View.GONE
                            }
                            is StoredReagentUiState.Success -> {
                                Log.d("StoredReagentFragment", "Processing Success state with ${state.reagents.size} items")
                                binding.progressBar.visibility = View.GONE
                                binding.textViewEmpty.visibility = View.GONE
                                binding.textViewError.visibility = View.GONE
                                binding.recyclerViewReagents.visibility = View.VISIBLE

                                // Force update to ensure rendering
                                storedReagentAdapter.submitList(null)
                                storedReagentAdapter.submitList(state.reagents)

                                binding.swipeRefresh.isRefreshing = false
                            }
                            is StoredReagentUiState.Empty -> {
                                Log.d("StoredReagentFragment", "Processing Empty state")
                                binding.progressBar.visibility = View.GONE
                                binding.recyclerViewReagents.visibility = View.GONE
                                binding.textViewEmpty.visibility = View.VISIBLE
                                binding.textViewError.visibility = View.GONE
                                binding.textViewEmpty.text = getString(R.string.no_reagents_found)
                                binding.textViewEmpty.setTextColor(requireContext().getThemeColor(R.attr.colorEmptyState))
                                binding.swipeRefresh.isRefreshing = false
                            }
                            is StoredReagentUiState.NoSearchResults -> {
                                Log.d("StoredReagentFragment", "Processing NoSearchResults state: ${state.searchTerm}")
                                binding.progressBar.visibility = View.GONE
                                binding.recyclerViewReagents.visibility = View.GONE
                                binding.textViewEmpty.visibility = View.VISIBLE
                                binding.textViewError.visibility = View.GONE
                                binding.textViewEmpty.text = "No results found for barcode: '${state.searchTerm}'"
                                binding.textViewEmpty.setTextColor(requireContext().getThemeColor(R.attr.colorError))
                                binding.swipeRefresh.isRefreshing = false
                            }
                            is StoredReagentUiState.Error -> {
                                Log.d("StoredReagentFragment", "Processing Error state: ${state.message}")
                                showError(state.message)
                                binding.swipeRefresh.isRefreshing = false
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("StoredReagentFragment", "Error processing state: $state", e)
                    }
                }
            }
        }

        // Storage object observer
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.storageObject.collect { storageObject ->
                    Log.d("StoredReagentFragment", "Storage object updated: $storageObject")
                    storageObject?.let {
                        activity?.title = "Reagents in ${it.objectName}"
                    }
                }
            }
        }

        // Loading more observer
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoadingMore.collect { loading ->
                    isLoading = loading
                    binding.progressBarLoadMore.visibility = if (loading) View.VISIBLE else View.GONE
                }
            }
        }

        // Search indicator observer
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isSearchingByBarcode.collect { isSearching ->
                    binding.searchIndicator.visibility = if (isSearching) View.VISIBLE else View.GONE
                    if (isSearching) {
                        val barcode = viewModel.currentBarcodeState.value ?: ""
                        binding.barcodeText.text = "Searching: $barcode"
                    }
                }
            }
        }

        // Current barcode observer
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentBarcodeState.collect { barcode ->
                    if (!barcode.isNullOrEmpty()) {
                        binding.barcodeText.text = "Searching: $barcode"
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.locationPaths.collect { paths ->
                    storedReagentAdapter.updateLocationPaths(paths)
                }
            }
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            arguments?.getInt("STORAGE_OBJECT_ID", -1)?.let { storageObjectId ->
                if (storageObjectId != -1) {
                    viewModel.refresh(storageObjectId)
                } else {
                    binding.swipeRefresh.isRefreshing = false
                }
            }
        }
    }

    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.recyclerViewReagents.visibility = View.GONE
        binding.textViewEmpty.visibility = View.GONE
        binding.textViewError.visibility = View.VISIBLE
        binding.textViewError.text = message
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}