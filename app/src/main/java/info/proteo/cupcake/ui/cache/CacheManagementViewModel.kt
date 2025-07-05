package info.proteo.cupcake.ui.cache

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.proteo.cupcake.data.repository.CacheStatistics
import info.proteo.cupcake.data.offline.OfflineManager
import info.proteo.cupcake.data.offline.OfflineStatus
import info.proteo.cupcake.data.offline.SyncProgress
import info.proteo.cupcake.data.offline.SyncResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for cache management UI
 */
@HiltViewModel
class CacheManagementViewModel @Inject constructor(
    private val offlineManager: OfflineManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CacheManagementUiState())
    val uiState: StateFlow<CacheManagementUiState> = _uiState.asStateFlow()
    
    val offlineStatus: StateFlow<OfflineStatus> = offlineManager.offlineStatus
    val syncProgress: StateFlow<SyncProgress> = offlineManager.syncProgress
    
    init {
        loadCacheStatistics()
    }
    
    /**
     * Load current cache statistics
     */
    fun loadCacheStatistics() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val stats = offlineManager.getCacheStatistics() as CacheStatistics
                val capabilities = offlineManager.getOfflineCapabilities()
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    cacheStats = stats,
                    offlineCapabilities = capabilities,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load cache statistics"
                )
            }
        }
    }
    
    /**
     * Perform smart sync
     */
    fun performSmartSync() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(syncInProgress = true)
                
                val result = offlineManager.performSmartSync()
                
                _uiState.value = _uiState.value.copy(
                    syncInProgress = false,
                    lastSyncResult = result.getOrNull(),
                    error = result.exceptionOrNull()?.message
                )
                
                // Reload stats after sync
                loadCacheStatistics()
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    syncInProgress = false,
                    error = e.message ?: "Sync failed"
                )
            }
        }
    }
    
    /**
     * Force full sync
     */
    fun forceFullSync() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(syncInProgress = true)
                
                val result = offlineManager.forceFullSync()
                
                _uiState.value = _uiState.value.copy(
                    syncInProgress = false,
                    lastSyncResult = result.getOrNull(),
                    error = result.exceptionOrNull()?.message
                )
                
                loadCacheStatistics()
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    syncInProgress = false,
                    error = e.message ?: "Full sync failed"
                )
            }
        }
    }
    
    /**
     * Retry failed synchronizations
     */
    fun retryFailedSync() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(syncInProgress = true)
                
                val result = offlineManager.retryFailedSync()
                
                _uiState.value = _uiState.value.copy(
                    syncInProgress = false,
                    lastSyncResult = result.getOrNull(),
                    error = result.exceptionOrNull()?.message
                )
                
                loadCacheStatistics()
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    syncInProgress = false,
                    error = e.message ?: "Retry sync failed"
                )
            }
        }
    }
    
    /**
     * Clean cache
     */
    fun cleanCache() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val result = offlineManager.cleanCache()
                val freedBytes = result.getOrNull() ?: 0L
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    lastCleanupBytes = freedBytes,
                    error = result.exceptionOrNull()?.message
                )
                
                loadCacheStatistics()
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Cache cleanup failed"
                )
            }
        }
    }
    
    /**
     * Clear all cache
     */
    fun clearAllCache() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val result = offlineManager.clearAllCache()
                val success = result.getOrNull() ?: false
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = if (success) null else "Failed to clear cache"
                )
                
                loadCacheStatistics()
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Clear cache failed"
                )
            }
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * Format bytes for display
     */
    fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
            bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }
    
    /**
     * Get cache usage color based on percentage
     */
    fun getCacheUsageColor(percentage: Float): androidx.compose.ui.graphics.Color {
        return when {
            percentage >= 90f -> androidx.compose.ui.graphics.Color.Red
            percentage >= 75f -> androidx.compose.ui.graphics.Color(0xFFFF9800) // Orange
            percentage >= 50f -> androidx.compose.ui.graphics.Color(0xFFFFC107) // Amber
            else -> androidx.compose.ui.graphics.Color.Green
        }
    }
    
    /**
     * Get sync status display text
     */
    fun getSyncStatusText(syncResult: SyncResult?): String {
        return syncResult?.let { result ->
            if (result.hasErrors) {
                "Last sync: ${result.successCount} successful, ${result.failureCount} failed"
            } else {
                "Last sync: ${result.successCount} items synchronized"
            }
        } ?: "No sync performed yet"
    }
}

/**
 * UI state for cache management screen
 */
data class CacheManagementUiState(
    val isLoading: Boolean = false,
    val cacheStats: CacheStatistics? = null,
    val offlineCapabilities: info.proteo.cupcake.data.offline.OfflineCapabilities? = null,
    val syncInProgress: Boolean = false,
    val lastSyncResult: SyncResult? = null,
    val lastCleanupBytes: Long? = null,
    val error: String? = null
)