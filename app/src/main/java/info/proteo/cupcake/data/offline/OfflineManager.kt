package info.proteo.cupcake.data.offline

import android.util.Log
import info.proteo.cupcake.data.repository.CacheRepository
import info.proteo.cupcake.data.cache.FileCacheManager
import info.proteo.cupcake.shared.data.model.annotation.Annotation
import info.proteo.cupcake.shared.data.model.shareddocuments.SharedDocument
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central manager for all offline functionality including caching, editing, and synchronization
 */
@Singleton
class OfflineManager @Inject constructor(
    private val offlineAnnotationHandler: OfflineAnnotationHandler,
    private val syncService: SyncService,
    private val cacheRepository: CacheRepository,
    private val fileCacheManager: FileCacheManager,
    private val networkConnectivityMonitor: NetworkConnectivityMonitor,
    private val pendingChangesDao: info.proteo.cupcake.data.local.dao.offline.PendingChangesDao,
    private val pendingFileOperationsDao: info.proteo.cupcake.data.local.dao.offline.PendingFileOperationsDao
) {
    
    companion object {
        private const val TAG = "OfflineManager"
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _offlineStatus = MutableStateFlow(OfflineStatus())
    val offlineStatus: StateFlow<OfflineStatus> = _offlineStatus.asStateFlow()
    
    private val _syncProgress = MutableStateFlow(SyncProgress())
    val syncProgress: StateFlow<SyncProgress> = _syncProgress.asStateFlow()
    
    init {
        // Start monitoring and auto-sync
        startMonitoring()
    }
    
    /**
     * Start offline monitoring and automatic sync
     */
    fun startMonitoring() {
        scope.launch {
            // Monitor network status and pending changes
            combine(
                networkConnectivityMonitor.networkStatusFlow(),
                pendingChangesDao.getPendingCount(),
                pendingFileOperationsDao.getPendingCount()
            ) { networkStatus, pendingChanges, pendingFiles ->
                OfflineStatus(
                    isOnline = networkStatus.isConnected,
                    connectionType = networkStatus.connectionType,
                    isMetered = networkStatus.isMetered,
                    pendingChanges = pendingChanges,
                    pendingFileOperations = pendingFiles,
                    hasUnsyncedChanges = pendingChanges > 0 || pendingFiles > 0
                )
            }.distinctUntilChanged().collect { status ->
                _offlineStatus.value = status
                
                // Auto-start sync when network becomes available
                if (status.isOnline && status.hasUnsyncedChanges) {
                    Log.d(TAG, "Network available with ${status.pendingChanges} pending changes, starting auto-sync")
                    performSmartSync()
                }
            }
        }
        
        // Start background sync service
        syncService.startAutoSync()
        Log.d(TAG, "Offline monitoring started")
    }
    
    /**
     * Stop offline monitoring
     */
    fun stopMonitoring() {
        syncService.stopAutoSync()
        Log.d(TAG, "Offline monitoring stopped")
    }
    
    // Annotation Operations
    
    /**
     * Edit annotation (works offline)
     */
    suspend fun editAnnotation(
        annotationId: Int,
        changes: AnnotationChanges
    ): Result<Annotation> {
        return if (networkConnectivityMonitor.isConnected()) {
            // Try online edit first
            try {
                // This would call the actual service
                // For now, fall back to offline
                offlineAnnotationHandler.editAnnotationOffline(annotationId, changes)
            } catch (e: Exception) {
                Log.w(TAG, "Online edit failed, using offline", e)
                offlineAnnotationHandler.editAnnotationOffline(annotationId, changes)
            }
        } else {
            // Offline edit
            offlineAnnotationHandler.editAnnotationOffline(annotationId, changes)
        }
    }
    
    /**
     * Create annotation (works offline)
     */
    suspend fun createAnnotation(annotation: Annotation): Result<Annotation> {
        return if (networkConnectivityMonitor.isConnected()) {
            try {
                // Try online create first
                // For now, fall back to offline
                offlineAnnotationHandler.createAnnotationOffline(annotation)
            } catch (e: Exception) {
                Log.w(TAG, "Online create failed, using offline", e)
                offlineAnnotationHandler.createAnnotationOffline(annotation)
            }
        } else {
            offlineAnnotationHandler.createAnnotationOffline(annotation)
        }
    }
    
    /**
     * Delete annotation (works offline)
     */
    suspend fun deleteAnnotation(annotationId: Int): Result<Unit> {
        return if (networkConnectivityMonitor.isConnected()) {
            try {
                // Try online delete first
                // For now, fall back to offline
                offlineAnnotationHandler.deleteAnnotationOffline(annotationId)
            } catch (e: Exception) {
                Log.w(TAG, "Online delete failed, using offline", e)
                offlineAnnotationHandler.deleteAnnotationOffline(annotationId)
            }
        } else {
            offlineAnnotationHandler.deleteAnnotationOffline(annotationId)
        }
    }
    
    /**
     * Get annotations with offline changes
     */
    fun getAnnotations(): Flow<List<Annotation>> {
        return offlineAnnotationHandler.getAnnotationsWithOfflineChanges()
    }
    
    /**
     * Get annotation by ID with offline changes
     */
    suspend fun getAnnotation(annotationId: Int): Annotation? {
        return offlineAnnotationHandler.getAnnotationWithOfflineChanges(annotationId)
    }
    
    // Caching Operations
    
    /**
     * Cache annotation for offline access
     */
    suspend fun cacheAnnotation(annotation: Annotation): Result<Any> {
        return cacheRepository.smartCacheAnnotation(annotation)
    }
    
    /**
     * Cache multiple annotations
     */
    suspend fun cacheAnnotations(annotations: List<Annotation>): Result<List<Any>> {
        return try {
            val results = mutableListOf<Any>()
            var errors = 0
            
            annotations.forEach { annotation ->
                cacheRepository.smartCacheAnnotation(annotation).fold(
                    onSuccess = { result -> results.add(result) },
                    onFailure = { errors++ }
                )
            }
            
            if (errors == 0) {
                Result.success(results)
            } else {
                Result.failure(Exception("Failed to cache $errors/${annotations.size} annotations"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Cache shared document
     */
    suspend fun cacheSharedDocument(document: SharedDocument): Result<Any> {
        return cacheRepository.cacheSharedDocument(document)
    }
    
    /**
     * Preload content for offline use
     */
    suspend fun preloadContent(
        annotations: List<Annotation>,
        documents: List<SharedDocument> = emptyList()
    ): Result<Any> {
        return cacheRepository.preloadFrequentContent(annotations, documents)
    }
    
    // Sync Operations
    
    /**
     * Perform smart sync based on network conditions
     */
    suspend fun performSmartSync(): Result<SyncResult> {
        updateSyncProgress(SyncProgress(isActive = true, progress = 0f))
        
        return try {
            val result = syncService.performSmartSync()
            
            updateSyncProgress(SyncProgress(
                isActive = false,
                progress = 1f,
                lastSyncTime = System.currentTimeMillis(),
                lastResult = result.getOrNull()
            ))
            
            result
        } catch (e: Exception) {
            updateSyncProgress(SyncProgress(
                isActive = false,
                progress = 0f,
                error = e.message
            ))
            Result.failure(e)
        }
    }
    
    /**
     * Force full sync regardless of network conditions
     */
    suspend fun forceFullSync(): Result<SyncResult> {
        updateSyncProgress(SyncProgress(isActive = true, progress = 0f))
        
        return try {
            val result = syncService.performFullSync()
            
            updateSyncProgress(SyncProgress(
                isActive = false,
                progress = 1f,
                lastSyncTime = System.currentTimeMillis(),
                lastResult = result.getOrNull()
            ))
            
            result
        } catch (e: Exception) {
            updateSyncProgress(SyncProgress(
                isActive = false,
                progress = 0f,
                error = e.message
            ))
            Result.failure(e)
        }
    }
    
    /**
     * Retry failed synchronizations
     */
    suspend fun retryFailedSync(): Result<SyncResult> {
        return syncService.retryFailedSync()
    }
    
    // Status and Management
    
    /**
     * Check if annotation is cached
     */
    fun isAnnotationCached(annotation: Annotation): Boolean {
        return cacheRepository.isAnnotationCached(annotation)
    }
    
    /**
     * Check if annotation has pending changes
     */
    suspend fun hasPendingChanges(annotationId: Int): Boolean {
        return offlineAnnotationHandler.hasPendingChanges(annotationId)
    }
    
    /**
     * Get comprehensive cache statistics
     */
    fun getCacheStatistics(): Any {
        return cacheRepository.getCacheStatistics()
    }
    
    /**
     * Get current sync status
     */
    suspend fun getCurrentSyncStatus(): SyncStatus {
        return syncService.getSyncStatus()
    }
    
    /**
     * Clear all cache
     */
    suspend fun clearAllCache(): Result<Boolean> {
        return cacheRepository.clearAllCache()
    }
    
    /**
     * Clean old cache data
     */
    suspend fun cleanCache(): Result<Long> {
        return cacheRepository.cleanCache()
    }
    
    /**
     * Get offline capabilities status
     */
    fun getOfflineCapabilities(): OfflineCapabilities {
        val currentStatus = _offlineStatus.value
        
        return OfflineCapabilities(
            canEditOffline = true,
            canCreateOffline = true,
            canDeleteOffline = true,
            canCacheFiles = true,
            canSyncWhenOnline = currentStatus.isOnline,
            hasUnsyncedChanges = currentStatus.hasUnsyncedChanges,
            cacheStats = cacheRepository.getCacheStatistics()
        )
    }
    
    private fun updateSyncProgress(progress: SyncProgress) {
        _syncProgress.value = progress
    }
}

/**
 * Current offline status
 */
data class OfflineStatus(
    val isOnline: Boolean = false,
    val connectionType: ConnectionType = ConnectionType.NONE,
    val isMetered: Boolean = false,
    val pendingChanges: Int = 0,
    val pendingFileOperations: Int = 0,
    val hasUnsyncedChanges: Boolean = false
)

/**
 * Sync progress information
 */
data class SyncProgress(
    val isActive: Boolean = false,
    val progress: Float = 0f,
    val currentOperation: String? = null,
    val lastSyncTime: Long? = null,
    val lastResult: SyncResult? = null,
    val error: String? = null
)

/**
 * Offline capabilities status
 */
data class OfflineCapabilities(
    val canEditOffline: Boolean,
    val canCreateOffline: Boolean,
    val canDeleteOffline: Boolean,
    val canCacheFiles: Boolean,
    val canSyncWhenOnline: Boolean,
    val hasUnsyncedChanges: Boolean,
    val cacheStats: Any
)