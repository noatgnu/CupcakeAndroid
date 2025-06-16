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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class InstrumentFragment : Fragment(), InstrumentActivity.SearchQueryListener {

    private var _binding: FragmentInstrumentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InstrumentViewModel by viewModels()
    private lateinit var instrumentAdapter: InstrumentAdapter

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
        observeInstruments()
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

    private fun observeInstruments() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.instruments.collectLatest { result ->
                    binding.swipeRefreshLayout.isRefreshing = false

                    result.onSuccess { response ->
                        instrumentAdapter.submitList(response.results)
                        binding.emptyView.visibility =
                            if (response.results.isEmpty()) View.VISIBLE else View.GONE
                    }.onFailure { error ->
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