package info.proteo.cupcake.ui.session

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import info.proteo.cupcake.R
import info.proteo.cupcake.data.remote.model.instrument.Instrument
import info.proteo.cupcake.data.remote.model.instrument.InstrumentUsage
import info.proteo.cupcake.data.remote.service.CreateAnnotationRequest
import info.proteo.cupcake.data.repository.InstrumentRepository
import info.proteo.cupcake.data.repository.InstrumentUsageRepository
import info.proteo.cupcake.ui.instrument.InstrumentAdapter
import info.proteo.cupcake.ui.instrument.InstrumentUsageAdapter
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.compareTo
import kotlin.div
import kotlin.text.compareTo
import kotlin.text.format
import kotlin.text.toDouble

class CreateAnnotationDialogHandler(
    private val fragment: Fragment,
    private val lifecycleOwner: LifecycleOwner,
    private val instrumentRepository: InstrumentRepository,
    private val instrumentUsageRepository: InstrumentUsageRepository,
    private val onAnnotationCreated: (request: CreateAnnotationRequest, filePart: MultipartBody.Part?) -> Unit
) {
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

    private var instrumentBookings: List<InstrumentUsage> = emptyList()
    private var selectedStartTime: Long? = null
    private var selectedEndTime: Long? = null

    private lateinit var instrumentListAdapter: InstrumentAdapter

    private var photoUri: Uri? = null
    private var videoFile: File? = null
    private var videoUri: Uri? = null
    private var audioFile: File? = null
    private var audioUri: Uri? = null
    private var mediaRecorder: MediaRecorder? = null
    private var isAudioRecording = false

    private var selectedFileUri: Uri? = null

    // Permission request launchers
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var audioPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var videoPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var storagePermissionLauncher: ActivityResultLauncher<String>

    // Activity result launchers
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private lateinit var selectImageLauncher: ActivityResultLauncher<String>
    private lateinit var selectFileLauncher: ActivityResultLauncher<String>
    private lateinit var takeVideoLauncher: ActivityResultLauncher<Uri>


    private var dialogView: View? = null
    private var instrumentBookingDialogView: View? = null
    private var instrumentSelectionDialogView: View? = null


    init {
        // Initialize permission launchers
        initPermissionLaunchers()
        initActivityResultLaunchers()
    }

    private fun initPermissionLaunchers() {
        cameraPermissionLauncher = fragment.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                takePicture()
            } else {
                Toast.makeText(fragment.requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }

        audioPermissionLauncher = fragment.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                startAudioRecording()
            } else {
                Toast.makeText(fragment.requireContext(), "Microphone permission denied", Toast.LENGTH_SHORT).show()
            }
        }

        videoPermissionLauncher = fragment.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions.all { it.value }) {
                startVideoRecording()
            } else {
                Toast.makeText(fragment.requireContext(), "Video recording permissions denied", Toast.LENGTH_SHORT).show()
            }
        }

        storagePermissionLauncher = fragment.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                openFilePicker()
            } else {
                Toast.makeText(fragment.requireContext(), "Storage permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun initActivityResultLaunchers() {
        takePictureLauncher = fragment.registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            if (success && photoUri != null) {
                handleImageCaptured(photoUri!!)
            }
        }

        selectImageLauncher = fragment.registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let { handleImageSelected(it) }
        }

        selectFileLauncher = fragment.registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let { handleFileSelected(it) }
        }

        takeVideoLauncher = fragment.registerForActivityResult(
            ActivityResultContracts.CaptureVideo()
        ) { success ->
            if (success && videoUri != null) {
                handleVideoCaptured(videoUri!!)
            }
        }
    }

    fun showCreateAnnotationDialog(sessionId: String, stepId: Int) {
        val dialogView = LayoutInflater.from(fragment.requireContext())
            .inflate(R.layout.dialog_create_annotation, null)

        this.dialogView = dialogView
        // Initialize UI components
        val spinnerAnnotationType = dialogView.findViewById<Spinner>(R.id.spinnerAnnotationType)
        val textAnnotationContainer = dialogView.findViewById<View>(R.id.textAnnotationContainer)
        val imageAnnotationContainer = dialogView.findViewById<View>(R.id.imageAnnotationContainer)
        val fileAnnotationContainer = dialogView.findViewById<View>(R.id.fileAnnotationContainer)
        val audioAnnotationContainer = dialogView.findViewById<View>(R.id.audioAnnotationContainer)
        val videoAnnotationContainer = dialogView.findViewById<View>(R.id.videoAnnotationContainer)
        val calculatorAnnotationContainer = dialogView.findViewById<View>(R.id.calculatorAnnotationContainer)
        val molarityCalculatorContainer = dialogView.findViewById<View>(R.id.molarityCalculatorContainer)
        val instrumentAnnotationContainer = dialogView.findViewById<View>(R.id.instrumentAnnotationContainer)
        val editTextAnnotation = dialogView.findViewById<TextInputEditText>(R.id.editTextAnnotation)

        // Hide notes container - should never be used for annotations
        val notesContainer = dialogView.findViewById<View>(R.id.notesContainer)
        notesContainer.visibility = View.GONE

        // Reset state
        selectedFileUri = null
        photoUri = null
        videoUri = null
        selectedInstrument = null

        // Set up annotation types
        val annotationTypes = arrayOf("Text", "Image", "File", "Audio", "Video", "Calculator", "Molarity Calculator", "Instrument")
        val adapter = ArrayAdapter(fragment.requireContext(), android.R.layout.simple_spinner_item, annotationTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerAnnotationType.adapter = adapter

        // Set up the UI elements for each annotation type
        setupImageAnnotationContainer(dialogView)
        setupFileAnnotationContainer(dialogView)
        setupAudioAnnotationContainer(dialogView)
        setupVideoAnnotationContainer(dialogView)
        setupInstrumentAnnotationContainer(dialogView)

        // Handle annotation type selection
        spinnerAnnotationType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                // Hide all containers first
                textAnnotationContainer.visibility = View.GONE
                imageAnnotationContainer.visibility = View.GONE
                fileAnnotationContainer.visibility = View.GONE
                audioAnnotationContainer.visibility = View.GONE
                videoAnnotationContainer.visibility = View.GONE
                calculatorAnnotationContainer.visibility = View.GONE
                molarityCalculatorContainer.visibility = View.GONE
                instrumentAnnotationContainer.visibility = View.GONE

                // Show only relevant container
                when (annotationTypes[position]) {
                    "Text" -> textAnnotationContainer.visibility = View.VISIBLE
                    "Image" -> imageAnnotationContainer.visibility = View.VISIBLE
                    "File" -> {
                        fileAnnotationContainer.visibility = View.VISIBLE
                        textAnnotationContainer.visibility = View.VISIBLE // Show text for file annotations
                    }
                    "Audio" -> audioAnnotationContainer.visibility = View.VISIBLE
                    "Video" -> videoAnnotationContainer.visibility = View.VISIBLE
                    "Calculator" -> calculatorAnnotationContainer.visibility = View.VISIBLE
                    "Molarity Calculator" -> molarityCalculatorContainer.visibility = View.VISIBLE
                    "Instrument" -> instrumentAnnotationContainer.visibility = View.VISIBLE
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }

        // Create and show the dialog
        val dialog = AlertDialog.Builder(fragment.requireContext())
            .setTitle("Create Annotation")
            .setView(dialogView)
            .setPositiveButton("Create", null) // Set to null initially to prevent auto-dismiss
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()

        // Set up the positive button click listener
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val selectedType = annotationTypes[spinnerAnnotationType.selectedItemPosition]
            val annotationText = editTextAnnotation.text.toString().trim()

            // Validate inputs based on type
            if (validateAnnotation(selectedType, annotationText)) {
                // Create the annotation request
                var request = CreateAnnotationRequest(
                    annotation = annotationText,
                    annotationType = selectedType.lowercase(),
                    step = stepId,
                    session = sessionId
                )

                // Add additional data for specific annotation types
                when (selectedType) {
                    "Instrument" -> {
                        if (selectedStartTime == null || selectedEndTime == null || selectedInstrument == null) {
                            Toast.makeText(fragment.requireContext(), "Please select an instrument and time range", Toast.LENGTH_SHORT).show()
                        } else {
                            request = request.copy(
                                instrument = selectedInstrument?.id,
                                timeStarted = selectedStartTime?.let {
                                    SimpleDateFormat(
                                        "yyyy-MM-dd'T'HH:mm:ss",
                                        Locale.getDefault()
                                    ).format(Date(it))
                                },
                                timeEnded = selectedEndTime?.let {
                                    SimpleDateFormat(
                                        "yyyy-MM-dd'T'HH:mm:ss",
                                        Locale.getDefault()
                                    ).format(Date(it))
                                }
                            )
                        }
                    }
                }

                val filePart: MultipartBody.Part? = when (selectedType) {
                    "Image" -> {
                        photoUri?.let { uri ->
                            createFilePartFromUri(uri, "image/*")
                        }
                    }
                    "Audio" -> {
                        audioUri?.let { uri ->
                            createFilePartFromUri(uri, "audio/*")
                        }
                    }
                    "Video" -> {
                        videoUri?.let { uri ->
                            createFilePartFromUri(uri, "video/*")
                        }
                    }
                    "File" -> {
                        selectedFileUri?.let { uri ->
                            createFilePartFromUri(uri, null)
                        }
                    }
                    else -> null
                }

                onAnnotationCreated(request, filePart)
                dialog.dismiss()
            }
        }
    }

    private fun validateAnnotation(type: String, text: String): Boolean {
        when (type) {
            "Text" -> {
                if (text.isEmpty()) {
                    Toast.makeText(fragment.requireContext(), "Please enter text for the annotation", Toast.LENGTH_SHORT).show()
                    return false
                }
            }
            "File" -> {
                if (selectedFileUri == null) {
                    Toast.makeText(fragment.requireContext(), "Please select a file", Toast.LENGTH_SHORT).show()
                    return false
                }
            }
            "Image" -> {
                if (photoUri == null) {
                    Toast.makeText(fragment.requireContext(), "Please capture or select an image", Toast.LENGTH_SHORT).show()
                    return false
                }
            }
            "Video" -> {
                if (videoUri == null) {
                    Toast.makeText(fragment.requireContext(), "Please capture a video", Toast.LENGTH_SHORT).show()
                    return false
                }
            }
            "Audio" -> {
                if (audioUri == null) {
                    Toast.makeText(fragment.requireContext(), "Please record audio", Toast.LENGTH_SHORT).show()
                    return false
                }
            }
            "Instrument" -> {
                if (selectedInstrument == null) {
                    Toast.makeText(fragment.requireContext(), "Please select an instrument", Toast.LENGTH_SHORT).show()
                    return false
                }
            }
        }
        return true
    }

    private fun setupImageAnnotationContainer(dialogView: View) {
        val buttonTakePhoto = dialogView.findViewById<Button>(R.id.buttonTakePhoto)
        val buttonSelectImage = dialogView.findViewById<Button>(R.id.buttonSelectImage)
        val previewImage = dialogView.findViewById<ImageView>(R.id.previewImage)

        buttonTakePhoto.setOnClickListener {
            checkCameraPermission()
        }

        buttonSelectImage.setOnClickListener {
            selectImageLauncher.launch("image/*")
        }

        photoUri?.let {
            uri -> updateImagePreview(dialogView)
        }
    }

    private fun updateImagePreview(dialogView: View) {
        val previewImage = dialogView.findViewById<ImageView>(R.id.previewImage)
        photoUri?.let { uri ->
            try {
                // Clear any previous image and set the new one
                previewImage.setImageURI(null)
                previewImage.setImageURI(uri)
                previewImage.visibility = View.VISIBLE
            } catch (e: Exception) {
                Log.e("CreateAnnotationDialog", "Error loading image preview", e)
                Toast.makeText(
                    fragment.requireContext(),
                    "Failed to load image preview",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setupFileAnnotationContainer(dialogView: View) {
        val buttonSelectFile = dialogView.findViewById<Button>(R.id.buttonSelectFile)
        val textSelectedFileName = dialogView.findViewById<TextView>(R.id.textSelectedFileName)

        buttonSelectFile.setOnClickListener {
            checkStoragePermission()
        }
    }

    private fun setupAudioAnnotationContainer(dialogView: View) {
        val buttonRecordAudio = dialogView.findViewById<Button>(R.id.buttonRecordAudio)
        val textAudioStatus = dialogView.findViewById<TextView>(R.id.textAudioStatus)

        buttonRecordAudio.setOnClickListener {
            if (isAudioRecording) {
                stopAudioRecording()
            } else {
                checkAudioPermission()
            }
        }
    }

    private fun setupVideoAnnotationContainer(dialogView: View) {
        val buttonRecordVideo = dialogView.findViewById<Button>(R.id.buttonRecordVideo)
        val textVideoStatus = dialogView.findViewById<TextView>(R.id.textVideoStatus)

        buttonRecordVideo.setOnClickListener {
            checkVideoPermission()
        }
    }

    private fun setupInstrumentAnnotationContainer(dialogView: View) {
        val buttonSelectInstrument = dialogView.findViewById<Button>(R.id.buttonSelectInstrument)
        val textSelectedInstrument = dialogView.findViewById<TextView>(R.id.textSelectedInstrument)

        buttonSelectInstrument.setOnClickListener {
            showInstrumentSelectionDialog(textSelectedInstrument)
        }
    }

    private fun showInstrumentSelectionDialog(textSelectedInstrument: TextView) {
        val dialogView = LayoutInflater.from(fragment.requireContext())
            .inflate(R.layout.dialog_instrument_selection, null)
        this.instrumentSelectionDialogView = dialogView

        val searchView = dialogView.findViewById<SearchView>(R.id.searchInstrument)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerViewInstruments)
        val progressBar = dialogView.findViewById<View>(R.id.progressBar)
        val noResultsText = dialogView.findViewById<TextView>(R.id.textViewNoResults)
        val buttonPrevPage = dialogView.findViewById<Button>(R.id.buttonPrevPage)
        val buttonNextPage = dialogView.findViewById<Button>(R.id.buttonNextPage)

        // Setup recycler view
        recyclerView.layoutManager = LinearLayoutManager(fragment.requireContext())
        instrumentListAdapter = InstrumentAdapter { instrumentId ->
            lifecycleOwner.lifecycleScope.launch {
                val progressBar = dialogView?.findViewById<View>(R.id.progressBar)
                progressBar?.visibility = View.VISIBLE

                // Get instrument details
                val instrumentResult = instrumentRepository.getInstrument(instrumentId)
                instrumentResult.collect { result ->
                    result.onSuccess { instrument ->
                        selectedInstrument = instrument
                        val textSelectedInstrument = dialogView?.findViewById<TextView>(R.id.textSelectedInstrument)
                        textSelectedInstrument?.text = "Selected: ${instrument.instrumentName}"
                        textSelectedInstrument?.visibility = View.VISIBLE

                        // Get upcoming bookings
                        fetchUpcomingBookings(instrument.id)
                    }
                    result.onFailure { error ->
                        Toast.makeText(fragment.requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                progressBar?.visibility = View.GONE
            }
        }
        recyclerView.adapter = instrumentListAdapter

        // Initial loading of instruments
        loadInstruments(searchView.query.toString(), recyclerView, progressBar, noResultsText, buttonPrevPage, buttonNextPage)

        // Search functionality
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                currentSearchQuery = query ?: ""
                currentInstrumentPage = 0
                loadInstruments(currentSearchQuery, recyclerView, progressBar, noResultsText, buttonPrevPage, buttonNextPage)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) {
                    currentSearchQuery = ""
                    currentInstrumentPage = 0
                    loadInstruments(currentSearchQuery, recyclerView, progressBar, noResultsText, buttonPrevPage, buttonNextPage)
                }
                return true
            }
        })

        // Pagination
        buttonPrevPage.setOnClickListener {
            if (currentInstrumentPage > 0) {
                currentInstrumentPage--
                loadInstruments(currentSearchQuery, recyclerView, progressBar, noResultsText, buttonPrevPage, buttonNextPage)
            }
        }

        buttonNextPage.setOnClickListener {
            if (hasMoreInstruments) {
                currentInstrumentPage++
                loadInstruments(currentSearchQuery, recyclerView, progressBar, noResultsText, buttonPrevPage, buttonNextPage)
            }
        }

        val dialog = AlertDialog.Builder(fragment.requireContext())
            .setTitle("Select Instrument")
            .setView(dialogView)
            .setNegativeButton("Cancel", { dialog, _ ->
                selectedInstrument = null
                selectedStartTime = null
                selectedEndTime = null
                dialog.dismiss()
            })
            .setPositiveButton("Select", { dialog, _ ->
                if (selectedInstrument == null || selectedStartTime == null || selectedEndTime == null) {
                    Toast.makeText(fragment.requireContext(), "Please select an instrument", Toast.LENGTH_SHORT).show()
                } else {
                    updateMainDialogInstrumentInfo()
                    dialog.dismiss()
                }
            })
            .create()

        dialog.show()
    }

    private fun loadInstruments(
        query: String,
        recyclerView: RecyclerView,
        progressBar: View,
        noResultsText: TextView,
        buttonPrevPage: Button,
        buttonNextPage: Button
    ) {
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        noResultsText.visibility = View.GONE

        val offset = currentInstrumentPage * instrumentsPerPage
        val limit = instrumentsPerPage

        lifecycleOwner.lifecycleScope.launch {
            try {
                val result = instrumentRepository.getInstruments(
                    search = if (query.isNotEmpty()) query else null,
                    limit = limit,
                    offset = offset
                ).collect {
                    it.onSuccess { response ->
                        progressBar.visibility = View.GONE

                        if (response.results.isEmpty()) {
                            noResultsText.visibility = View.VISIBLE
                            recyclerView.visibility = View.GONE
                        } else {
                            noResultsText.visibility = View.GONE
                            recyclerView.visibility = View.VISIBLE
                            instrumentListAdapter?.submitList(response.results)
                        }

                        hasMoreInstruments = response.next != null
                        buttonPrevPage.isEnabled = currentInstrumentPage > 0
                        buttonNextPage.isEnabled = hasMoreInstruments
                    }
                    it.onFailure { error ->
                        progressBar.visibility = View.GONE
                        noResultsText.visibility = View.VISIBLE
                        noResultsText.text = "Error loading instruments: ${error.message}"
                        recyclerView.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                noResultsText.visibility = View.VISIBLE
                noResultsText.text = "Error: ${e.message}"
                recyclerView.visibility = View.GONE
            }
        }
    }

    // Permission handling methods
    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                fragment.requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                takePicture()
            }
            fragment.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                showPermissionRationaleDialog("Camera Permission",
                    "Camera permission is needed to take photos for annotations.",
                    Manifest.permission.CAMERA,
                    cameraPermissionLauncher)
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun checkAudioPermission() {
        when {
            ContextCompat.checkSelfPermission(
                fragment.requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                startAudioRecording()
            }
            fragment.shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) -> {
                showPermissionRationaleDialog("Microphone Permission",
                    "Microphone permission is needed to record audio for annotations.",
                    Manifest.permission.RECORD_AUDIO,
                    audioPermissionLauncher)
            }
            else -> {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun checkVideoPermission() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        } else {
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        if (permissions.all { permission ->
                ContextCompat.checkSelfPermission(fragment.requireContext(), permission) == PackageManager.PERMISSION_GRANTED
            }) {
            startVideoRecording()
        } else {
            videoPermissionLauncher.launch(permissions)
        }
    }

    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // On Android 13+, we don't need storage permission to pick files
            openFilePicker()
        } else {
            when {
                ContextCompat.checkSelfPermission(
                    fragment.requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    openFilePicker()
                }
                fragment.shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                    showPermissionRationaleDialog("Storage Permission",
                        "Storage permission is needed to select files for annotations.",
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        storagePermissionLauncher)
                }
                else -> {
                    storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }
    }

    private fun showPermissionRationaleDialog(
        title: String,
        message: String,
        permission: String,
        permissionLauncher: ActivityResultLauncher<String>
    ) {
        AlertDialog.Builder(fragment.requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Grant") { _, _ ->
                permissionLauncher.launch(permission)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun takePicture() {
        try {
            val photoFile = createImageFile()
            photoUri = FileProvider.getUriForFile(
                fragment.requireContext(),
                "${fragment.requireContext().packageName}.fileprovider",
                photoFile
            )
            takePictureLauncher.launch(photoUri!!)
        } catch (e: IOException) {
            Toast.makeText(fragment.requireContext(), "Error creating image file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startAudioRecording() {
        try {
            audioFile = createAudioFile()
            audioUri = FileProvider.getUriForFile(
                fragment.requireContext(),
                "${fragment.requireContext().packageName}.fileprovider",
                audioFile!!
            )

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(fragment.requireContext())
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(audioFile?.absolutePath)
                prepare()
                start()
            }

            isAudioRecording = true

            val buttonRecordAudio = dialogView?.findViewById<Button>(R.id.buttonRecordAudio)
            val textAudioStatus = dialogView?.findViewById<TextView>(R.id.textAudioStatus)

            buttonRecordAudio?.text = "Stop Recording"
            textAudioStatus?.text = "Recording in progress..."
            textAudioStatus?.visibility = View.VISIBLE

            buttonRecordAudio?.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_stop_recording, 0, 0, 0
            )

        } catch (e: Exception) {
            Log.e("CreateAnnotationDialog", "Error starting audio recording: ${e.message}", e)
            Toast.makeText(fragment.requireContext(),
                "Failed to start recording: ${e.message}",
                Toast.LENGTH_SHORT).show()
            resetAudioRecording()
        }
    }

    private fun stopAudioRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null

            // Update UI
            val buttonRecordAudio = dialogView?.findViewById<Button>(R.id.buttonRecordAudio)
            val textAudioStatus = dialogView?.findViewById<TextView>(R.id.textAudioStatus)

            buttonRecordAudio?.text = "Record Audio"
            textAudioStatus?.text = "Recording saved (${getFormattedFileSize(audioFile)})"

            // Reset recording icon
            buttonRecordAudio?.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_audio_record, 0, 0, 0
            )

            isAudioRecording = false
        } catch (e: Exception) {
            Log.e("CreateAnnotationDialog", "Error stopping audio recording: ${e.message}", e)
            Toast.makeText(fragment.requireContext(),
                "Failed to save recording: ${e.message}",
                Toast.LENGTH_SHORT).show()
            resetAudioRecording()
        }
    }

    private fun resetAudioRecording() {
        mediaRecorder?.release()
        mediaRecorder = null
        isAudioRecording = false
        audioFile?.delete()
        audioFile = null
        audioUri = null

        val buttonRecordAudio = dialogView?.findViewById<Button>(R.id.buttonRecordAudio)
        buttonRecordAudio?.text = "Record Audio"
        buttonRecordAudio?.setCompoundDrawablesWithIntrinsicBounds(
            R.drawable.ic_audio_record, 0, 0, 0
        )
    }

    private fun createAudioFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = fragment.requireContext().getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        return File.createTempFile(
            "AUDIO_${timeStamp}_",
            ".m4a",
            storageDir
        )
    }

    private fun getFormattedFileSize(file: File?): String {
        if (file == null || !file.exists()) return "0 KB"

        val size = file.length()
        if (size <= 0) return "0 KB"

        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()

        return DecimalFormat("#,##0.#").format(size / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
    }

    private fun startVideoRecording() {
        try {
            val videoFile = createVideoFile()
            videoUri = FileProvider.getUriForFile(
                fragment.requireContext(),
                "${fragment.requireContext().packageName}.fileprovider",
                videoFile
            )
            takeVideoLauncher.launch(videoUri!!)
        } catch (e: IOException) {
            Toast.makeText(fragment.requireContext(), "Error creating video file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openFilePicker() {
        selectFileLauncher.launch("*/*")
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = fragment.requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    private fun createVideoFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = fragment.requireContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        return File.createTempFile(
            "VIDEO_${timeStamp}_",
            ".mp4",
            storageDir
        ).also {
            videoFile = it
        }
    }

    private fun handleImageCaptured(uri: Uri) {
        photoUri = uri
        val dialogView = dialogView?.findViewById<View>(R.id.previewImage) as? ImageView
        dialogView?.setImageURI(uri)
        dialogView?.visibility = View.VISIBLE
    }

    private fun handleImageSelected(uri: Uri) {
        photoUri = uri
        val dialogView = dialogView?.findViewById<View>(R.id.previewImage) as? ImageView
        dialogView?.setImageURI(uri)
        Log.d("CreateAnnotationDialog", "Image selected: $uri")
        dialogView?.visibility = View.VISIBLE
    }

    private fun handleFileSelected(uri: Uri) {
        selectedFileUri = uri
        val textView = fragment.requireView().findViewById<View>(R.id.textSelectedFileName) as? TextView

        val fileName = getFileNameFromUri(uri)
        textView?.text = fileName ?: "Selected file"
        textView?.visibility = View.VISIBLE
    }

    private fun handleVideoCaptured(uri: Uri) {
        videoUri = uri
        val textView = dialogView?.findViewById<View>(R.id.textVideoStatus) as? TextView
        textView?.text = "Video recorded successfully"
        Log.d("CreateAnnotationDialog", "video selected: $videoUri")
        textView?.visibility = View.VISIBLE
    }


    private fun createFilePartFromUri(uri: Uri, defaultMimeType: String?): MultipartBody.Part? {
        return try {
            val contentResolver = fragment.requireContext().contentResolver
            val mimeType = contentResolver.getType(uri) ?: defaultMimeType ?: "application/octet-stream"
            val inputStream = contentResolver.openInputStream(uri)
            val fileName = getFileNameFromUri(uri) ?: "attachment"

            val byteArray = inputStream?.readBytes()
            inputStream?.close()

            if (byteArray != null) {
                val requestFile = byteArray.toRequestBody(mimeType.toMediaTypeOrNull())
                MultipartBody.Part.createFormData("file", fileName, requestFile)
            } else null
        } catch (e: Exception) {
            Log.e("CreateAnnotationDialog", "Error creating file part: ${e.message}")
            null
        }
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        val contentResolver = fragment.requireContext().contentResolver
        val cursor = contentResolver.query(uri, null, null, null, null)

        return cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            it.moveToFirst()
            if (nameIndex != -1) it.getString(nameIndex) else null
        } ?: uri.lastPathSegment
    }

    fun cleanup() {
        if (isAudioRecording) {
            stopAudioRecording()
        }
        mediaRecorder?.release()
        mediaRecorder = null
    }

    private fun fetchUpcomingBookings(instrumentId: Int) {
        // Calculate date range for next 2 weeks
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val startDate = Calendar.getInstance()
        val endDate = Calendar.getInstance()
        endDate.add(Calendar.DAY_OF_MONTH, 14) // 2 weeks from now

        val timeStarted = dateFormat.format(startDate.time)
        val timeEnded = dateFormat.format(endDate.time)

        lifecycleOwner.lifecycleScope.launch {
            instrumentUsageRepository.getInstrumentUsages(
                limit = 10,
                timeStarted = timeStarted,
                timeEnded = timeEnded,
                instrument = instrumentId.toString(),
                searchType = "usage"
            ).collect { result ->
                result.onSuccess { response ->
                    instrumentBookings = response.results
                    showBookingDialog()
                }
                result.onFailure { error ->
                    Toast.makeText(fragment.requireContext(),
                        "Failed to load bookings: ${error.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showBookingDialog() {
        // Create dialog layout
        val bookingDialogView = LayoutInflater.from(fragment.requireContext())
            .inflate(R.layout.dialog_instrument_booking, null)

        instrumentBookingDialogView = bookingDialogView

        // Setup booking list
        val recyclerViewBookings = bookingDialogView.findViewById<RecyclerView>(R.id.recyclerViewBookings)
        recyclerViewBookings.layoutManager = LinearLayoutManager(fragment.requireContext())

        // Create adapter for bookings
        val bookingsAdapter = InstrumentUsageAdapter(instrumentBookings)
        recyclerViewBookings.adapter = bookingsAdapter

        // Setup date/time pickers
        val buttonStartTime = bookingDialogView.findViewById<Button>(R.id.buttonStartTime)
        val buttonEndTime = bookingDialogView.findViewById<Button>(R.id.buttonEndTime)
        val textNoBookings = bookingDialogView.findViewById<TextView>(R.id.textNoBookings)

        // Show appropriate message if no bookings
        if (instrumentBookings.isEmpty()) {
            recyclerViewBookings.visibility = View.GONE
            textNoBookings.visibility = View.VISIBLE
        } else {
            recyclerViewBookings.visibility = View.VISIBLE
            textNoBookings.visibility = View.GONE
        }

        // Setup time selection buttons
        buttonStartTime.setOnClickListener { showDateTimePicker(true) }
        buttonEndTime.setOnClickListener { showDateTimePicker(false) }

        // Create and show dialog
        AlertDialog.Builder(fragment.requireContext())
            .setTitle("Schedule Instrument Time")
            .setView(bookingDialogView)
            .setPositiveButton("Confirm") { _, _ ->
                if (validateTimeSelection()) {

                    Toast.makeText(fragment.requireContext(),
                        "Time slot selected successfully",
                        Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(fragment.requireContext(),
                        "Please select valid start and end times that don't overlap with existing bookings",
                        Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateMainDialogInstrumentInfo() {
        val textSelectedInstrument = dialogView?.findViewById<TextView>(R.id.textSelectedInstrument)
        if (textSelectedInstrument != null && selectedInstrument != null) {
            val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
            val startStr = selectedStartTime?.let { dateFormat.format(Date(it)) } ?: "Not set"
            val endStr = selectedEndTime?.let { dateFormat.format(Date(it)) } ?: "Not set"

            textSelectedInstrument.text = "Selected: ${selectedInstrument?.instrumentName}\n" +
                "Time: $startStr to $endStr"
            textSelectedInstrument.visibility = View.VISIBLE
        }
    }

    private fun showDateTimePicker(isStartTime: Boolean) {
        val calendar = Calendar.getInstance()

        // Show date picker
        DatePickerDialog(
            fragment.requireContext(),
            { _, year, month, day ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, day)

                // After date is selected, show time picker
                TimePickerDialog(
                    fragment.requireContext(),
                    { _, hourOfDay, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(Calendar.MINUTE, minute)

                        // Update selected time
                        if (isStartTime) {
                            selectedStartTime = calendar.timeInMillis
                        } else {
                            selectedEndTime = calendar.timeInMillis
                        }
                        updateTimeDisplay(isStartTime)
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateTimeDisplay(isStartTime: Boolean) {
        val buttonToUpdate = if (isStartTime)
            instrumentBookingDialogView?.findViewById<Button>(R.id.buttonStartTime)
        else
            instrumentBookingDialogView?.findViewById<Button>(R.id.buttonEndTime)

        val time = if (isStartTime) selectedStartTime else selectedEndTime
        if (time != null) {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            buttonToUpdate?.text = dateFormat.format(Date(time))
        }

        // Update the display of selected time period
        updateSelectedTimePeriodDisplay()
    }

    private fun updateSelectedTimePeriodDisplay() {
        if (selectedStartTime != null && selectedEndTime != null) {
            val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
            val startStr = dateFormat.format(Date(selectedStartTime!!))
            val endStr = dateFormat.format(Date(selectedEndTime!!))

            val selectedInstrumentNameText = instrumentSelectionDialogView?.findViewById<TextView>(R.id.textSelectedInstrument)
            if (selectedInstrumentNameText != null && selectedInstrument != null) {
                Log.d("CreateAnnotationDialog", "Selected instrument: ${selectedInstrument?.instrumentName}, Start: $startStr, End: $endStr")
                selectedInstrumentNameText.text = "${selectedInstrument?.instrumentName}\nSelected: $startStr to $endStr"
            }
        }
    }

    private fun validateTimeSelection(): Boolean {
        val start = selectedStartTime ?: return false
        val end = selectedEndTime ?: return false

        // Basic validation
        if (end <= start) return false

        // Check for conflicts with existing bookings
        return instrumentBookings.none { booking ->
            val bookingStart = parseApiDateTime(booking.timeStarted)
            val bookingEnd = parseApiDateTime(booking.timeEnded)

            // Check if there's overlap
            (start < bookingEnd && end > bookingStart)
        }
    }

    private fun parseApiDateTime(dateTimeString: String?): Long {
        if (dateTimeString == null) return 0L
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            format.parse(dateTimeString)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

}