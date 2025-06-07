package info.proteo.cupcake.ui.session

import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.R
import info.proteo.cupcake.SessionActivity
import info.proteo.cupcake.data.remote.model.protocol.ProtocolModel
import info.proteo.cupcake.data.remote.model.protocol.ProtocolSection
import info.proteo.cupcake.data.remote.model.protocol.ProtocolStep
import info.proteo.cupcake.data.remote.model.protocol.StepReagent
import info.proteo.cupcake.data.remote.service.SessionCreateRequest
import info.proteo.cupcake.data.remote.service.SessionService
import info.proteo.cupcake.data.repository.ProtocolStepRepository
import info.proteo.cupcake.data.repository.StorageRepository
import info.proteo.cupcake.data.repository.StoredReagentRepository
import info.proteo.cupcake.data.repository.UserRepository
import info.proteo.cupcake.databinding.FragmentSessionBinding
import info.proteo.cupcake.util.ProtocolHtmlRenderer.renderAsHtml
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.toString

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

    @Inject lateinit var protocolStepRepository: ProtocolStepRepository
    @Inject lateinit var userRepository: UserRepository

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

        binding.reagentsSectionHeader.setOnClickListener {
            toggleReagentsSection()
        }
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
        sidebarAdapter = SessionSidebarAdapter { step, section -> // Modified lambda
            displayStepContent(step, section) // Pass section here
            sidebarAdapter.setSelectedStep(step.id, section.id) // Update selection in adapter
            binding.drawerLayout.closeDrawer(GravityCompat.START) // Close drawer after selection
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
                viewModel.loadSessionDetails(sessionId)

                val protocolsResult = sessionService.getAssociatedProtocolTitles(sessionId)
                protocolsResult.onSuccess { protocols ->
                    if (protocols.isNotEmpty()) {
                        val relevantProtocolId = protocols[0].id
                        this@SessionFragment.protocolId = relevantProtocolId
                        viewModel.loadProtocolDetails(relevantProtocolId)
                    } else {
                        showError("No protocols associated with this session")
                        binding.progressBar.visibility = View.GONE
                    }
                }

                protocolsResult.onFailure { error ->
                    showError("Failed to load protocols for session: ${error.message}")
                    binding.progressBar.visibility = View.GONE
                }
            } catch (e: Exception) {
                showError("Error loading session: ${e.message}")
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.protocol.collectLatest { protocol ->
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

                    var stepDisplayed = false
                    if (sections.isNotEmpty()) {
                        val firstSection = sections[0]
                        val firstSectionSteps = stepsMap[firstSection.id]

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
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentStepReagentInfo.collectLatest { reagentInfoList ->
                updateReagentViews(reagentInfoList)
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

        viewModel.loadReagentInfoForStep(step, sessionId) // This will trigger updateReagentViews

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
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        Log.e("SessionFragment", message)
    }

    override fun onDestroyView() {
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

    private fun loadReagentInfo(step: ProtocolStep) { // This seems unused, viewModel.loadReagentInfoForStep is used
        viewModel.loadReagentInfoForStep(step, sessionId)
    }

    private fun updateReagentViews(reagentInfoList: List<DisplayableStepReagent>) {
        // Ensure adapter is set up if it wasn't and there's data
        if (!isReagentAdapterInitialized && reagentInfoList.isNotEmpty()) {
            setupReagentAdapter()
        }

        if (reagentInfoList.isEmpty()) {
            binding.reagentsSectionCard.visibility = View.GONE
            // stepMetadataContainer is inside the card, its visibility is implicitly handled.
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
}