package info.proteo.cupcake.ui.protocol

import androidx.fragment.app.viewModels
import android.os.Bundle
import android.text.Html
import android.text.method.ScrollingMovementMethod
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.R
import info.proteo.cupcake.data.remote.service.ProtocolService
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
        setupSessionsList(protocolId)
        observeViewModel()
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
                    binding.protocolDescription.text = Html.fromHtml(it.protocolDescription, Html.FROM_HTML_MODE_COMPACT)
                    binding.protocolDescription.movementMethod = ScrollingMovementMethod.getInstance()

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
}