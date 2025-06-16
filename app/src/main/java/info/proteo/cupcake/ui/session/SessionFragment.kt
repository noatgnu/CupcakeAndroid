package info.proteo.cupcake.ui.session

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.Spinner
import androidx.appcompat.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.R
import info.proteo.cupcake.SessionManager
import info.proteo.cupcake.data.local.entity.user.UserPreferencesEntity
import info.proteo.cupcake.shared.data.model.protocol.ProtocolModel
import info.proteo.cupcake.shared.data.model.protocol.ProtocolSection
import info.proteo.cupcake.shared.data.model.protocol.ProtocolStep
import info.proteo.cupcake.shared.data.model.protocol.StepReagent
import info.proteo.cupcake.shared.data.model.annotation.Annotation
import info.proteo.cupcake.shared.data.model.protocol.TimeKeeper
import info.proteo.cupcake.data.remote.service.CreateAnnotationRequest
import info.proteo.cupcake.data.remote.service.SessionCreateRequest
import info.proteo.cupcake.data.remote.service.SessionService
import info.proteo.cupcake.data.remote.service.UpdateAnnotationRequest
import info.proteo.cupcake.data.repository.AnnotationRepository
import info.proteo.cupcake.data.repository.InstrumentRepository
import info.proteo.cupcake.data.repository.InstrumentUsageRepository
import info.proteo.cupcake.data.repository.ProtocolStepRepository
import info.proteo.cupcake.data.repository.StorageRepository
import info.proteo.cupcake.data.repository.StoredReagentRepository
import info.proteo.cupcake.data.repository.TimeKeeperRepository
import info.proteo.cupcake.data.repository.UserRepository
import info.proteo.cupcake.databinding.FragmentSessionBinding
import info.proteo.cupcake.util.ProtocolHtmlRenderer.renderAsHtml
import info.proteo.cupcake.wearos.TimeKeeperSyncService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import kotlin.collections.isNotEmpty

@AndroidEntryPoint
class SessionFragment : Fragment() {
    private var _binding: FragmentSessionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SessionViewModel by viewModels()

    @Inject
    lateinit var sessionService: SessionService
    @Inject
    lateinit var storedReagentRepository: StoredReagentRepository
    @Inject
    lateinit var storageRepository: StorageRepository
    @Inject
    lateinit var sessionManager: SessionManager
    @Inject
    lateinit var instrumentRepository: InstrumentRepository
    @Inject
    lateinit var instrumentUsageRepository: InstrumentUsageRepository
    @Inject
    lateinit var timeKeeperRepository: TimeKeeperRepository
    @Inject
    lateinit var timeKeeperSyncService: TimeKeeperSyncService

    private var currentTimeKeeper: TimeKeeper? = null
    private var countDownTimer: CountDownTimer? = null
    private var timeRemainingMillis: Long = 0
    private var isTimerRunning = false

    private var protocolId: Int = -1
    private var sessionId: String = ""
    private var isNewSession = false

    private lateinit var sidebarAdapter: SessionSidebarAdapter

    private var previousStepMenuItem: MenuItem? = null
    private var nextStepMenuItem: MenuItem? = null

    private var currentStep: ProtocolStep? = null
    private var currentSection: ProtocolSection? = null

    private lateinit var reagentAdapter: SessionStepReagentAdapter
    private var isReagentAdapterInitialized = false

    private var annotationAdapter: SessionAnnotationAdapter? = null
    private var currentAnnotationDialog: AlertDialog? = null
    private var dialogView: View? = null

    private var selectedFileUri: Uri? = null

    @Inject lateinit var protocolStepRepository: ProtocolStepRepository
    @Inject lateinit var userRepository: UserRepository
    @Inject lateinit var annotationRepository: AnnotationRepository

    private var userPreference: UserPreferencesEntity? = null


    private var currentAnnotationOffset = 0
    private val annotationsPerPage = 10
    private var hasMoreAnnotations = false
    private lateinit var annotationDialogHandler: CreateAnnotationDialogHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSessionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let {
            protocolId = it.getInt("protocolId", -1)
            sessionId = it.getString("sessionId", "")
            isNewSession = it.getBoolean("isNewSession", false)
        }

        Log.d("SessionFragment", "onCreate with protocolId: $protocolId, sessionId: $sessionId, isNewSession: $isNewSession")


        setupSidebar()
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_step_navigation, menu)
                menuInflater.inflate(R.menu.menu_session, menu)

                previousStepMenuItem = menu.findItem(R.id.action_previous_step)
                nextStepMenuItem = menu.findItem(R.id.action_next_step)
                previousStepMenuItem?.isEnabled = false
                nextStepMenuItem?.isEnabled = false
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_previous_step -> {
                        navigateToPreviousStep()
                        true
                    }
                    R.id.action_next_step -> {
                        navigateToNextStep()
                        true
                    }
                    R.id.action_toggle_sidebar -> {
                        toggleSidebar()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        if (isNewSession && protocolId != -1) {
            createNewSession(protocolId)
        } else if (sessionId.isNotEmpty()) {
            loadExistingSession(sessionId)
        } else {
            showError("Invalid session parameters")
        }

        observeViewModel()

        setupPaginationButtons()
        binding.reagentsSectionHeader.setOnClickListener {
            toggleReagentsSection()
        }
        binding.fabAddAnnotation.setOnClickListener {
            annotationDialogHandler.showCreateAnnotationDialog(
                sessionId = viewModel.session.value?.uniqueId ?: "",
                stepId = currentStep?.id ?: 0
            )
        }

        annotationDialogHandler = CreateAnnotationDialogHandler(
            fragment = this,
            lifecycleOwner = viewLifecycleOwner,
            instrumentRepository = instrumentRepository,
            instrumentUsageRepository = instrumentUsageRepository,
            onAnnotationCreated = { request, filePart ->
                Log.d("SessionFragment", "Creating annotation with request: $request and filePart: $filePart")
                viewModel.createAnnotation(request, filePart)
            }
        )

    }

    private fun setupStepTimer(step: ProtocolStep) {
        // Cancel any existing timer
        countDownTimer?.cancel()

        // Reset UI
        binding.timerCard.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d("SessionFragment", "session ${viewModel.session.value}")
                val existingTimeKeeper = viewModel.session.value?.timeKeeper?.firstOrNull {
                    it.step == step.id
                }
                val timeKeeper = if (existingTimeKeeper != null) {
                    Log.d("SessionFragment", "Using existing TimeKeeper: ${existingTimeKeeper.id}")
                    existingTimeKeeper
                } else {
                    Log.d("SessionFragment", "Fetching TimeKeeper for step: ${step.id}")
                    val result = protocolStepRepository.getTimeKeeper(
                        id = step.id,
                        session = sessionId
                    )
                    result.getOrNull()
                }
                if (timeKeeper != null) {
                    currentTimeKeeper = timeKeeper
                    //timeKeeperSyncService.syncTimeKeeper(currentTimeKeeper!!)

                    timeRemainingMillis = if (timeKeeper.currentDuration == null) {
                        (step.stepDuration ?: 0) * 1000L
                    } else {
                        (timeKeeper.currentDuration!!) * 1000L
                    }
                    if (timeKeeper.started) {
                        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
                        formatter.timeZone = TimeZone.getTimeZone("UTC")
                        val startTime = formatter.parse(timeKeeper.startTime)?.time ?: 0L

                        val elapsedSeconds = ((System.currentTimeMillis() - startTime) / 1000).toInt()
                        val initialDuration = timeKeeper.currentDuration ?: 0
                        val remainingDuration = (initialDuration - elapsedSeconds).coerceAtLeast(0)
                        timeRemainingMillis = remainingDuration * 1000L
                    }


                    // Only show timer if step has a duration
                    if ((step.stepDuration ?: 0) > 0) {
                        binding.timerCard.visibility = View.VISIBLE
                        updateTimerDisplay(timeRemainingMillis)

                        // Set button state
                        val buttonIcon = if (timeKeeper.started) R.drawable.ic_pause else R.drawable.ic_play_arrow
                        binding.timerPlayPauseButton.setImageResource(buttonIcon)
                        isTimerRunning = timeKeeper.started

                        // Start timer if it was already running
                        if (timeKeeper.started && timeRemainingMillis > 0) {
                            startCountdownTimer(timeRemainingMillis)
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e("SessionFragment", "Exception getting timekeeper", e)
            }
        }

        // Set up play/pause button click listener
        binding.timerPlayPauseButton.setOnClickListener {
            toggleTimer()
        }
    }

    private fun toggleTimer() {
        currentTimeKeeper?.let { timeKeeper ->
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    if (isTimerRunning) {
                        // Pause timer
                        countDownTimer?.cancel()
                        binding.timerPlayPauseButton.setImageResource(R.drawable.ic_play_arrow)

                        // Update TimeKeeper with the current remaining duration in seconds
                        val updatedTimeKeeper = timeKeeper.copy(
                            started = false,
                            currentDuration = (timeRemainingMillis / 1000).toInt() // Convert ms to seconds
                        )
                        val result = timeKeeperRepository.updateTimeKeeper(timeKeeper.id, updatedTimeKeeper)
                        result.onSuccess {
                            currentTimeKeeper = it
                            //timeKeeperSyncService.syncTimeKeeper(
                            //    it
                            //)
                        }
                    } else {
                        // Start timer
                        startCountdownTimer(timeRemainingMillis)
                        binding.timerPlayPauseButton.setImageResource(R.drawable.ic_pause)

                        // Update TimeKeeper - just set started flag
                        val updatedTimeKeeper = timeKeeper.copy(started = true)
                        val result = timeKeeperRepository.updateTimeKeeper(timeKeeper.id, updatedTimeKeeper)
                        result.onSuccess {
                            currentTimeKeeper = it
                            //timeKeeperSyncService.syncTimeKeeper(
                            //    it
                            //)
                        }
                    }
                    isTimerRunning = !isTimerRunning
                } catch (e: Exception) {
                    Log.e("SessionFragment", "Error toggling timer: ${e.message}")
                }
            }
        }
    }

    private fun startCountdownTimer(milliseconds: Long) {
        countDownTimer?.cancel()

        countDownTimer = object : CountDownTimer(milliseconds, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeRemainingMillis = millisUntilFinished
                updateTimerDisplay(millisUntilFinished)
            }

            override fun onFinish() {
                timeRemainingMillis = 0
                updateTimerDisplay(0)
                isTimerRunning = false
                binding.timerPlayPauseButton.setImageResource(R.drawable.ic_play_arrow)

                // Update TimeKeeper to store 0 remaining time
                currentTimeKeeper?.let { timeKeeper ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        val updatedTimeKeeper = timeKeeper.copy(
                            started = false,
                            currentDuration = 0
                        )
                        timeKeeperRepository.updateTimeKeeper(timeKeeper.id, updatedTimeKeeper)
                            .onSuccess {
                                currentTimeKeeper = it
                                //timeKeeperSyncService.syncTimeKeeper(it)
                            }
                    }
                }

                // Alert the user that time is up with vibration
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = requireContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    vibratorManager.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(500)
                }

                Toast.makeText(requireContext(), "Step timer completed!", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    private fun updateTimerDisplay(milliseconds: Long) {
        val hours = milliseconds / (1000 * 60 * 60)
        val minutes = (milliseconds % (1000 * 60 * 60)) / (1000 * 60)
        val seconds = (milliseconds % (1000 * 60)) / 1000

        val timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds)
        binding.timerText.text = timeString
    }

    private fun toggleReagentsSection() {
        if (binding.stepMetadataContainer.visibility == View.VISIBLE) {
            binding.stepMetadataContainer.visibility = View.GONE
            binding.reagentsExpandIcon.setImageResource(R.drawable.ic_expand_more)
            binding.reagentsExpandIcon.contentDescription = "Expand reagents section"
        } else {
            binding.stepMetadataContainer.visibility = View.VISIBLE
            binding.reagentsExpandIcon.setImageResource(R.drawable.ic_expand_less)
            binding.reagentsExpandIcon.contentDescription = "Collapse reagents section"
        }
    }

    private fun setupPaginationButtons() {
        binding.fabPrevAnnotation.setOnClickListener {
            if (currentAnnotationOffset > 0) {
                currentAnnotationOffset -= annotationsPerPage
                if (currentAnnotationOffset < 0) currentAnnotationOffset = 0
                loadAnnotationsForCurrentStep()
            }
        }

        binding.fabNextAnnotation.setOnClickListener {
            if (hasMoreAnnotations) {
                currentAnnotationOffset += annotationsPerPage
                loadAnnotationsForCurrentStep()
            }
        }



        updateAnnotationPaginationButtons()
    }

    private fun updateAnnotationPaginationButtons(hasMore: Boolean = false) {
        binding.fabPrevAnnotation.isEnabled = currentAnnotationOffset > 0
        binding.fabNextAnnotation.isEnabled = hasMore

        binding.fabPrevAnnotation.visibility = if (currentAnnotationOffset > 0) View.VISIBLE else View.INVISIBLE
        binding.fabNextAnnotation.visibility = if (hasMore) View.VISIBLE else View.INVISIBLE
    }


    private fun setupReagentAdapter() {
        reagentAdapter = SessionStepReagentAdapter(
            onBookReagentClick = { displayStepContent ->
                showReagentBookingDialog(displayStepContent.stepReagent)
            },
            storedReagentRepository = storedReagentRepository
        )

        binding.stepMetadataContainer.removeAllViews() // Clear previous views if any

        val recyclerView = RecyclerView(requireContext()).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = reagentAdapter
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        binding.stepMetadataContainer.addView(recyclerView)
        isReagentAdapterInitialized = true
    }

    private fun toggleSidebar() {
        val drawerLayout = binding.drawerLayout
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    private fun setupSidebar() {
        sidebarAdapter = SessionSidebarAdapter { step, section ->
            displayStepContent(step, section)
            sidebarAdapter.setSelectedStep(step.id, section.id)
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }

        binding.sidebarRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = sidebarAdapter
        }
    }


    private fun createNewSession(protocolId: Int) {
        binding.progressBar.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val request = SessionCreateRequest(protocolIds = listOf(protocolId))
                val result = sessionService.createSession(request)

                result.onSuccess { session ->
                    this@SessionFragment.sessionId = session.uniqueId
                    viewModel.loadSessionDetails(session.uniqueId)
                    viewModel.loadProtocolDetails(protocolId)
                }

                result.onFailure { error ->
                    showError("Failed to create session: ${error.message}")
                    binding.progressBar.visibility = View.GONE
                }
            } catch (e: Exception) {
                showError("Error creating session: ${e.message}")
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun loadExistingSession(sessionId: String) {
        binding.progressBar.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d("SessionFragment", "Loading existing session: $sessionId")

                viewModel.loadSessionDetails(sessionId)

                val protocolsResult = sessionService.getAssociatedProtocolTitles(sessionId)
                protocolsResult.onSuccess { protocols ->
                    if (protocols.isNotEmpty()) {
                        Log.d("SessionFragment", "Loading protocol with ID: ${protocols[0].id}")
                        viewModel.loadProtocolDetails(protocols[0].id)
                        checkAndNavigateToRecentStep(sessionId, protocols[0].id)
                    }
                }
            } catch (e: Exception) {
                Log.e("SessionFragment", "Error loading session", e)
            }
        }
    }

    private fun checkAndNavigateToRecentStep(sessionId: String, protocolId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val user = userRepository.getUserFromActivePreference()
                if (user != null) {
                    val recentSession = viewModel.getRecentSession(user.id, sessionId, protocolId)

                    if (recentSession?.stepId != null) {
                        // Use first() instead of collectLatest to wait for protocol once
                        // without setting up a permanent collector
                        val protocol = viewModel.protocol.first { it != null }

                        val stepId = recentSession.id
                        val step = protocol?.steps?.find { it.id == stepId }
                        val sectionId = step?.stepSection
                        val section = protocol?.sections?.find { it.id == sectionId }
                        Log.d("SessionFragment", "Recent step found: $stepId in section: $sectionId")
                        if (step != null && section != null) {
                            displayStepContent(step, section)
                            sidebarAdapter.setSelectedStep(step.id, section.id)
                            Log.d("SessionFragment", "Navigated to recent step: ${step.id}")
                            return@launch // Exit after successful navigation
                        }
                    }
                    // No return here - let the fallback in observeViewModel handle it
                }
            } catch (e: Exception) {
                Log.e("SessionFragment", "Error retrieving recent step: ${e.message}")
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentUserPreference.collectLatest { preference ->
                userPreference = preference
                setupAnnotationsSection()
                // You might want to add a log here to confirm preference collection
                Log.d("SessionFragment", "UserPreference updated: $preference")
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {

            viewModel.protocol.collectLatest { protocol ->
                Log.d("SessionFragment", "Protocol updated: ${protocol?.protocolTitle}")
                if (protocol != null) {
                    binding.protocolTitle.text = protocol.protocolTitle
                    requireActivity().title = protocol.protocolTitle ?: getString(R.string.session_activity_title_placeholder)

                    val sections = protocol.sections ?: emptyList()
                    val stepsMap = mutableMapOf<Int, List<ProtocolStep>>()
                    protocol.steps?.forEach { step ->
                        val sectionId = step.stepSection ?: return@forEach
                        val sectionSteps = stepsMap.getOrDefault(sectionId, emptyList())
                        stepsMap[sectionId] = sectionSteps + step
                    }

                    sidebarAdapter.updateData(sections, stepsMap)
                    Log.d("SessionFragment", "Protocol loaded: ${protocol.protocolTitle}, Sections: ${sections.size}, Steps: ${stepsMap.size}")
                    var stepDisplayed = false
                    if (sections.isNotEmpty()) {
                        val firstSection = sections[0]
                        val firstSectionSteps = stepsMap[firstSection.id]
                        Log.d("SessionFragment", "First section: ${firstSection.sectionDescription}, Steps: ${firstSectionSteps?.size ?: 0}")
                        if (firstSectionSteps?.isNotEmpty() == true) {
                            val firstStep = firstSectionSteps[0]
                            sidebarAdapter.setSelectedStep(firstStep.id, firstSection.id)
                            displayStepContent(firstStep, firstSection)
                            stepDisplayed = true
                        }
                    }

                    if (!stepDisplayed) {
                        binding.webView.visibility = View.GONE
                        binding.viewPager.visibility = View.VISIBLE // Show ViewPager if no step is displayed
                        binding.reagentsSectionCard.visibility = View.GONE // Hide reagents if no step
                    }

                } else {
                    binding.protocolTitle.text = getString(R.string.protocol_title)
                    requireActivity().title = getString(R.string.session_activity_title_placeholder)
                    sidebarAdapter.updateData(emptyList(), emptyMap())
                    binding.webView.visibility = View.GONE
                    binding.viewPager.visibility = View.VISIBLE
                    binding.reagentsSectionCard.visibility = View.GONE // Hide reagents if no protocol
                }
            }

        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.session.collectLatest { session ->
                session?.let {
                    Log.d("SessionFragment", "Session loaded: ${it.uniqueId}")
                    try {
                        //checkAndNavigateToRecentStep(it.uniqueId, protocolId)
                    } catch (e: Exception) {

                    }
                    if (currentStep != null) {
                        viewModel.updateRecentSession(session, protocolId, null, stepId = currentStep!!.id)
                    } else {
                        viewModel.updateRecentSession(session, protocolId, null, stepId = null)
                    }

                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentStepReagentInfo.collectLatest { reagentInfoList ->
                updateReagentViews(reagentInfoList)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.hasEditPermission.collect { hasPermission ->
                    binding.fabAddAnnotation.visibility = if (hasPermission) View.VISIBLE else View.GONE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.hasMoreAnnotations.collectLatest { hasMore ->
                hasMoreAnnotations = hasMore
                updateAnnotationPaginationButtons(hasMore)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentAnnotationOffset.collect { offset ->
                binding.fabPrevAnnotation.visibility = if (offset > 0) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { isLoading ->
                    binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                }
            }
        }
    }


    private fun displayStepContent(step: ProtocolStep, section: ProtocolSection) {
        currentStep = step
        currentSection = section
        updateNavigationButtons()

        Log.d("SessionFragment", "Displaying step: ${step.id} from section: ${section.sectionDescription}")
        binding.currentSectionTitle.text = section.sectionDescription
        binding.currentSectionTitle.visibility = View.VISIBLE

        viewModel.loadReagentInfoForStep(step, sessionId)
        setupStepTimer(step)
        if (!isReagentAdapterInitialized) {
            setupReagentAdapter()
        }

        if (step.reagents.isNullOrEmpty()) {
            binding.reagentsSectionCard.visibility = View.GONE
        } else {
            binding.reagentsSectionCard.visibility = View.VISIBLE
            // Initialize as collapsed
            binding.stepMetadataContainer.visibility = View.GONE
            binding.reagentsExpandIcon.setImageResource(R.drawable.ic_expand_more)
            binding.reagentsExpandIcon.contentDescription = "Expand reagents section"
        }

        val webView = binding.webView

        webView.settings.apply {
            javaScriptEnabled = false
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
        }

        val isNightMode = (resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        if (isNightMode) {
            webView.setBackgroundColor(Color.TRANSPARENT)
        } else {
            webView.setBackgroundColor(Color.WHITE)
        }

        val processedHtml = step.renderAsHtml(resources)

        webView.loadDataWithBaseURL(null, processedHtml, "text/html", "UTF-8", null)

        binding.webView.visibility = View.VISIBLE
        binding.viewPager.visibility = View.GONE

        loadAnnotationsForCurrentStep()
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        Log.e("SessionFragment", message)
    }

    override fun onDestroyView() {
        countDownTimer?.cancel()
        countDownTimer = null

        super.onDestroyView()
        _binding = null
    }

    private fun updateNavigationButtons() {
        val protocol = viewModel.protocol.value ?: return
        val sections = protocol.sections ?: emptyList()
        val stepsMap = createStepsMap(protocol)

        val hasPreviousStep = findPreviousStep(currentStep, currentSection, sections, stepsMap) != null
        val hasNextStep = findNextStep(currentStep, currentSection, sections, stepsMap) != null

        previousStepMenuItem?.isEnabled = hasPreviousStep
        nextStepMenuItem?.isEnabled = hasNextStep
    }

    private fun navigateToPreviousStep() {
        val protocol = viewModel.protocol.value ?: return
        val sections = protocol.sections ?: emptyList()
        val stepsMap = createStepsMap(protocol)

        val previousStepInfo = findPreviousStep(currentStep, currentSection, sections, stepsMap) ?: return
        val (previousStep, previousSection) = previousStepInfo

        displayStepContent(previousStep, previousSection)
        sidebarAdapter.setSelectedStep(previousStep.id, previousSection.id)
    }

    private fun navigateToNextStep() {
        val protocol = viewModel.protocol.value ?: return
        val sections = protocol.sections ?: emptyList()
        val stepsMap = createStepsMap(protocol)

        val nextStepInfo = findNextStep(currentStep, currentSection, sections, stepsMap) ?: return
        val (nextStep, nextSection) = nextStepInfo

        displayStepContent(nextStep, nextSection)
        sidebarAdapter.setSelectedStep(nextStep.id, nextSection.id)
    }

    private fun createStepsMap(protocol: ProtocolModel): Map<Int, List<ProtocolStep>> {
        val stepsMap = mutableMapOf<Int, List<ProtocolStep>>()
        protocol.steps?.forEach { step ->
            val sectionId = step.stepSection ?: return@forEach
            val sectionSteps = stepsMap.getOrDefault(sectionId, emptyList())
            stepsMap[sectionId] = sectionSteps + step
        }
        return stepsMap
    }

    private fun findPreviousStep(
        currentStep: ProtocolStep?,
        currentSection: ProtocolSection?,
        sections: List<ProtocolSection>,
        stepsMap: Map<Int, List<ProtocolStep>>
    ): Pair<ProtocolStep, ProtocolSection>? {
        if (currentStep == null || currentSection == null) return null

        val sectionSteps = stepsMap[currentSection.id] ?: return null
        val currentIndex = sectionSteps.indexOfFirst { it.id == currentStep.id }

        if (currentIndex > 0) {
            return Pair(sectionSteps[currentIndex - 1], currentSection)
        }

        val sectionIndex = sections.indexOfFirst { it.id == currentSection.id }
        if (sectionIndex > 0) {
            val previousSection = sections[sectionIndex - 1]
            val previousSectionSteps = stepsMap[previousSection.id] ?: return null
            if (previousSectionSteps.isNotEmpty()) {
                return Pair(previousSectionSteps.last(), previousSection)
            }
        }

        return null
    }

    private fun findNextStep(
        currentStep: ProtocolStep?,
        currentSection: ProtocolSection?,
        sections: List<ProtocolSection>,
        stepsMap: Map<Int, List<ProtocolStep>>
    ): Pair<ProtocolStep, ProtocolSection>? {
        if (currentStep == null || currentSection == null) return null

        val sectionSteps = stepsMap[currentSection.id] ?: return null
        val currentIndex = sectionSteps.indexOfFirst { it.id == currentStep.id }

        if (currentIndex < sectionSteps.size - 1) {
            return Pair(sectionSteps[currentIndex + 1], currentSection)
        }

        val sectionIndex = sections.indexOfFirst { it.id == currentSection.id }
        if (sectionIndex < sections.size - 1) {
            val nextSection = sections[sectionIndex + 1]
            val nextSectionSteps = stepsMap[nextSection.id] ?: return null
            if (nextSectionSteps.isNotEmpty()) {
                return Pair(nextSectionSteps.first(), nextSection)
            }
        }

        return null
    }


    private fun updateReagentViews(reagentInfoList: List<DisplayableStepReagent>) {
        if (!isReagentAdapterInitialized && reagentInfoList.isNotEmpty()) {
            setupReagentAdapter()
        }

        if (reagentInfoList.isEmpty()) {
            binding.reagentsSectionCard.visibility = View.GONE
        } else {
            binding.reagentsSectionCard.visibility = View.VISIBLE
            // The visibility of stepMetadataContainer (expanded/collapsed) is managed by
            // displayStepContent (for initial state) and toggleReagentsSection (for user interaction).
        }

        // Only submit list if adapter is initialized (which it should be if reagentInfoList is not empty)
        if (isReagentAdapterInitialized) {
            reagentAdapter.submitList(reagentInfoList)
        }
    }

    private fun showReagentBookingDialog(reagent: StepReagent) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_book_reagent, null)

        val searchView = dialogView.findViewById<SearchView>(R.id.searchView)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerViewReagents)
        val quantityEditText = dialogView.findViewById<EditText>(R.id.editTextQuantity)
        val notesEditText = dialogView.findViewById<EditText>(R.id.editTextNotes)
        val noResultsText = dialogView.findViewById<TextView>(R.id.textViewNoResults)

        var currentOffset = 0
        var isLoadingMore = false
        var hasMoreItems = true
        var currentQuery = ""

        lateinit var adapter: SessionStoredReagentSearchAdapter

        // Define loadMoreReagents function
        fun loadMoreReagents() {
            if (isLoadingMore) return
            isLoadingMore = true
            adapter.setLoading(true)

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val result = viewModel.getAvailableStoredReagents(
                        reagentName = currentQuery,
                        offset = currentOffset,
                        limit = 20
                    )

                    val storedReagents = result.first
                    hasMoreItems = result.second

                    if (currentOffset == 0) {
                        adapter.submitList(storedReagents)
                        noResultsText.visibility = if (storedReagents.isEmpty()) View.VISIBLE else View.GONE
                    } else {
                        adapter.appendItems(storedReagents)
                    }

                    if (storedReagents.isNotEmpty()) {
                        currentOffset += storedReagents.size
                    }
                } catch (e: Exception) {
                    Log.e("SessionFragment", "Error loading reagents: ${e.message}")
                    noResultsText.visibility = View.VISIBLE
                } finally {
                    isLoadingMore = false
                    adapter.setLoading(false)
                }
            }
        }

        adapter = SessionStoredReagentSearchAdapter(
            onItemClick = { storedReagent ->
                if (quantityEditText.text.isNullOrEmpty()) {
                    val requiredAmount = reagent.quantity.toString()
                    quantityEditText.setText(requiredAmount)
                }
            },
            onLoadMore = {
                if (!isLoadingMore && hasMoreItems) {
                    loadMoreReagents()
                }
            },
            storageRepository
        )



        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter



        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                currentQuery = newText ?: ""
                currentOffset = 0
                hasMoreItems = true
                loadMoreReagents()
                return true
            }
        })


        quantityEditText.setText(reagent.quantity.toString())

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Book ${reagent.reagent.name}")
            .setView(dialogView)
            .setPositiveButton("Book", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val selectedReagent = adapter.getSelectedReagent()
            if (selectedReagent == null) {
                Toast.makeText(requireContext(), "Please select a reagent", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val quantityText = quantityEditText.text.toString()
            if (quantityText.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a quantity", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val quantity = quantityText.toFloatOrNull()
            if (quantity == null || quantity <= 0) {
                Toast.makeText(requireContext(), "Invalid quantity", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.bookReagent(
                storedReagent = selectedReagent,
                stepReagent = reagent,
                quantity = quantity,
                session = sessionId,
                notes = notesEditText.text.toString().takeIf { it.isNotEmpty() }
            )

            dialog.dismiss()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE // Consider moving this inside loadMoreReagents or make it specific

            val result = viewModel.getAvailableStoredReagents(reagent.reagent.name)
            val storedReagents = result.first
            hasMoreItems = result.second // Initialize hasMoreItems for the initial load

            binding.progressBar.visibility = View.GONE // And here

            if (storedReagents.isEmpty()) {
                noResultsText.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                noResultsText.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                adapter.submitList(storedReagents)
                if (storedReagents.isNotEmpty()) { // Initialize offset for subsequent loads
                    currentOffset = storedReagents.size
                }
            }
        }
    }

    private fun loadAnnotationsForCurrentStep() {
        currentStep?.id?.let { stepId ->
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    Toast.makeText(context, "Loading annotations ${currentAnnotationOffset+1} to ${currentAnnotationOffset + annotationsPerPage}", Toast.LENGTH_SHORT).show()
                    binding.progressBar.visibility = View.VISIBLE
                    viewModel.loadAnnotationsForStep(
                        stepId = stepId,
                        sessionId = sessionId,
                        offset = currentAnnotationOffset,
                        limit = annotationsPerPage
                    )
                    binding.progressBar.visibility = View.GONE
                } catch (e: Exception) {
                    binding.progressBar.visibility = View.GONE
                    showError("Error loading annotations: ${e.message}")
                }
            }
        }
    }

    private fun observeAnnotations() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.stepAnnotations.collectLatest { annotations ->
                Log.d("SessionFragment", "Loaded ${annotations.size} annotations for step ${currentStep?.id}")
                if (annotations.isEmpty()) {
                    binding.annotationsEmptyState.visibility = View.VISIBLE
                    binding.annotationsRecyclerView.visibility = View.GONE
                } else {
                    binding.annotationsEmptyState.visibility = View.GONE
                    binding.annotationsRecyclerView.visibility = View.VISIBLE
                    annotationAdapter?.submitList(annotations)
                }
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_PICK_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            selectedFileUri = data?.data

            dialogView?.findViewById<TextView>(R.id.textSelectedFileName)?.let { textView ->
                selectedFileUri?.let { uri ->
                    val fileName = getFileNameFromUri(uri) ?: "Selected file"
                    textView.text = fileName
                    textView.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        val contentResolver = requireContext().contentResolver
        val cursor = contentResolver.query(uri, null, null, null, null)

        return cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            it.moveToFirst()
            if (nameIndex != -1) it.getString(nameIndex) else null
        }
    }

    private fun createAnnotation(text: String, fileUri: Uri?, type: String = "text") {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE

            try {
                // Prepare the file if present
                val filePart = fileUri?.let { uri ->
                    val contentResolver = requireContext().contentResolver
                    val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
                    val inputStream = contentResolver.openInputStream(uri)
                    val fileName = getFileNameFromUri(uri) ?: "attachment"

                    val byteArray = inputStream?.readBytes()
                    inputStream?.close()

                    if (byteArray != null) {
                        val requestFile = byteArray.toRequestBody(mimeType.toMediaTypeOrNull())
                        MultipartBody.Part.createFormData("file", fileName, requestFile)
                    } else null
                }

                // Create the annotation request
                val request = CreateAnnotationRequest(
                    annotation = text,
                    annotationType = type,
                    step = currentStep?.id,
                    session = sessionId
                )

                val result = annotationRepository.createAnnotationInRepository(request, filePart)

                if (result.isSuccess) {
                    // Refresh annotations list
                    loadAnnotationsForCurrentStep()
                    Toast.makeText(context, "Annotation added successfully", Toast.LENGTH_SHORT).show()
                } else {
                    showError("Failed to create annotation: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                showError("Error creating annotation: ${e.message}")
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }



    private fun setupAnnotationsSection() {
        if (_binding == null) return

        val baseUrl = sessionManager.getBaseUrl()

        if (annotationAdapter == null && userPreference != null) {
            annotationAdapter = SessionAnnotationAdapter(
                onItemClick = { annotation -> handleAnnotationClick(annotation) },
                onRetranscribeClick = { annotation -> handleRetranscribeClick(annotation) },
                onAnnotationUpdate = { updatedAnnotation, translation, transcription ->
                    updateAnnotation(updatedAnnotation, translation, transcription)
                },
                onAnnotationRename = {
                    annotation -> viewModel.renameAnnotation(annotation.id, annotation.annotationName)
                },
                onAnnotationDelete = {
                    annotation -> viewModel.deleteAnnotation(annotation.id)
                },
                annotationRepository = annotationRepository,
                instrumentRepository = instrumentRepository,
                instrumentUsageRepository = instrumentUsageRepository,
                userPreferencesEntity = userPreference!!,
                baseUrl

            )
            binding.annotationsRecyclerView.adapter = annotationAdapter
        }

        observeAnnotations()
    }

    private fun updateAnnotation(updatedAnnotation: Annotation, translation: String?, transcription: String?) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val request = UpdateAnnotationRequest(
                    annotation = updatedAnnotation.annotation,
                    translation = translation,
                    transcription = transcription
                )
                val result = annotationRepository.updateAnnotationInRepository(updatedAnnotation.id, request, null)

                if (!result.isSuccess) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            requireContext(),
                            "Failed to save changes: ${result.exceptionOrNull()?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    val ann = result.getOrNull()
                    val updatedList = annotationAdapter?.currentList?.toMutableList()
                    val position = updatedList?.indexOfFirst { it.annotation.id == updatedAnnotation.id } ?: -1
                    if (position != -1 && updatedList != null && ann != null) {
                        val annWithPermission = updatedList[position]
                        updatedList[position] = annWithPermission.copy(annotation = ann)
                        annotationAdapter?.submitList(updatedList)

                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Error saving changes: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun handleAnnotationClick(annotation: Annotation) {
        if (annotation.file != null) {
            // Show options to view or download the file
            val options = arrayOf("View", "Download")
            AlertDialog.Builder(requireContext())
                .setTitle("Annotation File")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> viewAnnotationFile(annotation)
                        1 -> downloadAnnotationFile(annotation)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            // Show the annotation text in a dialog
            AlertDialog.Builder(requireContext())
                .setTitle("Annotation")
                .setMessage(annotation.annotation)
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun viewAnnotationFile(annotation: Annotation) {
        // Implementation would download and open the file
        Toast.makeText(context, "Viewing file not implemented in this example", Toast.LENGTH_SHORT).show()
    }

    private fun downloadAnnotationFile(annotation: Annotation) {
        Toast.makeText(context, "Downloading file not implemented in this example", Toast.LENGTH_SHORT).show()
    }

    private fun handleRetranscribeClick(annotation: Annotation) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                val result = annotationRepository.retranscribe(annotation.id, null)
                if (result.isSuccess) {
                    Toast.makeText(context, "Retranscription requested", Toast.LENGTH_SHORT).show()
                } else {
                    showError("Failed to retranscribe: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                showError("Error requesting retranscription: ${e.message}")
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
    private fun Float.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }



    companion object {
        private const val FILE_PICK_REQUEST_CODE = 1001
        private const val TAG = "SessionFragment"
    }
}