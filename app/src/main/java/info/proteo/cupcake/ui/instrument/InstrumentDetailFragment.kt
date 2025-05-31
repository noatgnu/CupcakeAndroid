package info.proteo.cupcake.ui.instrument

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import info.proteo.cupcake.databinding.DialogEditInstrumentBinding
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import info.proteo.cupcake.ui.user.UserSearchAdapter
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.R
import info.proteo.cupcake.SessionManager
import info.proteo.cupcake.SupportInformationActivity
import info.proteo.cupcake.data.remote.model.annotation.AnnotationFolder
import info.proteo.cupcake.data.remote.model.instrument.Instrument
import info.proteo.cupcake.databinding.FragmentInstrumentDetailBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@AndroidEntryPoint
class InstrumentDetailFragment : Fragment() {

    private var _binding: FragmentInstrumentDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InstrumentDetailViewModel by viewModels()
    private val args: InstrumentDetailFragmentArgs by navArgs()

    private lateinit var annotationAdapter: InstrumentAnnotationAdapter
    private var currentSelectedFolderId: Int? = null

    private val pickImageRequest = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            try {
                val inputStream = requireContext().contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                val base64Image = convertBitmapToBase64(bitmap)

                viewModel.instrument.value?.getOrNull()?.let { currentInstrument ->
                    val updatedInstrument = currentInstrument.copy(image = base64Image)
                    viewModel.updateInstrumentDetails(updatedInstrument)

                    binding.instrumentDetailImage.setImageBitmap(bitmap)
                    binding.instrumentDetailImage.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Log.e("InstrumentDetail", "Error processing image", e)
                Snackbar.make(binding.root, "Error processing image: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    @Inject
    lateinit var sessionManager: SessionManager

    private val openDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInstrumentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupMenu()
        observeViewModel()

        viewModel.loadInstrumentDetailsAndPermissions(args.instrumentId)

        binding.tabLayoutAnnotationFolders.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.tag?.let { tag ->
                    if (tag is Int) {
                        currentSelectedFolderId = tag
                        viewModel.loadAnnotationsForFolder(tag)
                        Log.d("InstrumentDetail", "Tab selected, folder ID: $tag")
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {
                tab?.tag?.let { tag ->
                    if (tag is Int && currentSelectedFolderId != tag) { // Avoid reloading if already selected
                        currentSelectedFolderId = tag
                        viewModel.loadAnnotationsForFolder(tag)
                        Log.d("InstrumentDetail", "Tab reselected, folder ID: $tag")
                    }
                }
            }
        })
    }


    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_instrument_detail, menu)
            }

            override fun onPrepareMenu(menu: Menu) {
                val canManage = viewModel.canManageInstrument.value == true
                Log.d("InstrumentDetailFragment", "onPrepareMenu - canManage: $canManage")
                menu.findItem(R.id.action_edit_instrument)?.isVisible = canManage
                menu.findItem(R.id.action_manage_access)?.isVisible = canManage
                menu.findItem(R.id.action_attach_document)?.isVisible = canManage
                menu.findItem(R.id.action_delete_instrument)?.isVisible = canManage
                menu.findItem(R.id.action_view_maintenance)?.isVisible = canManage
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_edit_instrument -> {
                        viewModel.instrument.value?.getOrNull()?.let { currentInstrument ->
                            showEditInstrumentDialog(currentInstrument)
                        } ?: Snackbar.make(binding.root, "Instrument data not loaded yet. Cannot edit.", Snackbar.LENGTH_SHORT).show()
                        true
                    }
                    R.id.action_update_image -> {
                        pickImageRequest.launch("image/*")
                        true
                    }
                    R.id.action_view_support_info -> {
                        val intent = Intent(requireContext(), SupportInformationActivity::class.java).apply {
                            putExtra("INSTRUMENT_ID", args.instrumentId)
                        }
                        startActivity(intent)
                        true
                    }
                    R.id.action_view_maintenance -> {
                        // TODO: Navigate to maintenance history screen
                        true
                    }
                    R.id.action_manage_access -> {
                        showManagePermissionsDialog()
                        true
                    }
                    R.id.action_attach_document -> {
                        if (currentSelectedFolderId != null) {
                            // TODO: Implement attach document functionality
                        } else {
                            Snackbar.make(binding.root, "Please select an annotation folder first.", Snackbar.LENGTH_SHORT).show()
                        }
                        true
                    }
                    R.id.action_delete_instrument -> {
                        showDeleteConfirmationDialog()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupRecyclerView() {
        annotationAdapter = InstrumentAnnotationAdapter { annotation ->
            downloadAnnotation(annotation.id)
        }
        binding.recyclerViewAnnotations.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = annotationAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.isLoadingInstrument.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBarInstrument.isVisible = isLoading
            if (!isLoading) {
                binding.layoutContent.visibility = View.VISIBLE
            } else {
                binding.layoutContent.visibility = View.GONE
            }
        }

        viewModel.instrument.observe(viewLifecycleOwner) { result ->
            result.fold(
                onSuccess = { instrument ->
                    binding.instrumentDetailName.text = instrument.instrumentName ?: "N/A"
                    binding.instrumentDetailDescription.text = instrument.instrumentDescription ?: "No description available."
                    binding.instrumentDetailCreatedAt.text = formatDate(instrument.createdAt)
                    binding.instrumentDetailUpdatedAt.text = formatDate(instrument.updatedAt)
                    binding.instrumentDetailMaxPreapprovalDays.text = instrument.maxDaysAheadPreApproval?.toString() ?: "N/A"
                    binding.instrumentDetailMaxUsagePreapprovalWindow.text = instrument.maxDaysWithinUsagePreApproval?.toString() ?: "N/A"

                    if (!instrument.image.isNullOrEmpty()) {
                        binding.instrumentDetailImage.load(instrument.image) {
                            placeholder(R.drawable.ic_placeholder_image)
                            error(R.drawable.ic_placeholder_image)
                        }
                        binding.instrumentDetailImage.visibility = View.VISIBLE
                    } else {
                        binding.instrumentDetailImage.visibility = View.GONE
                    }
                    viewModel.canManageInstrument.value?.let { currentCanManageState ->
                        setupAnnotationFolderTabs(instrument.annotationFolders, currentCanManageState)
                    }
                },
                onFailure = { error ->
                    Log.e("InstrumentDetail", "Error loading instrument details", error)
                    Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                    binding.instrumentDetailName.text = "Error loading data"
                    binding.instrumentDetailDescription.text = error.message
                    binding.tabLayoutAnnotationFolders.visibility = View.GONE
                    binding.textViewEmptyAnnotations.text = "Failed to load instrument details."
                    binding.textViewEmptyAnnotations.visibility = View.VISIBLE
                }
            )
        }

        viewModel.canManageInstrument.observe(viewLifecycleOwner) { canManage ->
            Log.d("InstrumentDetailFragment", "canManageInstrument observed: $canManage")
            requireActivity().invalidateOptionsMenu()
            viewModel.instrument.value?.getOrNull()?.let { currentInstrument ->
                setupAnnotationFolderTabs(currentInstrument.annotationFolders, canManage)
            }
        }

        viewModel.isLoadingFolderAnnotations.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBarAnnotations.isVisible = isLoading
            if (isLoading) {
                binding.recyclerViewAnnotations.isVisible = false
                binding.textViewEmptyAnnotations.isVisible = false
            }
        }

        viewModel.folderAnnotations.observe(viewLifecycleOwner) { result ->
            result.fold(
                onSuccess = { response ->
                    val annotations = response.results
                    annotationAdapter.submitList(annotations)
                    if (annotations.isNullOrEmpty()) {
                        binding.textViewEmptyAnnotations.text = "No annotations in this folder."
                        binding.textViewEmptyAnnotations.isVisible = true
                        binding.recyclerViewAnnotations.isVisible = false
                    } else {
                        binding.textViewEmptyAnnotations.isVisible = false
                        binding.recyclerViewAnnotations.isVisible = true
                    }
                },
                onFailure = { error ->
                    Log.e("InstrumentDetail", "Error loading annotations", error)
                    annotationAdapter.submitList(emptyList())
                    binding.textViewEmptyAnnotations.text = "Error loading annotations."
                    binding.textViewEmptyAnnotations.isVisible = true
                    binding.recyclerViewAnnotations.isVisible = false
                    Snackbar.make(binding.root, "Failed to load annotations: ${error.message}", Snackbar.LENGTH_LONG).show()
                }
            )
        }

        viewModel.isUpdatingInstrument.observe(viewLifecycleOwner) { isUpdating ->
            if (isUpdating) {
                Snackbar.make(binding.root, "Updating instrument...", Snackbar.LENGTH_INDEFINITE).show()
            }
        }

        viewModel.updateResult.observe(viewLifecycleOwner) { result ->
            result.fold(
                onSuccess = {
                    Snackbar.make(binding.root, "Instrument updated successfully.", Snackbar.LENGTH_LONG).show()
                },
                onFailure = { error ->
                    Log.e("InstrumentDetail", "Failed to update instrument", error)
                    Snackbar.make(binding.root, "Error updating instrument: ${error.message}", Snackbar.LENGTH_LONG).show()
                }
            )
        }

        viewModel.deletionResult.observe(viewLifecycleOwner) { result ->
            result.fold(
                onSuccess = {
                    Snackbar.make(binding.root, "Instrument deleted successfully", Snackbar.LENGTH_SHORT).show()
                    requireActivity().onBackPressed()
                },
                onFailure = { error ->
                    Log.e("InstrumentDetail", "Failed to delete instrument", error)
                    Snackbar.make(
                        binding.root,
                        "Failed to delete instrument: ${error.message ?: "Unknown error"}",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            )
        }
    }

    private fun setupAnnotationFolderTabs(folders: List<AnnotationFolder>?, canManage: Boolean) {
        Log.d("InstrumentDetail", "Setting up annotation folder tabs with canManage: $canManage, folders: ${folders?.size ?: 0}")
        binding.tabLayoutAnnotationFolders.removeAllTabs()
        val restrictedFolders = listOf("Maintenance", "Certificates")

        val visibleFolders = folders?.filter { folder ->
            val name = folder.folderName ?: ""
            if (restrictedFolders.any { it.equals(name, ignoreCase = true) }) {
                canManage
            } else {
                true
            }
        }

        if (visibleFolders.isNullOrEmpty()) {
            binding.tabLayoutAnnotationFolders.visibility = View.GONE
            binding.recyclerViewAnnotations.visibility = View.GONE
            binding.progressBarAnnotations.visibility = View.GONE
            if (folders.isNullOrEmpty()) {
                binding.textViewEmptyAnnotations.text = "No annotation folders configured."
            } else {
                binding.textViewEmptyAnnotations.text = "No accessible annotation folders."
            }
            binding.textViewEmptyAnnotations.visibility = View.VISIBLE
            Log.d("InstrumentDetail", "No visible annotation folders. All: ${folders?.joinToString { it.folderName ?: "N/A" }}, CanManage: $canManage")
            currentSelectedFolderId = null
            annotationAdapter.submitList(emptyList())
            return
        }

        binding.tabLayoutAnnotationFolders.visibility = View.VISIBLE
        binding.textViewEmptyAnnotations.visibility = View.GONE

        var tabToSelect: TabLayout.Tab? = null
        var selectedFolderStillVisible = false

        visibleFolders.forEachIndexed { index, folder ->
            val tab = binding.tabLayoutAnnotationFolders.newTab()
            tab.text = folder.folderName
            tab.tag = folder.id
            binding.tabLayoutAnnotationFolders.addTab(tab)
            Log.d("InstrumentDetail", "Added tab: ${folder.folderName} with ID ${folder.id}")

            if (folder.id == currentSelectedFolderId) {
                tabToSelect = tab
                selectedFolderStillVisible = true
            }
            if (tabToSelect == null && index == 0 && currentSelectedFolderId == null) {
                tabToSelect = tab
            }
        }

        if (binding.tabLayoutAnnotationFolders.tabCount > 0) {
            if (tabToSelect == null && !selectedFolderStillVisible) {
                tabToSelect = binding.tabLayoutAnnotationFolders.getTabAt(0)
                currentSelectedFolderId = tabToSelect?.tag as? Int
            }
            tabToSelect?.select()
            if (tabToSelect == null && binding.tabLayoutAnnotationFolders.tabCount > 0) {
                binding.tabLayoutAnnotationFolders.getTabAt(0)?.let {
                    it.select() // Ensure a tab is selected if logic above didn't pick one
                    currentSelectedFolderId = it.tag as? Int
                }
            }
        } else {
            currentSelectedFolderId = null
            annotationAdapter.submitList(emptyList())
            binding.recyclerViewAnnotations.visibility = View.GONE
            binding.textViewEmptyAnnotations.text = "No annotations to display."
            binding.textViewEmptyAnnotations.visibility = View.VISIBLE
        }
    }


    private fun formatDate(dateString: String?): String {
        if (dateString.isNullOrEmpty()) return "N/A"
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
            parser.timeZone = TimeZone.getTimeZone("UTC")
            val date = parser.parse(dateString)
            val formatter = SimpleDateFormat("MMM dd, yyyy, HH:mm", Locale.getDefault())
            formatter.timeZone = TimeZone.getDefault() // Local timezone
            date?.let { formatter.format(it) } ?: "N/A"
        } catch (e: Exception) {
            Log.e("InstrumentDetail", "Error formatting date: $dateString", e)
            "Invalid Date"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerViewAnnotations.adapter = null
        _binding = null
    }

    private fun downloadAnnotation(annotationId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getAnnotationDownloadToken(annotationId).collect { result ->
                result.onSuccess { response ->
                    Log.d("InstrumentDetail", "Download token received: ${response.signedToken}")
                    val baseUrl = sessionManager.getBaseUrl()
                    if (baseUrl.isNullOrEmpty()) {
                        Snackbar.make(binding.root, "Error: Base URL not configured.", Snackbar.LENGTH_LONG).show()
                        return@onSuccess
                    }
                    val effectiveBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
                    val downloadUrl = "${effectiveBaseUrl}api/annotation/download_signed?token=${response.signedToken}"
                    Log.d("InstrumentDetail", "Attempting to download from: $downloadUrl")

                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = downloadUrl.toUri()
                    }
                    try {
                        openDocumentLauncher.launch(intent)
                    } catch (e: Exception) {
                        Log.e("InstrumentDetail", "No app to handle download intent", e)
                        Snackbar.make(binding.root, "No application found to open this document type.", Snackbar.LENGTH_LONG).show()
                    }
                }.onFailure { error ->
                    Log.e("InstrumentDetail", "Error downloading annotation", error)
                    Snackbar.make(binding.root, "Error getting download link: ${error.message}", Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showEditInstrumentDialog(instrument: Instrument) {
        val dialogBinding = DialogEditInstrumentBinding.inflate(layoutInflater)

        dialogBinding.editTextInstrumentName.setText(instrument.instrumentName)
        dialogBinding.editTextInstrumentDescription.setText(instrument.instrumentDescription)
        dialogBinding.editTextMaxDaysAhead.setText(instrument.maxDaysAheadPreApproval?.toString() ?: "")
        dialogBinding.editTextMaxDaysWithin.setText(instrument.maxDaysWithinUsagePreApproval?.toString() ?: "")
        dialogBinding.editTextDaysWarranty.setText(instrument.daysBeforeWarrantyNotification?.toString() ?: "")
        dialogBinding.editTextDaysMaintenance.setText(instrument.daysBeforeMaintenanceNotification?.toString() ?: "")

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Edit Instrument")
            .setView(dialogBinding.root)
            .create()

        dialogBinding.buttonSave.setOnClickListener {
            val name = dialogBinding.editTextInstrumentName.text.toString()
            if (name.isBlank()) {
                dialogBinding.layoutInstrumentName.error = "Name cannot be empty"
                return@setOnClickListener
            }

            val description = dialogBinding.editTextInstrumentDescription.text.toString()
            val maxDaysAhead = dialogBinding.editTextMaxDaysAhead.text.toString().toIntOrNull()
            val maxDaysWithin = dialogBinding.editTextMaxDaysWithin.text.toString().toIntOrNull()
            val daysWarranty = dialogBinding.editTextDaysWarranty.text.toString().toIntOrNull()
            val daysMaintenance = dialogBinding.editTextDaysMaintenance.text.toString().toIntOrNull()

            val updatedInstrument = instrument.copy(
                instrumentName = name,
                instrumentDescription = description.ifEmpty { null },
                maxDaysAheadPreApproval = maxDaysAhead,
                maxDaysWithinUsagePreApproval = maxDaysWithin,
                daysBeforeWarrantyNotification = daysWarranty,
                daysBeforeMaintenanceNotification = daysMaintenance
            )

            viewModel.updateInstrumentDetails(updatedInstrument)
            dialog.dismiss()
        }

        dialogBinding.buttonCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }



    private fun convertBitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        return "data:image/png;base64," + Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun showManagePermissionsDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(
            R.layout.dialog_manage_instrument_permissions, null
        )

        val searchEditText = dialogView.findViewById<TextInputEditText>(R.id.editTextSearchUser)
        val searchResultsRecyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerViewUserResults)
        val permissionLayout = dialogView.findViewById<LinearLayout>(R.id.layoutUserPermissions)
        val userNameTextView = dialogView.findViewById<TextView>(R.id.textViewSelectedUser)
        val switchCanView = dialogView.findViewById<SwitchMaterial>(R.id.switchCanView)
        val switchCanBook = dialogView.findViewById<SwitchMaterial>(R.id.switchCanBook)
        val switchCanManage = dialogView.findViewById<SwitchMaterial>(R.id.switchCanManage)
        val buttonSave = dialogView.findViewById<Button>(R.id.buttonSavePermissions)
        val buttonCancel = dialogView.findViewById<Button>(R.id.buttonCancelPermissions)

        permissionLayout.visibility = View.GONE

        val searchResultAdapter = UserSearchAdapter { selectedUser ->
            userNameTextView.text = selectedUser.username
            permissionLayout.visibility = View.VISIBLE

            switchCanView.isChecked = false
            switchCanBook.isChecked = false
            switchCanManage.isChecked = false

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val instrumentId = args.instrumentId
                    val permissionResult = viewModel.getInstrumentPermissionFor(
                        instrumentId, selectedUser.username
                    )

                    permissionResult.collect { result ->
                        result.fold(
                            onSuccess = { permission ->
                                switchCanView.isChecked = permission.canView
                                switchCanBook.isChecked = permission.canBook
                                switchCanManage.isChecked = permission.canManage
                            },
                            onFailure = { error ->
                                Snackbar.make(
                                    binding.root,
                                    "Error loading permissions: ${error.message}",
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                } catch (e: Exception) {
                    Log.e("InstrumentDetail", "Error getting permissions", e)
                }
            }
        }

        searchResultsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = searchResultAdapter
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }

        var searchJob: Job? = null
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(text: Editable?) {
                searchJob?.cancel()
                if (text?.length ?: 0 >= 2) {
                    searchJob = viewLifecycleOwner.lifecycleScope.launch {
                        try {
                            val searchResult = viewModel.searchUsers(text.toString())
                            searchResult.fold(
                                onSuccess = { response ->
                                    searchResultAdapter.submitList(response.results)
                                },
                                onFailure = { error ->
                                    Log.e("InstrumentDetail", "Error searching users", error)
                                    searchResultAdapter.submitList(emptyList())
                                }
                            )
                        } catch (e: Exception) {
                            Log.e("InstrumentDetail", "Error in user search", e)
                        }
                    }
                } else {
                    searchResultAdapter.submitList(emptyList())
                }
            }
        })

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Manage Instrument Access")
            .setView(dialogView)
            .create()

        buttonSave.setOnClickListener {
            val username = userNameTextView.text.toString()
            if (username.isNotEmpty()) {
                val canView = switchCanView.isChecked
                val canBook = switchCanBook.isChecked
                val canManage = switchCanManage.isChecked

                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val result = viewModel.assignInstrumentPermission(
                            args.instrumentId,
                            username,
                            canManage,
                            canBook,
                            canView
                        )

                        result.fold(
                            onSuccess = {
                                Snackbar.make(
                                    binding.root,
                                    "Permissions updated successfully",
                                    Snackbar.LENGTH_SHORT
                                ).show()
                                dialog.dismiss()
                            },
                            onFailure = { error ->
                                Snackbar.make(
                                    binding.root,
                                    "Error updating permissions: ${error.message}",
                                    Snackbar.LENGTH_LONG
                                ).show()
                            }
                        )
                    } catch (e: Exception) {
                        Log.e("InstrumentDetail", "Error assigning permissions", e)
                        Snackbar.make(
                            binding.root,
                            "Error: ${e.message}",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDeleteConfirmationDialog() {
        val instrument = viewModel.instrument.value?.getOrNull()
        if (instrument == null) {
            Snackbar.make(binding.root, "Cannot delete: instrument details not loaded", Snackbar.LENGTH_LONG).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Delete Instrument")
            .setMessage("Are you sure you want to delete ${instrument.instrumentName}? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteInstrument(args.instrumentId)
                Snackbar.make(binding.root, "Deleting instrument...", Snackbar.LENGTH_INDEFINITE).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}