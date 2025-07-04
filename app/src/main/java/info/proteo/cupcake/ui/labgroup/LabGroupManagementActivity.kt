package info.proteo.cupcake.ui.labgroup

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.R
import info.proteo.cupcake.databinding.ActivityLabGroupManagementBinding
import info.proteo.cupcake.shared.data.model.user.LabGroup
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LabGroupManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLabGroupManagementBinding
    private val viewModel: LabGroupManagementViewModel by viewModels()
    private lateinit var adapter: LabGroupManagementAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLabGroupManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        setupSearch()
        observeViewModel()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        adapter = LabGroupManagementAdapter(
            onLabGroupClick = { labGroup ->
                navigateToMemberActivity(labGroup)
            }
        )

        binding.recyclerLabGroups.apply {
            layoutManager = LinearLayoutManager(this@LabGroupManagementActivity)
            adapter = this@LabGroupManagementActivity.adapter
        }
    }

    private fun setupClickListeners() {
        binding.btnCreateLabGroup.setOnClickListener {
            showCreateLabGroupDialog()
        }

        // Filter chips
        binding.chipAllGroups.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                viewModel.setFilter(FilterType.ALL)
                binding.chipProfessionalOnly.isChecked = false
                binding.chipRegularOnly.isChecked = false
            }
        }

        binding.chipProfessionalOnly.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                viewModel.setFilter(FilterType.PROFESSIONAL_ONLY)
                binding.chipAllGroups.isChecked = false
                binding.chipRegularOnly.isChecked = false
            }
        }

        binding.chipRegularOnly.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                viewModel.setFilter(FilterType.REGULAR_ONLY)
                binding.chipAllGroups.isChecked = false
                binding.chipProfessionalOnly.isChecked = false
            }
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener { text ->
            viewModel.searchLabGroups(text.toString())
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        updateUI(state)
                    }
                }

                launch {
                    viewModel.createLabGroupResult.collect { result ->
                        result?.let {
                            handleCreateResult(it)
                            viewModel.clearCreateResult()
                        }
                    }
                }

                launch {
                    viewModel.updateLabGroupResult.collect { result ->
                        result?.let {
                            handleUpdateResult(it)
                            viewModel.clearUpdateResult()
                        }
                    }
                }

                launch {
                    viewModel.deleteLabGroupResult.collect { result ->
                        result?.let {
                            handleDeleteResult(it)
                            viewModel.clearDeleteResult()
                        }
                    }
                }
            }
        }
    }

    private fun updateUI(state: LabGroupManagementUiState) {

        // Loading state
        binding.loadingState.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        binding.recyclerLabGroups.visibility = if (state.isLoading) View.GONE else View.VISIBLE

        // Empty state
        binding.emptyState.visibility = if (!state.isLoading && state.filteredLabGroups.isEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }

        // Lab groups data
        if (!state.isLoading) {
            adapter.submitList(state.filteredLabGroups)
        }

        // Create button visibility (only for staff)
        binding.btnCreateLabGroup.visibility = if (state.isStaff) View.VISIBLE else View.GONE

        // Error handling
        state.error?.let { error ->
            Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    private fun showCreateLabGroupDialog() {
        CreateEditLabGroupDialog.newInstance(
            labGroup = null,
            onResult = { name, description, isProfessional, serviceStorageId ->
                viewModel.createLabGroup(name, description, isProfessional, serviceStorageId)
            }
        ).show(supportFragmentManager, "CreateLabGroupDialog")
    }

    private fun showEditLabGroupDialog(labGroup: LabGroup) {
        CreateEditLabGroupDialog.newInstance(
            labGroup = labGroup,
            onResult = { name, description, isProfessional, serviceStorageId ->
                viewModel.updateLabGroup(
                    labGroup.id,
                    name,
                    description,
                    isProfessional,
                    serviceStorageId
                )
            }
        ).show(supportFragmentManager, "EditLabGroupDialog")
    }

    private fun showDeleteConfirmationDialog(labGroup: LabGroup) {
        AlertDialog.Builder(this)
            .setTitle("Delete Lab Group")
            .setMessage("Are you sure you want to delete \"${labGroup.name}\"? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteLabGroup(labGroup.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun startMemberManagementActivity(labGroup: LabGroup) {
        val intent = Intent(this, LabGroupMemberManagementActivity::class.java).apply {
            putExtra("LAB_GROUP_ID", labGroup.id)
            putExtra("LAB_GROUP_NAME", labGroup.name)
        }
        startActivity(intent)
    }

    private fun startMemberViewActivity(labGroup: LabGroup) {
        val intent = Intent(this, LabGroupMemberViewActivity::class.java).apply {
            putExtra("LAB_GROUP_ID", labGroup.id)
            putExtra("LAB_GROUP_NAME", labGroup.name)
        }
        startActivity(intent)
    }

    private fun handleCreateResult(result: Result<LabGroup>) {
        if (result.isSuccess) {
            val labGroup = result.getOrNull()
            Toast.makeText(this, "Lab group \"${labGroup?.name}\" created successfully", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to create lab group: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun handleUpdateResult(result: Result<LabGroup>) {
        if (result.isSuccess) {
            val labGroup = result.getOrNull()
            Toast.makeText(this, "Lab group \"${labGroup?.name}\" updated successfully", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to update lab group: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun handleDeleteResult(result: Result<Unit>) {
        if (result.isSuccess) {
            Toast.makeText(this, "Lab group deleted successfully", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to delete lab group: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun navigateToLabGroupDetail(labGroupId: Int) {
        val intent = Intent(this, LabGroupDetailActivity::class.java).apply {
            putExtra("labGroupId", labGroupId)
        }
        startActivity(intent)
    }

    private fun navigateToMemberActivity(labGroup: LabGroup) {
        // Navigate to lab group detail which includes member viewing
        navigateToLabGroupDetail(labGroup.id)
    }

    private fun isUserManagerOfGroup(labGroup: LabGroup, currentUser: info.proteo.cupcake.shared.data.model.user.User?): Boolean {
        // Check if current user is a manager of this lab group
        // This depends on your User model structure - you may need to adjust based on your actual data model
        return currentUser?.managedLabGroups?.any { it.id == labGroup.id } ?: false
    }
}