package info.proteo.cupcake.data.local.dao.offline

import androidx.room.*
import info.proteo.cupcake.data.offline.OfflineAnnotationMetadata
import info.proteo.cupcake.data.offline.ConflictResolution
import kotlinx.coroutines.flow.Flow

/**
 * DAO for managing offline annotation metadata
 */
@Dao
interface OfflineAnnotationDao {
    
    @Query("SELECT * FROM offline_annotations WHERE annotationId = :annotationId")
    suspend fun getMetadata(annotationId: Int): OfflineAnnotationMetadata?
    
    @Query("SELECT * FROM offline_annotations WHERE hasUnsyncedChanges = 1")
    fun getUnsyncedAnnotations(): Flow<List<OfflineAnnotationMetadata>>
    
    @Query("SELECT * FROM offline_annotations WHERE isOfflineCreated = 1")
    fun getOfflineCreatedAnnotations(): Flow<List<OfflineAnnotationMetadata>>
    
    @Query("SELECT * FROM offline_annotations WHERE conflictResolution IS NOT NULL")
    fun getConflictingAnnotations(): Flow<List<OfflineAnnotationMetadata>>
    
    @Query("SELECT COUNT(*) FROM offline_annotations WHERE hasUnsyncedChanges = 1")
    fun getUnsyncedCount(): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM offline_annotations WHERE isOfflineCreated = 1")
    fun getOfflineCreatedCount(): Flow<Int>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(metadata: OfflineAnnotationMetadata)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(metadataList: List<OfflineAnnotationMetadata>)
    
    @Update
    suspend fun update(metadata: OfflineAnnotationMetadata)
    
    @Delete
    suspend fun delete(metadata: OfflineAnnotationMetadata)
    
    @Query("DELETE FROM offline_annotations WHERE annotationId = :annotationId")
    suspend fun deleteByAnnotationId(annotationId: Int)
    
    @Query("UPDATE offline_annotations SET hasUnsyncedChanges = :hasChanges, lastModified = :timestamp WHERE annotationId = :annotationId")
    suspend fun updateSyncStatus(annotationId: Int, hasChanges: Boolean, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE offline_annotations SET conflictResolution = :resolution WHERE annotationId = :annotationId")
    suspend fun setConflictResolution(annotationId: Int, resolution: ConflictResolution)
    
    @Query("DELETE FROM offline_annotations WHERE hasUnsyncedChanges = 0 AND isOfflineCreated = 0 AND lastModified < :cutoffTime")
    suspend fun cleanupOldSyncedMetadata(cutoffTime: Long): Int
}