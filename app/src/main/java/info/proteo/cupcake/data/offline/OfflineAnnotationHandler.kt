package info.proteo.cupcake.data.offline

import android.util.Log
import info.proteo.cupcake.data.cache.CachedAnnotationService
import info.proteo.cupcake.data.cache.FileCacheManager
import info.proteo.cupcake.data.local.dao.annotation.AnnotationDao
import info.proteo.cupcake.data.local.entity.annotation.AnnotationEntity
import info.proteo.cupcake.shared.data.model.annotation.Annotation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles offline annotation editing and synchronization
 * Manages local changes and syncs them when network is available
 */
@Singleton
class OfflineAnnotationHandler @Inject constructor(
    private val annotationDao: AnnotationDao,
    private val cachedAnnotationService: CachedAnnotationService,
    private val fileCacheManager: FileCacheManager,
    private val pendingChangesDao: info.proteo.cupcake.data.local.dao.offline.PendingChangesDao,
    private val networkConnectivityMonitor: NetworkConnectivityMonitor
) {
    
    companion object {
        private const val TAG = "OfflineAnnotationHandler"
    }
    
    /**
     * Edit annotation offline - stores changes locally for later sync
     */
    suspend fun editAnnotationOffline(
        annotationId: Int,
        changes: AnnotationChanges
    ): Result<Annotation> = withContext(Dispatchers.IO) {
        try {
            // Get current annotation from local cache
            val currentEntity = annotationDao.getById(annotationId)
                ?: return@withContext Result.failure(Exception("Annotation not found locally"))
            
            // Apply changes to local entity
            val updatedEntity = applyChangesToEntity(currentEntity, changes)
            
            // Save updated annotation locally
            annotationDao.update(updatedEntity)
            
            // Create pending change record for sync
            val pendingChange = PendingChange(
                annotationId = annotationId,
                changeType = ChangeType.UPDATE,
                changes = changes,
                timestamp = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING
            )
            pendingChangesDao.insert(pendingChange)
            
            // Convert to domain object
            val annotation = convertEntityToDomain(updatedEntity)
            
            Log.d(TAG, "Annotation $annotationId edited offline")
            Result.success(annotation)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error editing annotation offline: $annotationId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Create annotation offline
     */
    suspend fun createAnnotationOffline(
        annotation: Annotation
    ): Result<Annotation> = withContext(Dispatchers.IO) {
        try {
            // Generate temporary ID for offline creation
            val tempId = generateTempId()
            val annotationWithTempId = annotation.copy(id = tempId)
            
            // Save annotation locally
            val entity = convertDomainToEntity(annotationWithTempId)
            annotationDao.insert(entity)
            
            // Create pending change record
            val pendingChange = PendingChange(
                annotationId = tempId,
                changeType = ChangeType.CREATE,
                changes = AnnotationChanges.fromAnnotation(annotationWithTempId),
                timestamp = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING
            )
            pendingChangesDao.insert(pendingChange)
            
            Log.d(TAG, "Annotation created offline with temp ID: $tempId")
            Result.success(annotationWithTempId)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating annotation offline", e)
            Result.failure(e)
        }
    }
    
    /**
     * Delete annotation offline
     */
    suspend fun deleteAnnotationOffline(annotationId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Mark annotation as deleted locally (soft delete)
            val entity = annotationDao.getById(annotationId)
                ?: return@withContext Result.failure(Exception("Annotation not found"))
            
            val deletedEntity = entity.copy(isDeleted = true)
            annotationDao.update(deletedEntity)
            
            // Create pending change record
            val pendingChange = PendingChange(
                annotationId = annotationId,
                changeType = ChangeType.DELETE,
                changes = AnnotationChanges(),
                timestamp = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING
            )
            pendingChangesDao.insert(pendingChange)
            
            Log.d(TAG, "Annotation $annotationId marked for deletion offline")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting annotation offline: $annotationId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get all annotations including offline changes
     */
    fun getAnnotationsWithOfflineChanges(): Flow<List<Annotation>> {
        return annotationDao.getAllAnnotations()
            .map { entities ->
                entities
                    .filter { !it.isDeleted }
                    .map { convertEntityToDomain(it) }
            }
    }
    
    /**
     * Get annotation by ID with offline changes
     */
    suspend fun getAnnotationWithOfflineChanges(annotationId: Int): Annotation? {
        return annotationDao.getById(annotationId)?.let { entity ->
            if (!entity.isDeleted) {
                convertEntityToDomain(entity)
            } else null
        }
    }
    
    /**
     * Sync all pending changes when network is available
     */
    suspend fun syncPendingChanges(): Result<SyncResult> = withContext(Dispatchers.IO) {
        if (!networkConnectivityMonitor.isConnected()) {
            return@withContext Result.failure(Exception("No network connection"))
        }
        
        try {
            val pendingChanges = pendingChangesDao.getAllPending().firstOrNull() ?: emptyList()
            var successCount = 0
            var failureCount = 0
            val errors = mutableListOf<Exception>()
            
            pendingChanges.forEach { pendingChange ->
                try {
                    when (pendingChange.changeType) {
                        ChangeType.CREATE -> {
                            syncCreateChange(pendingChange)
                            successCount++
                        }
                        ChangeType.UPDATE -> {
                            syncUpdateChange(pendingChange)
                            successCount++
                        }
                        ChangeType.DELETE -> {
                            syncDeleteChange(pendingChange)
                            successCount++
                        }
                    }
                    
                    // Mark as synced
                    pendingChangesDao.update(pendingChange.copy(syncStatus = SyncStatus.SYNCED))
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing change for annotation ${pendingChange.annotationId}", e)
                    errors.add(e)
                    failureCount++
                    
                    // Mark as failed
                    pendingChangesDao.update(pendingChange.copy(syncStatus = SyncStatus.FAILED))
                }
            }
            
            val result = SyncResult(
                totalChanges = pendingChanges.size,
                successCount = successCount,
                failureCount = failureCount,
                errors = errors
            )
            
            Log.d(TAG, "Sync completed: $successCount successful, $failureCount failed")
            Result.success(result)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during sync operation", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get pending changes count
     */
    suspend fun getPendingChangesCount(): Int {
        return pendingChangesDao.getPendingCount().firstOrNull() ?: 0
    }
    
    /**
     * Check if annotation has pending changes
     */
    suspend fun hasPendingChanges(annotationId: Int): Boolean {
        return pendingChangesDao.hasPendingChanges(annotationId).firstOrNull() ?: false
    }
    
    /**
     * Clear all synced changes
     */
    suspend fun clearSyncedChanges(): Int {
        return pendingChangesDao.deleteSynced()
    }
    
    private suspend fun syncCreateChange(pendingChange: PendingChange) {
        val annotation = annotationDao.getById(pendingChange.annotationId)
            ?: throw Exception("Local annotation not found")
        
        // Create annotation remotely
        val createResult = cachedAnnotationService.createAnnotation(
            mapOf(), // partMap would need to be constructed from changes
            null // file part
        )
        
        if (createResult.isSuccess) {
            val remoteAnnotation = createResult.getOrThrow()
            
            // Update local annotation with real ID
            val updatedEntity = annotation.copy(
                id = remoteAnnotation.id,
                isTempId = false
            )
            annotationDao.update(updatedEntity)
        } else {
            throw createResult.exceptionOrNull() ?: Exception("Create failed")
        }
    }
    
    private suspend fun syncUpdateChange(pendingChange: PendingChange) {
        // Update annotation remotely
        val updateResult = cachedAnnotationService.updateAnnotation(
            pendingChange.annotationId,
            mapOf(), // partMap would need to be constructed from changes
            null // file part
        )
        
        if (updateResult.isFailure) {
            throw updateResult.exceptionOrNull() ?: Exception("Update failed")
        }
    }
    
    private suspend fun syncDeleteChange(pendingChange: PendingChange) {
        // Delete annotation remotely
        val deleteResult = cachedAnnotationService.deleteAnnotation(pendingChange.annotationId)
        
        if (deleteResult.isSuccess) {
            // Remove from local database
            annotationDao.getById(pendingChange.annotationId)?.let { entity ->
                annotationDao.delete(entity)
            }
        } else {
            throw deleteResult.exceptionOrNull() ?: Exception("Delete failed")
        }
    }
    
    private fun applyChangesToEntity(
        entity: AnnotationEntity,
        changes: AnnotationChanges
    ): AnnotationEntity {
        return entity.copy(
            annotation = changes.annotation ?: entity.annotation,
            annotationName = changes.annotationName ?: entity.annotationName,
            summary = changes.summary ?: entity.summary,
            transcription = changes.transcription ?: entity.transcription,
            translation = changes.translation ?: entity.translation,
            fixed = changes.fixed ?: entity.fixed,
            scratched = changes.scratched ?: entity.scratched,
            updatedAt = System.currentTimeMillis().toString()
        )
    }
    
    private fun convertEntityToDomain(entity: AnnotationEntity): Annotation {
        // This should use the existing conversion logic from AnnotationService
        return Annotation(
            id = entity.id,
            step = entity.step,
            session = entity.session,
            annotation = entity.annotation,
            file = entity.file,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            annotationType = entity.annotationType,
            transcribed = entity.transcribed,
            transcription = entity.transcription,
            language = entity.language,
            translation = entity.translation,
            scratched = entity.scratched,
            annotationName = entity.annotationName,
            folder = emptyList(), // Would need to load folder relationships
            summary = entity.summary,
            instrumentUsage = null,
            metadataColumns = null,
            fixed = entity.fixed,
            user = null, // Would need to load user relationship
            storedReagent = entity.storedReagent
        )
    }
    
    private fun convertDomainToEntity(annotation: Annotation): AnnotationEntity {
        return AnnotationEntity(
            id = annotation.id,
            step = annotation.step,
            session = annotation.session,
            annotation = annotation.annotation,
            file = annotation.file,
            createdAt = annotation.createdAt,
            updatedAt = annotation.updatedAt,
            annotationType = annotation.annotationType,
            transcribed = annotation.transcribed,
            transcription = annotation.transcription,
            language = annotation.language,
            translation = annotation.translation,
            scratched = annotation.scratched,
            annotationName = annotation.annotationName,
            summary = annotation.summary,
            fixed = annotation.fixed,
            userId = annotation.user?.id,
            storedReagent = annotation.storedReagent,
            folderId = annotation.folder.firstOrNull()?.id,
            isDeleted = false,
            isTempId = annotation.id < 0
        )
    }
    
    private fun generateTempId(): Int {
        return -(System.currentTimeMillis().toInt())
    }
}