package info.proteo.cupcake.ui.labgroup.dialog

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.R
import info.proteo.cupcake.databinding.DialogAddMemberBinding
import info.proteo.cupcake.shared.data.model.user.User
import info.proteo.cupcake.ui.labgroup.adapter.UserSelectionAdapter
import info.proteo.cupcake.ui.user.UserSearchViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddMemberDialog : DialogFragment() {

    private var _binding: DialogAddMemberBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: UserSearchViewModel by viewModels()
    private lateinit var adapter: UserSelectionAdapter
    
    private var labGroupId: Int = -1
    private var onMemberAdded: ((User) -> Unit)? = null
    private var existingMembers: List<User> = emptyList()
    private var searchJob: Job? = null
    private var lastQuery: String = ""
    private var lastUserList: List<User> = emptyList()

    companion object {
        fun newInstance(
            labGroupId: Int,
            existingMembers: List<User>,
            onMemberAdded: (User) -> Unit
        ): AddMemberDialog {
            val dialog = AddMemberDialog()
            dialog.labGroupId = labGroupId
            dialog.existingMembers = existingMembers
            dialog.onMemberAdded = onMemberAdded
            return dialog
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        
        // Prevent dialog from being dismissed on touch outside during search
        dialog.setCanceledOnTouchOutside(true)
        
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddMemberBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
        
        // Show initial empty state with helpful message
        binding.emptyState.visibility = View.VISIBLE
        binding.tvEmptyMessage.text = "Start typing to search for users"
        
        // Focus the search input and show keyboard
        binding.etSearchUsers.requestFocus()
    }

    private fun setupRecyclerView() {
        adapter = UserSelectionAdapter { user ->
            onMemberAdded?.invoke(user)
            dismiss()
        }

        binding.recyclerUsers.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@AddMemberDialog.adapter
        }
    }

    private fun setupClickListeners() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        // Clear search button
        binding.btnClearSearch.setOnClickListener {
            binding.etSearchUsers.text?.clear()
        }

        // Set up typeahead search with debouncing
        binding.etSearchUsers.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                
                // Show/hide clear button based on text content
                binding.btnClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                
                // Don't trigger search if query hasn't actually changed
                if (query == lastQuery) {
                    return
                }
                
                // Cancel previous search job
                searchJob?.cancel()
                
                // Start new search with debouncing
                searchJob = lifecycleScope.launch {
                    delay(500) // 500ms debounce to reduce rapid calls
                    
                    // Only search if query is empty or has at least 2 characters
                    if (query.isEmpty() || query.length >= 2) {
                        viewModel.searchUsers(query)
                    }
                }
            }
        })

        // Handle search action on keyboard
        binding.etSearchUsers.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.etSearchUsers.text?.toString()?.trim() ?: ""
                searchJob?.cancel()
                viewModel.searchUsers(query)
                true
            } else {
                false
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUI(state)
                }
            }
        }
    }

    private fun updateUI(state: info.proteo.cupcake.ui.user.UserSearchUiState) {
        // Loading state
        binding.progressLoading.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        
        // Get current search query
        val currentQuery = binding.etSearchUsers.text?.toString()?.trim() ?: ""
        
        // Filter out existing members
        val existingMemberIds = existingMembers.map { it.id }.toSet()
        val filteredUsers = state.users.filter { user ->
            user.id !in existingMemberIds
        }
        
        // Only update adapter if the query or user list has actually changed
        if (currentQuery != lastQuery || state.users != lastUserList) {
            adapter.updateSearchQuery(currentQuery)
            adapter.submitList(filteredUsers)
            lastQuery = currentQuery
            lastUserList = state.users
        }
        
        // Empty state
        val isEmpty = filteredUsers.isEmpty() && !state.isLoading
        binding.emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        
        // Update empty state message based on different conditions
        when {
            state.isLoading -> {
                binding.emptyState.visibility = View.GONE
            }
            currentQuery.isNotEmpty() && currentQuery.length < 2 -> {
                binding.emptyState.visibility = View.VISIBLE
                binding.tvEmptyMessage.text = "Type at least 2 characters to search"
            }
            state.users.isNotEmpty() && filteredUsers.isEmpty() -> {
                binding.emptyState.visibility = View.VISIBLE
                binding.tvEmptyMessage.text = "All found users are already members"
            }
            state.users.isEmpty() && currentQuery.isNotEmpty() -> {
                binding.emptyState.visibility = View.VISIBLE
                binding.tvEmptyMessage.text = "No users found matching \"$currentQuery\""
            }
            currentQuery.isEmpty() -> {
                binding.emptyState.visibility = View.VISIBLE
                binding.tvEmptyMessage.text = "Start typing to search for users"
            }
            else -> {
                binding.emptyState.visibility = View.VISIBLE
                binding.tvEmptyMessage.text = "No users found"
            }
        }
        
        // Error handling
        state.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel()
        _binding = null
    }
}