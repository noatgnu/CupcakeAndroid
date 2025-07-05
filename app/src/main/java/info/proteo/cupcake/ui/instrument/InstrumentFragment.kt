package info.proteo.cupcake.ui.instrument

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.InstrumentActivity
import info.proteo.cupcake.databinding.FragmentInstrumentBinding
import info.proteo.cupcake.data.repository.UserRepository
import info.proteo.cupcake.data.repository.InstrumentRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class InstrumentFragment : Fragment(), InstrumentActivity.SearchQueryListener {

    private var _binding: FragmentInstrumentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InstrumentViewModel by viewModels()
    private lateinit var instrumentAdapter: InstrumentAdapter
    
    @Inject
    lateinit var userRepository: UserRepository
    
    @Inject
    lateinit var instrumentRepository: InstrumentRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInstrumentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSwipeRefresh()
        setupFilterControls()
        setupSearchFunctionality()
        setupFAB()
        observeInstruments()
        checkUserPermissions()
    }

    private fun setupRecyclerView() {
        instrumentAdapter = InstrumentAdapter { instrumentId ->
            findNavController().navigate(
                InstrumentFragmentDirections.actionInstrumentFragmentToInstrumentDetailFragment(instrumentId)
            )
        }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = instrumentAdapter

            val dividerItemDecoration = DividerItemDecoration(
                requireContext(),
                (layoutManager as LinearLayoutManager).orientation
            )
            addItemDecoration(dividerItemDecoration)

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItem = layoutManager.findFirstVisibleItemPosition()

                    if ((visibleItemCount + firstVisibleItem) >= totalItemCount - 5
                        && firstVisibleItem >= 0 && dy > 0 && viewModel.hasMoreData) {
                        viewModel.loadMoreInstruments()
                    }
                }
            })
        }
    }

    // Implement the SearchQueryListener interface
    override fun onSearchQuery(query: String?, isBarcode: Boolean) {
        Log.d("InstrumentFragment", "Search query received: $query, isBarcode: $isBarcode")
        viewModel.search(query, isBarcode)
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadInitialInstruments()
        }
    }

    private fun setupFilterControls() {
        // Handle bookable filter switch
        binding.bookableFilterSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setBookingFilter(if (isChecked) true else null)
        }
    }

    private fun setupSearchFunctionality() {
        // Handle search input
        binding.etSearchInstruments.setOnEditorActionListener { _, _, _ ->
            val query = binding.etSearchInstruments.text.toString().trim()
            viewModel.search(query.ifEmpty { null }, false)
            true
        }
    }

    private fun setupFAB() {
        // Handle FAB click for adding instruments (if user has permissions)
        binding.fabAddInstrument.setOnClickListener {
            showCreateInstrumentDialog()
        }
    }

    private fun showCreateInstrumentDialog() {
        val dialog = CreateInstrumentDialog(
            fragment = this,
            instrumentRepository = instrumentRepository,
            onInstrumentCreated = { instrument ->
                Toast.makeText(requireContext(), "Instrument '${instrument.instrumentName}' created successfully!", Toast.LENGTH_SHORT).show()
                // Refresh the instruments list
                viewModel.loadInitialInstruments()
            },
            onError = { errorMessage ->
                Toast.makeText(requireContext(), "Error: $errorMessage", Toast.LENGTH_LONG).show()
            }
        )
        dialog.show()
    }

    private fun checkUserPermissions() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val user = userRepository.getUserFromActivePreference()
                // Show FAB only if user is staff
                binding.fabAddInstrument.visibility = if (user?.isStaff == true) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            } catch (e: Exception) {
                // Hide FAB on any exception
                binding.fabAddInstrument.visibility = View.GONE
            }
        }
    }


    private fun observeInstruments() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.instruments.collectLatest { result ->
                    binding.swipeRefreshLayout.isRefreshing = false
                    binding.loadingState.visibility = View.GONE

                    result.onSuccess { response ->
                        instrumentAdapter.submitList(response.results)
                        binding.emptyView.visibility =
                            if (response.results.isEmpty()) View.VISIBLE else View.GONE
                    }.onFailure { error ->
                        binding.emptyView.visibility = View.GONE
                        Toast.makeText(requireContext(),
                            "Error loading instruments: ${error.localizedMessage}",
                            Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerView.adapter = null
        _binding = null
    }
}