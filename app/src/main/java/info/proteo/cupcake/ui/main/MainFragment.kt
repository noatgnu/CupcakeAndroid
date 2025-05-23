package info.proteo.cupcake.ui.main

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.R
import info.proteo.cupcake.data.remote.model.message.MessageThread
import info.proteo.cupcake.databinding.FragmentMainBinding
import info.proteo.cupcake.ui.message.MessageThreadAdapter
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainFragment : Fragment() {
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by viewModels()
    private lateinit var threadAdapter: MessageThreadAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMenu()
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        threadAdapter = MessageThreadAdapter { thread ->
            navigateToMessageDetail(thread)
        }

        binding.recyclerViewThreads.apply {
            adapter = threadAdapter
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }
    }

    private fun setupObservers() {
        viewModel.userData.observe(viewLifecycleOwner) { user ->
            binding.textViewUsername.text = user.username
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.messageThreads.collect { threads ->
                    updateMessageThreadsUI(threads)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { isLoading ->
                    binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                }
            }
        }

        viewModel.loadUserData()
        Log.d("MainFragment", "User data loaded: ${viewModel.userData.value?.username}")
    }

    private fun setupClickListeners() {
        binding.textViewViewAll.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_messageActivity)
        }
    }

    private fun updateMessageThreadsUI(threads: List<MessageThread>) {
        threadAdapter.submitList(threads)

        binding.textViewEmptyState.visibility = if (threads.isEmpty() && !viewModel.isLoading.value) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }


    private fun navigateToMessageDetail(thread: MessageThread) {
        // Navigate to thread detail screen with thread ID
        val bundle = Bundle().apply {
            putInt("threadId", thread.id)
        }
        findNavController().navigate(R.id.action_mainFragment_to_threadDetailFragment, bundle)
    }

    private fun setupMenu() {
        val menuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.overflow, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.nav_settings -> {
                        findNavController().navigate(R.id.action_main_to_settings)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}