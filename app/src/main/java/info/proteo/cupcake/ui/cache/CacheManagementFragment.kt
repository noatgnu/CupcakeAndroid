package info.proteo.cupcake.ui.cache

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.R
import info.proteo.cupcake.databinding.FragmentCacheManagementBinding
import info.proteo.cupcake.data.repository.CacheStatistics
import info.proteo.cupcake.data.offline.OfflineStatus
import info.proteo.cupcake.data.offline.SyncProgress
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CacheManagementFragment : Fragment() {

    private var _binding: FragmentCacheManagementBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: CacheManagementViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCacheManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupClickListeners()
        observeViewModel()
        
        // Load initial data
        viewModel.loadCacheStatistics()
    }

    private fun setupClickListeners() {
        binding.apply {
            // Sync actions
            buttonSmartSync.setOnClickListener {
                viewModel.performSmartSync()
            }
            
            buttonFullSync.setOnClickListener {
                showConfirmationDialog(
                    title = "Force Full Sync",
                    message = "This will download all data again and may take a while. Continue?",
                    action = { viewModel.forceFullSync() }
                )
            }
            
            buttonRetrySync.setOnClickListener {
                viewModel.retryFailedSync()
            }
            
            // Cache actions
            buttonCleanCache.setOnClickListener {
                showConfirmationDialog(
                    title = "Clean Cache",
                    message = "This will remove old and unused cache files. Continue?",
                    action = { viewModel.cleanCache() }
                )
            }
            
            buttonClearAllCache.setOnClickListener {
                showConfirmationDialog(
                    title = "Clear All Cache",
                    message = "This will remove ALL cached data. You'll need to sync again. Continue?",
                    action = { viewModel.clearAllCache() }
                )
            }
            
            // Refresh button
            buttonRefresh.setOnClickListener {
                viewModel.loadCacheStatistics()
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                updateUI(state)
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.offlineStatus.collectLatest { status ->
                updateOfflineStatus(status)
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.syncProgress.collectLatest { progress ->
                updateSyncProgress(progress)
            }
        }
    }

    private fun updateUI(state: CacheManagementUiState) {
        binding.apply {
            // Loading state
            progressBarMain.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            
            // Sync state
            progressBarSync.visibility = if (state.syncInProgress) View.VISIBLE else View.GONE
            buttonSmartSync.isEnabled = !state.syncInProgress && !state.isLoading
            buttonFullSync.isEnabled = !state.syncInProgress && !state.isLoading
            buttonRetrySync.isEnabled = !state.syncInProgress && !state.isLoading
            
            // Cache actions
            buttonCleanCache.isEnabled = !state.isLoading
            buttonClearAllCache.isEnabled = !state.isLoading
            buttonRefresh.isEnabled = !state.isLoading
            
            // Cache statistics
            state.cacheStats?.let { stats ->
                updateCacheStatistics(stats)
            }
            
            // Sync result
            state.lastSyncResult?.let { result ->
                textSyncStatus.text = viewModel.getSyncStatusText(result)
                textSyncStatus.visibility = View.VISIBLE
            }
            
            // Cleanup result
            state.lastCleanupBytes?.let { bytes ->
                if (bytes > 0) {
                    showSnackbar("Freed ${viewModel.formatBytes(bytes)} of cache")
                }
            }
            
            // Error handling
            state.error?.let { error ->
                showSnackbar(error, isError = true)
                viewModel.clearError()
            }
        }
    }

    private fun updateCacheStatistics(stats: CacheStatistics) {
        binding.apply {
            // Total cache size
            textTotalCacheSize.text = viewModel.formatBytes(stats.totalSizeBytes)
            
            // Database size (approximate based on document files)
            textDatabaseSize.text = viewModel.formatBytes(stats.totalSizeBytes / 4) // Rough estimate
            
            // Media cache size (images + audio files approximation)
            val mediaSize = (stats.imageFiles + stats.audioFiles) * 1024 * 100L // Rough estimate
            textMediaCacheSize.text = viewModel.formatBytes(mediaSize)
            
            // Total files as pending changes indicator
            textPendingChanges.text = "${stats.totalFiles} cached files"
            
            // Available bytes as offline data
            textOfflineDataSize.text = viewModel.formatBytes(stats.totalSizeBytes)
            
            // Cache usage percentage
            val usagePercentage = stats.usagePercentage
            
            progressBarCacheUsage.progress = usagePercentage.toInt()
            textCacheUsage.text = "${usagePercentage.toInt()}% used"
            
            // Last sync time - hide for now since it's not in CacheStatistics
            textLastSyncTime.visibility = View.GONE
        }
    }

    private fun updateOfflineStatus(status: OfflineStatus) {
        binding.apply {
            textOfflineStatus.text = if (status.isOnline) {
                "Online"
            } else {
                "Offline"
            }
            
            // Update status color
            val colorRes = if (status.isOnline) {
                android.R.color.holo_green_dark
            } else {
                android.R.color.holo_orange_dark
            }
            
            textOfflineStatus.setTextColor(resources.getColor(colorRes, null))
            
            // Update pending changes info
            if (status.hasUnsyncedChanges) {
                textPendingChanges.text = "${status.pendingChanges + status.pendingFileOperations} pending changes"
            }
        }
    }

    private fun updateSyncProgress(progress: SyncProgress) {
        binding.apply {
            if (progress.isActive) {
                progressBarSyncDetails.visibility = View.VISIBLE
                textSyncProgress.visibility = View.VISIBLE
                
                val progressInt = (progress.progress * 100).toInt()
                progressBarSyncDetails.max = 100
                progressBarSyncDetails.progress = progressInt
                
                textSyncProgress.text = "${progressInt}% complete"
                
                progress.currentOperation?.let { operation ->
                    textCurrentSyncItem.text = "Current: $operation"
                    textCurrentSyncItem.visibility = View.VISIBLE
                } ?: run {
                    textCurrentSyncItem.visibility = View.GONE
                }
            } else {
                progressBarSyncDetails.visibility = View.GONE
                textSyncProgress.visibility = View.GONE
                textCurrentSyncItem.visibility = View.GONE
            }
        }
    }

    private fun showConfirmationDialog(title: String, message: String, action: () -> Unit) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Continue") { _, _ -> action() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSnackbar(message: String, isError: Boolean = false) {
        val snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
        if (isError) {
            snackbar.setBackgroundTint(resources.getColor(android.R.color.holo_red_dark, null))
        }
        snackbar.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}