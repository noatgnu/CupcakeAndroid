package info.proteo.cupcake

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.R
import info.proteo.cupcake.shared.data.model.protocol.TimeKeeper
import info.proteo.cupcake.databinding.ActivityTimeKeeperBinding
import info.proteo.cupcake.databinding.DialogAddTimeKeeperBinding
import info.proteo.cupcake.ui.timekeeper.TimeKeeperAdapter
import info.proteo.cupcake.ui.timekeeper.TimeKeeperViewModel
import info.proteo.cupcake.ui.timekeeper.TimeKeeperDisplayItem
import info.proteo.cupcake.data.repository.SessionRepository
import info.proteo.cupcake.data.repository.ProtocolStepRepository
import info.proteo.cupcake.data.local.dao.protocol.SessionDao
import javax.inject.Inject
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TimeKeeperActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTimeKeeperBinding
    private val viewModel: TimeKeeperViewModel by viewModels()
    private lateinit var adapter: TimeKeeperAdapter
    
    @Inject
    lateinit var sessionRepository: SessionRepository
    
    @Inject
    lateinit var protocolStepRepository: ProtocolStepRepository
    
    @Inject
    lateinit var sessionDao: SessionDao
    
    private var cachedDisplayItems: List<TimeKeeperDisplayItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set up edge-to-edge with transparent status bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        
        binding = ActivityTimeKeeperBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupStatusBarBackground()

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Timekeepers"
        
        // Set toolbar colors
        binding.toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white))
        binding.toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, R.color.white))
        binding.toolbar.overflowIcon?.setTint(ContextCompat.getColor(this, R.color.white))

        setupRecyclerView()
        setupObservers()
        setupClickListeners()

        viewModel.loadTimeKeepers()
    }

    private fun setupRecyclerView() {
        adapter = TimeKeeperAdapter(
            onItemClick = { timeKeeper -> showTimeKeeperOptions(timeKeeper) },
            onStartClick = { timeKeeper ->
                val initialDuration = timeKeeper.currentDuration ?: 30
                viewModel.startTimer(timeKeeper.id, timeKeeper.step, initialDuration)
            },
            onPauseClick = { timeKeeper -> viewModel.pauseTimer(timeKeeper.id) },
            onResetClick = { timeKeeper ->
                val initialDuration = timeKeeper.currentDuration ?: 30
                viewModel.resetTimer(timeKeeper.id, initialDuration)
            },
        )

        binding.recyclerViewTimeKeepers.apply {
            adapter = this@TimeKeeperActivity.adapter
            layoutManager = LinearLayoutManager(this@TimeKeeperActivity)
            addItemDecoration(DividerItemDecoration(this@TimeKeeperActivity, DividerItemDecoration.VERTICAL))
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.timeKeepers.collectLatest { timeKeepers ->
                loadDisplayItems(timeKeepers)
                binding.textViewEmpty.visibility = if (timeKeepers.isEmpty() && !viewModel.isLoading.value) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        lifecycleScope.launch {
            viewModel.error.collectLatest { errorMessage ->
                if (errorMessage != null) {
                    Toast.makeText(this@TimeKeeperActivity, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
        }

        // Observe activeTimers for real-time countdown updates
        lifecycleScope.launch {
            viewModel.activeTimers.collectLatest { activeTimers ->
                // Update only timer states without refetching session/step data
                updateTimerStates(activeTimers)
            }
        }
    }

    private suspend fun loadDisplayItems(timeKeepers: List<TimeKeeper>) {
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

    private fun setupClickListeners() {
        binding.fabAddTimeKeeper.setOnClickListener {
            showAddTimeKeeperDialog()
        }
    }

    private fun showAddTimeKeeperDialog() {
        val dialogBinding = DialogAddTimeKeeperBinding.inflate(layoutInflater)

        MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("Add") { dialog, _ ->
                val sessionIdText = dialogBinding.editTextSession.text.toString()
                val stepIdText = dialogBinding.editTextStep.text.toString()
                val durationText = dialogBinding.editTextDuration.text.toString()

                try {
                    val sessionId = if (sessionIdText.isBlank()) null else sessionIdText.toInt()
                    val stepId = if (stepIdText.isBlank()) null else stepIdText.toInt()
                    val duration = if (durationText.isBlank()) null else durationText.toInt()
                    val started = dialogBinding.switchStarted.isChecked
                    val durationInSeconds = duration?.times(60) ?: 30 * 60
                    viewModel.createTimeKeeper(sessionId, stepId, started, durationInSeconds)
                    dialog.dismiss()
                } catch (e: NumberFormatException) {
                    Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showTimeKeeperOptions(timeKeeper: TimeKeeper) {
        val options = arrayOf("Delete", "Cancel")

        AlertDialog.Builder(this)
            .setTitle("Timekeeper Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> confirmDelete(timeKeeper)
                }
            }
            .show()
    }

    private fun confirmDelete(timeKeeper: TimeKeeper) {
        AlertDialog.Builder(this)
            .setTitle("Delete Timekeeper")
            .setMessage("Are you sure you want to delete this timekeeper?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteTimeKeeper(timeKeeper.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupStatusBarBackground() {
        // Get the actual resolved color from the theme
        val typedArray = theme.obtainStyledAttributes(intArrayOf(
            android.R.attr.colorPrimary
        ))
        val resolvedColor = typedArray.getColor(0, ContextCompat.getColor(this, R.color.primary))
        typedArray.recycle()
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Extend the toolbar to cover the status bar area
            binding.toolbar.let { toolbar ->
                // Set the toolbar background color
                toolbar.setBackgroundColor(resolvedColor)
                toolbar.elevation = 0f
                toolbar.alpha = 1.0f
                
                // Extend toolbar height to include status bar
                val toolbarParams = toolbar.layoutParams
                val typedValue = android.util.TypedValue()
                theme.resolveAttribute(android.R.attr.actionBarSize, typedValue, true)
                val actionBarHeight = resources.getDimensionPixelSize(typedValue.resourceId)
                toolbarParams.height = actionBarHeight + systemBars.top
                toolbar.layoutParams = toolbarParams
                
                // Add top padding to toolbar content so it appears below status bar
                toolbar.setPadding(
                    toolbar.paddingLeft,
                    systemBars.top,
                    toolbar.paddingRight,
                    toolbar.paddingBottom
                )
            }
            
            windowInsets
        }
        
        // Set appropriate status bar appearance for both themes
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = false
    }
}