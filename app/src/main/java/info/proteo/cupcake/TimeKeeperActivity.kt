package info.proteo.cupcake

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.R
import info.proteo.cupcake.data.remote.model.protocol.TimeKeeper
import info.proteo.cupcake.databinding.ActivityTimeKeeperBinding
import info.proteo.cupcake.databinding.DialogAddTimeKeeperBinding
import info.proteo.cupcake.ui.timekeeper.TimeKeeperAdapter
import info.proteo.cupcake.ui.timekeeper.TimeKeeperViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TimeKeeperActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTimeKeeperBinding
    private val viewModel: TimeKeeperViewModel by viewModels()
    private lateinit var adapter: TimeKeeperAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTimeKeeperBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Timekeepers"

        setupRecyclerView()
        setupObservers()
        setupClickListeners()

        // Load initial data
        viewModel.loadTimeKeepers()
    }

    private fun setupRecyclerView() {
        adapter = TimeKeeperAdapter(
            onItemClick = { timeKeeper -> showTimeKeeperOptions(timeKeeper) },
            onStartClick = { timeKeeper ->
                val initialDuration = timeKeeper.currentDuration ?: 30f
                viewModel.startTimer(timeKeeper.id, timeKeeper.step, initialDuration)
            },
            onPauseClick = { timeKeeper -> viewModel.pauseTimer(timeKeeper.id) },
            onResetClick = { timeKeeper ->
                val initialDuration = timeKeeper.currentDuration ?: 30f
                viewModel.resetTimer(timeKeeper.id, initialDuration)
            }
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
                adapter.submitList(timeKeepers)
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

        lifecycleScope.launch {
            viewModel.activeTimers.collectLatest { timers ->
                adapter.updateActiveTimers(timers)
            }
        }
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
                    val duration = if (durationText.isBlank()) null else durationText.toFloat()
                    val started = dialogBinding.switchStarted.isChecked
                    // convert duration to seconds
                    val durationInSeconds = duration?.times(60) ?: 30f * 60
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
}