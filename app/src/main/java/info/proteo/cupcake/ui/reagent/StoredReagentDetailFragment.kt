package info.proteo.cupcake.ui.reagent

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import java.util.TimeZone
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.R
import info.proteo.cupcake.SessionManager
import info.proteo.cupcake.shared.data.model.reagent.StoredReagent
import info.proteo.cupcake.databinding.FragmentStoredReagentDetailBinding
import info.proteo.cupcake.ui.barcode.BarcodeScannerFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.core.net.toUri

@AndroidEntryPoint
class StoredReagentDetailFragment : Fragment() {
    private var barcodeResult: String? = null
    private lateinit var actionAdapter: ReagentActionAdapter
    private var _binding: FragmentStoredReagentDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StoredReagentDetailViewModel by viewModels()
    private var menu: Menu? = null
    private var progressDialog: AlertDialog? = null
    private lateinit var documentAdapter: ReagentDocumentBrowserAdapter
    private lateinit var sessionManager: SessionManager


    private val pickImageRequest = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            try {
                val inputStream = requireContext().contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                updateReagentPhoto(bitmap)
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Failed to load image", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private val barcodeFormatMap = mapOf(
        "CODE128" to "CODE 128 (Auto)",
        "CODE128A" to "CODE 128 A",
        "CODE128B" to "CODE 128 B",
        "CODE128C" to "CODE 128 C",
        "EAN13" to "EAN-13",
        "EAN8" to "EAN-8",
        "UPC" to "UPC-A",
        "UPCE" to "UPC-E",
        "CODE39" to "CODE 39",
        "ITF14" to "ITF-14",
        "MSI" to "MSI",
        "pharmacode" to "Pharmacode",
        "codabar" to "Codabar"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(requireContext())
    }




    private fun updateReagentPhoto(bitmap: Bitmap) {
        val base64Image = convertBitmapToBase64(bitmap)

        viewModel.storedReagent.value?.let { reagent ->
            val updatedReagent = reagent.copy(pngBase64 = base64Image)

            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.saveStoredReagent(updatedReagent).collectLatest { result ->
                    result.onSuccess {
                        Snackbar.make(binding.root, "Photo updated successfully", Snackbar.LENGTH_SHORT).show()
                    }.onFailure { exception ->
                        Snackbar.make(binding.root, "Failed to update photo: ${exception.message}", Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun convertBitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        return "data:image/png;base64," + Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStoredReagentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupBarcodeFormatSpinner()
        setupReagentActionsList()
        setupDocumentBrowser()
        setupObservers()


        arguments?.getInt("REAGENT_ID", -1)?.let { reagentId ->
            if (reagentId != -1) {
                viewModel.loadStoredReagent(reagentId)
                viewModel.loadReagentActions(reagentId, true)
            } else {
                showError("Invalid reagent ID")
            }
        }
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_stored_reagent_details, menu)

            }

            override fun onPrepareMenu(menu: Menu) {
                menu.findItem(R.id.action_edit_photo)?.isVisible = viewModel.canEdit.value
                menu.findItem(R.id.action_edit_reagent)?.isVisible = viewModel.canEdit.value
                menu.findItem(R.id.action_update_barcode)?.isVisible = viewModel.canEdit.value
                menu.findItem(R.id.action_add_reagent)?.isVisible = viewModel.canUse.value
                menu.findItem(R.id.action_reserve_reagent)?.isVisible = viewModel.canUse.value
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_update_barcode -> {
                        showBarcodeUpdateDialog()
                        true
                    }
                    R.id.action_edit_reagent -> {
                        showEditReagentDialog()
                        true
                    }
                    R.id.action_edit_photo -> {
                        pickImageRequest.launch("image/*")
                        true
                    }
                    R.id.action_add_reagent -> {
                        showReagentActionDialog("add")
                        true
                    }
                    R.id.action_reserve_reagent -> {
                        showReagentActionDialog("reserve")
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.canEdit.collect {
                        requireActivity().invalidateOptionsMenu()
                    }
                }

                launch {
                    viewModel.canDelete.collect {
                        requireActivity().invalidateOptionsMenu()
                    }
                }
            }
        }

    }

    private fun showBarcodeUpdateDialog() {
        val options = arrayOf("Scan Barcode", "Enter Manually")

        AlertDialog.Builder(requireContext())
            .setTitle("Update Barcode")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> launchBarcodeScanner()
                    1 -> showManualBarcodeEntryDialog()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun launchBarcodeScanner() {
        val scannerFragment = BarcodeScannerFragment().apply {
            setOnBarcodeDetectedListener { barcode ->
                viewModel.setBarcodeResult(barcode)

                activity?.runOnUiThread {
                    parentFragmentManager.popBackStack()
                }
            }
        }

        parentFragmentManager.beginTransaction()
            .add(android.R.id.content, scannerFragment, "barcode_scanner")
            .addToBackStack("barcode_scanner")
            .commit()
    }

    private fun showManualBarcodeEntryDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_input_barcode, null)

        val editTextBarcode = dialogView.findViewById<TextInputEditText>(R.id.editTextBarcode)

        AlertDialog.Builder(requireContext())
            .setTitle("Enter Barcode")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val barcode = editTextBarcode.text.toString().trim()
                if (barcode.isNotEmpty()) {
                    viewModel.setBarcodeResult(barcode)
                } else {
                    Snackbar.make(binding.root, "Barcode cannot be empty", Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateReagentBarcode(barcode: String) {
        viewModel.storedReagent.value?.let { reagent ->
            val updatedReagent = reagent.copy(barcode = barcode)
            val progressDialog = AlertDialog.Builder(requireContext())
                .setView(R.layout.dialog_loading)
                .setCancelable(false)
                .create()
            progressDialog.show()

            viewLifecycleOwner.lifecycleScope.launch {
                Log.d("StoredReagentDetailFragment", "Updating barcode: $barcode for reagent ID: ${reagent.id}")
                viewModel.saveStoredReagent(updatedReagent).collectLatest { result ->
                    progressDialog.dismiss()
                    result.onSuccess {
                        Snackbar.make(binding.root, "Barcode updated successfully", Snackbar.LENGTH_SHORT).show()
                        viewModel.generateBarcode(barcode)
                        Log.d("StoredReagentDetailFragment", "Barcode updated successfully: $barcode")
                    }.onFailure { exception ->
                        Snackbar.make(binding.root, "Failed to update barcode: ${exception.message}", Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun setupBarcodeFormatSpinner() {
        val formatLabels = barcodeFormatMap.values.toList()
        val formatValues = barcodeFormatMap.keys.toList()

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, formatLabels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerBarcodeFormat.adapter = adapter
        binding.spinnerBarcodeFormat.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.updateBarcodeFormat(formatValues[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.storedReagent.collectLatest { reagent ->
                reagent?.let {
                    displayReagentInfo(it)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.barcodeBitmap.collectLatest { bitmap ->
                binding.progressBarBarcode.visibility = View.GONE
                if (bitmap != null) {
                    binding.imageViewBarcode.setImageBitmap(bitmap)
                    binding.imageViewBarcode.visibility = View.VISIBLE
                    binding.textViewBarcodeError.visibility = View.GONE
                } else {
                    binding.imageViewBarcode.visibility = View.GONE
                    binding.textViewBarcodeError.visibility = View.VISIBLE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                binding.contentLayout.visibility = if (isLoading) View.GONE else View.VISIBLE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedBarcodeFormat.collectLatest { format ->
                val position = barcodeFormatMap.keys.toList().indexOf(format)
                if (position >= 0) {
                    binding.spinnerBarcodeFormat.setSelection(position)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.locationPath.collectLatest { path ->
                Log.d("StoredReagentDetailFragment", "Location path: $path")
                if (!path.isNullOrEmpty()) {
                    binding.textViewLocation.text = path
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.reagentActions.collectLatest { actions ->
                actionAdapter.submitList(actions)
                Log.d("StoredReagentDetail", "Submitted ${actions.size} items to adapter")

                binding.emptyActionsView.visibility = if (actions.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoadingActions.collectLatest { isLoading ->
                binding.swipeRefreshLayoutActions.isRefreshing = isLoading && actionAdapter.itemCount == 0
                binding.progressBarLoadingMoreActions.visibility =
                    if (isLoading && actionAdapter.itemCount > 0) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.actionLoadError.collectLatest { errorMsg ->
                if (errorMsg != null) {
                    Snackbar.make(binding.root, errorMsg, Snackbar.LENGTH_LONG).show()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isBarcodeUpdating.collectLatest { isUpdating ->
                if (isUpdating) {
                    val progressDialog = AlertDialog.Builder(requireContext())
                        .setView(R.layout.dialog_loading)
                        .setCancelable(false)
                        .create()
                    progressDialog.show()

                    this@StoredReagentDetailFragment.progressDialog = progressDialog
                } else {
                    progressDialog?.dismiss()
                    progressDialog = null
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.barcodeUpdateStatus.collectLatest { result ->
                result?.let {
                    it.fold(
                        onSuccess = { message ->
                            Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
                        },
                        onFailure = { exception ->
                            Snackbar.make(binding.root, "Failed to update barcode: ${exception.message}",
                                Snackbar.LENGTH_SHORT).show()
                        }
                    )
                    viewModel.clearBarcodeUpdateStatus()
                }
            }
        }
    }

    private fun setupDocumentBrowser() {
        documentAdapter = ReagentDocumentBrowserAdapter()

        binding.recyclerViewDocuments.apply {
            adapter = documentAdapter
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        documentAdapter.setOnItemClickListener { document ->
            document.id.let { annotationId ->
                downloadDocument(annotationId)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.documentFolders.collectLatest { folders ->
                binding.tabLayout.removeAllTabs()
                folders.forEach { folder ->
                    binding.tabLayout.addTab(
                        binding.tabLayout.newTab().setText(folder.name)
                    )
                }

                if (folders.isEmpty()) {
                    binding.emptyDocumentsView.text = "No document folders available"
                    binding.emptyDocumentsView.visibility = View.VISIBLE
                    binding.tabLayout.visibility = View.GONE
                } else {
                    binding.emptyDocumentsView.visibility = View.GONE
                    binding.tabLayout.visibility = View.VISIBLE
                }
            }
        }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                arguments?.getInt("REAGENT_ID", -1)?.let { reagentId ->
                    if (reagentId != -1) {
                        viewModel.loadDocuments(reagentId, tab.text.toString())
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.documents.collectLatest { documents ->
                documentAdapter.submitList(documents)
                binding.emptyDocumentsView.visibility =
                    if (documents.isEmpty()) View.VISIBLE else View.GONE
                binding.recyclerViewDocuments.visibility =
                    if (documents.isEmpty()) View.GONE else View.VISIBLE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoadingDocuments.collectLatest { isLoading ->
                binding.progressBarDocuments.visibility =
                    if (isLoading) View.VISIBLE else View.GONE
            }
        }

        arguments?.getInt("REAGENT_ID", -1)?.let { reagentId ->
            if (reagentId != -1) {
                viewModel.loadDocumentFolders(reagentId)
            }
        }
    }

    private fun downloadDocument(annotationId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getDocumentDownloadToken(annotationId).collect { result ->
                result.onSuccess { response ->
                    val baseUrl = sessionManager.getBaseUrl()
                    val downloadUrl = "${baseUrl}api/reagent_documents/download_signed?token=${response.token}"

                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = downloadUrl.toUri()
                    }
                    try {
                        startActivity(intent)
                    } catch (e: Exception) {
                        Snackbar.make(binding.root, "No app available to open this document",
                            Snackbar.LENGTH_SHORT).show()
                    }
                }.onFailure { error ->
                    Snackbar.make(binding.root, "Error downloading document: ${error.message}",
                        Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun displayReagentInfo(reagent: StoredReagent) {
        binding.textViewReagentName.text = reagent.reagent.name
        binding.textViewQuantity.text = buildString {
            append(reagent.quantity)
            append(" ")
            append(reagent.reagent.unit)
        }
        binding.textViewCurrentQuantity.text = buildString {
            append(reagent.currentQuantity)
            append(" ")
            append(reagent.reagent.unit)
        }
        binding.textViewDescription.text = "${reagent.notes}"
        binding.textViewOwner.text = reagent.user.username
        binding.textViewLastUpdated.text = formatDate(reagent.updatedAt)
        binding.textViewExpiryDate.text = formatExpiryDate(reagent.expirationDate)
        viewModel.setReagentUnit(reagent.reagent.unit)
        if (reagent.shareable) {
            binding.imageViewShareable.setImageResource(R.drawable.ic_outline_folder_shared_24)
            binding.textViewShareable.text = "Shareable"
            binding.imageViewShareable.setColorFilter(
                ContextCompat.getColor(requireContext(), R.color.success),
                PorterDuff.Mode.SRC_IN
            )
        } else {
            binding.imageViewShareable.setImageResource(R.drawable.ic_outline_folder_shared_24)
            binding.textViewShareable.text = "Not shareable"
            binding.imageViewShareable.setColorFilter(
                ContextCompat.getColor(requireContext(), R.color.danger),
                PorterDuff.Mode.SRC_IN
            )
        }

        reagent.pngBase64?.let { base64Image ->
            try {
                val base64Content = if (base64Image.startsWith("data:image/png;base64,")) {
                    base64Image.substring("data:image/png;base64,".length)
                } else {
                    base64Image
                }

                val imageBytes = Base64.decode(base64Content, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                binding.imageViewReagentDetail.setImageBitmap(bitmap)
                binding.imageViewReagentDetail.visibility = View.VISIBLE
            } catch (e: Exception) {
                binding.imageViewReagentDetail.visibility = View.GONE
            }
        } ?: run {
            binding.imageViewReagentDetail.visibility = View.GONE
        }

        if (reagent.barcode !== null) {
            binding.progressBarBarcode.visibility = View.VISIBLE
            binding.imageViewBarcode.visibility = View.GONE
            binding.textViewBarcodeError.visibility = View.GONE
            viewModel.generateBarcode(reagent.barcode!!)
        } else {
            binding.progressBarBarcode.visibility = View.GONE
            binding.imageViewBarcode.visibility = View.GONE
            binding.textViewBarcodeError.visibility = View.VISIBLE
        }

        reagent.storageObject.let { location ->
            binding.textViewLocation.text = location.objectName
            location.id.let { locationId ->
                viewModel.loadLocationPath(locationId)
            }
        }

    }

    private fun formatExpiryDate(expiryDateStr: String?): String {
        if (expiryDateStr.isNullOrEmpty()) return "N/A"

        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val expiryDate = dateFormat.parse(expiryDateStr) ?: return expiryDateStr
            val currentDate = Calendar.getInstance().time

            val diffMs = expiryDate.time - currentDate.time
            val diffDays = diffMs / (24 * 60 * 60 * 1000)

            return when {
                diffDays < 0 -> "$expiryDateStr (Expired ${-diffDays} days ago)"
                diffDays == 0L -> "$expiryDateStr (Expires today)"
                diffDays == 1L -> "$expiryDateStr (Expires tomorrow)"
                diffDays < 30 -> "$expiryDateStr (Expires in $diffDays days)"
                else -> "$expiryDateStr (${diffDays} days left)"
            }
        } catch (e: Exception) {
            return expiryDateStr
        }
    }

    private fun formatDate(timestamp: String?): String {
        if (timestamp.isNullOrEmpty()) return "Unknown"

        try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            format.timeZone = TimeZone.getTimeZone("UTC")
            val date = format.parse(timestamp) ?: return timestamp
            val diffMs = System.currentTimeMillis() - date.time

            return when {
                diffMs < 60 * 1000 -> "Just now"
                diffMs < 60 * 60 * 1000 -> "${diffMs / (60 * 1000)} min ago"
                diffMs < 24 * 60 * 60 * 1000 -> "${diffMs / (60 * 60 * 1000)} hours ago"
                diffMs < 7 * 24 * 60 * 60 * 1000 -> "${diffMs / (24 * 60 * 60 * 1000)} days ago"
                else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
            }
        } catch (e: Exception) {
            Log.e("StoredReagentDetailFragment", "Error formatting date: $timestamp", e)
            return timestamp
        }
    }

    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.contentLayout.visibility = View.GONE
        binding.layoutError.visibility = View.VISIBLE
        binding.textViewError.text = message
    }

    override fun onDestroyView() {
        progressDialog?.dismiss()
        progressDialog = null
        super.onDestroyView()
        _binding = null
    }

    private fun setupReagentActionsList() {
        actionAdapter = ReagentActionAdapter()
        binding.swipeRefreshLayoutActions.layoutParams.height = resources.getDimensionPixelSize(
            android.R.dimen.thumbnail_height
        )
        binding.recyclerViewActions.apply {
            adapter = actionAdapter
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5
                        && firstVisibleItemPosition >= 0) {
                        arguments?.getInt("REAGENT_ID", -1)?.let { reagentId ->
                            if (reagentId != -1) {
                                viewModel.loadMoreActions(reagentId)
                            }
                        }
                    }
                }
            })
        }

        binding.swipeRefreshLayoutActions.setOnRefreshListener {
            arguments?.getInt("REAGENT_ID", -1)?.let { reagentId ->
                if (reagentId != -1) {
                    viewModel.loadReagentActions(reagentId, true)
                }
            }
        }
    }

    private fun showReagentActionDialog(actionType: String) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(
            R.layout.dialog_reagent_action, null
        )

        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val editTextQuantity = dialogView.findViewById<TextInputEditText>(R.id.editTextQuantity)
        val textViewUnit = dialogView.findViewById<TextView>(R.id.textViewUnit)
        val editTextNotes = dialogView.findViewById<TextInputEditText>(R.id.editTextNotes)

        dialogTitle.text = if (actionType == "ADD") "Add Reagent" else "Reserve Reagent"
        textViewUnit.text = viewModel.getReagentUnit() ?: ""

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Confirm") { _, _ ->
                val quantityText = editTextQuantity.text.toString()
                val notes = editTextNotes.text.toString().takeIf { it.isNotBlank() }

                if (quantityText.isNotEmpty()) {
                    val quantity = quantityText.toFloatOrNull() ?: 0f

                    if (quantity <= 0) {
                        Snackbar.make(binding.root, "Quantity must be greater than 0", Snackbar.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    viewLifecycleOwner.lifecycleScope.launch {
                        val result = if (actionType == "ADD") {
                            viewModel.addReagent(quantity, notes)
                        } else {
                            viewModel.reserveReagent(quantity, notes)
                        }

                        result.collect { actionResult ->
                            actionResult.fold(
                                onSuccess = {
                                    arguments?.getInt("REAGENT_ID", -1)?.let { reagentId ->
                                        if (reagentId != -1) {
                                            viewModel.loadStoredReagent(reagentId)
                                            viewModel.loadReagentActions(reagentId, true)
                                        }
                                    }

                                    Snackbar.make(
                                        binding.root,
                                        if (actionType == "ADD") "Reagent added successfully" else "Reagent reserved successfully",
                                        Snackbar.LENGTH_SHORT
                                    ).show()
                                },
                                onFailure = { error ->
                                    Log.d("StoredReagentDetailFragment", "Error adding/reserving reagent: ${error.message}")
                                    Snackbar.make(
                                        binding.root,
                                        "Error: ${error.message ?: "Unknown error"}",
                                        Snackbar.LENGTH_LONG
                                    ).show()
                                }
                            )
                        }
                    }
                } else {
                    Snackbar.make(binding.root, "Please enter a quantity", Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun showEditReagentDialog() {
        val currentReagent = viewModel.storedReagent.value ?: return

        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_edit_stored_reagent, null)

        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val editTextQuantity = dialogView.findViewById<TextInputEditText>(R.id.editTextQuantity)
        val editTextNotes = dialogView.findViewById<TextInputEditText>(R.id.editTextNotes)
        val editTextExpiryDate = dialogView.findViewById<TextInputEditText>(R.id.editTextExpiryDate)
        val switchShareable = dialogView.findViewById<SwitchMaterial>(R.id.switchShareable)

        dialogTitle.text = "Edit ${currentReagent.reagent.name}"
        editTextQuantity.setText(currentReagent.quantity.toString())
        editTextNotes.setText(currentReagent.notes)
        editTextExpiryDate.setText(currentReagent.expirationDate ?: "")
        switchShareable.isChecked = currentReagent.shareable

        editTextExpiryDate.setOnClickListener {
            val calendar = Calendar.getInstance()

            if (!editTextExpiryDate.text.isNullOrEmpty()) {
                try {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val date = dateFormat.parse(editTextExpiryDate.text.toString())
                    if (date != null) {
                        calendar.time = date
                    }
                } catch (e: Exception) {
                }
            }

            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(selectedYear, selectedMonth, selectedDay)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                editTextExpiryDate.setText(dateFormat.format(selectedDate.time))
            }, year, month, day).show()
        }

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                try {
                    val updatedQuantity = editTextQuantity.text.toString().toFloat()
                    val updatedNotes = editTextNotes.text.toString()
                    val updatedExpiryDate = editTextExpiryDate.text.toString().takeIf { it.isNotEmpty() }
                    val updatedShareable = switchShareable.isChecked

                    val updatedReagent = currentReagent.copy(
                        quantity = updatedQuantity,
                        notes = updatedNotes,
                        expirationDate = updatedExpiryDate,
                        shareable = updatedShareable
                    )

                    saveReagent(updatedReagent)
                } catch (e: Exception) {
                    Snackbar.make(binding.root, "Invalid input values", Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveReagent(reagent: StoredReagent) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.saveStoredReagent(reagent).collect { result ->
                result.fold(
                    onSuccess = { savedReagent ->
                        Snackbar.make(binding.root, "Reagent saved successfully", Snackbar.LENGTH_SHORT).show()
                        viewModel.loadStoredReagent(savedReagent.id)
                    },
                    onFailure = { error ->
                        Snackbar.make(binding.root, "Error: ${error.message}", Snackbar.LENGTH_LONG).show()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }

}