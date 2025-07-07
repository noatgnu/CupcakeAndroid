package info.proteo.cupcake.ui.timekeeper

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.databinding.FragmentTimeKeeperBinding
import info.proteo.cupcake.data.repository.SessionRepository
import info.proteo.cupcake.data.repository.ProtocolStepRepository
import info.proteo.cupcake.data.local.dao.protocol.SessionDao
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TimeKeeperFragment : Fragment() {

    companion object {
        fun newInstance() = TimeKeeperFragment()
    }

    private var _binding: FragmentTimeKeeperBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TimeKeeperViewModel by viewModels()
    
    @Inject
    lateinit var sessionRepository: SessionRepository
    
    @Inject
    lateinit var protocolStepRepository: ProtocolStepRepository
    
    @Inject
    lateinit var sessionDao: SessionDao

    private lateinit var adapter: TimeKeeperAdapter
    private var cachedDisplayItems: List<TimeKeeperDisplayItem> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTimeKeeperBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        observeViewModel()
        
        // Load initial data
        viewModel.loadTimeKeepers()
    }

    private fun setupRecyclerView() {
        adapter = TimeKeeperAdapter(
            onItemClick = { timeKeeper ->
                // Handle item click if needed
            },
            onStartClick = { timeKeeper ->
                val initialDuration = timeKeeper.currentDuration ?: 0
                viewModel.startTimer(timeKeeper.id, timeKeeper.step, initialDuration)
            },
            onPauseClick = { timeKeeper ->
                viewModel.pauseTimer(timeKeeper.id)
            },
            onResetClick = { timeKeeper ->
                val initialDuration = timeKeeper.currentDuration ?: 0
                viewModel.resetTimer(timeKeeper.id, initialDuration)
            }
        )

        binding.timersRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@TimeKeeperFragment.adapter
        }
    }

    private fun observeViewModel() {
        // Observe timeKeepers and activeTimers to create display items
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.timeKeepers.collect { timeKeepers ->
                if (timeKeepers.isEmpty()) {
                    binding.emptyState.visibility = View.VISIBLE
                    binding.timersRecyclerView.visibility = View.GONE
                    binding.statusMessage.text = "No active timers"
                } else {
                    binding.emptyState.visibility = View.GONE
                    binding.timersRecyclerView.visibility = View.VISIBLE
                    binding.statusMessage.text = "${timeKeepers.size} timer(s) available"
                    
                    // Convert to display items with session and step info
                    loadDisplayItems(timeKeepers)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { error ->
                if (error != null) {
                    binding.statusMessage.text = "Error: $error"
                }
            }
        }

        // Observe activeTimers for real-time countdown updates
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.activeTimers.collect { activeTimers ->
                // Update only timer states without refetching session/step data
                updateTimerStates(activeTimers)
            }
        }
    }

    private suspend fun loadDisplayItems(timeKeepers: List<info.proteo.cupcake.shared.data.model.protocol.TimeKeeper>) {
        val displayItems = timeKeepers.map { timeKeeper ->
            val session = timeKeeper.session?.let { sessionId ->
                try {
                    // Always fetch fresh data from server first
                    val sessionsResult = sessionRepository.getUserSessions(limit = 100)
                    val freshSession = sessionsResult.getOrNull()?.results?.find { it.id == sessionId }
                    
                    if (freshSession != null) {
                        freshSession
                    } else {
                        // Only fallback to cache if server fetch fails
                        val cachedSession = sessionDao.getById(sessionId)
                        cachedSession?.let { sessionEntity ->
                            info.proteo.cupcake.shared.data.model.protocol.Session(
                                id = sessionEntity.id,
                                user = sessionEntity.user,
                                uniqueId = sessionEntity.uniqueId,
                                enabled = sessionEntity.enabled,
                                name = sessionEntity.name,
                                timeKeeper = emptyList(),
                                startedAt = sessionEntity.startedAt,
                                endedAt = sessionEntity.endedAt,
                                protocols = emptyList(),
                                projects = emptyList(),
                                createdAt = sessionEntity.createdAt,
                                updatedAt = sessionEntity.updatedAt
                            )
                        }
                    }
                } catch (e: Exception) {
                    // If everything fails, try cache as last resort
                    try {
                        val cachedSession = sessionDao.getById(sessionId)
                        cachedSession?.let { sessionEntity ->
                            info.proteo.cupcake.shared.data.model.protocol.Session(
                                id = sessionEntity.id,
                                user = sessionEntity.user,
                                uniqueId = sessionEntity.uniqueId,
                                enabled = sessionEntity.enabled,
                                name = sessionEntity.name,
                                timeKeeper = emptyList(),
                                startedAt = sessionEntity.startedAt,
                                endedAt = sessionEntity.endedAt,
                                protocols = emptyList(),
                                projects = emptyList(),
                                createdAt = sessionEntity.createdAt,
                                updatedAt = sessionEntity.updatedAt
                            )
                        }
                    } catch (cacheException: Exception) {
                        null
                    }
                }
            }
            
            val step = timeKeeper.step?.let { stepId ->
                try {
                    protocolStepRepository.getProtocolStepById(stepId).getOrNull()
                } catch (e: Exception) {
                    null
                }
            }
            
            val timerState = viewModel.activeTimers.value[timeKeeper.id]
            
            TimeKeeperDisplayItem(
                timeKeeper = timeKeeper,
                session = session,
                step = step,
                timerState = timerState
            )
        }
        
        cachedDisplayItems = displayItems
        adapter.submitList(displayItems)
    }

    private fun updateTimerStates(activeTimers: Map<Int, TimeKeeperViewModel.TimerState>) {
        // Update cached display items with new timer states
        val updatedDisplayItems = cachedDisplayItems.map { displayItem ->
            val updatedTimerState = activeTimers[displayItem.timeKeeper.id]
            displayItem.copy(timerState = updatedTimerState)
        }
        
        cachedDisplayItems = updatedDisplayItems
        adapter.submitList(updatedDisplayItems)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}