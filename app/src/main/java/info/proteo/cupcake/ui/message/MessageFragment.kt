package info.proteo.cupcake.ui.message

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.R
import info.proteo.cupcake.shared.data.model.message.MessageThread
import info.proteo.cupcake.databinding.FragmentMessageBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MessageFragment : Fragment() {

    private var _binding: FragmentMessageBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MessageViewModel by viewModels()
    private lateinit var threadAdapter: MessageThreadAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMessageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupObservers()
        setupListeners()
    }

    private fun setupRecyclerView() {
        threadAdapter = MessageThreadAdapter { thread ->
            navigateToThreadDetail(thread)
        }

        binding.recyclerViewThreads.apply {
            adapter = threadAdapter
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                    if (!viewModel.isLoading.value &&
                        (visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5 &&
                        firstVisibleItemPosition >= 0) {
                        viewModel.loadMoreThreads()
                    }
                }
            })
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.messageThreads.collectLatest { threads ->
                    threadAdapter.submitList(threads)

                    binding.textViewEmptyState.visibility = if (threads.isEmpty() &&
                        !viewModel.isLoading.value) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { isLoading ->
                    binding.progressBar.visibility = if (isLoading &&
                        viewModel.messageThreads.value.isEmpty()) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }

                    binding.swipeRefreshLayout.isRefreshing = isLoading &&
                            viewModel.messageThreads.value.isNotEmpty()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.hasError.collect { hasError ->
                    if (hasError) {
                        Snackbar.make(
                            binding.root,
                            R.string.error_loading_messages,
                            Snackbar.LENGTH_LONG
                        ).setAction(R.string.retry) {
                            viewModel.loadMoreThreads()
                        }.show()
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadFirstPage()
        }
    }

    private fun navigateToThreadDetail(thread: MessageThread) {
        val bundle = Bundle().apply {
            putInt("threadId", thread.id)
        }
        findNavController().navigate(R.id.action_messageFragment_to_threadDetailFragment, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.messageThreads.value.isNotEmpty()) {
            view?.postDelayed({
                viewModel.loadFirstPage()
            }, 300)
        }
    }


}