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
import info.proteo.cupcake.SessionActivity
import info.proteo.cupcake.TimeKeeperActivity
import info.proteo.cupcake.data.local.entity.protocol.RecentSessionEntity
import info.proteo.cupcake.shared.data.model.message.MessageThread
import info.proteo.cupcake.shared.data.model.protocol.TimeKeeper
import info.proteo.cupcake.databinding.FragmentMainBinding
import info.proteo.cupcake.ui.message.MessageThreadAdapter
import info.proteo.cupcake.ui.session.RecentSessionAdapter
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
    private lateinit var recentSessionAdapter: RecentSessionAdapter

    private fun setupRecentSessionsSection() {
        recentSessionAdapter = RecentSessionAdapter { recentSession ->
            val intent = Intent(requireContext(), SessionActivity::class.java)
            intent.putExtra("sessionId", recentSession.sessionUniqueId)
            intent.putExtra("protocolId", recentSession.protocolId)
            intent.putExtra("isNewSession", false)
            startActivity(intent)
        }

        binding.recyclerViewRecentSessions.apply {
            adapter = recentSessionAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }





    private fun updateRecentSessionsUI(sessions: List<RecentSessionEntity>) {
        recentSessionAdapter.submitList(sessions)

        if (viewModel.userData.value != null) {
            binding.recentSessionsSection.visibility = View.VISIBLE

            if (sessions.isEmpty()) {
                binding.recyclerViewRecentSessions.visibility = View.GONE
                binding.emptySessionsState.visibility = View.VISIBLE
            } else {
                binding.recyclerViewRecentSessions.visibility = View.VISIBLE
                binding.emptySessionsState.visibility = View.GONE
            }
        } else {
            binding.recentSessionsSection.visibility = View.GONE
        }
    }

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
        setupRecentSessionsSection()
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
                binding.loginCard.visibility = View.GONE

                binding.activeTimekeepersSection.visibility = View.VISIBLE
                binding.textViewRecentMessages.visibility = View.VISIBLE
                binding.textViewViewAll.visibility = View.VISIBLE
                binding.recyclerViewThreads.visibility = View.VISIBLE


            } else {
                binding.textViewUsername.text = ""
                binding.loginCard.visibility = View.VISIBLE

                binding.activeTimekeepersSection.visibility = View.GONE
                binding.textViewRecentMessages.visibility = View.GONE
                binding.textViewViewAll.visibility = View.GONE
                binding.recyclerViewThreads.visibility = View.GONE
                threadAdapter.submitList(emptyList())

                binding.emptyMessagesState.visibility = View.VISIBLE
                binding.loadingMessagesState.visibility = View.GONE
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
                        binding.loadingMessagesState.visibility = if (isLoading) View.VISIBLE else View.GONE

                        if (!isLoading && viewModel.messageThreads.value.isEmpty()) {
                            updateMessageThreadsUI(viewModel.messageThreads.value)
                        }
                    } else {
                        binding.loadingMessagesState.visibility = View.GONE
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.recentSessions.collect { sessions ->
                    updateRecentSessionsUI(sessions)
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
            binding.emptyMessagesState.visibility = if (threads.isEmpty() && !viewModel.isLoading.value) {
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