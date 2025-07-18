package info.proteo.cupcake.ui.labgroup

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.R
import info.proteo.cupcake.databinding.ActivityLabGroupDetailBinding
import info.proteo.cupcake.shared.data.model.user.LabGroup
import info.proteo.cupcake.shared.data.model.user.User
import info.proteo.cupcake.ui.base.BaseActivity
import info.proteo.cupcake.ui.labgroup.CreateEditLabGroupDialog
import info.proteo.cupcake.ui.labgroup.dialog.AddMemberDialog
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LabGroupDetailActivity : BaseActivity<ActivityLabGroupDetailBinding>() {
    private val detailViewModel: LabGroupDetailViewModel by viewModels()
    private val managementViewModel: LabGroupManagementViewModel by viewModels()
    
    private var currentLabGroup: LabGroup? = null
    private var canManageGroup = false
    private var labGroupId: Int = -1

    override fun createBinding(): ActivityLabGroupDetailBinding {
        return ActivityLabGroupDetailBinding.inflate(layoutInflater)
    }

    override fun onCreateBinding(savedInstanceState: Bundle?) {
        labGroupId = intent.getIntExtra("labGroupId", -1)
        if (labGroupId == -1) {
            finish()
            return
        }

        setupToolbar()
        setupFragment()
        observeViewModels()
        
        // Load lab group detail data
        detailViewModel.loadLabGroupDetail(labGroupId)
        // managementViewModel loads user permissions automatically in init
    }
    
    override fun setupWindowInsets() {
        // This activity has a toolbar, so use AppBar inset handling
        setupAppBarInsets(binding.toolbar)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
        
        // Sync status bar color with toolbar
        syncStatusBarWithToolbar(binding.toolbar)
    }

    private fun setupFragment() {
        val fragment = LabGroupDetailFragment().apply {
            arguments = Bundle().apply {
                putInt("labGroupId", labGroupId)
            }
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun observeViewModels() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe detail view model for lab group data
                launch {
                    detailViewModel.uiState.collect { state ->
                        currentLabGroup = state.labGroup
                        updatePermissions()
                    }
                }
                
                // Observe management view model for user permissions
                launch {
                    managementViewModel.uiState.collect { state ->
                        updatePermissions()
                    }
                }
                
                // Observe remove user result to refresh detail view
                launch {
                    managementViewModel.removeUserResult.collect { result ->
                        result?.let {
                            if (it.isSuccess) {
                                // Refresh the detail view when user removal succeeds
                                detailViewModel.refresh(labGroupId)
                                managementViewModel.clearRemoveUserResult()
                            } else {
                                Toast.makeText(this@LabGroupDetailActivity, 
                                    "Failed to remove user: ${it.exceptionOrNull()?.message}", 
                                    Toast.LENGTH_LONG).show()
                                managementViewModel.clearRemoveUserResult()
                            }
                        }
                    }
                }
                
                // Observe add manager result
                launch {
                    managementViewModel.addManagerResult.collect { result ->
                        result?.let {
                            if (it.isSuccess) {
                                android.util.Log.d("LabGroupDetailActivity", "Add manager success - calling refresh")
                                // Simple approach: just refresh everything
                                detailViewModel.refresh(labGroupId)
                                managementViewModel.loadLabGroups() // Update the main list too
                                Toast.makeText(this@LabGroupDetailActivity, 
                                    "Manager added successfully", 
                                    Toast.LENGTH_SHORT).show()
                                managementViewModel.clearAddManagerResult()
                            } else {
                                Toast.makeText(this@LabGroupDetailActivity, 
                                    "Failed to add manager: ${it.exceptionOrNull()?.message}", 
                                    Toast.LENGTH_LONG).show()
                                managementViewModel.clearAddManagerResult()
                            }
                        }
                    }
                }
                
                // Observe remove manager result
                launch {
                    managementViewModel.removeManagerResult.collect { result ->
                        result?.let {
                            if (it.isSuccess) {
                                android.util.Log.d("LabGroupDetailActivity", "Remove manager success - calling refresh")
                                // Simple approach: just refresh everything
                                detailViewModel.refresh(labGroupId)
                                managementViewModel.loadLabGroups() // Update the main list too
                                Toast.makeText(this@LabGroupDetailActivity, 
                                    "Manager removed successfully", 
                                    Toast.LENGTH_SHORT).show()
                                managementViewModel.clearRemoveManagerResult()
                            } else {
                                Toast.makeText(this@LabGroupDetailActivity, 
                                    "Failed to remove manager: ${it.exceptionOrNull()?.message}", 
                                    Toast.LENGTH_LONG).show()
                                managementViewModel.clearRemoveManagerResult()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updatePermissions() {
        val managementState = managementViewModel.uiState.value
        canManageGroup = managementState.isStaff || isUserManagerOfGroup(currentLabGroup, managementState.currentUser)
        invalidateOptionsMenu()
    }

    private fun isUserManagerOfGroup(labGroup: LabGroup?, currentUser: info.proteo.cupcake.shared.data.model.user.User?): Boolean {
        return currentUser?.managedLabGroups?.any { it.id == labGroup?.id } ?: false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Show menu if user can manage OR if we're still determining permissions and user is staff
        val managementState = managementViewModel.uiState.value
        val shouldShowMenu = canManageGroup || 
                           (currentLabGroup != null && managementState.isStaff) ||
                           (currentLabGroup == null && managementState.isStaff) // Show for staff even before lab group loads
        
        if (shouldShowMenu) {
            menuInflater.inflate(R.menu.menu_lab_group_detail, menu)
            
            // Enable/disable menu items based on actual permissions and data availability
            menu.findItem(R.id.action_edit_lab_group)?.isEnabled = currentLabGroup != null && canManageGroup
            menu.findItem(R.id.action_add_member)?.isEnabled = currentLabGroup != null && canManageGroup
            menu.findItem(R.id.action_delete_lab_group)?.isEnabled = currentLabGroup != null && canManageGroup
            
            return true
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_edit_lab_group -> {
                editLabGroup()
                true
            }
            R.id.action_add_member -> {
                addMember()
                true
            }
            R.id.action_delete_lab_group -> {
                deleteLabGroup()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun editLabGroup() {
        currentLabGroup?.let { labGroup ->
            CreateEditLabGroupDialog.newInstance(
                labGroup = labGroup,
                onResult = { name, description, isProfessional, serviceStorageId ->
                    managementViewModel.updateLabGroup(
                        labGroup.id,
                        name,
                        description,
                        isProfessional,
                        serviceStorageId
                    )
                    // Refresh the detail view
                    detailViewModel.refresh(labGroupId)
                }
            ).show(supportFragmentManager, "EditLabGroupDialog")
        }
    }

    private fun addMember() {
        currentLabGroup?.let { labGroup ->
            val currentMembers = detailViewModel.uiState.value.allMembers
            AddMemberDialog.newInstance(
                labGroupId = labGroup.id,
                existingMembers = currentMembers,
                onMemberAdded = { user ->
                    // Add the user to the lab group
                    managementViewModel.addUserToLabGroup(labGroup.id, user.id)
                    // Refresh the detail view to show the new member
                    detailViewModel.refresh(labGroupId)
                    Toast.makeText(this, "Added ${user.username} to lab group", Toast.LENGTH_SHORT).show()
                }
            ).show(supportFragmentManager, "AddMemberDialog")
        }
    }


    private fun deleteLabGroup() {
        currentLabGroup?.let { labGroup ->
            AlertDialog.Builder(this)
                .setTitle("Delete Lab Group")
                .setMessage("Are you sure you want to delete \"${labGroup.name}\"? This action cannot be undone.")
                .setPositiveButton("Delete") { _, _ ->
                    managementViewModel.deleteLabGroup(labGroup.id)
                    finish() // Close detail view after deletion
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    fun canUserManageGroup(): Boolean {
        return canManageGroup
    }

    fun removeMemberFromGroup(user: User) {
        currentLabGroup?.let { labGroup ->
            AlertDialog.Builder(this)
                .setTitle("Remove Member")
                .setMessage("Are you sure you want to remove ${user.firstName ?: user.username} from \"${labGroup.name}\"?")
                .setPositiveButton("Remove") { _, _ ->
                    managementViewModel.removeUserFromLabGroup(labGroup.id, user.id)
                    Toast.makeText(this, "Removing ${user.username} from lab group...", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    fun addManagerToGroup(user: User) {
        currentLabGroup?.let { labGroup ->
            AlertDialog.Builder(this)
                .setTitle("Add Manager")
                .setMessage("Are you sure you want to make ${user.firstName ?: user.username} a manager of \"${labGroup.name}\"?")
                .setPositiveButton("Add as Manager") { _, _ ->
                    managementViewModel.addManagerToLabGroup(labGroup.id, user.id)
                    Toast.makeText(this, "Adding ${user.username} as manager...", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    fun removeManagerFromGroup(user: User) {
        currentLabGroup?.let { labGroup ->
            AlertDialog.Builder(this)
                .setTitle("Remove Manager")
                .setMessage("Are you sure you want to remove ${user.firstName ?: user.username} as manager of \"${labGroup.name}\"?\n\nThey will remain as a regular member.")
                .setPositiveButton("Remove Manager") { _, _ ->
                    managementViewModel.removeManagerFromLabGroup(labGroup.id, user.id)
                    Toast.makeText(this, "Removing ${user.username} as manager...", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}