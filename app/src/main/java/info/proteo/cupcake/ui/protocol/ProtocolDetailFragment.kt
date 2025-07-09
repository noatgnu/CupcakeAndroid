package info.proteo.cupcake.ui.protocol

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import androidx.fragment.app.viewModels
import android.os.Bundle
import android.text.Html
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.R
import info.proteo.cupcake.SessionActivity
import info.proteo.cupcake.shared.data.model.protocol.ProtocolModel
import info.proteo.cupcake.shared.data.model.protocol.ProtocolStep
import info.proteo.cupcake.shared.data.model.protocol.Session
import info.proteo.cupcake.data.remote.service.ProtocolService
import info.proteo.cupcake.data.remote.service.UpdateProtocolRequest
import info.proteo.cupcake.data.remote.service.UserPermissionResponse
import info.proteo.cupcake.data.repository.ProtocolRepository
import info.proteo.cupcake.data.repository.UserRepository
import info.proteo.cupcake.databinding.FragmentProtocolDetailBinding
import info.proteo.cupcake.ui.session.SessionAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ProtocolDetailFragment : Fragment() {
    private var _binding: FragmentProtocolDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProtocolDetailViewModel by viewModels()
    private var protocolId: Int = 0

    companion object {
        fun newInstance(protocolId: Int): ProtocolDetailFragment {
            return ProtocolDetailFragment().apply {
                arguments = Bundle().apply {
                    putInt("protocolId", protocolId)
                }
            }
        }
    }

    private lateinit var sessionAdapter: SessionAdapter
    private lateinit var sectionAdapter: ProtocolSectionAdapter

    @Inject lateinit var protocolService: ProtocolService
    @Inject lateinit var userRepository: UserRepository
    @Inject lateinit var protocolRepository: ProtocolRepository


    private var protocolPermissions: UserPermissionResponse? = null
    private val sessionPermissions = mutableMapOf<String, UserPermissionResponse>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            protocolId = it.getInt("protocolId")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProtocolDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        viewModel.loadProtocolDetails(protocolId)
        checkProtocolPermissions(protocolId)
        setupSessionsList(protocolId)
        setupSectionsList()
        observeViewModel()
        ViewCompat.setNestedScrollingEnabled(binding.protocolDescription, true)
        binding.protocolDescription.setOnTouchListener { v, event ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            false
        }

        binding.fabSession.setOnClickListener {
            showSessionOptions()
        }

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.protocol_detail_menu, menu)
            }

            override fun onPrepareMenu(menu: Menu) {
                menu.findItem(R.id.action_edit_protocol)?.isVisible = protocolPermissions?.edit == true
                menu.findItem(R.id.action_delete_protocol)?.isVisible = protocolPermissions?.delete == true
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_edit_protocol -> {
                        showProtocolEditOptions()
                        true
                    }
                    R.id.action_delete_protocol -> {
                        // Handle delete protocol action
                        true
                    }
                    android.R.id.home -> {
                        findNavController().navigateUp()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupToolbar() {
        (activity as? AppCompatActivity)?.let {
            binding.toolbar?.let { toolbar ->
                it.setSupportActionBar(toolbar)
                it.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.protocol.collectLatest { protocol ->
                protocol?.let {
                    binding.protocolTitle.text = it.protocolTitle
                    binding.protocolDescription.settings.apply {
                        javaScriptEnabled = false
                        useWideViewPort = true
                        builtInZoomControls = true
                        displayZoomControls = false
                        layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
                        loadWithOverviewMode = true
                    }
                    val nightModeFlags = resources.configuration.uiMode and
                        Configuration.UI_MODE_NIGHT_MASK
                    val isDarkMode = nightModeFlags == Configuration.UI_MODE_NIGHT_YES

                    binding.protocolDescription.setBackgroundColor(
                        if (isDarkMode) Color.BLACK else Color.WHITE
                    )

                    val cssStyle = if (isDarkMode) {
                        "body{color:#FFFFFF;background-color:#121212;font-family:sans-serif;font-size:14px;} " +
                            "table{width:100%;border-collapse:collapse;} " +
                            "th,td{border:1px solid #444;padding:8px;}"
                    } else {
                        "body{color:#000000;background-color:#FFFFFF;font-family:sans-serif;font-size:14px;} " +
                            "table{width:100%;border-collapse:collapse;} " +
                            "th,td{border:1px solid #ddd;padding:8px;}"
                    }

                    binding.protocolDescription.loadDataWithBaseURL(
                        null,
                        "<html><head><meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                            "<style>$cssStyle</style>" +
                            "</head><body>${it.protocolDescription}</body></html>",
                        "text/html",
                        "UTF-8",
                        null
                    )

                    val sectionsCount = it.sections?.size ?: 0
                    val stepsCount = it.steps?.size ?: 0
                    binding.protocolStats.text = "Sections: $sectionsCount | Steps: $stepsCount"

                    binding.protocolStatus.text = "Status: ${if (it.enabled) "Public" else "Private"}"

                    if (!it.tags.isNullOrEmpty()) {
                        binding.protocolTagsHeader.visibility = View.VISIBLE
                        binding.protocolTagsChipGroup.visibility = View.VISIBLE
                        binding.protocolTagsChipGroup.removeAllViews()

                        it.tags?.forEach { protocolTag ->
                            val chip = Chip(requireContext())
                            chip.text = protocolTag.tag.tag
                            chip.isClickable = false
                            binding.protocolTagsChipGroup.addView(chip)
                        }
                    }

                    if (!it.reagents.isNullOrEmpty()) {
                        binding.protocolReagentsHeader.visibility = View.VISIBLE
                        binding.protocolReagentsContainer.visibility = View.VISIBLE
                        binding.protocolReagentsContainer.removeAllViews()

                        it.reagents?.forEach { reagent ->
                            val reagentView = layoutInflater.inflate(
                                R.layout.item_protocol_reagent,
                                binding.protocolReagentsContainer,
                                false
                            )

                            val nameView = reagentView.findViewById<TextView>(R.id.reagentName)
                            val quantityView = reagentView.findViewById<TextView>(R.id.reagentQuantity)

                            nameView.text = reagent.reagent.name
                            quantityView.text = "${reagent.quantity} ${reagent.reagent.unit}"

                            binding.protocolReagentsContainer.addView(reagentView)
                        }
                    }

                    val sections = it.sections ?: emptyList()
                    if (sections.isNotEmpty()) {
                        binding.sectionsHeader.visibility = View.VISIBLE
                        binding.sectionsRecyclerView.visibility = View.VISIBLE
                    } else {
                        binding.sectionsHeader.visibility = View.GONE
                        binding.sectionsRecyclerView.visibility = View.GONE
                    }


                    (activity as? AppCompatActivity)?.supportActionBar?.title = it.protocolTitle
                }
            }

        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.sectionStepsMap.collectLatest { stepsMap ->
                val sections = viewModel.protocol.value?.sections ?: emptyList()
                Log.d("ProtocolDetailFragment", "Sections: ${sections.size}, StepsMap: ${stepsMap.size}")
                sectionAdapter.updateData(sections, stepsMap)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.sessions.collectLatest { sessions ->
                binding.sessionsRecyclerView.visibility =
                    if (sessions.isEmpty()) View.GONE else View.VISIBLE
                binding.noSessionsText.visibility =
                    if (sessions.isEmpty()) View.VISIBLE else View.GONE
                sessionAdapter.updateSessions(sessions)
                sessions.forEach { session ->
                    checkSessionPermission(session)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }


    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupSectionsList() {
        sectionAdapter = ProtocolSectionAdapter { step ->
            // Handle step click, perhaps navigate to step detail or show a dialog
            showStepDetailDialog(step)
        }

        binding.sectionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = sectionAdapter
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }
    }


    private fun setupSessionsList(protocolId: Int) {
        sessionAdapter = SessionAdapter { session ->
            //findNavController().navigate(
            //    R.id.action_to_session_detail,
            //    Bundle().apply { putInt("sessionId", session.id) }
            //)
        }

        binding.sessionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = sessionAdapter
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }
    }

    private fun checkProtocolPermissions(protocolId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val permissionResult = userRepository.checkProtocolPermission(protocolId)
                permissionResult.onSuccess { permission ->
                    protocolPermissions = permission
                    updateUIBasedOnPermissions()
                }
            } catch (e: Exception) {
                Log.e("ProtocolDetailFragment", "Error checking protocol permissions", e)
            }
        }
    }

    private fun checkSessionPermission(session: Session) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val permissionResult = userRepository.checkSessionPermission(session.uniqueId)
                permissionResult.onSuccess { permission ->
                    sessionPermissions[session.uniqueId] = permission
                    sessionAdapter.updateSessionPermissions(sessionPermissions)
                }
            } catch (e: Exception) {
                Log.e("ProtocolDetailFragment", "Error checking session permissions", e)
            }
        }
    }

    private fun updateUIBasedOnPermissions() {
        protocolPermissions?.let { permissions ->
            if (!permissions.view) {
                findNavController().navigateUp()
                return
            }

            activity?.invalidateOptionsMenu()
        }
    }

    private fun showProtocolEditOptions() {
        val options = arrayOf(
            "Edit Title/Description",
            "Toggle Public Visibility",
            "Manage Viewers/Editors",
            "Manage Tags"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Protocol")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditTitleDescriptionDialog()
                    1 -> toggleProtocolVisibility()
                    2 -> manageUsers()
                    3 -> manageTags()
                }
            }
            .show()
    }

    private fun showEditTitleDescriptionDialog() {
        val protocol = viewModel.protocol.value ?: return

        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_edit_protocol, null)
        val titleEditText = dialogView.findViewById<EditText>(R.id.protocolTitleEdit)
        val descriptionEditText = dialogView.findViewById<EditText>(R.id.protocolDescriptionEdit)

        titleEditText.setText(protocol.protocolTitle)
        descriptionEditText.setText(Html.fromHtml(protocol.protocolDescription, Html.FROM_HTML_MODE_COMPACT))

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Protocol Details")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                updateProtocolDetails(
                    titleEditText.text.toString(),
                    descriptionEditText.text.toString()
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateProtocolDetails(title: String, description: String) {
        val protocol = viewModel.protocol.value ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            binding.progressBar?.visibility = View.VISIBLE
            try {
                val request = UpdateProtocolRequest(
                    protocolTitle = title,
                    protocolDescription = description
                )

                protocolRepository.updateProtocol(protocol.id, request)
                    .onSuccess {
                        Toast.makeText(requireContext(), "Protocol updated", Toast.LENGTH_SHORT).show()
                        viewModel.loadProtocolDetails(protocolId)
                    }
                    .onFailure {
                        Toast.makeText(requireContext(), "Failed to update protocol", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {
                Log.e("ProtocolDetailFragment", "Error updating protocol", e)
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar?.visibility = View.GONE
            }
        }
    }

    private fun toggleProtocolVisibility() {
        val protocol = viewModel.protocol.value ?: return
        val newEnabled = !protocol.enabled

        viewLifecycleOwner.lifecycleScope.launch {
            binding.progressBar?.visibility = View.VISIBLE
            try {
                val request = UpdateProtocolRequest(protocolTitle = protocol.protocolTitle, protocolDescription = protocol.protocolDescription, enabled = newEnabled)

                protocolRepository.updateProtocol(protocol.id, request)
                    .onSuccess {
                        val statusMessage = if (newEnabled) "Protocol is now public" else "Protocol is now private"
                        Toast.makeText(requireContext(), statusMessage, Toast.LENGTH_SHORT).show()
                        viewModel.loadProtocolDetails(protocolId)
                    }
                    .onFailure {
                        Toast.makeText(requireContext(), "Failed to update visibility", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {
                Log.e("ProtocolDetailFragment", "Error toggling visibility", e)
            } finally {
                binding.progressBar?.visibility = View.GONE
            }
        }
    }

    private fun manageUsers() {
        val protocol = viewModel.protocol.value ?: return

        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_manage_users, null)

        val tabLayout = dialogView.findViewById<TabLayout>(R.id.tabLayout)
        val userSearchView = dialogView.findViewById<androidx.appcompat.widget.SearchView>(R.id.searchUser)
        val usersRecyclerView = dialogView.findViewById<RecyclerView>(R.id.usersRecyclerView)

        usersRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        usersRecyclerView.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Manage Protocol Users")
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .create()

        var currentRole = "viewer" // Default tab

        // Set up tab selection listener
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentRole = if (tab.position == 0) "viewer" else "editor"
                loadUsers(protocol.id, currentRole, usersRecyclerView)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        // Set up search functionality
        userSearchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                    searchUsers(query, currentRole, protocol.id, usersRecyclerView)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) {
                    // If search box is cleared, show current users
                    loadUsers(protocol.id, currentRole, usersRecyclerView)
                }
                return true
            }
        })

        // Load initial users
        loadUsers(protocol.id, currentRole, usersRecyclerView)
        dialog.show()
    }

    private fun loadUsers(protocolId: Int, role: String, recyclerView: RecyclerView) {
        binding.progressBar?.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val users = if (role == "viewer") {
                    protocolRepository.getViewers(protocolId).getOrNull() ?: emptyList()
                } else {
                    protocolRepository.getEditors(protocolId).getOrNull() ?: emptyList()
                }

                val adapter = ProtocolUserListSearchAdapter(users, onRemoveClick = { user ->
                    removeUserFromRole(protocolId, user.username, role)
                    loadUsers(protocolId, role, recyclerView)
                })

                recyclerView.adapter = adapter
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to load users", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar?.visibility = View.GONE
            }
        }
    }

    private fun searchUsers(query: String, role: String, protocolId: Int, recyclerView: RecyclerView) {
        binding.progressBar?.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val searchResult = userRepository.searchUsers(query).getOrNull()?.results ?: emptyList()

                val currentUsers = if (role == "viewer") {
                    protocolRepository.getViewers(protocolId).getOrNull() ?: emptyList()
                } else {
                    protocolRepository.getEditors(protocolId).getOrNull() ?: emptyList()
                }

                // Filter out users who already have this role
                val filteredUsers = searchResult.filter { searchUser ->
                    currentUsers.none { it.username == searchUser.username }
                }

                val adapter = ProtocolUserSearchAdapter(filteredUsers) { user ->
                    addUserToRole(protocolId, user.username, role)
                    loadUsers(protocolId, role, recyclerView)
                }

                recyclerView.adapter = adapter
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to search users", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar?.visibility = View.GONE
            }
        }
    }

    private fun addUserToRole(protocolId: Int, username: String, role: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.progressBar?.visibility = View.VISIBLE
            try {
                protocolRepository.addUserRole(protocolId, username, role)
                    .onSuccess {
                        Toast.makeText(requireContext(), "User added as $role", Toast.LENGTH_SHORT).show()
                    }
                    .onFailure {
                        Toast.makeText(requireContext(), "Failed to add user", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar?.visibility = View.GONE
            }
        }
    }

    private fun removeUserFromRole(protocolId: Int, username: String, role: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.progressBar?.visibility = View.VISIBLE
            try {
                protocolRepository.removeUserRole(protocolId, username, role)
                    .onSuccess {
                        Toast.makeText(requireContext(), "User removed from $role role", Toast.LENGTH_SHORT).show()
                    }
                    .onFailure {
                        Toast.makeText(requireContext(), "Failed to remove user", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar?.visibility = View.GONE
            }
        }
    }

    private fun manageTags() {
        val protocol = viewModel.protocol.value ?: return

        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_manage_tags, null)
        val tagInput = dialogView.findViewById<EditText>(R.id.tagInput)
        val tagsContainer = dialogView.findViewById<ChipGroup>(R.id.tagsChipGroup)

        // Add search functionality
        val searchLayout = layoutInflater.inflate(
            R.layout.protocol_tag_search_layout,
            dialogView as ViewGroup,
            false
        )
        val searchView = searchLayout.findViewById<SearchView>(R.id.searchView)
        val searchResults = searchLayout.findViewById<RecyclerView>(R.id.searchResults)
        searchResults.layoutManager = LinearLayoutManager(requireContext())

        // Add search layout at the top
        (dialogView as ViewGroup).addView(searchLayout, 0)

        // Add existing tags to chip group
        refreshTagChips(protocol, tagsContainer)

        // Search adapter for tags
        val tagSearchAdapter = ProtocolTagSearchAdapter(emptyList()) { tag ->
            // When tag is selected, add it to the protocol
            addExistingTag(protocol.id, tag.id)
            // Hide search results after selection
            searchResults.visibility = View.GONE
        }
        searchResults.adapter = tagSearchAdapter

        // Set up search functionality
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                    searchTags(query, tagSearchAdapter, searchResults, protocol.id)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText?.length ?: 0 >= 2) {
                    searchTags(newText.toString(), tagSearchAdapter, searchResults, protocol.id)
                } else if (newText.isNullOrBlank()) {
                    searchResults.visibility = View.GONE
                }
                return true
            }
        })

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Manage Tags")
            .setView(dialogView)
            .setPositiveButton("Add New Tag") { _, _ ->
                val tagName = tagInput.text.toString().trim()
                if (tagName.isNotBlank()) {
                    addTag(protocol.id, tagName)
                }
            }
            .setNegativeButton("Done", null)
            .create()

        dialog.show()
    }

    private fun refreshTagChips(protocol: ProtocolModel, tagsContainer: ChipGroup) {
        tagsContainer.removeAllViews()
        protocol.tags?.forEach { protocolTag ->
            val chip = Chip(requireContext()).apply {
                text = protocolTag.tag.tag
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    removeTag(protocol.id, protocolTag.tag.id)
                    tagsContainer.removeView(this)
                }
            }
            tagsContainer.addView(chip)
        }
    }

    private fun searchTags(query: String, adapter: ProtocolTagSearchAdapter, recyclerView: RecyclerView, protocolId: Int) {
        binding.progressBar?.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val currentTagIds = viewModel.protocol.value?.tags?.map { it.tag.id } ?: emptyList()

                val result = viewModel.searchTags(query)
                result.fold(
                    onSuccess = { response ->
                        // Filter out tags that are already added to the protocol
                        val filteredTags = response.results.filter { tag ->
                            !currentTagIds.contains(tag.id)
                        }

                        if (filteredTags.isNotEmpty()) {
                            adapter.updateTags(filteredTags)
                            recyclerView.visibility = View.VISIBLE
                        } else {
                            recyclerView.visibility = View.GONE
                        }
                    },
                    onFailure = {
                        Toast.makeText(requireContext(), "Failed to search tags", Toast.LENGTH_SHORT).show()
                        recyclerView.visibility = View.GONE
                    }
                )
            } catch (e: Exception) {
                Log.e("ProtocolDetailFragment", "Error searching tags", e)
                recyclerView.visibility = View.GONE
            } finally {
                binding.progressBar?.visibility = View.GONE
            }
        }
    }

    private fun addExistingTag(protocolId: Int, tagId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.progressBar?.visibility = View.VISIBLE
            try {
                // First get the tag name
                val tagResult = viewModel.getTagById(tagId)
                tagResult.fold(
                    onSuccess = { tag ->
                        // Add the tag to the protocol
                        protocolRepository.addTagToProtocol(protocolId, tag.tag.tag)
                            .onSuccess {
                                Toast.makeText(requireContext(), "Tag added", Toast.LENGTH_SHORT).show()
                                viewModel.loadProtocolDetails(protocolId)
                            }
                            .onFailure {
                                Toast.makeText(requireContext(), "Failed to add tag", Toast.LENGTH_SHORT).show()
                            }
                    },
                    onFailure = {
                        Toast.makeText(requireContext(), "Failed to retrieve tag details", Toast.LENGTH_SHORT).show()
                    }
                )
            } catch (e: Exception) {
                Log.e("ProtocolDetailFragment", "Error adding existing tag", e)
            } finally {
                binding.progressBar?.visibility = View.GONE
            }
        }
    }


    private fun addTag(protocolId: Int, tagName: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.progressBar?.visibility = View.VISIBLE
            try {
                protocolRepository.addTagToProtocol(protocolId, tagName)
                    .onSuccess {
                        Toast.makeText(requireContext(), "Tag added", Toast.LENGTH_SHORT).show()
                        viewModel.loadProtocolDetails(protocolId)
                    }
                    .onFailure {
                        Toast.makeText(requireContext(), "Failed to add tag", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {
                Log.e("ProtocolDetailFragment", "Error adding tag", e)
            } finally {
                binding.progressBar?.visibility = View.GONE
            }
        }
    }

    private fun removeTag(protocolId: Int, tagId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.progressBar?.visibility = View.VISIBLE
            try {
                protocolRepository.removeTagFromProtocol(protocolId, tagId)
                    .onSuccess {
                        Toast.makeText(requireContext(), "Tag removed", Toast.LENGTH_SHORT).show()
                        viewModel.loadProtocolDetails(protocolId)
                    }
                    .onFailure {
                        Toast.makeText(requireContext(), "Failed to remove tag", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {
                Log.e("ProtocolDetailFragment", "Error removing tag", e)
            } finally {
                binding.progressBar?.visibility = View.GONE
            }
        }
    }

    private fun showStepDetailDialog(step: ProtocolStep) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_step_detail, null)

        dialogView.findViewById<TextView>(R.id.stepTitle).text = "Step ${step.stepId}"
        dialogView.findViewById<TextView>(R.id.stepDescription).text = step.stepDescription


        val reagentsContainer = dialogView.findViewById<LinearLayout>(R.id.reagentsContainer)
        step.reagents?.forEach { reagent ->
            val reagentView = layoutInflater.inflate(
                R.layout.item_reagent_simple, reagentsContainer, false)
            reagentView.findViewById<TextView>(R.id.reagentName).text = reagent.reagent.name
            reagentView.findViewById<TextView>(R.id.reagentQuantity).text =
                "${reagent.quantity} ${reagent.reagent.unit}"
            reagentsContainer.addView(reagentView)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Step Details")
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun showSessionOptions() {
        val protocol = viewModel.protocol.value ?: return
        val sessions = viewModel.sessions.value

        // Create options based on available sessions
        val options = mutableListOf<String>().apply {
            add("Create New Session")
            if (sessions.isNotEmpty()) {
                add("Select Existing Session")
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Session Options")
            .setItems(options.toTypedArray()) { _, which ->
                when(which) {
                    0 -> createNewSession(protocol.id)
                    1 -> showExistingSessionsDialog(sessions)
                }
            }
            .show()
    }

    private fun createNewSession(protocolId: Int) {
        val intent = Intent(requireContext(), SessionActivity::class.java)
        intent.putExtra("protocolId", protocolId)
        intent.putExtra("isNewSession", true)
        startActivity(intent)
    }

    private fun showExistingSessionsDialog(sessions: List<Session>) {
        val sessionNames = sessions.map { session ->
            "Session ${session.uniqueId.take(8)} (${session.createdAt?.substring(0, 10) ?: "Unknown date"})"
        }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Select Session")
            .setItems(sessionNames) { _, which ->
                val selectedSession = sessions[which]
                startExistingSession(selectedSession.uniqueId)
            }
            .show()
    }

    private fun startExistingSession(sessionId: String) {
        val intent = Intent(requireContext(), SessionActivity::class.java)
        intent.putExtra("sessionId", sessionId)
        intent.putExtra("isNewSession", false)

        startActivity(intent)
    }
}