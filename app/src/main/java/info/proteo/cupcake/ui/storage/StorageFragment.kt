package info.proteo.cupcake.ui.storage

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.R
import info.proteo.cupcake.StoredReagentActivity
import info.proteo.cupcake.shared.data.model.storage.StorageObject
import info.proteo.cupcake.data.remote.service.StoredReagentService
import info.proteo.cupcake.databinding.FragmentStorageBinding
import info.proteo.cupcake.ui.barcode.BarcodeScannerFragment
import info.proteo.cupcake.ui.storage.adapter.StorageAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.core.view.get
import androidx.core.view.size
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.collections.remove

@AndroidEntryPoint
class StorageFragment : Fragment() {

    private var _binding: FragmentStorageBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StorageViewModel by viewModels()
    private lateinit var storageAdapter: StorageAdapter
    private var isLoading = false
    private val PAGE_SIZE = 20
    private var currentStorageObjectId: Int? = null

    @Inject
    lateinit var storedReagentService: StoredReagentService

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStorageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupSwipeRefresh()
        setupReagentButton()
        setupSearchFunctionality()
        setupFAB()
        setupMenu()
        viewModel.loadStorageObjects()
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_barcode_scan, menu)
                for (i in 0 until menu.size) {
                    val menuItem = menu[i]
                    menuItem.icon?.setTint(ContextCompat.getColor(requireContext(), R.color.white))
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_scan_barcode -> {
                        openBarcodeScanner()
                        true
                    }
                    R.id.action_search_reagent -> {
                        showSearchDialog()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner)
    }

    private fun openBarcodeScanner() {
        val intent = Intent(requireContext(), StoredReagentActivity::class.java).apply {
            putExtra(StoredReagentActivity.EXTRA_STORAGE_OBJECT_ID, currentStorageObjectId)
            putExtra(StoredReagentActivity.EXTRA_OPEN_SCANNER, true)
        }
        startActivity(intent)
    }

    private fun setupRecyclerView() {
        storageAdapter = StorageAdapter { storageObject ->
            currentStorageObjectId = storageObject.id
            viewModel.loadStorageObjects(storageObject.id)
            checkForStoredReagents(storageObject.id)
        }

        binding.recyclerViewStorage.apply {
            adapter = storageAdapter
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                    if (!isLoading &&
                        (visibleItemCount + firstVisibleItemPosition) >= totalItemCount &&
                        firstVisibleItemPosition >= 0 &&
                        totalItemCount >= PAGE_SIZE) {

                        viewModel.loadMoreStorageObjects()
                        isLoading = true
                    }
                }
            })
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.storageState.collectLatest { state ->
                when (state) {
                    is StorageUiState.Loading -> {
                        binding.loadingState.visibility = View.VISIBLE
                        binding.recyclerViewStorage.visibility = View.GONE
                        binding.emptyState.visibility = View.GONE
                        binding.errorState.visibility = View.GONE
                    }
                    is StorageUiState.Success -> {
                        binding.loadingState.visibility = View.GONE
                        binding.recyclerViewStorage.visibility = View.VISIBLE
                        binding.emptyState.visibility = View.GONE
                        binding.errorState.visibility = View.GONE
                        storageAdapter.submitList(state.storageObjects)
                    }
                    is StorageUiState.Empty -> {
                        binding.loadingState.visibility = View.GONE
                        binding.recyclerViewStorage.visibility = View.GONE
                        binding.emptyState.visibility = View.VISIBLE
                        binding.errorState.visibility = View.GONE
                    }
                    is StorageUiState.Error -> {
                        binding.loadingState.visibility = View.GONE
                        binding.recyclerViewStorage.visibility = View.GONE
                        binding.emptyState.visibility = View.GONE
                        binding.errorState.visibility = View.VISIBLE
                        binding.textViewError.text = state.message
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentPath.collectLatest { path ->
                updateBreadcrumbs(path)
            }
            viewModel.isLoadingMore.collectLatest { isLoadingMore ->
                isLoading = isLoadingMore
            }
        }
    }

    private fun setupReagentButton() {
        binding.buttonViewReagents.apply {
            visibility = View.GONE
        }
    }

    private fun setupSearchFunctionality() {
        // Handle search input
        binding.etSearchStorage.setOnEditorActionListener { _, _, _ ->
            val query = binding.etSearchStorage.text.toString().trim()
            if (query.isNotEmpty()) {
                showSearchDialog()
            }
            true
        }
    }

    private fun setupFAB() {
        binding.fabStorageAction.setOnClickListener {
            // Always show create menu for + FAB
            showStorageActionMenu()
        }
    }

    private fun showStorageActionMenu() {
        val options = if (currentStorageObjectId != null) {
            // Inside a storage location - can create both
            arrayOf(
                "Create Reagent",
                "Create Storage Object"
            )
        } else {
            // At root level - can only create storage objects
            arrayOf(
                "Create Storage Object"
            )
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Create New")
            .setItems(options) { _, which ->
                if (currentStorageObjectId != null) {
                    // Inside storage location
                    when (which) {
                        0 -> createReagent()
                        1 -> createStorageObject()
                    }
                } else {
                    // At root level
                    when (which) {
                        0 -> createStorageObject()
                    }
                }
            }
            .show()
    }
    
    private fun createReagent() {
        val currentStorage = viewModel.currentPath.value.lastOrNull()
        val storageName = currentStorage?.objectName ?: ""

        val intent = Intent(requireContext(), StoredReagentActivity::class.java).apply {
            putExtra(StoredReagentActivity.EXTRA_STORAGE_OBJECT_ID, currentStorageObjectId)
            putExtra(StoredReagentActivity.EXTRA_STORAGE_NAME, storageName)
            putExtra(StoredReagentActivity.EXTRA_CREATE_REAGENT, true)
        }
        startActivity(intent)
    }
    
    private fun createStorageObject() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_create_storage_object, null)
        
        val nameInput = dialogView.findViewById<EditText>(R.id.editTextStorageName)
        val typeDropdown = dialogView.findViewById<com.google.android.material.textfield.MaterialAutoCompleteTextView>(R.id.autoCompleteStorageType)
        
        // Define storage types based on backend model
        val storageTypes = arrayOf(
            "shelf", "box", "fridge", "freezer", 
            "room", "building", "floor", "other"
        )
        
        val storageTypeLabels = arrayOf(
            "Shelf", "Box", "Fridge", "Freezer",
            "Room", "Building", "Floor", "Other"
        )
        
        val adapter = android.widget.ArrayAdapter(requireContext(), 
            android.R.layout.simple_dropdown_item_1line, storageTypeLabels)
        typeDropdown.setAdapter(adapter)
        typeDropdown.setText("Shelf", false) // Set default to Shelf
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Create Storage Object")
            .setView(dialogView)
            .setPositiveButton("Create") { _, _ ->
                val name = nameInput.text.toString().trim()
                val selectedIndex = storageTypeLabels.indexOf(typeDropdown.text.toString())
                val type = if (selectedIndex >= 0) storageTypes[selectedIndex] else "shelf"
                
                if (name.isNotEmpty()) {
                    viewModel.createStorageObject(name, type, currentStorageObjectId)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun checkForStoredReagents(storageObjectId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.buttonViewReagents.visibility = View.GONE

            // Show the buttons card when inside a storage location
            binding.buttonsCard.visibility = View.VISIBLE
            
            // Show FAB for actions
            binding.fabStorageAction.visibility = View.VISIBLE

            try {
                val result = storedReagentService.getStoredReagents(0, 1, storageObjectId)
                result.onSuccess { response ->
                    if (response.count > 0) {
                        binding.buttonViewReagents.apply {
                            visibility = View.VISIBLE
                            text = getString(R.string.view_reagents, response.count)
                            setOnClickListener {
                                navigateToStoredReagents(storageObjectId)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Even in case of error, keep the card visible
                binding.buttonsCard.visibility = View.VISIBLE
            }
        }
    }

    private fun navigateToStoredReagents(storageObjectId: Int) {
        val intent = Intent(requireContext(), StoredReagentActivity::class.java).apply {
            putExtra(StoredReagentActivity.EXTRA_STORAGE_OBJECT_ID, storageObjectId)
        }
        startActivity(intent)
    }

    private fun updateBreadcrumbs(path: List<StorageObject>) {
        binding.breadcrumbsContainer.removeAllViews()

        val rootItem = TextView(requireContext()).apply {
            text = "home"
            setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(16, 0, 16, 0)
            setOnClickListener { viewModel.loadStorageObjects(null) }
        }
        binding.breadcrumbsContainer.addView(rootItem)

        path.forEachIndexed { index, storageObject ->
            val separator = TextView(requireContext()).apply {
                text = ">"
                setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                textSize = 16f
                setPadding(8, 0, 8, 0)
            }
            binding.breadcrumbsContainer.addView(separator)

            val segmentView = TextView(requireContext()).apply {
                text = storageObject.objectName
                setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                textSize = 16f
                setTypeface(typeface, Typeface.BOLD)
                setPadding(16, 0, 16, 0)

                if (index == path.size - 1) {
                    alpha = 1.0f
                } else {
                    alpha = 0.85f
                    setOnClickListener { viewModel.loadStorageObjects(storageObject.id) }
                }
            }
            binding.breadcrumbsContainer.addView(segmentView)
        }

        binding.scrollBreadcrumbs.post {
            binding.scrollBreadcrumbs.fullScroll(View.FOCUS_RIGHT)
        }

        val currentLocation = path.lastOrNull()
        if (currentLocation != null) {
            currentStorageObjectId = currentLocation.id
            checkForStoredReagents(currentLocation.id)
        } else {
            // At root level
            currentStorageObjectId = null
            binding.buttonsCard.visibility = View.GONE
            binding.fabStorageAction.visibility = View.VISIBLE
        }
    }

    private fun createBreadcrumbChip(text: String): Chip {
        return Chip(requireContext()).apply {
            this.text = text
            isCheckable = false
            isClickable = true
            setChipBackgroundColorResource(R.color.primary)
            setTextColor(getResources().getColor(R.color.light))
        }
    }

    private fun showSearchDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_search_reagent, null)

        val searchInput = dialogView.findViewById<EditText>(R.id.editTextSearch)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Search Reagents")
            .setView(dialogView)
            .setPositiveButton("Search") { _, _ ->
                val searchTerm = searchInput.text.toString().trim()
                if (searchTerm.isNotEmpty()) {
                    navigateToReagentSearch(searchTerm)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun navigateToReagentSearch(searchTerm: String) {
        val intent = Intent(requireContext(), StoredReagentActivity::class.java).apply {
            putExtra(StoredReagentActivity.EXTRA_STORAGE_OBJECT_ID, currentStorageObjectId)
            putExtra(StoredReagentActivity.EXTRA_SEARCH_TERM, searchTerm)
        }
        startActivity(intent)
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
            binding.swipeRefresh.isRefreshing = false
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }



    companion object {
        const val BARCODE_SCAN_REQUEST = 100
    }



}