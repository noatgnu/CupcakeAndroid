package info.proteo.cupcake.ui.protocol

import android.content.res.Configuration
import android.graphics.Color
import androidx.fragment.app.viewModels
import android.os.Bundle
import android.text.Html
import android.text.method.ScrollingMovementMethod
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.R
import info.proteo.cupcake.data.remote.model.protocol.Session
import info.proteo.cupcake.data.remote.service.ProtocolService
import info.proteo.cupcake.data.remote.service.UserPermissionResponse
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

    private lateinit var sessionAdapter: SessionAdapter

    @Inject lateinit var protocolService: ProtocolService
    @Inject lateinit var userRepository: UserRepository

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
        observeViewModel()
        ViewCompat.setNestedScrollingEnabled(binding.protocolDescription, true)
        binding.protocolDescription.setOnTouchListener { v, event ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            false
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
                        // Handle edit protocol action
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
            it.setSupportActionBar(binding.toolbar)
            it.supportActionBar?.setDisplayHomeAsUpEnabled(true)
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

                        it.tags.forEach { protocolTag ->
                            val chip = com.google.android.material.chip.Chip(requireContext())
                            chip.text = protocolTag.tag.tag
                            chip.isClickable = false
                            binding.protocolTagsChipGroup.addView(chip)
                        }
                    }

                    if (!it.reagents.isNullOrEmpty()) {
                        binding.protocolReagentsHeader.visibility = View.VISIBLE
                        binding.protocolReagentsContainer.visibility = View.VISIBLE
                        binding.protocolReagentsContainer.removeAllViews()

                        it.reagents.forEach { reagent ->
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


                    (activity as? AppCompatActivity)?.supportActionBar?.title = it.protocolTitle
                }
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
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
            // If user doesn't have view permission, navigate back
            if (!permissions.view) {
                findNavController().navigateUp()
                return
            }

            // Update UI to show edit/delete options if user has those permissions
            activity?.invalidateOptionsMenu()
        }
    }
}