package info.proteo.cupcake.ui.profile

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.R
import info.proteo.cupcake.databinding.FragmentUserProfileBinding
import info.proteo.cupcake.shared.data.model.user.LabGroup
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UserProfileFragment : Fragment() {

    private var _binding: FragmentUserProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UserProfileViewModel by viewModels()

    private lateinit var labGroupAdapter: LabGroupAdapter
    private lateinit var managedLabGroupAdapter: LabGroupAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Enable options menu for this fragment
        setHasOptionsMenu(true)
        
        setupRecyclerViews()
        observeViewModel()
    }

    private fun setupRecyclerViews() {
        // Lab Groups RecyclerView
        labGroupAdapter = LabGroupAdapter { labGroup ->
            onLabGroupClick(labGroup)
        }
        
        binding.recyclerLabGroups.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = labGroupAdapter
        }

        // Managed Lab Groups RecyclerView
        managedLabGroupAdapter = LabGroupAdapter { labGroup ->
            onLabGroupClick(labGroup)
        }
        
        binding.recyclerManagedLabGroups.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = managedLabGroupAdapter
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_user_profile, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_edit_profile -> {
                showEditProfileDialog()
                true
            }
            R.id.action_change_password -> {
                showChangePasswordDialog()
                true
            }
            R.id.action_export_data -> {
                showExportDataDialog()
                true
            }
            R.id.action_import_data -> {
                showImportDataDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        updateUI(state)
                    }
                }

                launch {
                    viewModel.profileUpdateResult.collect { result ->
                        result?.let {
                            handleProfileUpdateResult(it)
                            viewModel.clearProfileUpdateResult()
                        }
                    }
                }

                launch {
                    viewModel.passwordChangeResult.collect { result ->
                        result?.let {
                            handlePasswordChangeResult(it)
                            viewModel.clearPasswordChangeResult()
                        }
                    }
                }

                launch {
                    viewModel.exportDataResult.collect { result ->
                        result?.let {
                            handleExportDataResult(it)
                            viewModel.clearExportDataResult()
                        }
                    }
                }
            }
        }
    }

    private fun updateUI(state: UserProfileUiState) {
        binding.apply {
            // Loading state
            progressLoading.visibility = if (state.isLoading) View.VISIBLE else View.GONE

            // Error state
            if (state.error != null) {
                Toast.makeText(requireContext(), state.error, Toast.LENGTH_LONG).show()
            }

            // User data
            state.user?.let { user ->
                val fullName = listOfNotNull(user.firstName, user.lastName)
                    .joinToString(" ")
                    .ifBlank { user.username }
                
                tvUserName.text = fullName
                tvUsername.text = "@${user.username}"
                tvEmail.text = user.email ?: "No email provided"

                // Staff badge
                chipStaff.visibility = if (state.isStaff) View.VISIBLE else View.GONE
            }

            // Lab Groups
            if (state.labGroups.isEmpty()) {
                recyclerLabGroups.visibility = View.GONE
                tvEmptyLabGroups.visibility = View.VISIBLE
            } else {
                recyclerLabGroups.visibility = View.VISIBLE
                tvEmptyLabGroups.visibility = View.GONE
                labGroupAdapter.submitList(state.labGroups)
            }

            // Managed Lab Groups
            if (state.managedLabGroups.isEmpty()) {
                cardManagedLabGroups.visibility = View.GONE
            } else {
                cardManagedLabGroups.visibility = View.VISIBLE
                managedLabGroupAdapter.submitList(state.managedLabGroups)
            }
        }
    }

    private fun onLabGroupClick(labGroup: LabGroup) {
        // TODO: Navigate to lab group details or handle click
        Toast.makeText(
            requireContext(), 
            "Clicked on ${labGroup.name}", 
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showEditProfileDialog() {
        val currentUser = viewModel.uiState.value.user ?: return

        val dialogLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 40)
        }

        val firstNameEdit = EditText(requireContext()).apply {
            hint = "First Name"
            setText(currentUser.firstName ?: "")
        }

        val lastNameEdit = EditText(requireContext()).apply {
            hint = "Last Name"
            setText(currentUser.lastName ?: "")
        }

        val emailEdit = EditText(requireContext()).apply {
            hint = "Email"
            setText(currentUser.email ?: "")
        }

        dialogLayout.addView(firstNameEdit)
        dialogLayout.addView(lastNameEdit)
        dialogLayout.addView(emailEdit)

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Profile")
            .setView(dialogLayout)
            .setPositiveButton("Save") { _, _ ->
                viewModel.updateProfile(
                    firstName = firstNameEdit.text.toString(),
                    lastName = lastNameEdit.text.toString(),
                    email = emailEdit.text.toString()
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showChangePasswordDialog() {
        val dialogLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 40)
        }

        val oldPasswordEdit = EditText(requireContext()).apply {
            hint = "Current Password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        val newPasswordEdit = EditText(requireContext()).apply {
            hint = "New Password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        val confirmPasswordEdit = EditText(requireContext()).apply {
            hint = "Confirm New Password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        dialogLayout.addView(oldPasswordEdit)
        dialogLayout.addView(newPasswordEdit)
        dialogLayout.addView(confirmPasswordEdit)

        AlertDialog.Builder(requireContext())
            .setTitle("Change Password")
            .setView(dialogLayout)
            .setPositiveButton("Change") { _, _ ->
                val oldPassword = oldPasswordEdit.text.toString()
                val newPassword = newPasswordEdit.text.toString()
                val confirmPassword = confirmPasswordEdit.text.toString()

                if (newPassword != confirmPassword) {
                    Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newPassword.length < 6) {
                    Toast.makeText(requireContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                viewModel.changePassword(oldPassword, newPassword)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showExportDataDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Export Data")
            .setMessage("Export all your data? This may take a few minutes.")
            .setPositiveButton("Export") { _, _ ->
                viewModel.exportData()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showImportDataDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Import Data")
            .setMessage("Data import functionality is not yet implemented.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun handleProfileUpdateResult(result: Result<Unit>) {
        if (result.isSuccess) {
            Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                requireContext(), 
                "Failed to update profile: ${result.exceptionOrNull()?.message}", 
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun handlePasswordChangeResult(result: Result<Unit>) {
        if (result.isSuccess) {
            Toast.makeText(requireContext(), "Password changed successfully", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                requireContext(), 
                "Failed to change password: ${result.exceptionOrNull()?.message}", 
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun handleExportDataResult(result: Result<Unit>) {
        if (result.isSuccess) {
            Toast.makeText(requireContext(), "Data export started", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                requireContext(), 
                "Failed to start data export: ${result.exceptionOrNull()?.message}", 
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}