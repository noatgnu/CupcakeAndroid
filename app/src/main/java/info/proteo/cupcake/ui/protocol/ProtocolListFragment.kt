package info.proteo.cupcake.ui.protocol

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.R
import info.proteo.cupcake.SessionActivity
import info.proteo.cupcake.databinding.FragmentProtocolListBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProtocolListFragment : Fragment() {
    private var _binding: FragmentProtocolListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProtocolListViewModel by viewModels()
    private lateinit var protocolAdapter: ProtocolAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProtocolListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        observeViewModel()
        viewModel.loadInitialProtocols()
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.protocol_list_menu, menu)
                val searchItem = menu.findItem(R.id.action_search)
                val searchView = searchItem?.actionView as? SearchView

                searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        viewModel.searchProtocols(query)
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        if (newText.isNullOrBlank()) {
                            viewModel.searchProtocols(null)
                        }
                        return true
                    }
                })
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    android.R.id.home -> {
                        activity?.onBackPressedDispatcher?.onBackPressed()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupToolbar() {
        (activity as? AppCompatActivity)?.let {
            it.setSupportActionBar(binding.toolbar)
            it.supportActionBar?.title = "Protocols"
            it.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }
    }



    private fun setupRecyclerView() {
        protocolAdapter = ProtocolAdapter(
            onProtocolClick = { protocol ->
                try {
                    val bundle = Bundle().apply {
                        putInt("protocolId", protocol.id)
                    }
                    findNavController().navigate(
                        R.id.action_protocolListFragment_to_protocolDetailFragment,
                        bundle
                    )
                } catch (e: Exception) {
                    Log.e("ProtocolListFragment", "Navigation error", e)
                }
            },
            onSessionClick = { session ->
                // Handle session click - navigate to session activity
                try {
                    val intent = Intent(requireContext(), SessionActivity::class.java)
                    intent.putExtra("sessionId", session.uniqueId)
                    intent.putExtra("isNewSession", false)
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("ProtocolListFragment", "Session navigation error", e)
                }
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = protocolAdapter

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItem = layoutManager.findFirstVisibleItemPosition()

                    if (!viewModel.isLoading.value && !viewModel.isLastPage.value &&
                        (visibleItemCount + firstVisibleItem) >= totalItemCount - 5) {
                        viewModel.loadMoreProtocols()
                    }
                }
            })
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
                if (isLoading) {
                    // When loading, always hide the empty view
                    binding.emptyView.visibility = View.GONE
                } else {
                    binding.emptyView.visibility = if (viewModel.protocols.value.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }

        lifecycleScope.launch {
            viewModel.protocols.collectLatest { protocols ->
                protocolAdapter.submitList(protocols)
                if (!viewModel.isLoading.value) {
                    binding.emptyView.visibility = if (protocols.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}