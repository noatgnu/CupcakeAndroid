package info.proteo.cupcake.ui.main

import android.content.Intent
import android.os.Bundle
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
import info.proteo.cupcake.LoginActivity
import info.proteo.cupcake.R
import info.proteo.cupcake.TimeKeeperActivity
import info.proteo.cupcake.data.remote.model.message.MessageThread
import info.proteo.cupcake.data.remote.model.protocol.TimeKeeper
import info.proteo.cupcake.databinding.FragmentMainBinding
import info.proteo.cupcake.ui.message.MessageThreadAdapter
import info.proteo.cupcake.ui.timekeeper.ActiveTimeKeeperPreviewAdapter
import kotlinx.coroutines.launch
import kotlin.jvm.java

@AndroidEntryPoint
class MainFragment : Fragment() {
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by viewModels()
    private lateinit var threadAdapter: MessageThreadAdapter
    private lateinit var activeTimekeeperAdapter: ActiveTimeKeeperPreviewAdapter


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
        setupActiveTimekeepersSection()
        setupObservers()
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadUserData()
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

    private fun setupActiveTimekeepersSection() {
        activeTimekeeperAdapter = ActiveTimeKeeperPreviewAdapter { timekeeper ->
            val intent = Intent(requireContext(), TimeKeeperActivity::class.java)
            intent.putExtra("timeKeeperId", timekeeper.id)
            startActivity(intent)
        }

        binding.recyclerViewActiveTimekeepers.apply {
            adapter = activeTimekeeperAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        binding.textViewViewAllTimekeepers.setOnClickListener {
            val intent = Intent(requireContext(), TimeKeeperActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupObservers() {
        viewModel.userData.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                binding.textViewUsername.text = user.username
                binding.buttonLogin.visibility = View.GONE

                binding.activeTimekeepersSection.visibility = View.VISIBLE
                binding.textViewRecentMessages.visibility = View.VISIBLE
                binding.textViewViewAll.visibility = View.VISIBLE
                binding.recyclerViewThreads.visibility = View.VISIBLE


            } else {
                binding.textViewUsername.text = ""
                binding.buttonLogin.visibility = View.VISIBLE

                binding.activeTimekeepersSection.visibility = View.GONE
                binding.textViewRecentMessages.visibility = View.GONE
                binding.textViewViewAll.visibility = View.GONE
                binding.recyclerViewThreads.visibility = View.GONE
                threadAdapter.submitList(emptyList())

                binding.textViewEmptyState.text = getString(R.string.please_log_in)
                binding.textViewEmptyState.visibility = View.VISIBLE
                binding.progressBar.visibility = View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.messageThreads.collect { threads ->
                    if (viewModel.userData.value != null) {
                        updateMessageThreadsUI(threads)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.activeTimekeepers.collect { timekeepers ->
                    updateActiveTimekeepersUI(timekeepers)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.activeTimerStates.collect { timerStates ->
                    activeTimekeeperAdapter.updateActiveTimers(timerStates)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.activeTimekeepersCount.collect { count ->
                    binding.textViewActiveCount.text = "$count active timekeepers"
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { isLoading ->
                    if (viewModel.userData.value != null) {
                        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE

                        if (!isLoading && viewModel.messageThreads.value.isEmpty()) {
                            updateMessageThreadsUI(viewModel.messageThreads.value)
                        }
                    } else {
                        binding.progressBar.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.textViewViewAll.setOnClickListener {
            if (viewModel.userData.value != null) {
                findNavController().navigate(R.id.action_mainFragment_to_messageActivity)
            }
        }
        binding.buttonLogin.setOnClickListener {
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
        }
    }

    private fun updateMessageThreadsUI(threads: List<MessageThread>) {
        threadAdapter.submitList(threads)
        if (viewModel.userData.value != null) {
            binding.textViewEmptyState.text = getString(R.string.no_messages_yet)
            binding.textViewEmptyState.visibility = if (threads.isEmpty() && !viewModel.isLoading.value) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    private fun navigateToMessageDetail(thread: MessageThread) {
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

    private fun updateActiveTimekeepersUI(timekeepers: List<TimeKeeper>) {
        activeTimekeeperAdapter.submitList(timekeepers)

        if (viewModel.userData.value != null) {
            binding.activeTimekeepersSection.visibility = View.VISIBLE

            if (timekeepers.isEmpty() && !viewModel.isLoading.value) {
                binding.textViewActiveCount.text = "No active timekeepers"
                binding.recyclerViewActiveTimekeepers.visibility = View.GONE
            } else {
                binding.textViewActiveCount.text = "${viewModel.activeTimekeepersCount.value} active timekeepers"
                binding.recyclerViewActiveTimekeepers.visibility = View.VISIBLE
            }
        } else {
            binding.activeTimekeepersSection.visibility = View.GONE
        }
    }
}