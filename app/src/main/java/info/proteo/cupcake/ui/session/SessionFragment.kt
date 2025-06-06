package info.proteo.cupcake.ui.session

import android.content.res.Configuration
import android.graphics.Color
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
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.R
import info.proteo.cupcake.data.remote.model.protocol.ProtocolStep
import info.proteo.cupcake.data.remote.service.SessionCreateRequest
import info.proteo.cupcake.data.remote.service.SessionService
import info.proteo.cupcake.databinding.FragmentSessionBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SessionFragment : Fragment() {
    private var _binding: FragmentSessionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SessionViewModel by viewModels()

    @Inject
    lateinit var sessionService: SessionService

    private var protocolId: Int = -1
    private var sessionId: String = ""
    private var isNewSession = false

    private lateinit var sidebarAdapter: SessionSidebarAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            protocolId = it.getInt("protocolId", -1)
            sessionId = it.getString("sessionId", "")
            isNewSession = it.getBoolean("isNewSession", false)
        }

        Log.d("SessionFragment", "onCreate with protocolId: $protocolId, sessionId: $sessionId, isNewSession: $isNewSession")
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

        setupSidebar()
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_session, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
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
        sidebarAdapter = SessionSidebarAdapter { step ->
            displayStepContent(step)
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
                val request = SessionCreateRequest(listOf(protocolId))
                val result = sessionService.createSession(request)

                result.onSuccess { session ->
                    sessionId = session.uniqueId
                    viewModel.loadSessionDetails(session.uniqueId)

                    // Load protocol details since we know the ID
                    viewModel.loadProtocolDetails(protocolId)
                }

                result.onFailure { error ->
                    showError("Failed to create session: ${error.message}")
                }
            } catch (e: Exception) {
                showError("Error creating session: ${e.message}")
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun loadExistingSession(sessionId: String) {
        binding.progressBar.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                viewModel.loadSessionDetails(sessionId)

                // For existing sessions, we need to load the associated protocols
                val protocolsResult = sessionService.getAssociatedProtocolTitles(sessionId)
                protocolsResult.onSuccess { protocols ->
                    if (protocols.isNotEmpty()) {
                        viewModel.loadProtocolDetails(protocols[0].id)
                    } else {
                        showError("No protocols associated with this session")
                    }
                }

                protocolsResult.onFailure { error ->
                    showError("Failed to load protocols: ${error.message}")
                }
            } catch (e: Exception) {
                showError("Error loading session: ${e.message}")
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.protocol.collectLatest { protocol ->
                protocol?.let {
                    binding.protocolTitle.text = it.protocolTitle

                    // Update the activity title
                    requireActivity().title = it.protocolTitle ?: "Session"

                    // Load sections and steps for the sidebar
                    val sections = it.sections ?: emptyList()
                    val stepsMap = mutableMapOf<Int, List<ProtocolStep>>()

                    // Group steps by section
                    it.steps?.forEach { step ->
                        val sectionId = step.stepSection ?: return@forEach
                        val sectionSteps = stepsMap.getOrDefault(sectionId, emptyList())
                        stepsMap[sectionId] = sectionSteps + step
                    }

                    sidebarAdapter.updateData(sections, stepsMap)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.session.collectLatest { session ->
                session?.let {
                    // Update UI with session info if needed
                    Log.d("SessionFragment", "Session loaded: ${it.uniqueId}")
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }
    }

    private fun displayStepContent(step: ProtocolStep) {

        val webView = WebView(requireContext())
        webView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        // Configure WebView settings
        webView.settings.apply {
            javaScriptEnabled = false
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
        }

        // Set WebView background based on theme
        val isNightMode = (resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        if (isNightMode) {
            webView.setBackgroundColor(Color.TRANSPARENT)
        }

        val cssStyle = if (isNightMode) {
            "<style>body{color:#FFFFFF; font-family: sans-serif; line-height: 1.6;} " +
                "a{color:#9ECBFF;} img{max-width:100%; height:auto;}</style>"
        } else {
            "<style>body{color:#000000; font-family: sans-serif; line-height: 1.6;} " +
                "a{color:#0366D6;} img{max-width:100%; height:auto;}</style>"
        }

        val htmlContent = step.stepDescription ?: "No description available"
        val wrappedHtml = "<html><head>$cssStyle</head><body>$htmlContent</body></html>"

        webView.loadDataWithBaseURL(null, wrappedHtml, "text/html", "UTF-8", null)

    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        Log.e("SessionFragment", message)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}