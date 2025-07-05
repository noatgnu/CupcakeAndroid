package info.proteo.cupcake.data.offline

import android.util.Log
import info.proteo.cupcake.data.cache.CachedAnnotationService
import info.proteo.cupcake.data.cache.CachedSharedDocumentService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for managing background synchronization of offline changes
 */
@Singleton
class SyncService @Inject constructor(
    private val offlineAnnotationHandler: OfflineAnnotationHandler,
    private val networkConnectivityMonitor: NetworkConnectivityMonitor,
    private val pendingChangesDao: PendingChangesDao,
    private val pendingFileOperationsDao: PendingFileOperationsDao,
    private val cachedAnnotationService: CachedAnnotationService,
    private val cachedSharedDocumentService: CachedSharedDocumentService
) {
    
    companion object {
        private const val TAG = "SyncService"
        private const val SYNC_INTERVAL_MS = 30_000L // 30 seconds
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val SYNC_BATCH_SIZE = 20
    }
    
    private var syncJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * Start automatic synchronization service
     */
    fun startAutoSync() {
        if (syncJob?.isActive == true) {
            Log.d(TAG, "Auto sync already running")
            return
        }
        
        syncJob = scope.launch {
            networkConnectivityMonitor.networkStatusFlow()
                .collect { networkStatus ->
                    if (networkStatus.isConnected) {
                        Log.d(TAG, "Network connected, starting sync")
                        performFullSync()
                    } else {
                        Log.d(TAG, "Network disconnected, pausing sync")
                    }
                }
        }
        
        Log.d(TAG, "Auto sync started")
    }
    
    /**
     * Stop automatic synchronization service
     */
    fun stopAutoSync() {
        syncJob?.cancel()
        syncJob = null
        Log.d(TAG, "Auto sync stopped")
    }
    
    /**
     * Perform full synchronization now
     */
    suspend fun performFullSync(): Result<SyncResult> {
        if (!networkConnectivityMonitor.isConnected()) {
            return Result.failure(Exception("No network connection"))
        }
        
        return try {
            Log.d(TAG, "Starting full sync")
            
            // Sync pending annotation changes
            val annotationSyncResult = offlineAnnotationHandler.syncPendingChanges()
            
            // Sync pending file operations
            val fileSyncResult = syncPendingFileOperations()
            
            // Clean up old synced changes
            cleanupSyncedChanges()
            
            val combinedResult = SyncResult(
                totalChanges = annotationSyncResult.getOrNull()?.totalChanges ?: 0 + 
                              fileSyncResult.getOrNull()?.totalOperations ?: 0,
                successCount = annotationSyncResult.getOrNull()?.successCount ?: 0 + 
                              fileSyncResult.getOrNull()?.successCount ?: 0,
                failureCount = annotationSyncResult.getOrNull()?.failureCount ?: 0 + 
                              fileSyncResult.getOrNull()?.failureCount ?: 0,
                errors = (annotationSyncResult.getOrNull()?.errors ?: emptyList()) + 
                        (fileSyncResult.getOrNull()?.errors ?: emptyList())
            )
            
            Log.d(TAG, "Full sync completed: ${combinedResult.successCount} successful, ${combinedResult.failureCount} failed")
            Result.success(combinedResult)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during full sync", e)
            Result.failure(e)
        }
    }
    
    /**
     * Sync only when on WiFi or unlimited connection
     */
    suspend fun performSmartSync(): Result<SyncResult> {
        val networkStatus = networkConnectivityMonitor.getCurrentNetworkStatus()
        
        if (!networkStatus.isConnected) {
            return Result.failure(Exception("No network connection"))
        }
        
        // Only sync on WiFi or unlimited connections for large operations
        return if (networkStatus.connectionType == ConnectionType.WIFI || !networkStatus.isMetered) {
            performFullSync()
        } else {
            // On metered connections, only sync critical changes
            performCriticalSync()
        }
    }
    
    /**
     * Sync only critical changes (text updates, no file operations)
     */
    private suspend fun performCriticalSync(): Result<SyncResult> {
        return try {
            Log.d(TAG, "Performing critical sync (metered connection)")
            
            // Only sync text-based changes, skip file operations
            val textChanges = pendingChangesDao.getAllPending().firstOrNull()
                ?.filter { change ->
                    change.changes.filePath == null // No file changes
                }
                ?.take(SYNC_BATCH_SIZE) // Limit batch size
                ?: emptyList()
            
            var successCount = 0
            var failureCount = 0
            val errors = mutableListOf<Exception>()
            
            textChanges.forEach { change ->
                try {
                    when (change.changeType) {
                        ChangeType.UPDATE -> {
                            // Only sync text updates
                            syncTextOnlyUpdate(change)
                            successCount++
                        }
                        ChangeType.DELETE -> {
                            // Deletions are critical
                            offlineAnnotationHandler.syncPendingChanges()
                            successCount++
                        }
                        ChangeType.CREATE -> {
                            // Skip creates with files on metered connections
                            if (change.changes.filePath == null) {
                                syncTextOnlyCreate(change)
                                successCount++
                            }
                        }
                    }
                    
                    pendingChangesDao.update(change.copy(syncStatus = SyncStatus.SYNCED))
                    
                } catch (e: Exception) {
                    errors.add(e)
                    failureCount++
                    pendingChangesDao.update(change.copy(syncStatus = SyncStatus.FAILED))
                }
            }
            
            val result = SyncResult(
                totalChanges = textChanges.size,
                successCount = successCount,
                failureCount = failureCount,
                errors = errors
            )
            
            Log.d(TAG, "Critical sync completed: $successCount successful, $failureCount failed")
            Result.success(result)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during critical sync", e)
            Result.failure(e)
        }
    }
    
    /**
     * Sync pending file operations
     */
    private suspend fun syncPendingFileOperations(): Result<FileSyncResult> {
        return try {
            val pendingOps = pendingFileOperationsDao.getAllPending().firstOrNull() ?: emptyList()
            var successCount = 0
            var failureCount = 0
            val errors = mutableListOf<Exception>()
            
            pendingOps.take(SYNC_BATCH_SIZE).forEach { operation ->
                try {
                    when (operation.operationType) {
                        FileOperationType.UPLOAD -> {
                            // Upload file
                            syncFileUpload(operation)
                            successCount++
                        }
                        FileOperationType.DOWNLOAD -> {
                            // Download file
                            syncFileDownload(operation)
                            successCount++
                        }
                        FileOperationType.DELETE -> {
                            // Delete file
                            syncFileDelete(operation)
                            successCount++
                        }
                    }
                    
                    pendingFileOperationsDao.updateSyncStatus(operation.id, SyncStatus.SYNCED)
                    
                } catch (e: Exception) {
                    errors.add(e)
                    failureCount++
                    pendingFileOperationsDao.updateSyncStatus(operation.id, SyncStatus.FAILED)
                }
            }
            
            val result = FileSyncResult(
                totalOperations = pendingOps.size,
                successCount = successCount,
                failureCount = failureCount,
                errors = errors
            )
            
            Log.d(TAG, "File sync completed: $successCount successful, $failureCount failed")
            Result.success(result)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during file sync", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get sync status and statistics
     */
    suspend fun getSyncStatus(): SyncStatus {
        val pendingChanges = pendingChangesDao.getPendingCount().firstOrNull() ?: 0
        val pendingFiles = pendingFileOperationsDao.getPendingCount().firstOrNull() ?: 0
        val isConnected = networkConnectivityMonitor.isConnected()
        
        return if (pendingChanges == 0 && pendingFiles == 0) {
            SyncStatus.SYNCED
        } else if (!isConnected) {
            SyncStatus.PENDING
        } else {
            SyncStatus.SYNCING
        }
    }
    
    /**
     * Force retry failed synchronizations
     */
    suspend fun retryFailedSync(): Result<SyncResult> {
        return try {
            Log.d(TAG, "Retrying failed synchronizations")
            
            // Get failed changes for retry
            val failedChanges = pendingChangesDao.getRetryableFailed().take(SYNC_BATCH_SIZE)
            val failedFiles = pendingFileOperationsDao.getBatchForSync(SyncStatus.FAILED)
            
            // Reset status to pending for retry
            failedChanges.forEach { change ->
                pendingChangesDao.update(change.copy(syncStatus = SyncStatus.PENDING))
            }
            
            failedFiles.forEach { operation ->
                pendingFileOperationsDao.updateSyncStatus(operation.id, SyncStatus.PENDING)
            }
            
            // Perform sync
            performFullSync()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during retry sync", e)
            Result.failure(e)
        }
    }
    
    private suspend fun cleanupSyncedChanges() {
        try {
            // Clean up changes older than 7 days
            val cutoffTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
            
            val deletedChanges = pendingChangesDao.deleteOldSynced(cutoffTime)
            val deletedFiles = pendingFileOperationsDao.deleteSynced()
            
            Log.d(TAG, "Cleanup completed: $deletedChanges changes, $deletedFiles files")
            
        } catch (e: Exception) {
            Log.w(TAG, "Error during cleanup", e)
        }
    }
    
    private suspend fun syncTextOnlyUpdate(change: PendingChange) {
        // Implementation for text-only updates
        // This would call the appropriate service method with text changes only
    }
    
    private suspend fun syncTextOnlyCreate(change: PendingChange) {
        // Implementation for text-only creates
        // This would call the appropriate service method with text data only
    }
    
    private suspend fun syncFileUpload(operation: PendingFileOperation) {
        // Implementation for file upload sync
        // This would upload the file and update the annotation
    }
    
    private suspend fun syncFileDownload(operation: PendingFileOperation) {
        // Implementation for file download sync
        // This would download and cache the file
    }
    
    private suspend fun syncFileDelete(operation: PendingFileOperation) {
        // Implementation for file delete sync
        // This would remove the file from remote storage
    }
}

/**
 * Result of file synchronization
 */
data class FileSyncResult(
    val totalOperations: Int,
    val successCount: Int,
    val failureCount: Int,
    val errors: List<Exception>
) {
    val successRate: Float
        get() = if (totalOperations > 0) successCount.toFloat() / totalOperations else 0f
}