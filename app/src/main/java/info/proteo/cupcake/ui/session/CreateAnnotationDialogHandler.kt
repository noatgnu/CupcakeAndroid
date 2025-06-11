package info.proteo.cupcake.ui.session

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import info.proteo.cupcake.R
import info.proteo.cupcake.data.remote.model.instrument.Instrument
import info.proteo.cupcake.data.remote.service.CreateAnnotationRequest
import info.proteo.cupcake.data.repository.InstrumentRepository
import info.proteo.cupcake.data.repository.UserRepository
import info.proteo.cupcake.ui.instrument.InstrumentAdapter
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.toString

class CreateAnnotationDialogHandler(
    private val fragment: Fragment,
    private val lifecycleOwner: LifecycleOwner,
    private val instrumentRepository: InstrumentRepository,
    private val onAnnotationCreated: (CreateAnnotationRequest) -> Unit
) {
    private var dialogView: View? = null
    private var dialog: AlertDialog? = null

    // Media handling variables
    private var currentPhotoPath: String? = null
    private var selectedImageUri: Uri? = null
    private var recordedAudioUri: Uri? = null
    private var recordedVideoUri: Uri? = null
    private var selectedInstrumentId: Int? = null
    private var selectedFileUri: Uri? = null
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var instrumentCurrentOffset = 0
    private var instrumentPageSize = 5
    private var isLoading = false
    private var selectedInstrument: Instrument? = null
    private var hasMoreItems: Boolean = false

    private var currentInstrumentPage = 0
    private val instrumentsPerPage = 10
    private var hasMoreInstruments = false
    private var currentSearchQuery = ""

    private var instrumentListAdapter: InstrumentAdapter? = null

    // Activity result launchers
    private val takePictureLauncher = fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            currentPhotoPath?.let {
                selectedImageUri = Uri.fromFile(File(it))
                updateImagePreview()
            }
        }
    }

    private val pickImageLauncher = fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                updateImagePreview()
            }
        }
    }

    private val videoRecordLauncher = fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                recordedVideoUri = uri
                dialogView?.findViewById<TextView>(R.id.textVideoStatus)?.apply {
                    text = "Video recorded successfully"
                    visibility = View.VISIBLE
                }
            }
        }
    }

    private val pickFileLauncher = fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedFileUri = uri
                val fileName = getFileNameFromUri(uri)
                dialogView?.findViewById<TextView>(R.id.textSelectedFileName)?.apply {
                    text = "Selected file: $fileName"
                    visibility = View.VISIBLE
                }
            }
        }
    }

    private fun showInstrumentSelectionDialog() {
        val dialogView = LayoutInflater.from(fragment.requireContext())
            .inflate(R.layout.dialog_instrument_selection, null)

        // Initialize UI components
        val searchView = dialogView.findViewById<SearchView>(R.id.searchInstrument)
        val recyclerViewInstruments = dialogView.findViewById<RecyclerView>(R.id.recyclerViewInstruments)
        val noResultsText = dialogView.findViewById<TextView>(R.id.textViewNoResults)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBar)
        val prevButton = dialogView.findViewById<Button>(R.id.buttonPrevPage)
        val nextButton = dialogView.findViewById<Button>(R.id.buttonNextPage)
        // val selectedInstrumentText = dialogView.findViewById<TextView>(R.id.textSelectedInstrument) // This is for the selection dialog's own text view

        // Setup RecyclerView
        recyclerViewInstruments.layoutManager = LinearLayoutManager(fragment.requireContext())

        // Create and set adapter
        instrumentListAdapter = InstrumentAdapter { instrumentId ->
            lifecycleOwner.lifecycleScope.launch {
                instrumentRepository.getInstrumentPermissionFor(instrumentId, "").collect { result ->
                    result.onSuccess { permission ->
                        val hasPermission = permission.canBook || permission.canManage
                        if (hasPermission) {
                            selectedInstrumentId = instrumentId
                            // Find the selected instrument in the adapter's current list
                            instrumentListAdapter?.currentList?.find { it.id == instrumentId }?.let { instrument ->
                                // Update the TextView in the main CreateAnnotationDialog
                                this@CreateAnnotationDialogHandler.dialogView?.findViewById<TextView>(R.id.textSelectedInstrument)?.apply {
                                    text = "Selected: ${instrument.instrumentName}"
                                    visibility = View.VISIBLE
                                }
                                selectedInstrument = instrument // Store the selected instrument object
                            }
                            // Dismiss the instrument selection dialog
                            // 'this.dialog' should be the AlertDialog instance of the instrument selection dialog
                            this@CreateAnnotationDialogHandler.dialog?.dismiss()
                        } else {
                            Toast.makeText(fragment.context, "You don't have permission to use this instrument", Toast.LENGTH_SHORT).show()
                        }
                    }
                    result.onFailure {
                        Toast.makeText(fragment.context, "Failed to check permission", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        recyclerViewInstruments.adapter = instrumentListAdapter

        // Set up pagination buttons
        updatePaginationButtons(prevButton, nextButton) // This function needs to be defined or accessible

        prevButton.setOnClickListener {
            if (instrumentListAdapter != null) {
                if (currentInstrumentPage > 0) {
                    currentInstrumentPage--
                    loadInstruments(instrumentListAdapter!!, searchView.query.toString(), progressBar, noResultsText, recyclerViewInstruments, prevButton, nextButton)
                }
            }

        }

        nextButton.setOnClickListener {
            if (hasMoreInstruments) {
                currentInstrumentPage++
                loadInstruments(instrumentListAdapter!!, searchView.query.toString(), progressBar, noResultsText, recyclerViewInstruments, prevButton, nextButton)
            }
        }

        // Set up search functionality
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                currentSearchQuery = newText ?: ""
                currentInstrumentPage = 0 // Reset to first page on new search
                loadInstruments(instrumentListAdapter!!, currentSearchQuery, progressBar, noResultsText, recyclerViewInstruments, prevButton, nextButton)
                return true
            }
        })

        // Create and show dialog
        val instrumentDialog = AlertDialog.Builder(fragment.requireContext())
            .setTitle("Select Instrument")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .create()

        // Initial load of instruments
        loadInstruments(instrumentListAdapter!!, "", progressBar, noResultsText, recyclerViewInstruments, prevButton, nextButton)

        instrumentDialog.show()
        this.dialog = instrumentDialog
    }


    private fun checkInstrumentPermission(instrumentId: Int, callback: (Boolean) -> Unit) {
        lifecycleOwner.lifecycleScope.launch {
            try {
                instrumentRepository.getInstrumentPermissionFor(instrumentId, "").collect { result ->
                    result.onSuccess { permission ->
                        val hasPermission = permission.canBook || permission.canManage
                        callback(hasPermission)
                    }

                    result.onFailure {
                        callback(false)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking instrument permission", e)
                callback(false)
            }
        }
    }

    private fun setupAnnotationTypeSpinner() {
        val spinner = dialogView?.findViewById<Spinner>(R.id.spinnerAnnotationType)
        val annotationTypes = arrayOf("Text", "Image", "File", "Audio", "Video", "Calculator", "Molarity Calculator", "Instrument")
        val adapter = ArrayAdapter(fragment.requireContext(), android.R.layout.simple_spinner_item, annotationTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner?.adapter = adapter

        spinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                updateContainerVisibility(annotationTypes[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }
    }

    private fun updateContainerVisibility(selectedType: String) {
        val containers = mapOf(
            "Text" to R.id.textAnnotationContainer,
            "Image" to R.id.imageAnnotationContainer,
            "File" to R.id.fileAnnotationContainer,
            "Audio" to R.id.audioAnnotationContainer,
            "Video" to R.id.videoAnnotationContainer,
            "Calculator" to R.id.calculatorAnnotationContainer,
            "Molarity Calculator" to R.id.molarityCalculatorContainer,
            "Instrument" to R.id.instrumentAnnotationContainer
        )

        containers.forEach { (type, containerId) ->
            dialogView?.findViewById<View>(containerId)?.visibility =
                if (type == selectedType) View.VISIBLE else View.GONE
        }

        // Notes container is visible for all types except calculators
        dialogView?.findViewById<View>(R.id.notesContainer)?.visibility =
            if (selectedType == "Calculator" || selectedType == "Molarity Calculator") View.GONE else View.VISIBLE
    }

    private fun setupButtonListeners(sessionId: String, stepId: Int) {
        // Image buttons
        dialogView?.findViewById<Button>(R.id.buttonTakePhoto)?.setOnClickListener { captureImage() }
        dialogView?.findViewById<Button>(R.id.buttonSelectImage)?.setOnClickListener { selectImageFromGallery() }

        // File button
        dialogView?.findViewById<Button>(R.id.buttonSelectFile)?.setOnClickListener { selectFile() }

        // Audio button
        dialogView?.findViewById<Button>(R.id.buttonRecordAudio)?.setOnClickListener {
            if (isRecording) {
                stopAudioRecording()
            } else {
                startAudioRecording()
            }
        }

        // Video button
        dialogView?.findViewById<Button>(R.id.buttonRecordVideo)?.setOnClickListener { startVideoRecording() }

        // Instrument button
        dialogView?.findViewById<Button>(R.id.buttonSelectInstrument)?.setOnClickListener { showInstrumentSelectionDialog() }
    }

    private fun createAnnotation(sessionId: String, stepId: Int) {
        val spinner = dialogView?.findViewById<Spinner>(R.id.spinnerAnnotationType)
        val selectedType = spinner?.selectedItem as String

        val annotationText = when (selectedType) {
            "Text" -> dialogView?.findViewById<EditText>(R.id.editTextAnnotation)?.text?.toString() ?: ""
            "Calculator" -> "{}" // Empty JSON for calculator
            "Molarity Calculator" -> "[]" // Empty array for molarity calculator
            else -> ""
        }

        val notes = dialogView?.findViewById<EditText>(R.id.editTextNotes)?.text?.toString()

        val annotationType = when (selectedType) {
            "Text" -> "text"
            "Image" -> "image"
            "File" -> "file"
            "Audio" -> "audio"
            "Video" -> "video"
            "Calculator" -> "calculator"
            "Molarity Calculator" -> "mcalculator"
            "Instrument" -> "instrument"
            else -> "text"
        }

        val request = CreateAnnotationRequest(
            annotation = annotationText,
            annotationType = annotationType,
            session = sessionId,
            step = stepId,
            storedReagent = null,
            maintenance = null,
            instrument = selectedInstrumentId,
            timeStarted = null,
            timeEnded = null,
            instrumentJob = null,
            instrumentUserType = null
        )

        onAnnotationCreated(request)
    }

    private fun captureImage() {
        // Check if camera permission is granted
        if (ContextCompat.checkSelfPermission(fragment.requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            fragment.requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
            return
        }

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val photoFile = createImageFile()
        photoFile?.let {
            val photoURI = FileProvider.getUriForFile(
                fragment.requireContext(),
                "info.proteo.cupcake.fileprovider",
                it
            )
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            takePictureLauncher.launch(intent)
        }
    }

    private fun selectImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun createImageFile(): File? {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = fragment.requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            return File.createTempFile(
                "JPEG_${timeStamp}_",
                ".jpg",
                storageDir
            ).apply {
                currentPhotoPath = absolutePath
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error creating image file", e)
            return null
        }
    }

    private fun startAudioRecording() {
        // Check for recording permission
        if (ContextCompat.checkSelfPermission(fragment.requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            fragment.requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_AUDIO_PERMISSION)
            return
        }

        val fileName = "${fragment.requireContext().externalCacheDir?.absolutePath}/audio_recording.3gp"
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(fileName)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

            try {
                prepare()
                start()
                isRecording = true

                // Update UI to show recording in progress
                dialogView?.findViewById<TextView>(R.id.textAudioStatus)?.apply {
                    text = "Recording in progress..."
                    visibility = View.VISIBLE
                }

                // Change button text
                dialogView?.findViewById<Button>(R.id.buttonRecordAudio)?.text = "Stop Recording"

                recordedAudioUri = Uri.fromFile(File(fileName))
            } catch (e: Exception) {
                Log.e(TAG, "Error preparing media recorder", e)
                Toast.makeText(fragment.requireContext(), "Failed to start recording", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun stopAudioRecording() {
        if (isRecording) {
            try {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
                mediaRecorder = null
                isRecording = false

                // Update UI to show recording completed
                dialogView?.findViewById<TextView>(R.id.textAudioStatus)?.apply {
                    text = "Recording completed"
                    visibility = View.VISIBLE
                }

                // Reset button text
                dialogView?.findViewById<Button>(R.id.buttonRecordAudio)?.text = "Record Audio"
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping media recorder", e)
                Toast.makeText(fragment.requireContext(), "Failed to stop recording", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startVideoRecording() {
        // Check for camera and audio recording permissions
        if (ContextCompat.checkSelfPermission(fragment.requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(fragment.requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            fragment.requestPermissions(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
                REQUEST_VIDEO_PERMISSION
            )
            return
        }

        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        videoRecordLauncher.launch(intent)
    }

    private fun selectFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        pickFileLauncher.launch(intent)
    }


    private suspend fun checkInstrumentBookingPermission(instrumentId: Int): Boolean {
        return try {
            var canBook = false
            instrumentRepository.getInstrumentPermission(instrumentId).collect { result ->
                result.onSuccess { permission ->
                    canBook = permission.canBook
                }
            }
            canBook
        } catch (e: Exception) {
            false
        }
    }



    private fun updateImagePreview() {
        dialogView?.findViewById<ImageView>(R.id.previewImage)?.apply {
            visibility = View.VISIBLE
            setImageURI(selectedImageUri)
        }
    }

    private fun getFileNameFromUri(uri: Uri): String {
        val context = fragment.requireContext()
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    return cursor.getString(nameIndex)
                }
            }
        }
        return uri.lastPathSegment ?: "Unknown file"
    }

    private fun updatePaginationButtons(prevButton: Button, nextButton: Button) {
        prevButton.isEnabled = currentInstrumentPage > 0
        nextButton.isEnabled = hasMoreInstruments
    }

    private fun loadInstruments(
        adapter: InstrumentAdapter,
        query: String,
        progressBar: ProgressBar,
        noResultsText: TextView,
        recyclerView: RecyclerView,
        prevButton: Button,
        nextButton: Button
    ) {
        progressBar.visibility = View.VISIBLE
        noResultsText.visibility = View.GONE

        val offset = currentInstrumentPage * instrumentsPerPage

        lifecycleOwner.lifecycleScope.launch {
            try {
                instrumentRepository.getInstruments(
                    search = query.ifEmpty { null },
                    limit = instrumentsPerPage,
                    offset = offset
                ).collect { result ->
                    progressBar.visibility = View.GONE

                    result.onSuccess { response ->
                        val instruments = response.results
                        hasMoreInstruments = response.next != null

                        if (instruments.isEmpty()) {
                            noResultsText.visibility = View.VISIBLE
                            recyclerView.visibility = View.GONE
                        } else {
                            noResultsText.visibility = View.GONE
                            recyclerView.visibility = View.VISIBLE
                            adapter.submitList(instruments)
                        }

                        // Update pagination buttons
                        updatePaginationButtons(prevButton, nextButton)
                    }

                    result.onFailure { error ->
                        Toast.makeText(fragment.context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                        noResultsText.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                noResultsText.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
                Log.e(TAG, "Error loading instruments", e)
            }
        }
    }

    fun showCreateAnnotationDialog(sessionId: String, stepId: Int) {
        // Inflate the dialog layout
        dialogView = LayoutInflater.from(fragment.requireContext())
            .inflate(R.layout.dialog_create_annotation, null)

        // Get references to relevant views
        val spinnerAnnotationType = dialogView?.findViewById<Spinner>(R.id.spinnerAnnotationType)
        val textAnnotationContainer = dialogView?.findViewById<LinearLayout>(R.id.textAnnotationContainer)
        val fileAnnotationContainer = dialogView?.findViewById<MaterialCardView>(R.id.fileAnnotationContainer)
        val imageAnnotationContainer = dialogView?.findViewById<MaterialCardView>(R.id.imageAnnotationContainer)
        val audioAnnotationContainer = dialogView?.findViewById<MaterialCardView>(R.id.audioAnnotationContainer)
        val videoAnnotationContainer = dialogView?.findViewById<MaterialCardView>(R.id.videoAnnotationContainer)
        val calculatorAnnotationContainer = dialogView?.findViewById<LinearLayout>(R.id.calculatorAnnotationContainer)
        val molarityCalculatorContainer = dialogView?.findViewById<LinearLayout>(R.id.molarityCalculatorContainer)
        val instrumentAnnotationContainer = dialogView?.findViewById<MaterialCardView>(R.id.instrumentAnnotationContainer)
        val notesContainer = dialogView?.findViewById<LinearLayout>(R.id.notesContainer)

        notesContainer?.visibility = View.GONE

        // Set up annotation type spinner
        val annotationTypes = arrayOf("Text", "File", "Image", "Audio", "Video", "Calculator", "Molarity Calculator", "Instrument")
        val adapter = ArrayAdapter(fragment.requireContext(), android.R.layout.simple_spinner_item, annotationTypes)

        spinnerAnnotationType?.adapter = adapter

        // Set up annotation type selection listener
        spinnerAnnotationType?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Hide all containers first
                textAnnotationContainer?.visibility = View.GONE
                fileAnnotationContainer?.visibility = View.GONE
                imageAnnotationContainer?.visibility = View.GONE
                audioAnnotationContainer?.visibility = View.GONE
                videoAnnotationContainer?.visibility = View.GONE
                calculatorAnnotationContainer?.visibility = View.GONE
                molarityCalculatorContainer?.visibility = View.GONE
                instrumentAnnotationContainer?.visibility = View.GONE

                // Show the appropriate container based on selection
                when (position) {
                    0 -> textAnnotationContainer?.visibility = View.VISIBLE
                    1 -> {
                        fileAnnotationContainer?.visibility = View.VISIBLE
                        textAnnotationContainer?.visibility = View.VISIBLE // Allow text for file annotations
                    }
                    2 -> {
                        imageAnnotationContainer?.visibility = View.VISIBLE
                        //textAnnotationContainer?.visibility = View.VISIBLE // Allow text for image annotations
                    }
                    3 -> {
                        audioAnnotationContainer?.visibility = View.VISIBLE
                        //textAnnotationContainer?.visibility = View.VISIBLE // Allow text for audio annotations
                    }
                    4 -> {
                        videoAnnotationContainer?.visibility = View.VISIBLE
                        //textAnnotationContainer?.visibility = View.VISIBLE // Allow text for video annotations
                    }
                    5 -> calculatorAnnotationContainer?.visibility = View.VISIBLE
                    6 -> molarityCalculatorContainer?.visibility = View.VISIBLE
                    7 -> instrumentAnnotationContainer?.visibility = View.VISIBLE
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }

        //setupFileSelectionButton(dialogView)
        //setupImageSelectionButtons(dialogView)
        //setupAudioRecordingButton(dialogView)
        //setupVideoRecordingButton(dialogView)
        //setupInstrumentSelectionButton(dialogView)

        // Create and show the dialog
        dialog = AlertDialog.Builder(fragment.requireContext())
            .setTitle("Create Annotation")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val selectedAnnotationType = spinnerAnnotationType?.selectedItemPosition
                val annotationText = dialogView?.findViewById<EditText>(R.id.editTextAnnotation)?.text.toString()

                val request = when (selectedAnnotationType) {
                    0 -> createTextAnnotationRequest(sessionId, stepId, annotationText)
                    1 -> createFileAnnotationRequest(sessionId, stepId, annotationText)
                    2 -> createImageAnnotationRequest(sessionId, stepId, annotationText)
                    3 -> createAudioAnnotationRequest(sessionId, stepId, annotationText)
                    4 -> createVideoAnnotationRequest(sessionId, stepId, annotationText)
                    5 -> createCalculatorAnnotationRequest(sessionId, stepId)
                    6 -> createMolarityCalculatorAnnotationRequest(sessionId, stepId)
                    7 -> createInstrumentAnnotationRequest(sessionId, stepId, annotationText)
                    else -> createTextAnnotationRequest(sessionId, stepId, annotationText)
                }
                processAnnotationRequest(request, selectedAnnotationType!!)
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog?.show()
    }

    private fun createTextAnnotationRequest(sessionId: String, stepId: Int, text: String): CreateAnnotationRequest {
        return CreateAnnotationRequest(
            annotation = text,
            annotationType = "text",
            session = sessionId,
            step = stepId
        )
    }

    private fun createFileAnnotationRequest(sessionId: String, stepId: Int, text: String): CreateAnnotationRequest {
        return CreateAnnotationRequest(
            annotation = text,
            annotationType = "file",
            session = sessionId,
            step = stepId
        )
    }

    private fun createImageAnnotationRequest(sessionId: String, stepId: Int, text: String): CreateAnnotationRequest {
        return CreateAnnotationRequest(
            annotation = text,
            annotationType = "image",
            session = sessionId,
            step = stepId
        )
    }

    private fun createAudioAnnotationRequest(sessionId: String, stepId: Int, text: String): CreateAnnotationRequest {
        return CreateAnnotationRequest(
            annotation = text,
            annotationType = "audio",
            session = sessionId,
            step = stepId
        )
    }

    private fun createVideoAnnotationRequest(sessionId: String, stepId: Int, text: String): CreateAnnotationRequest {
        return CreateAnnotationRequest(
            annotation = text,
            annotationType = "video",
            session = sessionId,
            step = stepId
        )
    }

    private fun createCalculatorAnnotationRequest(sessionId: String, stepId: Int): CreateAnnotationRequest {
        return CreateAnnotationRequest(
            annotation = "",
            annotationType = "calculator",
            session = sessionId,
            step = stepId
        )
    }

    private fun createMolarityCalculatorAnnotationRequest(sessionId: String, stepId: Int): CreateAnnotationRequest {
        return CreateAnnotationRequest(
            annotation = "",
            annotationType = "molarity_calculator",
            session = sessionId,
            step = stepId
        )
    }

    private fun createInstrumentAnnotationRequest(sessionId: String, stepId: Int, text: String): CreateAnnotationRequest {
        return CreateAnnotationRequest(
            annotation = text,
            annotationType = "instrument",
            session = sessionId,
            step = stepId,
            instrument = selectedInstrumentId
        )
    }

    private fun processAnnotationRequest(request: CreateAnnotationRequest, annotationType: Int) {
        when (annotationType) {
            //1 -> processFileAnnotation(request)
            //2 -> processImageAnnotation(request)
            //3 -> processAudioAnnotation(request)
            //4 -> processVideoAnnotation(request)
            //else -> onAnnotationCreated(request)
        }
    }





    companion object {
        private const val TAG = "AnnotationDialog"
        private const val REQUEST_CAMERA_PERMISSION = 100
        private const val REQUEST_AUDIO_PERMISSION = 101
        private const val REQUEST_VIDEO_PERMISSION = 102
    }
}
